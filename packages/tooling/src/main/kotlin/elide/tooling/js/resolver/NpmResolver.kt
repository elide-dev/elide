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
import kotlinx.io.IOException
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import elide.annotations.Inject
import elide.runtime.Logging
import elide.tool.Argument
import elide.tool.Arguments
import elide.tool.Tool
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.project.manifest.NodePackageManifest

// Name of the `package.json` file.
private const val PACKAGE_JSON = "package.json"

// Name of the `package-lock.kdl` file.
private const val PACKAGE_LOCK_KDL = "package-lock.kdl"

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
 *   3) The `package-lock.kdl` file, if it exists, is symbolically linked into the NPM package root.
 *   4) Orogene is invoked with arguments which resemble the sample below; as a by-product, the `package-lock.kdl` file
 *       is written to the dependency root, as well as the populated `node_modules` folder.
 *   5) The `node_modules` folder is linked into the Elide project root to facilitate NPM and ESM resolution.
 *   6) If the `package-lock.kdl` was NOT present, the new lockfile is copied to the project root, deleted from the NPM
 *      package root, and symbolically linked from the project root, to match cached run behavior.
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
  private companion object {
    @JvmStatic private val logging by lazy { Logging.of(NpmResolver::class) }
  }

  // Resolved project info.
  private lateinit var project: ElideBuildState

  // Resolved package root path.
  private lateinit var packageRoot: Path

  // Resolved package cache path.
  private lateinit var packageCache: Path

  // Resolved existing package lock path (if present).
  private var existingPackageLock: Path? = null

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
    when (val existing = project.layout.projectRoot.resolve(PACKAGE_JSON).takeIf { Files.exists(it) }) {
      // with no extant `package.json` at the project root, render one on the fly from the project's elide manifest. we
      // can assume there is an elide manifest in all cases.
      null -> packageRoot.resolve(PACKAGE_JSON).also {
        manifestRenderer(npmManifest, it)
      }

      // otherwise, since the user has their own manifest, symbolically link it to the module root.
      else -> packageRoot.resolve(PACKAGE_JSON).also {
        if (!it.exists()) {
          Files.createSymbolicLink(
            it,
            existing.absolute(),
          )
        }
      }
    }
  }

  // If present in the project root, symbolically link `package-lock.kdl` into the NPM package root.
  private suspend fun linkPackageLockIfPresent(): Path? = withContext(IO) {
    project.layout.projectRoot.resolve(PACKAGE_LOCK_KDL).takeIf { Files.exists(it) }?.also { lockfile ->
      packageRoot.resolve(PACKAGE_LOCK_KDL).also {
        if (!it.exists()) {
          Files.createSymbolicLink(
            it,
            lockfile.absolute(),
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

  private suspend fun copyPackageLockIfNeeded(scope: CoroutineScope): Job = withContext(IO) {
    scope.launch {
      when (existingPackageLock) {
        // if there was no lockfile when we started this process, one will now have been written by Orogene to the NPM
        // package root. in this case, we need to copy it into the root project (so it can be tracked in source control)
        // then delete the NPM package root copy, and establish a symbolic link instead; this converges branch behavior
        // with the cached run behavior below, when a package lock is already present.
        null -> project.layout.projectRoot.resolve(PACKAGE_LOCK_KDL).let { projectLockfilePath ->
          val createdLockfile = project.layout.dependencies.resolve("npm").resolve(PACKAGE_LOCK_KDL)
          if (!createdLockfile.exists()) {
            logging.warn { "Orogene didn't create a lockfile, so we can't copy one into the project." }
          } else try {
            // copy the lockfile into the project root
            Files.copy(
              createdLockfile,
              projectLockfilePath,
            )

            // delete the lockfile from the NPM package root
            Files.delete(createdLockfile)

            // create a symbolic link to the lockfile in the NPM package root
            Files.createSymbolicLink(
              createdLockfile,
              projectLockfilePath.absolute(),
            )
          } catch (err: IOException) {
            logging.error("Failed to copy package lock file into project root", err)
          } catch (err: FileAlreadyExistsException) {
            logging.debug("Package lock file already exists in project root, skipping copy", err)
          }
        }

        // a package lock file already exists in the root project, so it was symbolically linked into the NPM package
        // root when we started resolution. therefore, no followup is needed, as any writes to the package lock will
        // propagate back through the symbolic link we established above.
        else -> {}
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
    existingPackageLock = linkPackageLockIfPresent()
    linkOrRenderPackageJson()
    args = buildOrogeneArguments()
    initialized.value = true
  }

  override suspend fun resolve(scope: CoroutineScope): Sequence<Job> = with(scope) {
    require(initialized.value) { "NPM resolver is not initialized" }
    return sequence {
      yield(launch { invokeOrogene(this) })
      yield(launch { linkNodeModules(this) })
      yield(launch { copyPackageLockIfNeeded(this) })
    }
  }
}
