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
package elide.tooling.md

import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.inputStream
import kotlin.text.trimIndent
import elide.tooling.web.WebBuilder
import elide.tooling.web.mdx.MdxBuilder

/**
 * ## Markdown Builder
 *
 * Renders Markdown source strings in various supported flavors to HTML; supported flavors of Markdown include the
 * baseline CommonMark flavor, as well as the GitHub Flavored Markdown (GFM) style.
 *
 * To render Markdown, select a flavor, and prepare a [MarkdownOptions] record which holds the [MarkdownFlavor] selected
 * for use. Then, use the [renderMarkdown] to render a source string to HTML.
 */
public object Markdown {
  /** Default Markdown flavor to use. */
  public val DEFAULT_MARKDOWN_FLAVOR: MarkdownFlavor = MarkdownFlavor.GitHub

  /**
   * ### Markdown Source Material
   *
   * Abstracts where source material for Markdown inputs comes from, via sealed subclasses which inherit various
   * behaviors.
   */
  public sealed interface MarkdownSourceMaterial {
    /**
     * Read the code associated with this source material.
     *
     * @return Markdown code.
     */
    public suspend fun code(): String
  }

  /**
   * ### Markdown Source File
   *
   * Represents a single Markdown source file specification, which knows the [path] the MD file in question and any
   * other inputs or options that may be relevant to this file only.
   *
   * @property path Path to the Markdown source file.
   */
  public interface MarkdownSourceFile : MarkdownSourceMaterial {
    public val path: Path

    override suspend fun code(): String = withContext(Dispatchers.IO) {
      path.inputStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
        reader.readText()
      }
    }
  }

  /**
   * ### Markdown Source Literal
   *
   * Represents a single chunk of Markdown code, expressed as a literal string.
   */
  public fun interface MarkdownSourceLiteral : MarkdownSourceMaterial

  /**
   * ### Markdown Options
   *
   * Specifies options which govern Markdown rendering.
   *
   * @property flavor Markdown flavor to use for rendering.
   * @property trimIndent Whether to trim indentation from Markdown source strings before rendering.
   */
  @JvmRecord public data class MarkdownOptions(
    public val flavor: MarkdownFlavor = DEFAULT_MARKDOWN_FLAVOR,
    public val trimIndent: Boolean = true,
  ) {
    public companion object {
      @JvmStatic public fun defaults(): MarkdownOptions = MarkdownOptions(
        flavor = DEFAULT_MARKDOWN_FLAVOR,
        trimIndent = true,
      )
    }
  }

  /**
   * ### Markdown Sources
   *
   * Handles state and specification of the sources in Markdown for a build operation.
   */
  @JvmRecord public data class MarkdownSources internal constructor (internal val srcs: List<MarkdownSourceMaterial>)

  /**
   * Create a Markdown flavour descriptor for the specified Markdown style.
   *
   * @param style Markdown flavor to use.
   * @return Markdown flavour descriptor for the specified style.
   */
  public fun markdownStyle(style: MarkdownFlavor = DEFAULT_MARKDOWN_FLAVOR): MarkdownFlavourDescriptor? = when (style) {
    MarkdownFlavor.CommonMark -> CommonMarkFlavourDescriptor()
    MarkdownFlavor.GitHub -> GFMFlavourDescriptor()
    else -> null
  }

  /**
   * Render a Markdown source string to HTML.
   *
   * @param style Markdown flavor to use for rendering.
   * @param options Markdown options to use for rendering.
   * @param descriptor Markdown flavour descriptor to use for rendering.
   * @param md Producer which yields the Markdown source string to render.
   * @return Rendered HTML string.
   */
  public suspend fun renderMarkdown(
    style: MarkdownFlavor = DEFAULT_MARKDOWN_FLAVOR,
    options: MarkdownOptions = MarkdownOptions(flavor = style),
    descriptor: MarkdownFlavourDescriptor? = markdownStyle(options.flavor),
    context: CoroutineContext = Dispatchers.IO,
    md: suspend () -> MarkdownSourceMaterial,
  ): String = withContext(context) {
    // mdx requires web builder stuff
    WebBuilder.load()

    val src = md().code().let {
      if (options.trimIndent) it.trimIndent() else it
    }
    when (options.flavor) {
      // for mdx, we use a native parser
      MarkdownFlavor.Mdx -> return@withContext MdxBuilder.renderMdx(
        src = src,
        options = options,
      )

      // for markdown, we use jvm-side libs
      else -> MarkdownParser(requireNotNull(descriptor)).buildMarkdownTreeFromString(src).let { parsedTree ->
        HtmlGenerator(src, parsedTree, descriptor).generateHtml()
      }
    }
  }
}
