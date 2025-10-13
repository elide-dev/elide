/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.tooling.js

import java.nio.file.Path
import jakarta.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.io.path.exists
import elide.exec.Tracing
import elide.runtime.Logging
import elide.tooling.Arguments
import elide.tooling.Tool
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurator.*
import elide.tooling.deps.DependencyResolver
import elide.tooling.js.resolver.NpmResolver
import elide.tooling.project.codecs.NodeManifestCodec
import elide.tooling.project.manifest.NodePackageManifest

/**
 * ## JavaScript Build Configurator
 */
internal class JsBuildConfigurator : BuildConfigurator {
  private companion object {
    @JvmStatic private val logging by lazy { Logging.of(JsBuildConfigurator::class) }
  }

  private val npmManifestCodec by lazy { NodeManifestCodec() }

  // Invoke the Orogene built-in tool with the given arguments.
  private suspend fun orogene(scope: CoroutineScope, args: Arguments): Deferred<Tool.Result> = withContext(IO) {
    scope.async {
      Tracing.ensureLoaded()
      when (val exitCode = dev.elide.cli.bridge.CliNativeBridge.runOrogene(
        arrayOf("orogene", "apply").plus(args.asArgumentList())
      )) {
        0 -> Tool.Result.Success
        else -> Tool.Result.UnspecifiedFailure.also {
          logging.error { "Orogene invocation failed with exit code $exitCode" }
        }
      }
    }
  }

  // Render a NPM package manifest to JSON and write it to the provided path.
  private suspend fun renderManifest(manifest: NodePackageManifest, path: Path): Unit = withContext(IO) {
    path.toFile().outputStream().use { stream ->
      npmManifestCodec.write(manifest, stream)
    }
  }

  override suspend fun contribute(state: ElideBuildState, config: BuildConfiguration) {
    val hasJsPackages = state.manifest.dependencies.npm.hasPackages()
    if (hasJsPackages) {
      logging.debug { "NPM dependencies detected; preparing NPM resolver" }

      // generate npm manifest from an elide manifest, or use the user's existing package.json.
      val manifestProvider: Provider<NodePackageManifest> = when (
        val existingManifest = state.layout.projectRoot.resolve("package.json").takeIf { it.exists() }
      ) {
        null -> Provider { npmManifestCodec.fromElidePackage(state.manifest) }
        else -> Provider { npmManifestCodec.parseAsFile(existingManifest, state.forManifest()) }
      }

      when (val existing = config.resolvers[DependencyResolver.NpmResolver::class]) {
        null -> NpmResolver({ state }, manifestProvider, ::orogene, ::renderManifest)
        else -> (existing as NpmResolver)
      }.also {
        config.resolvers[DependencyResolver.NpmResolver::class] = it
      }
    }
  }
}
