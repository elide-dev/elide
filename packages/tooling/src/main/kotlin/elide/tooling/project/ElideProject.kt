/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
@file:OptIn(DelicateElideApi::class)

package elide.tooling.project

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.SortedSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.io.path.extension
import kotlin.streams.asSequence
import elide.runtime.core.DelicateElideApi
import elide.runtime.plugins.env.EnvConfig.EnvVar
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Factory for loading source sets defined in the Elide project configuration.
 */
public fun interface SourceSetFactory {
  /**
   * Load a defined source set spec from the Elide package into an actual source set.
   *
   * @param root Root path of the project.
   * @param key Key (name) of the source set to load.
   * @param sourceSetSpec Specification of the source set to load.
   * @return The loaded source set, or null if the source set could not be loaded by this factory.
   */
  public suspend fun load(root: Path, key: String, sourceSetSpec: ElidePackageManifest.SourceSet): SourceSet?
}

/**
 * Loader which transforms a regular Elide project into a configured Elide project.
 */
public interface ElideProjectLoader {
  /**
   * Factory for producing source sets.
   */
  public val sourceSetFactory: SourceSetFactory
}

/** Information about an Elide project. */
public sealed interface ElideProject {
  /** Root path containing the project. */
  public val root: Path

  /** Project's parsed manifest. */
  public val manifest: ElidePackageManifest

  /** Injected project environment. */
  public val env: ProjectEnvironment?

  /**
   * Load this project's configuration, interpreting it through build configurators which are installed on the current
   * classpath.
   *
   * @return A configured project.
   */
  public suspend fun load(loader: ElideProjectLoader = DefaultProjectLoader): ElideConfiguredProject
}

/**
 * Default implementation of [ElideProjectLoader] which uses the default source set factory.
 */
public object DefaultProjectLoader : ElideProjectLoader {
  private fun matchSpec(spec: String): String {
    return when {
      spec.startsWith("glob:") || spec.startsWith("regex:") -> spec
      else -> "glob:$spec"
    }
  }

  private fun pathMatcher(spec: String): PathMatcher {
    return FileSystems.getDefault().getPathMatcher(matchSpec(spec))
  }

  override val sourceSetFactory: SourceSetFactory get() = object : SourceSetFactory {
    override suspend fun load(root: Path, key: String, sourceSetSpec: ElidePackageManifest.SourceSet): SourceSet? {
      val sourceFilePaths: Sequence<SourceFilePath> = sourceSetSpec.spec.stream().flatMap { spec ->
        Path.of(spec).let { path ->
          when {
            // if there are asterisks, or it doesn't exist as a file, resolve this path as a glob.
            '*' in spec || !Files.exists(path) -> pathMatcher(spec).let { matcher ->
              // select the deepest segment which doesn't have a glob

              // `blah/blah/**/*.kts`
              // `stk.*/**/halb/halb`
              //         ^ index
              //         |---------| <-- slice
              //          ^ indexOfFirst('/')
              //          blah/blah/ <-- reversed
              val rev = spec.reversed()
              val pathPrefix = when (val index = rev.indexOfLast { it == '*' }) {
                -1 -> path
                else -> rev.slice(index..rev.lastIndex).let { sliced ->
                  Path.of(sliced.substringAfter('/').reversed())
                }
              }.let {
                if (it.startsWith("/")) it else root.resolve(it)
              }
              if (!Files.exists(pathPrefix)) {
                throw IllegalArgumentException("Source set path '$pathPrefix' does not exist (from glob: '$spec')")
              }
              if (!Files.isDirectory(pathPrefix)) {
                throw IllegalArgumentException("Source set path '$pathPrefix' is not a directory (from glob: '$spec')")
              }
              if (!Files.isReadable(pathPrefix)) {
                throw IllegalArgumentException("Source set path '$pathPrefix' is not readable (from glob: '$spec')")
              }
              val rootRelative = root.relativize(pathPrefix)
              Files.walk(pathPrefix).filter { thisPath ->
                // before matching the path, we need to trim everything before the path prefix. this is because our
                // glob matcher is relative to the path prefix, not the root of the project.
                val srcRelative = pathPrefix.relativize(thisPath)
                if (srcRelative.toString().isEmpty()) {
                  false
                } else {
                  val srcRelativeSrcPath = rootRelative.resolve(srcRelative)
                  matcher.matches(srcRelativeSrcPath)
                }
              }.map {
                sourceSetFileFromPath(it)
              }
            }

            // otherwise, treat it as a regular file path, resolved from the project root.
            else -> listOf(sourceSetFileFromPath(root.resolve(path))).stream()
          }
        }
      }.asSequence()

      val taggedPaths = sourceFilePaths.toSortedSet()

      return object : SourceSet {
        override val name: SourceSetName = key
        override val languages: Set<SourceSetLanguage> = taggedPaths.mapNotNull { it.lang }.toSet()
        override val spec: List<String> = sourceSetSpec.spec
        override val paths: SourceTaggedPathSuite = taggedPaths
        override val type: SourceSetType = when (key) {
          "test" -> SourceSetType.Tests
          else -> SourceSetType.Sources
        }
      }
    }
  }
}

/** Name of the source set. */
public typealias SourceSetName = String

/** Sorted set of file paths. */
public typealias PathSuite = SortedSet<Path>

/** Sorted set of file paths. */
public typealias SourceTaggedPathSuite = SortedSet<SourceFilePath>

@JvmRecord public data class SourceFilePath(
  public val path: Path,
  public val lang: SourceSetLanguage? = null,
) : Comparable<SourceFilePath> {
  override fun compareTo(other: SourceFilePath): Int = path.compareTo(other.path)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as SourceFilePath
    return path == other.path
  }

  override fun hashCode(): Int {
    return path.hashCode()
  }
}

/** Describes known languages of source sets. */
public sealed interface SourceSetLanguage {
  /** Describes a variant or dialect of a known language. */
  public sealed interface SourceVariant<T> : SourceSetLanguage where T : SourceSetLanguage

  /** The source set specifies Java sources. */
  public data object Java : SourceSetLanguage

  /** The source set specifies Kotlin sources. */
  public data object Kotlin : SourceSetLanguage

  /** The source set specifies Kotlin Script sources. */
  public data object KotlinScript : SourceSetLanguage, SourceVariant<Kotlin>

  /** The source set specifies JavaScript sources. */
  public data object JavaScript : SourceSetLanguage

  /** The source set specifies JSX sources. */
  public data object JSX : SourceSetLanguage, SourceVariant<JavaScript>

  /** The source set specifies TypeScript sources. */
  public data object TypeScript : SourceSetLanguage

  /** The source set specifies JSX sources. */
  public data object TSX : SourceSetLanguage, SourceVariant<TypeScript>

  /** The source set specifies Python sources. */
  public data object Python : SourceSetLanguage

  /** The source set specifies Ruby sources. */
  public data object Ruby : SourceSetLanguage
}

/** Describes types of source sets. */
public sealed interface SourceSetType {
  /** The source set contains primary source code. */
  public data object Sources : SourceSetType

  /** The source set contains test source code. */
  public data object Tests : SourceSetType
}

/** Describes information about a configured source set in a [ElideConfiguredProject]. */
public interface SourceSet {
  /** Name assigned to this source set. */
  public val name: SourceSetName

  /** Type of this source set. */
  public val type: SourceSetType

  /** Languages held by this source set. */
  public val languages: Set<SourceSetLanguage>?

  /** Specified strings for this source set. */
  public val spec: List<String>

  /** Interpreted full suite of source set paths. */
  public val paths: SourceTaggedPathSuite
}

/** Describes source sets loaded for a [ElideConfiguredProject]. */
public interface SourceSets {
  /**
   * Indicate whether a source set with the given [name] exists.
   *
   * @return True if a source set with the given [name] exists, false otherwise.
   */
  public operator fun contains(name: SourceSetName): Boolean

  /**
   * Retrieve a source set by [name].
   *
   * @return The source set with the given [name], or null if no such source set exists.
   */
  public operator fun get(name: SourceSetName): SourceSet?

  /**
   * Find all source sets matching the specified [types].
   *
   * @param types Type(s) of source set to find.
   * @return A sequence of source sets matching the specified [types].
   */
  public fun find(vararg types: SourceSetType): Sequence<SourceSet>

  /**
   * Find all source sets matching the specified [langs].
   *
   * @param langs Language(s) of source set to find.
   * @return A sequence of source sets matching the specified [langs].
   */
  public fun find(vararg langs: SourceSetLanguage): Sequence<SourceSet>
}

/** Interpreted and cached info about an Elide project. */
public sealed interface ElideConfiguredProject : ElideProject {
  /** Source sets loaded for this project. */
  public val sourceSets: SourceSets
}

// Create a source file path with a path and lang; if no lang is provided, best efforts are made to detect one.
private fun sourceSetFileFromPath(path: Path, lang: SourceSetLanguage? = null): SourceFilePath {
  return SourceFilePath(
    path = path,
    lang = lang ?: when (path.extension.removePrefix(".")) {
      "java" -> SourceSetLanguage.Java
      "kt" -> SourceSetLanguage.Kotlin
      "kts" -> SourceSetLanguage.KotlinScript
      "mjs", "cjs", "js" -> SourceSetLanguage.JavaScript
      "jsx" -> SourceSetLanguage.JSX
      "mts", "cts", "ts" -> SourceSetLanguage.TypeScript
      "tsx" -> SourceSetLanguage.TSX
      "py" -> SourceSetLanguage.Python
      "rb" -> SourceSetLanguage.Ruby
      else -> null
    },
  )
}

/** Information about an Elide project. */
@JvmRecord @Serializable public data class ElideProjectInfo(
  override val root: Path,
  override val manifest: ElidePackageManifest,
  override val env: ProjectEnvironment? = null,
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
    )
  }
}

/** Environment settings applied to the project. */
@JvmRecord @Serializable public data class ProjectEnvironment private constructor(
  @Transient public val vars: Map<String, EnvVar> = sortedMapOf(),
) {
  public companion object {
    /** @return Project environment wrapping the provided [map] of env vars. */
    @JvmStatic public fun wrapping(map: Map<String, EnvVar>): ProjectEnvironment = ProjectEnvironment(vars = map)
  }
}

// Internal implementation class of a [ElideLoadedProject].
internal class ElideConfiguredProjectImpl(
  override val sourceSets: SourceSets,
  private val info: ElideProjectInfo,
) : ElideProject by info, ElideConfiguredProject {
  override suspend fun load(loader: ElideProjectLoader): ElideConfiguredProject {
    return this // already loaded
  }

  companion object {
    @JvmStatic fun configure(project: ElideProject, sourceSets: SourceSets): ElideConfiguredProject {
      return ElideConfiguredProjectImpl(
        sourceSets = sourceSets,
        info = project as? ElideProjectInfo ?: ElideProjectInfo(
          root = project.root,
          manifest = project.manifest,
          env = project.env,
        ),
      )
    }
  }
}
