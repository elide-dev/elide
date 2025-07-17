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

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.AttributesCustomizer
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.HtmlGenerator.DefaultTagRenderer
import org.intellij.markdown.parser.MarkdownParser
import org.yaml.snakeyaml.Yaml
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.LinkedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.HEAD
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.meta
import kotlinx.html.stream.appendHTML
import kotlinx.html.title
import kotlinx.html.unsafe
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.relativeTo
import kotlin.text.trimIndent
import elide.tooling.md.Markdown.MarkdownOptions
import elide.tooling.web.WebBuilder
import elide.tooling.web.mdx.MdxBuilder

// Constants for Markdown parsing and formatting.
private const val FRONTMATTER_FENCE = "---"

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
   * ### Markdown Frontmatter
   *
   * Describes parsed, or materialized, data as frontmatter for Markdown rendering.
   */
  public sealed interface Frontmatter : Map<String, Any?>

  /**
   * ### Known Frontmatter Properties
   *
   * Provides constants for known frontmatter properties, which are recognized by Elide's Markdown renderer.
   */
  public data object KnownProperties {
    /** Title to use for this page. */
    public const val TITLE: String = "title"

    /** Styles to include on this page. */
    public const val STYLES: String = "styles"

    /** Stylesheets to include on this page (alias for [STYLES]). */
    public const val STYLESHEETS: String = "stylesheets"

    /** Scripts to include on this page. */
    public const val SCRIPTS: String = "styles"
  }

  /**
   * ### Page Frontmatter
   *
   * Describes extended frontmatter for page-like objects, with elevated properties.
   */
  public sealed interface PageFrontmatter : Frontmatter {
    /**
     * Title to use for the page, if specified or defined.
     */
    public val title: String?
  }

  /**
   * ### Rendered Markdown
   *
   * Represents the final rendered forms of Markdown output, potentially including additional metadata.
   */
  public sealed interface RenderedMarkdown {
    /**
     * Return the rendered Markdown as a string.
     *
     * @return Rendered Markdown as a string.
     */
    public fun asString(): String

    /**
     * Return the frontmatter metadata associated with this rendered Markdown, if any.
     *
     * @return [Frontmatter] if metadata is available, or `null` if no metadata is associated with this rendered
     *   Markdown, or if frontmatter support was not enabled.
     */
    public fun metadata(): Frontmatter? = null
  }

  /**
   * ### Rendered Markdown String
   *
   * Simple implementation of [RenderedMarkdown] which holds a rendered Markdown string.
   */
  @JvmInline public value class RenderedMarkdownValue internal constructor(private val str: String) : RenderedMarkdown {
    override fun asString(): String = str
  }

  /**
   * ### Rendered Markdown String
   *
   * Simple implementation of [RenderedMarkdown] which holds a rendered Markdown string.
   */
  @JvmRecord public data class RenderedMarkdownWithMetadata internal constructor (
    private val content: String,
    private val metadata: Frontmatter,
  ) : RenderedMarkdown {
    override fun asString(): String = content
    override fun metadata(): Frontmatter? = metadata
  }

  /**
   * ### Frontmatter Data
   *
   * Regular map-like data, with certain important fields elevated to properties.
   *
   * @property title Optional title for the frontmatter, if specified.
   * @property all All frontmatter data, as a map of key-value pairs.
   */
  internal data class FrontmatterData internal constructor(
    override val title: String? = null,
    private val all: Map<String, Any?> = emptyMap(),
  ) : PageFrontmatter, Map<String, Any?> by all

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

    /**
     * Return the location of this source material, if available.
     *
     * @return [Path] if a location is available, or `null` if no location is specified or available.
     */
    public fun atPath(): Path? = null
  }

  /**
   * ### Markdown Source File
   *
   * Represents a single Markdown source file specification, which knows the path the MD file in question and any other
   * inputs or options that may be relevant to this file only.
   */
  public fun interface MarkdownSourceFile : MarkdownSourceMaterial {
    override fun atPath(): Path

    override suspend fun code(): String = withContext(Dispatchers.IO) {
      atPath().inputStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
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
   * Wrap markdown in the default template.
   *
   * @param metadata Frontmatter to render with, if any.
   * @param body Markdown body source to render in.
   * @param head Optional head block to configure the HTML document head.
   * @return Rendered HTML string, wrapped in a default HTML document template.
   */
  public fun defaultPage(
    metadata: Frontmatter?,
    body: String,
    head: HEAD.() -> Unit = {},
  ): StringBuilder = StringBuilder().appendHTML().html {
    head {
      meta(charset = "UTF-8")
      meta(name = "viewport", content = "width=device-width, initial-scale=1.0")

      (metadata as? PageFrontmatter)?.title?.let {
        title { +it }
      }
      head.invoke(this)
    }
    unsafe {
      raw(body)
    }
  }

  /**
   * ### Markdown Options
   *
   * Specifies options which govern Markdown rendering.
   *
   * @property flavor Markdown flavor to use for rendering.
   * @property trimIndent Whether to trim indentation from Markdown source strings before rendering.
   * @property frontmatter Whether to parse frontmatter from Markdown source strings before rendering.
   * @property titleProvider Optional provider for the page title, if any; this is used to set the `<title>` tag in the
   *   rendered HTML document.
   * @property frontmatterBuilder Optional builder function to apply to the Markdown source string before rendering.
   *   Expected to return frontmatter and remaining code to process, if provided.
   * @property linkRenderer Optional link renderer function to apply to links in the rendered Markdown source string.
   * @property renderer Optional template function to apply to the rendered Markdown source string before returning a
   *   result to the caller; typically used to splice rendered content into a final HTML document.
   */
  @JvmRecord public data class MarkdownOptions(
    public val flavor: MarkdownFlavor = DEFAULT_MARKDOWN_FLAVOR,
    public val trimIndent: Boolean = true,
    public val frontmatter: Boolean = true,
    public val titleProvider: () -> String? = { null },
    public val frontmatterBuilder: (String) -> Pair<String, Frontmatter?> = { frontmatter(it) },
    public val linkRenderer: ((Path?, String, String) -> CharSequence?)? = null,
    public val renderer: (Frontmatter?, StringBuilder) -> StringBuilder = { metadata, str ->
      defaultPage(metadata, str.toString())
    },
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
   * Assemble frontmatter from raw data.
   *
   * @param all Map of frontmatter data, where keys are frontmatter keys and values are frontmatter values.
   * @return [PageFrontmatter] containing the assembled frontmatter data.
   */
  public fun frontmatter(all: Map<String, Any?>): PageFrontmatter = FrontmatterData(
    title = (all[KnownProperties.TITLE] as? String)?.ifEmpty { null }?.ifBlank { null }?.trim(),
    all = all,
  )

  /**
   * Parse Markdown frontmatter from a source string. If no frontmatter is found (expected in YAML), then `null` is
   * returned.
   *
   * It is expected that frontmatter will exist by the time this method is called. Thus, `null` should only be returned
   * for malformed frontmatter, or if no frontmatter is found at all.
   *
   * @param subject Raw content to parse for frontmatter.
   * @return [Frontmatter] if frontmatter is found, or `null` if no frontmatter is found.
   */
  @Suppress("LoopWithTooManyJumpStatements")
  private fun parseFrontmatter(subject: String): Pair<String, Frontmatter?> {
    val lines = subject.lineSequence().iterator()
    var inner = false
    var contentSeen = false
    val builder = StringBuilder()
    return buildMap<String, Any?> {
      val frontmatterLines = LinkedList<String>()
      while (lines.hasNext()) {
        val line = lines.next()
        when {
          // start of frontmatter
          !inner && line.startsWith(FRONTMATTER_FENCE) -> {
            inner = true
            continue
          }

          // end of frontmatter
          inner && line.startsWith(FRONTMATTER_FENCE) -> {
            inner = false
            continue
          }

          // we are inside the frontmatter block
          inner -> {
            frontmatterLines.add(line)
            continue
          }

          else -> if (contentSeen) builder.appendLine(line) else {
            if (line.isNotBlank() && line.isNotEmpty()) {
              contentSeen = true // we have seen content, so we can stop parsing frontmatter
              builder.appendLine(line)
            } else {
              continue // skip empty lines until we see initial content
            }
          }
        }
      }

      // we have gathered frontmatter lines; parse them into properties
      if (frontmatterLines.isNotEmpty()) {
        val yaml = Yaml()
        yaml.load<Map<String, Any?>>(frontmatterLines.joinToString("\n"))?.let { parsed ->
          putAll(parsed)
        }
      }
    }.let { metadata ->
      when (metadata.isEmpty()) {
        true -> builder.toString() to null
        false -> builder.toString() to frontmatter(metadata)
      }
    }
  }

  /**
   * Assemble frontmatter by parsing code; this operation is **consumptive**, in that it will trim the frontmatter from
   * the top of the source code. The returned pair contains the remaining source code and the parsed data.
   *
   * @param subject Raw content to parse for frontmatter.
   * @return Pair of: (1) remaining code, and, (2) [Frontmatter], if any.
   */
  public fun frontmatter(subject: String): Pair<String, Frontmatter?> {
    var foundFrontmatter = false
    for (line in subject.lineSequence()) {
      when {
        line.isEmpty() || line.isBlank() -> continue  // skip initial empty lines, if any
        else -> {
          foundFrontmatter = line.startsWith(FRONTMATTER_FENCE)
          break
        }
      }
    }
    return when (foundFrontmatter) {
      false -> subject to null
      else -> parseFrontmatter(subject)
    }
  }

  // Render markdown with the specified descriptor, source string, and options.
  private fun renderMarkdown(
    use: MarkdownFlavourDescriptor,
    src: String,
    options: MarkdownOptions,
    frontmatter: Frontmatter? = null,
    location: Path? = null,
  ): RenderedMarkdown = MarkdownParser(use).buildMarkdownTreeFromString(src).let { parsedTree ->
    val generator = HtmlGenerator(src, parsedTree, use)
    val renderer = DefaultTagRenderer(
      includeSrcPositions = false,
      customizer = object: AttributesCustomizer {
        override fun invoke(node: ASTNode, tag: CharSequence, attrs: Iterable<CharSequence?>): Iterable<CharSequence?> {
          return when (tag) {
            "a" -> options.renderLinkAttrs(location, attrs)
            else -> attrs
          }
        }
      }
    )

    options.renderer(frontmatter, StringBuilder(
      generator.generateHtml(renderer)
    )).let { builder ->
      when (frontmatter) {
        null -> RenderedMarkdownValue(builder.toString())
        else -> RenderedMarkdownWithMetadata(builder.toString(), frontmatter)
      }
    }
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
  ): RenderedMarkdown = withContext(context) {
    // mdx requires web builder stuff
    WebBuilder.load()

    val item = md()
    val loc = item.atPath()
    val src = item.code().let {
      if (options.trimIndent) it.trimIndent() else it
    }
    when (options.flavor) {
      // for mdx, we use a native parser
      MarkdownFlavor.Mdx -> return@withContext MdxBuilder.renderMdx(
        src = src,
        options = options,
      ).let { rendered ->
        RenderedMarkdownValue(rendered)
      }

      // for markdown, we use jvm-side libs
      else -> when (options.frontmatter) {
        // there is no front-matter, so we don't need to swap out the markdown code we parse
        false -> renderMarkdown(requireNotNull(descriptor), src, options, location = loc)

        // there is front-matter, so make sure the code we pass is front-matter free (the underlying jetbrains markdown
        // parser doesn't handle front-matter for us)
        else -> options.frontmatterBuilder(src).let { (code, frontmatter) ->
          renderMarkdown(
            requireNotNull(descriptor),
            code,
            options,
            frontmatter,
            location = loc,
          )
        }
      }
    }
  }
}

private fun MarkdownOptions.renderLink(referrer: Path?, href: String): CharSequence = when {
  referrer != null -> referrer.parent.resolve(href).normalize().relativeTo(referrer.parent).let { a ->
    when {
      a.extension == "md" -> buildString {
        append(a.toString().removeSuffix(".md"))
        append(".html")
      }

      else -> a.toString()
    }
  }
  else -> href
}.let {
  when (val linker = linkRenderer) {
    null -> it
    else -> linker.invoke(referrer, href, it) ?: it
  }
}

private fun MarkdownOptions.renderLinkAttrs(referrer: Path?, attrs: Iterable<CharSequence?>): Iterable<CharSequence?> {
  val res = attrs.map { attr ->
    val prop = (attr as? String)?.substringBefore("=")?.removePrefix("\"")
    when (prop) {
      null -> null
      else -> when (prop) {
        "href" -> renderLink(referrer, attr.removePrefix("href=\"").removeSuffix("\""))
          .let { rendered -> "href=\"$rendered\"" }
        else -> attr
      }
    }
  }
  return res
}
