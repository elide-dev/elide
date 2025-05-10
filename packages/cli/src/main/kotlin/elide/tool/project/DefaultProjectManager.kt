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
package elide.tool.project

import java.nio.file.Path
import jakarta.inject.Provider
import kotlin.io.path.*
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.plugins.env.EnvConfig
import elide.tool.io.WorkdirManager
import elide.tooling.project.ElideProject
import elide.tooling.project.ElideProjectInfo
import elide.tooling.project.PackageManifestService
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.ProjectEnvironment
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

    private fun parseDotEnv(path: Path, builder: MutableMap<String, EnvConfig.EnvVar>) {
      if (!path.isRegularFile()) return

      runCatching {
        val fileName = path.name
        path.forEachLine { line ->
          if (line.isBlank()) return@forEachLine
          val (key, value) = line.split('=', limit = 2)
          builder.put(key, EnvConfig.EnvVar.fromDotenv(fileName, key, value))
        }
      }
    }

    // Read any present `.env` file in the project directory.
    @JvmStatic
    private fun readDotEnv(dir: Path): ProjectEnvironment {
      val env = buildMap {
        sequenceOf(
          dir.resolve(DOT_ENV_FILE).takeIf { it.isRegularFile() },
          dir.resolve(DOT_ENV_FILE_LOCAL).takeIf { it.isRegularFile() },
        ).filterNotNull().forEach {
          parseDotEnv(it, this)
        }
      }

      return ProjectEnvironment.wrapping(env)
    }
  }

  override suspend fun resolveProject(pathOverride: String?): ElideProject? {
    val rootBase = when (pathOverride) {
      null -> workdir.get().projectRoot()?.toPath()
      else -> Path.of(pathOverride)
    }
    val root = rootBase?.takeIf { it.isDirectory() } ?: return null
    val env = readDotEnv(root)

    // prefer an Elide manifest if present
    val manifests = manifests.get()
    var elideManifest = manifests.resolve(root).takeIf { it.isRegularFile() }?.let { manifestFile ->
      manifestFile.inputStream().use { manifests.parse(it, ProjectEcosystem.Elide) }
    } as ElidePackageManifest?

    // if no Elide manifest is found, attempt to merge other supported formats
    if (elideManifest == null) {
      val candidates = supportedThirdPartyManifests.mapNotNull { ecosystem ->
        manifests.resolve(root, ecosystem).takeIf { it.isRegularFile() }
      }.mapNotNull { manifestFile ->
        runCatching { manifests.parse(manifestFile) }.getOrNull()
      }

      elideManifest = manifests.merge(candidates.asIterable())
    }

    return ElideProjectInfo(
      root = root,
      env = env,
      manifest = elideManifest,
    )
  }
}
