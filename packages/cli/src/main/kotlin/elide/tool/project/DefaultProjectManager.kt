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
@file:Suppress("MnInjectionPoints")

package elide.tool.project

import java.nio.file.Path
import java.util.LinkedList
import jakarta.inject.Provider
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.io.path.*
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.EnvVar
import elide.tool.io.WorkdirManager
import elide.tooling.project.ElideProject
import elide.tooling.project.ElideProjectInfo
import elide.tooling.project.PackageManifestService
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.ProjectEnvironment
import elide.tooling.project.ProjectManager
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.flags.ProjectFlagKey
import elide.tooling.project.flags.ProjectFlagValue
import elide.tooling.project.flags.ProjectFlagsContext
import elide.tooling.project.manifest.ElidePackageManifest

/** Default implementation of [ProjectManager]. */
@Singleton
internal class DefaultProjectManager @Inject constructor(
  private val workdir: Provider<WorkdirManager>,
  private val manifests: Provider<PackageManifestService>,
) : ProjectManager {
  companion object {
    // Filename for `.env` files.
    private const val DOT_ENV_FILE = ".env"

    // Filename for local `.env` file.
    private const val DOT_ENV_FILE_LOCAL = ".env.local"

    private val supportedThirdPartyManifests = sequenceOf(
      ProjectEcosystem.Node,
      ProjectEcosystem.MavenPom,
      ProjectEcosystem.PythonRequirements,
    )

    private fun parseDotEnv(path: Path, builder: MutableMap<String, EnvVar>) {
      if (!path.isRegularFile()) return

      runCatching {
        val fileName = path.name
        path.forEachLine { line ->
          if (line.isBlank()) return@forEachLine
          val (key, value) = line.split('=', limit = 2)
          builder[key] = EnvVar.fromDotenv(fileName, key, value)
        }
      }
    }

    // Read any present `.env` file in the project directory.
    @JvmStatic
    private fun readDotEnv(dir: Path): ProjectEnvironment {
      return ProjectEnvironment.wrapping(buildMap {
        sequenceOf(
          dir.resolve(DOT_ENV_FILE).takeIf { it.isRegularFile() },
          dir.resolve(DOT_ENV_FILE_LOCAL).takeIf { it.isRegularFile() },
        ).filterNotNull().forEach {
          parseDotEnv(it, this)
        }
      })
    }
  }

  private suspend fun resolveWorkspace(leaf: Path): Path? = withContext(IO) {
    val top = leaf.absolute()
    val parents = LinkedList<Path>()
    var subject = top.parent
    while (subject != null) {
      val cfg = subject.resolve("elide.pkl")
      if (cfg.exists()) {
        parents.add(cfg.parent)
      }
      subject = subject.parent
    }
    parents.lastOrNull()
  }

  private fun ProjectManager.ProjectEvaluatorInputs.parseProjectParams(): ProjectManager.ProjectParams {
    val probableFlags = params?.filter { it.startsWith("-") } ?: emptyList()
    val probableTasks = params?.filter { !it.startsWith("-") } ?: emptyList()
    val flagsToValues = probableFlags.map { flag ->
      when {
        flag.startsWith("--no-") || flag.endsWith("=false") ->
          ProjectFlagKey.of(flag.replace("--no-", "--")) to ProjectFlagValue.False
        flag.endsWith("=true") -> ProjectFlagKey.of(flag) to ProjectFlagValue.True
        flag.contains('=') -> flag.split('=', limit = 2).let { segments ->
          ProjectFlagKey.of(segments.first()) to ProjectFlagValue.StringValue(segments.last())
        }
        else -> ProjectFlagKey.of(flag) to ProjectFlagValue.True
      }
    }
    return ProjectManager.ProjectParams(
      flags = flagsToValues,
      tasks = probableTasks,
      debug = debug,
      release = release,
    )
  }

  override suspend fun resolveProject(
    pathOverride: Path?,
    inputs: ProjectManager.ProjectEvaluatorInputs,
  ): ElideProject? {
    val evalParams = inputs.parseProjectParams()
    val flagCtx = ProjectFlagsContext.from(evalParams)

    val rootBase = when (pathOverride) {
      null -> workdir.get().projectRoot()?.toPath()
      else -> pathOverride
    }
    val root = rootBase?.takeIf { it.isDirectory() } ?: return null
    val workspaceRoot = resolveWorkspace(root)
    val env = readDotEnv(root)
    val rootEnv = workspaceRoot?.let { readDotEnv(it) }

    // prefer an Elide manifest if present; inform the manifest service of flag and project context
    val manifests = manifests.get().apply {
      configure(object: PackageManifestCodec.ManifestBuildState {
        override val isDebug: Boolean get() = false
        override val isRelease: Boolean get() = false
        override val flags: ProjectFlagsContext get() = flagCtx
        override val params: ProjectManager.ProjectParams get() = evalParams
      })
    }

    return coroutineScope {
      val elideManifestOp = async(IO) {
        manifests.resolve(root).takeIf { it.isRegularFile() }?.let { manifestFile ->
          manifestFile.inputStream().use {
            manifests.parse(it, ProjectEcosystem.Elide)
              .also { manifests.enforce(it).throwIfFailed() }
          }
        } as ElidePackageManifest?
      }

      val rootManifestOp = async(IO) {
        if (workspaceRoot == null) null else {
          manifests.resolve(workspaceRoot).takeIf { it.isRegularFile() }?.let { manifestFile ->
            manifestFile.inputStream()
              .use { manifests.parse(it, ProjectEcosystem.Elide) }
              .also { manifests.enforce(it).throwIfFailed() }
          } as ElidePackageManifest?
        }
      }

      var elideManifest: ElidePackageManifest? = async(IO) {
        listOf(elideManifestOp, rootManifestOp).awaitAll()
        var current: ElidePackageManifest? = elideManifestOp.await()
        val root: ElidePackageManifest? = rootManifestOp.await()
        when {
          current != null && root == null -> current
          current == null && root != null -> root
          current != null && root != null -> current.within(requireNotNull(workspaceRoot), root)
          else -> null
        }
      }.await()

      val rootManifest = rootManifestOp.await()

      // if no Elide manifest is found, attempt to merge other supported formats
      if (elideManifest == null) {
        val candidates = supportedThirdPartyManifests.mapNotNull { ecosystem ->
          manifests.resolve(root, ecosystem).takeIf { it.isRegularFile() }
        }.mapNotNull { manifestFile ->
          runCatching { manifests.parse(manifestFile) }.getOrNull()
        }

        elideManifest = manifests.merge(candidates.asIterable())
      }

      when {
        workspaceRoot != null && rootManifest != null -> ElideProjectInfo(
          root = root,
          env = rootEnv?.let { rootEnv + env } ?: env,
          manifest = elideManifest.within(workspaceRoot, rootManifest),
          workspace = workspaceRoot,
        )

        else -> ElideProjectInfo(
          root = root,
          env = rootEnv?.let { rootEnv + env } ?: env,
          manifest = elideManifest,
          workspace = workspaceRoot,
        )
      }
    }
  }
}
