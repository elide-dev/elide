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
@file:Suppress("DataClassPrivateConstructor")

package elide.tooling.project

import java.nio.file.Path
import kotlinx.serialization.Serializable
import elide.tooling.lockfile.InterpretedLockfile
import elide.tooling.project.manifest.ElidePackageManifest

/** Information about an Elide project. */
public sealed interface ElideProject {
  /** Root path containing the project. */
  public val root: Path

  /** Project's parsed manifest. */
  public val manifest: ElidePackageManifest

  /** Injected project environment. */
  public val env: ProjectEnvironment?

  /** Workspace root. */
  public val workspace: Path?

  /** @return Active workspace manifest, if any. */
  public fun activeWorkspace(): Pair<Path, ElidePackageManifest>? {
    return manifest.activeWorkspace()
  }

  /**
   * Load this project's configuration, interpreting it through build configurators which are installed on the current
   * classpath.
   *
   * @return A configured project.
   */
  public suspend fun load(loader: ElideProjectLoader): ElideConfiguredProject
}

/** Interpreted and cached info about an Elide project. */
public sealed interface ElideConfiguredProject : ElideProject {
  /** Source sets loaded for this project. */
  public val sourceSets: SourceSets

  /** Static path to binary resources. */
  public val resourcesPath: Path

  /** Active lockfile instance for this project. */
  public val activeLockfile: InterpretedLockfile?
}

/** Information about an Elide project. */
@JvmRecord @Serializable public data class ElideProjectInfo(
  override val root: Path,
  override val manifest: ElidePackageManifest,
  override val env: ProjectEnvironment? = null,
  override val workspace: Path? = null,
) : ElideProject {
  override suspend fun load(loader: ElideProjectLoader): ElideConfiguredProject {
    // process source sets
    val srcs = manifest.sources.map { pair ->
      requireNotNull(loader.sourceSetFactory.load(root, pair.key, pair.value)) {
        "Failed to load source set '${pair.key}' from manifest: No factory available to load this"
      }
    }.associateBy {
      it.name
    }

    val sourceSets = object : SourceSets {
      override fun contains(name: SourceSetName): Boolean = name in srcs
      override fun get(name: SourceSetName): SourceSet? = srcs[name]
      override fun find(vararg types: SourceSetType): Sequence<SourceSet> = srcs.values.asSequence()
        .filter { it.type in types }
      override fun find(vararg langs: SourceSetLanguage): Sequence<SourceSet> = srcs.values.asSequence()
        .filter { it.languages?.any { lang -> lang in langs } == true }
    }
    return ElideConfiguredProjectImpl(
      sourceSets = sourceSets,
      info = this,
      resourcesPath = loader.resourcesPath,
      activeLockfile = loader.lockfileLoader.loadLockfile(root),
    )
  }
}

// Internal implementation class of a [ElideLoadedProject].
internal class ElideConfiguredProjectImpl(
  override val sourceSets: SourceSets,
  override val resourcesPath: Path,
  override val activeLockfile: InterpretedLockfile?,
  private val info: ElideProjectInfo,
) : ElideProject by info, ElideConfiguredProject {
  override suspend fun load(loader: ElideProjectLoader): ElideConfiguredProject {
    return this // already loaded
  }

  companion object {
    @JvmStatic
    fun configure(
      project: ElideProject,
      lockfile: InterpretedLockfile?,
      sourceSets: SourceSets,
      resourcesPath: Path,
    ): ElideConfiguredProject {
      return ElideConfiguredProjectImpl(
        sourceSets = sourceSets,
        resourcesPath = resourcesPath,
        activeLockfile = lockfile,
        info = project as? ElideProjectInfo ?: ElideProjectInfo(
          root = project.root,
          manifest = project.manifest,
          env = project.env,
        ),
      )
    }
  }
}
