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
package elide.tooling.js.resolver

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import jakarta.inject.Provider
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import elide.annotations.Inject
import elide.tool.Argument
import elide.tool.Arguments
import elide.tool.Tool
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.project.manifest.NodePackageManifest

/**
 * ## NPM Resolver
 *
 * Resolver for dependencies from JavaScript's NPM ecosystem. This is implemented with native code through Orogene in
 * Rust. To facilitate resolution, the following steps take place:
 *
 * - Resolution of Elide project configuration takes place, and foreign manifests are loaded, as applicable. A final
 *   suite of dependencies is assembled ("sealed" in the resolver) for resolution.
 *
 * - At the time the resolver is sealed, Orogene arguments are calculated and made immutable; for dry runs, these flags
 *   can be emitted to the console or a file.
 *
 * - At the time the resolver is "resolved," final resolution and installation steps take place:
 *
 *   1) The NPM dependency and cache roots are created at `.dev/{cache,dependencies}/npm`.
 *   2) The `package.json` is symbolically linked, or rendered and written, at `.dev/dependencies/npm/package.json`.
 *   3) Orogene is invoked with arguments which resemble the sample below; as a by-product, the `package-lock.kdl` file
 *       is written to the dependency root, as well as the populated `node_modules` folder.
 *   4) The `node_modules` folder is linked into the Elide project root to facilitate NPM and ESM resolution.
 *
 * ### Arguments for Orogene
 *
 * Arguments are assembled to customize the Orogene dependency and cache roots; the following shows a sample invocation
 * of Orogene which roughly matches the behavior implemented by this resolver:
 *
 * ```
 * oro apply \
 *   --root $PWD/.dev/dependencies/npm \
 *   --cache $PWD/.dev/cache/npm \
 *   --json \
 *   --no-progress \
 *   --no-emoji \
 *   --no-first-time \
 *   --no-telemetry
 * ```
 *
 * Tracing emissions in JSON are consumed via stdout, and translated into build output statuses and progress indicators,
 * by the Elide CLI.
 *
 * ### Caller Delegation
 *
 * The NPM resolver expects two callables to be provided at construction time: the [orogeneInvoker] and the
 * [manifestRenderer].
 *
 * The [orogeneInvoker] is charged with invoking Orogene with a suite of string arguments. This function can suspend,
 * and it is expected to provide a [Deferred] [Tool.Result] indicating the outcome of the call.
 *
 * The [manifestRenderer] is charged with rendering a [NodePackageManifest] to JSON, and writing it to the provided
 * [Path]. This function can suspend. The package manifest in this case is always rendered from the Elide package
 * manifest, since existing `package.json` manifests are linked instead of written.
 *
 * ### Output Controller
 *
 * Callers should provide their own output hooks via the [orogeneInvoker]; Orogene will yield messages to the stdout
 * stream based on the arguments provided here. Those should be parsed and emitted, which is outside the scope of this
 * resolver's duties.
 */
public class NpmResolver @Inject constructor (
  private val buildInfoProvider: Provider<ElideBuildState>,
  private val manifestProvider: Provider<NodePackageManifest>,
  private val orogeneInvoker: suspend (CoroutineScope, Arguments) -> Deferred<Tool.Result>,
  private val manifestRenderer: suspend (NodePackageManifest, Path) -> Unit,
) : DependencyResolver.NpmResolver {
  // Resolved project info.
  private lateinit var project: ElideBuildState

  // Resolved package root path.
  private lateinit var packageRoot: Path

  // Resolved package cache path.
  private lateinit var packageCache: Path

  // Resolved manifest info.
  private lateinit var npmManifest: NodePackageManifest

  // Arguments to invoke Orogene with.
  private lateinit var args: Arguments

  // Whether this resolver has initialized yet.
  private val initialized = atomic(false)

  // Create the NPM dependency cache root, if needed.
  private suspend fun establishNodeCacheRoot(): Path = withContext(IO) {
    project.layout.cache.resolve("npm").also {
      if (!it.toFile().exists()) {
        it.toFile().mkdirs()
      }
    }
  }

  // Create the NPM package storage root, if needed.
  private suspend fun establishNodeDepsRoot(): Path = withContext(IO) {
    project.layout.dependencies.resolve("npm").also {
      if (!it.toFile().exists()) {
        it.toFile().mkdirs()
      }
    }
  }

  // Either symbolically link, or render and write, the `package.json` file, in the NPM package storage root.
  private suspend fun linkOrRenderPackageJson(): Path = withContext(IO) {
    when (val existing = project.layout.projectRoot.resolve("package.json").takeIf { Files.exists(it) }) {
      // with no extant `package.json` at the project root, render one on the fly from the project's elide manifest. we
      // can assume there is an elide manifest in all cases.
      null -> packageRoot.resolve("package.json").also {
        manifestRenderer(npmManifest, it)
      }

      // otherwise, since the user has their own manifest, symbolically link it to the module root.
      else -> packageRoot.resolve("package.json").also {
        if (!it.exists()) {
          Files.createSymbolicLink(
            it,
            existing.absolute(),
          )
        }
      }
    }
  }

  // Invoke Orogene to perform installation.
  private suspend fun invokeOrogene(scope: CoroutineScope): Deferred<Tool.Result> {
    return orogeneInvoker(scope, args)
  }

  private suspend fun linkNodeModules(scope: CoroutineScope): Job = withContext(IO) {
    scope.launch {
      project.layout.projectRoot.resolve("node_modules").let { nodeModules ->
        try {
          Files.createSymbolicLink(
            nodeModules,
            packageRoot.resolve("node_modules").absolute(),
          )
        } catch (_: FileAlreadyExistsException) {
          // ignore
        }
      }
    }
  }

  // Build a suite of Orogene arguments to invoke the installer.
  private fun buildOrogeneArguments(): Arguments = Arguments.empty().toMutable().apply {
    add(Argument.of("--root" to packageRoot.absolutePathString()))
    add(Argument.of("--json"))
    add(Argument.of("--no-progress"))
    add(Argument.of("--no-emoji"))
    add(Argument.of("--no-first-time"))
    add(Argument.of("--no-telemetry"))
  }.build()

  override fun close() {
    // nothing to do at this time
  }

  override suspend fun seal() {
    require(!initialized.value) { "NPM resolver is already initialized" }
    project = buildInfoProvider.get()
    npmManifest = manifestProvider.get()
    packageRoot = establishNodeDepsRoot()
    packageCache = establishNodeCacheRoot()
    linkOrRenderPackageJson()
    args = buildOrogeneArguments()
    initialized.value = true
  }

  override suspend fun resolve(scope: CoroutineScope): Sequence<Job> = with(scope) {
    require(initialized.value) { "NPM resolver is not initialized" }
    return sequence {
      yield(launch { invokeOrogene(this) })
      yield(launch { linkNodeModules(this) })
    }
  }
}
