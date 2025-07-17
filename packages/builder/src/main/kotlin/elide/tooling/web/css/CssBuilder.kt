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
package elide.tooling.web.css

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import elide.runtime.diag.Diagnostics
import elide.tooling.project.ElideProject
import elide.tooling.web.Browsers
import elide.tooling.web.WebBuilder

// JNI callables.
private const val ENABLE_MINIFICATION = "enableMinification"
private const val ENABLE_SCSS = "enableScss"
private const val ENABLE_SOURCE_MAPS = "enableSourceMaps"
private const val ENABLE_MODULES = "enableModules"
private const val ENABLE_BUNDLE = "enableBundle"
private const val ENABLE_DEBUG_LOGS = "enableDebugLogs"
private const val ABSOLUTE_PROJECT_ROOT = "absoluteProjectRoot"

/**
 * ## CSS Builder
 *
 * Accepts CSS code in the form of one or more source files, and runs them through Elide's CSS building pipeline, which
 * is powered by LightningCSS. Multiple CSS sources can be combined into one, and minified if desired.
 *
 * To build CSS code, use the following steps:
 *
 * 1. Assemble a suite of [CssOptions].
 * 2. Gather sources and configure the build via [configureCss].
 * 3. Execute the build via [buildCss].
 */
public object CssBuilder {
  /**
   * ### CSS Defaults
   *
   * Default constants and values for CSS processing and building.
   */
  public data object CssDefaults {
    /** Default target for minification of CSS. */
    public val DEFAULT_TARGETS: Array<String> = emptyArray()
  }

  /**
   * ### CSS Minification
   *
   * Root of a type hierarchy which specifies whether CSS should be minified, and how.
   */
  public sealed interface CssMinification

  /**
   * ### Active CSS Minification
   *
   * Specifies configuration properties for CSS minification, when active.
   *
   * @property targets The list of targets to minify for; this is passed to LightningCSS, which will use it to determine
   *   which CSS features to support in the output.
   */
  @JvmRecord public data class MinifyOptions internal constructor(
    public val targets: Array<String>,
  ) : CssMinification {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as MinifyOptions
      return targets.contentEquals(other.targets)
    }

    override fun hashCode(): Int {
      return targets.contentHashCode()
    }

    /** Factories for obtaining instances of [MinifyOptions]. */
    public companion object {
      /** @return Default minification options. */
      @JvmStatic public fun defaults(): MinifyOptions = MinifyOptions(
        targets = CssDefaults.DEFAULT_TARGETS,
      )
    }
  }

  /**
   * ### No CSS Minification
   *
   * Specifies that CSS should not be minified at all, and should be left as-is after building.
   */
  public data object NoMinification : CssMinification

  /**
   * ### CSS Options
   *
   * Specifies options which govern CSS processing and build behavior.
   *
   * @property debug Whether to enable debug output for CSS processing.
   * @property minify Whether to enable minification for CSS output.
   * @property modules Whether to enable CSS modules processing.
   * @property bundle Whether to bundle multiple CSS files into one.
   * @property projectRoot The root project path, if any, which is used to resolve relative paths in CSS.
   * @property sourceMap Whether to generate a source map for the CSS output.
   * @property scss Whether to enable SCSS/SASS pre-processing.
   * @property browsers Browser support configuration.
   */
  @JvmRecord public data class CssOptions (
    public val debug: Boolean = false,
    public val minify: CssMinification = MinifyOptions.defaults(),
    public val modules: Boolean = false,
    public val bundle: Boolean = false,
    public val projectRoot: Path? = null,
    public val sourceMap: Boolean = false,
    public val scss: Boolean = false,
    public val browsers: Browsers = Browsers.defaults(),
  ) {
    /** @return Indication of whether minification is active. */
    @JvmName(ENABLE_MINIFICATION) public fun enableMinification(): Boolean = minify != NoMinification

    /** @return Indication of whether SCSS/SASS pre-processing is active. */
    @JvmName(ENABLE_SCSS) public fun enableScss(): Boolean = scss

    /** @return Indication of whether source mapping is active. */
    @JvmName(ENABLE_SOURCE_MAPS) public fun enableSourceMaps(): Boolean = sourceMap

    /** @return Indication of whether CSS Modules are active. */
    @JvmName(ENABLE_MODULES) public fun enableModules(): Boolean = modules

    /** @return Indication of whether to bundle outputs. */
    @JvmName(ENABLE_BUNDLE) public fun enableBundle(): Boolean = bundle

    /** @return Indication of whether to emit debug logs. */
    @JvmName(ENABLE_DEBUG_LOGS) public fun enableDebugLogs(): Boolean = debug

    /** @return Absolute path string for the project root, if set, otherwise, `null`. */
    @JvmName(ABSOLUTE_PROJECT_ROOT) public fun absoluteProjectRoot(): String? = projectRoot?.absolutePathString()

    /** @return Supported browsers for this build run. */
    @JvmName(ABSOLUTE_PROJECT_ROOT) public fun supportedBrowsers(): Array<String> = browsers.asTokens().toTypedArray()

    /** Factories for obtaining instances of [CssOptions]. */
    public companion object {
      /** @return Default suite of CSS build options. */
      @JvmStatic public fun defaults(): CssOptions = CssOptions()

      /** @return Defaults with additional configuration from the specified [project]. */
      @JvmStatic public fun forProject(project: ElideProject): CssOptions = defaults().copy(
        projectRoot = project.root,
        browsers = when (val targets = project.manifest.web?.css?.targets) {
          // if no targets are specified for css specifically, use project-wide browser support settings
          null -> project.manifest.web?.browsers ?: Browsers.defaults()

          // otherwise, prefer css-specific targets
          else -> Browsers.parse(targets.map {
            when (val version = it.version) {
              null -> it.browser
              else -> "${it.browser} $version"
            }
          })
        }
      )
    }
  }

  /**
   * ### CSS Source Material
   *
   * Abstracts where source material for CSS inputs comes from, via sealed subclasses which inherit various behaviors.
   */
  public sealed interface CssSourceMaterial {
    /**
     * Read the code associated with this source material.
     *
     * @return CSS code.
     */
    public suspend fun code(): String
  }

  /**
   * ### CSS Source File
   *
   * Represents a single CSS source file specification, which knows the path the CSS file in question and any other
   * inputs or options that may be relevant to this file only.
   */
  public fun interface CssSourceFile : CssSourceMaterial {
    public fun asPath(): Path

    override suspend fun code(): String = withContext(Dispatchers.IO) {
      asPath().inputStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
        reader.readText()
      }
    }
  }

  /**
   * ### CSS Source Literal
   *
   * Represents a single chunk of CSS code, expressed as a literal string.
   */
  public fun interface CssSourceLiteral : CssSourceMaterial

  /**
   * ### CSS Sources
   *
   * Handles state and specification of the sources in CSS which act as inputs for a [CssBuild] operation; combined with
   * [CssOptions] for building these files, this class constitutes the main inputs for a [CssBuild].
   */
  @JvmRecord public data class CssSources internal constructor (internal val srcs: List<CssSourceMaterial>)

  /**
   * ### CSS Build
   *
   * Represents the state and outcome of a single CSS build operation, bound to the [CssOptions] and [CssSources] that
   * produced it.
   *
   * @property options The [CssOptions] that were used to configure this build.
   * @property sources The [CssSources] that were used as inputs for this build.
   */
  public class CssBuild internal constructor (
    public val options: CssOptions,
    public val sources: CssSources,
  )

  /**
   * ### CSS Result
   *
   * Holds the result of a CSS build operation, which includes access to the final built code, so long as there was no
   * error while building.
   */
  public fun interface CssResult {
    /**
     * Return the finalized CSS code as a string.
     *
     * @return Finalized CSS code.
     */
    public fun code(): List<String>
  }

  /**
   * Configure a [CssBuild] operation, given the [options] and [files] to use as inputs.
   *
   * @param options The [CssOptions] to use for this build operation.
   * @param files The [CssSourceFile]s to use as inputs for this build operation.
   * @return A [CssBuild] instance that can be executed via [buildCss].
   */
  public fun configureCss(options: CssOptions, files: Sequence<CssSourceMaterial>): CssBuild {
    WebBuilder.load()
    return CssBuild(options, CssSources(files.toList()))
  }

  /**
   * Execute the build phase of a CSS build operation (via [CssBuild]), having previously been configured via the
   * [configureCss] method.
   *
   * @param build The [CssBuild] to execute.
   * @return The resulting [CssBuild] after execution.
   */
  public suspend fun buildCss(build: CssBuild, ctx: CoroutineContext = Dispatchers.IO): CssResult = withContext(ctx) {
    build.sources.srcs.map { source ->
      source.code()
    }.map {
      async {
        runCatching {
          CssNative.buildCss(
            it,
            build.options,
            minify = build.options.enableMinification(),
            modules = build.options.enableModules(),
            sourceMaps = build.options.enableSourceMaps(),
            scss = build.options.enableScss(),
            browsers = build.options.browsers.asTokens().toTypedArray(),
          )
        }
      }
    }.awaitAll().let { results ->
      val diags = Diagnostics.query(true) { true }.toList()
      if (diags.isNotEmpty()) {
        error("CSS builder failed with diagnostics: ${diags.joinToString("\n")}")
      }

      CssResult {
        results.mapNotNull { it.getOrThrow() }
      }
    }
  }
}
