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
package elide.tooling.project

import elide.tooling.project.manifest.ElidePackageManifest
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.io.path.extension
import kotlin.streams.asSequence
import elide.runtime.Logging

/**
 * Factory for loading source sets defined in the Elide project configuration.
 */
public fun interface SourceSetFactory {
  public companion object {
    private val logging by lazy { Logging.of(SourceSetFactory::class) }
  }

  /**
   * Load a defined source set spec from the Elide package into an actual source set.
   *
   * @param root Root path of the project.
   * @param key Key (name) of the source set to load.
   * @param sourceSetSpec Specification of the source set to load.
   * @return The loaded source set, or null if the source set could not be loaded by this factory.
   */
  public suspend fun load(root: Path, key: String, sourceSetSpec: ElidePackageManifest.SourceSet): SourceSet?

  public data object Default : SourceSetFactory {
    private fun matchSpec(spec: String): String {
      return when {
        spec.startsWith("glob:") || spec.startsWith("regex:") -> spec
        else -> "glob:$spec"
      }
    }

    private fun pathMatcher(spec: String): PathMatcher {
      return FileSystems.getDefault().getPathMatcher(matchSpec(spec))
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
          "html" -> SourceSetLanguage.HTML
          "css" -> SourceSetLanguage.CSS
          "scss", "sass" -> SourceSetLanguage.SCSS
          "md" -> SourceSetLanguage.Markdown
          "mdx" -> SourceSetLanguage.MDX
          else -> null
        },
      )
    }

    override suspend fun load(root: Path, key: String, sourceSetSpec: ElidePackageManifest.SourceSet): SourceSet {
      val sourceFilePaths: Sequence<SourceFilePath> = sourceSetSpec.paths.mapNotNull { spec ->
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
                logging.debug("Source set path '{}' does not exist (from glob: '{}')", pathPrefix, spec)
                return@mapNotNull null
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
                  // special case: for `**/*.*`, everything matches with a prefix
                  if (spec.endsWith("**/*.*")) return@filter true
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
      }.asSequence().flatMap {
        it.asSequence()
      }

      val taggedPaths = sourceFilePaths.toSortedSet()

      return object : SourceSet {
        override val name: SourceSetName = key
        override val languages: Set<SourceSetLanguage> = taggedPaths.mapNotNull { it.lang }.toSet()
        override val spec: List<String> = sourceSetSpec.paths
        override val paths: SourceTaggedPathSuite = taggedPaths
        override val type: SourceSetType = when (key) {
          "test" -> SourceSetType.Tests
          else -> SourceSetType.Sources
        }
      }
    }
  }
}
