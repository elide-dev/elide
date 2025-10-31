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
@file:Suppress("KotlinConstantConditions")

package elide.tooling.web.html

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.inputStream
import elide.runtime.diag.Diagnostics
import elide.tooling.web.WebBuilder
import `in`.wilsonl.minifyhtml.Configuration as MinifyHtmlConfiguration
import `in`.wilsonl.minifyhtml.MinifyHtml as HtmlMinifier

/**
 * ## HTML Builder
 *
 * Accepts arbitrary HTML code and builds it into a minified/optimized version which remains compatible with a wide
 * variety of browsers and platforms. The HTML builder is used (mostly) by Elide's static site generator and other
 * server infrastructure.
 */
public object HtmlBuilder {
  /**
   * ### HTML Defaults
   *
   * Default constants and values for HTML processing and building.
   */
  public data object HtmlDefaults {
    /** Whether to remove HTML comments. */
    public const val MINIFY_COMMENTS: Boolean = true

    /** Whether to minify inline CSS in HTML. */
    public const val MINIFY_CSS: Boolean = true

    /** Whether to minify inline JS in HTML. */
    public const val MINIFY_JS: Boolean = true
  }

  /**
   * ### HTML Minification
   *
   * Root of a type hierarchy which specifies whether HTML should be minified, and how.
   */
  public sealed interface HtmlMinification {
    /** Whether to emit comments in the HTML output. */
    public val comments: Boolean get() = false

    /** Whether to minify inline CSS. */
    public val css: Boolean get() = false

    /** Whether to minify inline JavaScript. */
    public val js: Boolean get() = false
  }

  /**
   * ### Active HTML Minification
   *
   * Specifies configuration properties for HTML minification, when active.
   *
   * @property comments Whether to remove comments from the HTML output.
   */
  @JvmRecord public data class MinifyOptions (
    override val comments: Boolean = HtmlDefaults.MINIFY_COMMENTS,
    override val css: Boolean = HtmlDefaults.MINIFY_CSS,
    override val js: Boolean = HtmlDefaults.MINIFY_JS,
  ) : HtmlMinification {
    /** Factories for obtaining instances of [MinifyOptions]. */
    public companion object {
      /** @return Default minification options. */
      @JvmStatic public fun defaults(): MinifyOptions = MinifyOptions()
    }
  }

  /**
   * ### No HTML Minification
   *
   * Specifies that HTML should not be minified at all, and should be left as-is after building.
   */
  public data object NoMinification : HtmlMinification

  /**
   * ### HTML Source Material
   *
   * Abstracts where source material for HTML inputs comes from, via sealed subclasses which inherit various behaviors.
   */
  public sealed interface HtmlSourceMaterial {
    /**
     * Read the code associated with this source material.
     *
     * @return HTML code.
     */
    public suspend fun code(): String
  }

  /**
   * ### HTML Source File
   *
   * Represents a single HTML source file specification, which knows the path the HTML file in question and any other
   * inputs or options that may be relevant to this file only.
   */
  public fun interface HtmlSourceFile : HtmlSourceMaterial {
    public fun asPath(): Path

    override suspend fun code(): String = withContext(Dispatchers.IO) {
      asPath().inputStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
        reader.readText()
      }
    }
  }

  /**
   * ### HTML Source Literal
   *
   * Represents a single chunk of HTML code, expressed as a literal string.
   */
  public fun interface HtmlSourceLiteral : HtmlSourceMaterial

  /**
   * ### HTML Options
   *
   * Specifies options which govern HTML processing and build behavior.
   *
   * @property debug Whether to enable debug output for HTML processing.
   * @property minify Whether to enable minification for HTML output.
   */
  @JvmRecord public data class HtmlOptions (
    public val debug: Boolean = false,
    public val minify: HtmlMinification = MinifyOptions.defaults(),
  ) {
    /** Methods for obtaining instances of [HtmlBuilder.HtmlOptions]. */
    public companion object {
      /** @return Default suite of HTML options. */
      @JvmStatic public fun defaults(): HtmlOptions = HtmlOptions()
    }
  }

  /**
   * ### HTML Sources
   *
   * Handles state and specification of the sources in HTML which act as inputs for a [HtmlBuild] operation; combined
   * with [HtmlOptions] for building these files, this class constitutes the main inputs for a [HtmlBuild].
   */
  @JvmRecord public data class HtmlSources internal constructor (internal val srcs: List<HtmlSourceMaterial>)

  /**
   * ### HTML Build
   *
   * Represents the state and outcome of a single HTML build operation, bound to the [HtmlOptions] and [HtmlSources]
   * that produced it.
   *
   * @property options The [HtmlOptions] that were used to configure this build.
   * @property sources The [HtmlSources] that were used as inputs for this build.
   */
  public class HtmlBuild internal constructor (
    public val options: HtmlOptions,
    public val sources: HtmlSources,
  )

  /**
   * ### HTML Result
   *
   * Holds the result of a HTML build operation, which includes access to the final built code, so long as there was no
   * error while building.
   */
  public fun interface HtmlResult {
    /**
     * Return the finalized HTML code as a string.
     *
     * @return Finalized HTML code.
     */
    public fun code(): List<String>
  }

  /**
   * Configure a [HtmlBuild] operation, given the [options] and [files] to use as inputs.
   *
   * @param options The [HtmlOptions] to use for this build operation.
   * @param files The [HtmlSourceMaterial]s to use as inputs for this build operation.
   * @return A [HtmlBuild] instance that can be executed via [buildHtml].
   */
  public fun configureHtml(options: HtmlOptions, files: Sequence<HtmlSourceMaterial>): HtmlBuild {
    WebBuilder.load()
    return HtmlBuild(options, HtmlSources(files.toList()))
  }

  /**
   * Execute the build phase of a HTML build operation (via [HtmlBuild]), having previously been configured via the
   * [configureHtml] method.
   *
   * @param build The [HtmlBuild] to execute.
   * @return The resulting [HtmlBuild] after execution.
   */
  public suspend fun buildHtml(html: HtmlBuild, ctx: CoroutineContext = Dispatchers.IO): HtmlResult = withContext(ctx) {
    html.sources.srcs.map { source ->
      source.code()
    }.map { code ->
      async {
        runCatching {
          HtmlMinifier.minify(code, MinifyHtmlConfiguration.Builder().apply {
            setMinifyCss(html.options.minify.css)
            setMinifyJs(html.options.minify.js)
            setKeepComments(!html.options.minify.comments)
            setKeepHtmlAndHeadOpeningTags(true)
            setMinifyDoctype(true)
            setAllowNoncompliantUnquotedAttributeValues(true)
            setKeepClosingTags(true)
            setAllowRemovingSpacesBetweenAttributes(true)
          }.build())
        }
      }
    }.awaitAll().let { results ->
      val diags = Diagnostics.query(true) { true }.toList()
      if (diags.isNotEmpty()) {
        error("HTML builder failed with diagnostics: ${diags.joinToString("\n")}")
      }

      HtmlResult {
        results.map { it.getOrThrow() }
      }
    }
  }
}
