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
@file:Suppress("UnstableApiUsage")

package elide.tooling.web

import org.apache.commons.compress.archivers.zip.Zip64Mode
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.LinkedList
import java.util.concurrent.ConcurrentSkipListMap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.html.link
import kotlinx.html.script
import kotlin.collections.ifEmpty
import kotlin.io.path.absolute
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import elide.exec.ActionScope
import elide.exec.Result
import elide.exec.Task
import elide.exec.Task.Companion.fn
import elide.exec.asExecResult
import elide.exec.taskDependencies
import elide.runtime.Logging
import elide.runtime.lang.javascript.JavaScriptCompilerConfig
import elide.runtime.lang.javascript.JavaScriptPrecompiler
import elide.runtime.precompiler.Precompiler.PrecompileSourceInfo
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest
import elide.runtime.typescript.TypeScriptPrecompiler
import elide.tooling.archive.ZipTasks.zip
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.config.BuildConfigurator.BuildConfiguration
import elide.tooling.md.Markdown
import elide.tooling.md.Markdown.MarkdownOptions
import elide.tooling.md.Markdown.defaultPage
import elide.tooling.md.MarkdownFlavor
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.SourceFilePath
import elide.tooling.project.SourceSet
import elide.tooling.project.SourceSetLanguage
import elide.tooling.project.SourceSetType
import elide.tooling.project.SourceSets
import elide.tooling.project.manifest.ElidePackageManifest.StaticSite
import elide.tooling.web.css.CssBuilder
import elide.tooling.web.html.HtmlBuilder
import elide.tooling.web.html.HtmlBuilder.buildHtml
import elide.tooling.web.html.HtmlBuilder.configureHtml
import elide.util.toBase64String

// Constants for the site builder.
private const val STATIC_ASSETS_PATH_DEFAULT = "assets"
private const val STATIC_SITE_ARTIFACT_PATH = "sites"
private const val STYLESHEET = "stylesheet"
private const val STYLESHEETS = "stylesheets"
private const val SCRIPT = "script"
private const val SCRIPTS = "scripts"
private const val TYPE_TEXT_CSS = "text/css"
private const val TYPE_TEXT_JAVASCRIPT = "text/javascript"
private const val TYPE_MODULE = "module"
private const val WEB_SLASH = "/"
private const val DEFAULT_ASSET_IDENTITY_SIZE: UShort = 8u
private const val DEFAULT_ASSET_IDENTITY_ALGO = "SHA-1"

private interface Checkable {
  fun check(): String?
}

// Models a pair of paths: a source and target; used as an abstract concept within this configurator.
private sealed interface SourceTargetPair: Checkable {
  val source: Path
  val target: Path

  override fun check(): String? = when {
    !source.exists() -> "Source path does not exist: $source"
    else -> null
  }
}

// Models a pair of directories: a source root and target root.
@JvmInline private value class SourceTargetDirs private constructor (
  private val pair: Pair<Path, Path>
): SourceTargetPair {
  override val source: Path get() = pair.first
  override val target: Path get() = pair.second

  companion object {
    // Factory method to create a new source-target pair.
    @JvmStatic fun of(source: Path, target: Path): SourceTargetPair = SourceTargetDirs(source to target)
  }
}

// Models a pair of files: a source file and target file, which is a static asset or other unidentified file.
@JvmRecord private data class SourceTargetFile private constructor (
  override val source: Path,
  override val target: Path,
  val sourceSet: SourceSet,
): SourceTargetPair {
  companion object {
    // Factory method to create a new source-target pair for a regular file.
    @JvmStatic fun of(
      source: Path,
      target: Path,
      sourceSet: SourceSet,
    ): SourceTargetFile = SourceTargetFile(source, target, sourceSet)
  }

  fun withTarget(target: Path): SourceTargetFile = SourceTargetFile(
    source = source,
    target = target,
    sourceSet = sourceSet,
  )
}

// Models a pair of files: a source file and target file.
@JvmRecord private data class SourceTargetCode private constructor (
  val lang: SourceSetLanguage,
  override val source: Path,
  override val target: Path,
  val sourceSet: SourceSet,
  val sourceFile: SourceFilePath,
): SourceTargetPair {
  companion object {
    // Factory method to create a new source-target pair for a single source->target.
    @JvmStatic fun of(
      lang: SourceSetLanguage,
      source: Path,
      target: Path,
      sourceSet: SourceSet,
      file: SourceFilePath,
    ): SourceTargetCode = SourceTargetCode(lang, source, target, sourceSet, file)
  }

  fun withTarget(target: Path): SourceTargetCode = SourceTargetCode(
    lang = lang,
    source = source,
    target = target,
    sourceSet = sourceSet,
    sourceFile = sourceFile,
  )
}

// Implements builds for static sites, which show up as `ElidePackageManifest.StaticSite` artifacts.
internal class StaticSiteContributor : BuildConfigurator {
  private companion object {
    @JvmStatic private val logging by lazy {
      Logging.of(StaticSiteContributor::class.java)
    }

    private val assetCache by lazy {
      ConcurrentSkipListMap<Path, Path>()
    }
  }

  // Holds information about an asset, including its calculated hash (identity).
  @OptIn(ExperimentalStdlibApi::class)
  private class AssetIdentity(
    val asset: SourceTargetPair,
    private val identityBytes: ByteArray,
  ) {
    fun asBase64(): String = identityBytes.toBase64String()
    fun asHex(): String = identityBytes.toHexString()
  }

  // Describes settings which govern how assets are addressed and written.
  private sealed interface AssetIdentityConfiguration {
    /**
     * Generate a finalized encoded asset identity from the provided source code [info] and [content].
     *
     * @param info Info about this source code.
     * @param content Content of the source code.
     */
    fun encodedAssetIdentity(info: SourceTargetPair, content: ByteArray): String? = null

    /**
     * Generate a finalized encoded asset identity from the provided source code [info].
     *
     * @param info Info about this source code.
     * @return Asset identity (just the identity portion).
     */
    fun encodedAssetIdentity(info: SourceTargetPair): String? = encodedAssetIdentity(
      info,
      info.source.inputStream().buffered().use { reader ->
        reader.readBytes()
      },
    )

    /**
     * Generate an asset identity from the provided source code [info] and [content].
     *
     * @param info Info about this source code.
     * @param content Content of the source code.
     * @return Asset identity (just the identity portion).
     */
    fun assetIdentity(info: SourceTargetPair, content: ByteArray): AssetIdentity? = null

    /**
     * Generate an asset identity from the provided source code [info].
     *
     * @param info Info about this source code.
     * @return Asset identity (just the identity portion).
     */
    suspend fun assetIdentity(info: SourceTargetPair): AssetIdentity? = assetIdentity(
      info,
      info.source.inputStream().buffered().use { reader ->
        reader.readBytes()
      },
    )
  }

  // Don't change how assets are addressed.
  private data object UnchangedAssetReferences : AssetIdentityConfiguration

  // Use some identity-driven way of addressing assets.
  private sealed class AssetsByIdentity (private val hashAlgorithm: String) : AssetIdentityConfiguration {
    override fun assetIdentity(info: SourceTargetPair, content: ByteArray): AssetIdentity = MessageDigest.getInstance(
      hashAlgorithm
    ).let { digester ->
      digester.update(content)
      AssetIdentity(info, digester.digest())
    }
  }

  // Use base64 encoded identity with a fixed size and algorithm.
  private class AssetsByHexIdentity (
    private val size: UShort = DEFAULT_ASSET_IDENTITY_SIZE,
    algo: String = DEFAULT_ASSET_IDENTITY_ALGO,
  ) : AssetsByIdentity(algo) {
    override fun encodedAssetIdentity(info: SourceTargetPair, content: ByteArray): String? {
      return assetIdentity(info, content).asHex().takeLast(size.toInt())
    }
  }

  // Describes settings which govern how assets are handled.
  @JvmRecord private data class AssetConfiguration(
    val enabled: Boolean = true,
    val references: AssetIdentityConfiguration = if (enabled) AssetsByHexIdentity() else UnchangedAssetReferences,
  )

  // Describes resolved settings for a static site build.
  @JvmRecord private data class StaticSiteConfiguration(
    val name: String,
    val siteRoot: SourceTargetPair,
    val assetsRoot: SourceTargetPair,
    val srcs: List<SourceSet>,
    val deps: List<Task>,
    val site: StaticSite,
    val state: ElideBuildState,
    val config: BuildConfiguration,
    val scope: ActionScope,
    val project: ElideConfiguredProject,
    val dry: Boolean,
    val assets: AssetConfiguration = AssetConfiguration(),
    val rewriteLinks: Boolean = true,
    val minifyHtml: Boolean = true,
  ) {
    val taskGraph get() = config.taskGraph
    val allSrcs: Sequence<Pair<SourceSet, SourceFilePath>> get() = sequence {
      srcs.flatMap { sourceSet ->
        sourceSet.paths.map { path ->
          yield(sourceSet to path)
        }
      }
    }

    fun check(): String? = (
      siteRoot.check()
    )

    fun srcsOfType(lang: SourceSetLanguage): List<Pair<SourceSet, SourceFilePath>> = sequence {
      yieldAll(
        allSrcs
          .filter { it.first.type == SourceSetType.Sources }
          .filter { it.second.lang == lang }
      )
    }.toList()

    fun srcsOfUndefinedType(): List<Pair<SourceSet, SourceFilePath>> = sequence {
      yieldAll(
        allSrcs
          .filter { it.first.type == SourceSetType.Sources }
          .filter { it.second.lang == null }
      )
    }.toList()
  }

  // Find source sets containing web content.
  private fun SourceSets.webSourceSets(state: ElideBuildState, root: Path? = null): Sequence<SourceSet> = sequence {
    yieldAll(state.project.sourceSets.find(
      SourceSetLanguage.HTML,
      SourceSetLanguage.Markdown,
      SourceSetLanguage.MDX,
      SourceSetLanguage.CSS,
      SourceSetLanguage.SCSS,
      SourceSetLanguage.JavaScript,
      SourceSetLanguage.TypeScript,
      SourceSetLanguage.JSX,
      SourceSetLanguage.TSX,
      SourceSetLanguage.JSON,
      SourceSetLanguage.SVG,
    ).let {
      when (val rootPath = root) {
        null -> it
        else -> state.project.root.absolute().resolve(rootPath.relativeTo(state.project.root)).let { absRoot ->
          it.filter { sourceSet ->
            sourceSet.paths.all { candidate ->
              candidate.path.absolute().startsWith(absRoot)
            }
          }
        }
      }
    })
  }

  // Create a configuration for building a static site, given a set of dependencies and source sets.
  private fun ActionScope.staticSiteConfig(
    name: String,
    deps: List<Task>,
    srcs: List<SourceSet>,
    site: StaticSite,
    siteRoot: SourceTargetPair,
    assetsRoot: SourceTargetPair,
    state: ElideBuildState,
    config: BuildConfiguration,
  ): StaticSiteConfiguration = StaticSiteConfiguration(
    name = name,
    siteRoot = siteRoot,
    assetsRoot = assetsRoot,
    deps = deps,
    site = site,
    srcs = srcs,
    state = state,
    config = config,
    dry = config.settings.dry,
    project = state.project,
    scope = this,
  )

  private suspend fun StaticSiteConfiguration.createParentsIfNeeded(path: Path) = path.parent.let { parent ->
    // sanity check: should always be within site root, should always be directory
    assert(!parent.isRegularFile()) { "Cannot create parents for non-directory" }
    assert(!parent.name.contains('.')) { "This looks like a file" }

    when (parent.isAbsolute) {
      true -> parent.startsWith(siteRoot.target.absolute())
      false -> parent.startsWith(siteRoot.target) // relative paths are always relative to the site root
    }.let {
      require(it) { "Cannot create directories outside of site root" }
    }

    if (!parent.exists()) withContext(IO) {
      Files.createDirectories(parent)
    }
  }

  private suspend fun compareFilesForCopy(left: Path, right: Path): Boolean = withContext(IO) {
    !Files.exists(left) || !Files.exists(right) || !Files.isRegularFile(left) || !Files.isRegularFile(right) ||
      Files.size(left) != Files.size(right) || Files.getLastModifiedTime(left) != Files.getLastModifiedTime(right)
  }

  private fun StaticSiteConfiguration.assembleIdentityAssetPath(target: Path, identity: String): Path {
    val name = target.nameWithoutExtension
    val ext = target.extension
    val base = target.parent
    return base.resolve(buildString {
      append(name)
      append('.')
      append(identity)
      append('.')
      append(ext)
    })
  }

  private fun StaticSiteConfiguration.computeAssetIdentity(src: SourceTargetPair, bytes: ByteArray): Path? {
    return when (val cached = assetCache[src.source]) {
      null -> assets.references.encodedAssetIdentity(src, bytes)?.let { encoded ->
        assembleIdentityAssetPath(src.target, encoded).also {
          assetCache[src.source] = it
        }
      }
      else -> cached
    }
  }

  private suspend fun StaticSiteConfiguration.copyToTarget(src: SourceTargetPair) = when (dry) {
    true -> logging.info { "Copy (dry run): ${src.source} → ${src.target}" }

    false -> withContext(IO) {
      runCatching {
        val absTarget = src.target.absolute()
        val exists = absTarget.exists() && absTarget.isRegularFile()
        val doCopy = !exists || compareFilesForCopy(src.source, absTarget)

        if (!doCopy) logging.debug {
          "Skipping copy because files are already identical"
        } else createParentsIfNeeded(src.target).also {
          if (exists) {
            Files.copy(src.source, src.target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
          } else {
            Files.copy(src.source, src.target)
          }
        }
      }.onFailure {
        logging.error("Failed to copy source to target: ${src.source} → ${src.target}", it)
      }
    }
  }

  private suspend fun StaticSiteConfiguration.write(src: SourceTargetPair, str: String, asset: Boolean) = when (dry) {
    true -> logging.info { "Write (dry run): ${src.source} → ${src.target} (size: ${str.length})" }

    false -> withContext(IO) {
      val bytes = str.toByteArray(StandardCharsets.UTF_8)
      val finalizedTarget = when (asset) {
        // non-assets do not get transformed by identity
        false -> src.target

        // if identity is enabled, a non-null value is returned
        else -> when (val identity = computeAssetIdentity(src, bytes)) {
          null -> src.target  // no identity-based reference for asset, so just include it as-is
          else -> identity
        }
      }

      runCatching {
        createParentsIfNeeded(finalizedTarget).also {
          finalizedTarget.outputStream().bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(str)
          }
        }
      }.onFailure {
        logging.error("Failed to write to target: ${src.target}", it)
      }
    }
  }

  private suspend fun StaticSiteConfiguration.buildHtmlFile(src: SourceTargetCode): Result = when (minifyHtml) {
    // with no post-processing, we just copy the source to the target
    false -> Result.Nothing.also { copyToTarget(src) }

    // otherwise, we minify the HTML and write it to the target
    else -> runCatching {
      with(HtmlBuilder) {
        buildHtml(configureHtml(HtmlBuilder.HtmlOptions.defaults(), sequence {
          yield(HtmlBuilder.HtmlSourceFile { src.source })
        })).let { builtCss ->
          write(
            src,
            builtCss.code().single(),
            asset = false,
          )
        }
      }
    }.asExecResult()
  }

  @Suppress("UNUSED_PARAMETER")
  private fun StaticSiteConfiguration.assetHref(
    from: SourceTargetCode,
    asset: String,
    src: String,
  ): String = buildString {
    append(site.prefix)
    when (assets.references) {
      is AssetsByIdentity -> from.source.parent.resolve(src).let { assetPathFromSource ->
        requireNotNull(assetCache[assetPathFromSource]?.relativeTo(siteRoot.target.absolute())) {
          "Failed to locate in asset cache: ${from.source}"
        }
      }

      else -> assetsRoot.target.resolve(asset).relativeTo(siteRoot.target)
    }.let { maybeRewritten ->
      append(maybeRewritten)
    }
  }

  private fun tsToJsName(name: String): String = buildString {
    name.substringBeforeLast('.').let {
      append(it)
      if (it.endsWith(".ts") || it.endsWith(".cts")) {
        append(".js")
      } else {
        append(".mjs")
      }
    }
  }

  private fun rewriteExtension(href: String): String = when ('.' in href) {
    false -> href
    else -> buildString {
      append(href.substringBeforeLast('.'))
      when (val ext = href.substringAfterLast('.')) {
        "ts" -> append(".js")
        "mts" -> append(".mjs")
        "tsx" -> append(".mjs")
        "jsx" -> append(".mjs")
        "md" -> append(".html")
        "scss" -> append(".css")
        else -> append('.').append(ext) // keep the original extension
      }
    }
  }

  private suspend fun StaticSiteConfiguration.buildMarkdownFile(src: SourceTargetCode): Result = runCatching {
    Markdown.renderMarkdown(
      style = MarkdownFlavor.GitHub,
      options = MarkdownOptions.defaults().copy(
        linkRenderer = { path, href, default ->
          when (rewriteLinks) {
            // don't rewrite links unless configured to do so
            false -> default

            // otherwise, reparent links based on the prefix
            true -> when (site.prefix) {
              WEB_SLASH -> if (href.startsWith(WEB_SLASH)) rewriteExtension(default) else buildString {
                append(WEB_SLASH)
                append(rewriteExtension(default))
              }
              else -> buildList<String> {
                add("")
                addAll(site.prefix.split(WEB_SLASH))
                addAll(default.removePrefix(WEB_SLASH).split(WEB_SLASH))
              }.filter {
                !it.isEmpty() && !it.isBlank() && it != "."
              }.joinToString(
                WEB_SLASH
              )
            }
          }
        },
        renderer = { metadata, str ->
          defaultPage(metadata, str.toString()) {
            // configures the `<head>` of the page
            sequence {
              // site-wide stylesheets
              yieldAll(site.stylesheets)

              // page-level stylesheets
              (
                metadata?.get(Markdown.KnownProperties.STYLES) ?:
                metadata?.get(Markdown.KnownProperties.STYLESHEETS)
              )?.let { stylesheets ->
                when (stylesheets) {
                  is String -> listOf(stylesheets)
                  is List<*> -> stylesheets.filterIsInstance<String>()
                  else -> emptyList()
                }
              }?.let { stylesheets ->
                yieldAll(stylesheets)
              }
            }.forEach { stylesheet ->
              link(
                rel = STYLESHEET,
                type = TYPE_TEXT_CSS,
                href = assetHref(src, rewriteExtension(stylesheet), stylesheet)
              )
            }
            sequence {
              // site-wide scripts
              yieldAll(site.scripts)

              // page-level scripts
              metadata?.get(Markdown.KnownProperties.SCRIPTS)?.let { scripts ->
                when (scripts) {
                  is String -> listOf(scripts)
                  is List<*> -> scripts.filterIsInstance<String>()
                  else -> emptyList()
                }
              }?.let { scripts ->
                yieldAll(scripts)
              }
            }.forEach { script ->
              val isModule = script.endsWith(".mjs") || script.endsWith(".mts")
              val type = if (isModule) TYPE_MODULE else TYPE_TEXT_JAVASCRIPT
              script(type = type, src = assetHref(src, tsToJsName(script), script)) {
                if (!isModule) defer = true
              }
            }
          }
        },
      ),
    ) {
      Markdown.MarkdownSourceFile { src.source }
    }.let { rendered ->
      // now, pass it through the HTML builder, if so inclined
      when (minifyHtml) {
        false -> rendered.asString()
        true -> buildHtml(configureHtml(HtmlBuilder.HtmlOptions.defaults(), sequence {
          yield(HtmlBuilder.HtmlSourceLiteral { rendered.asString() })
        })).code().single()
      }.let { maybeMinified ->
        write(
          src.withTarget(src.target.parent.resolve(buildString {
            append(src.source.nameWithoutExtension)
            append(".html")
          })),
          maybeMinified,
          asset = false,
        )
      }
    }
  }.asExecResult()

  private suspend fun StaticSiteConfiguration.buildCssFile(scss: Boolean, src: SourceTargetCode): Result = runCatching {
    with(CssBuilder) {
      buildCss(configureCss(CssBuilder.CssOptions.forProject(project).copy(scss = scss), sequence {
        yield(CssBuilder.CssSourceFile { src.source })
      })).let { builtCss ->
        write(
          if (!scss) src else src.withTarget(
            src.target.parent.resolve(buildString {
              append(src.source.nameWithoutExtension)
              append(".css")
            })
          ),
          builtCss.code().single(),
          asset = true,
        )
      }
    }
  }.asExecResult()

  private suspend fun StaticSiteConfiguration.buildJsFile(src: SourceTargetCode): Result = runCatching {
    val sourceAbsolute = src.source.absolute()
    sourceAbsolute.bufferedReader(StandardCharsets.UTF_8).use { reader ->
      JavaScriptPrecompiler.precompile(
        PrecompileSourceRequest(
          source = PrecompileSourceInfo(name = src.source.name, path = sourceAbsolute),
          config = JavaScriptCompilerConfig.DEFAULT,
        ),
        reader.readText(),
      )
    }.let { compiled ->
      write(
        src,
        requireNotNull(compiled) { "Failed to build JavaScript at src '$sourceAbsolute'" },
        asset = true,
      )
    }
  }.asExecResult()

  private suspend fun StaticSiteConfiguration.buildTsFile(src: SourceTargetCode): Result = runCatching {
    val sourceAbsolute = src.source.absolute()
    sourceAbsolute.bufferedReader(StandardCharsets.UTF_8).use { reader ->
      TypeScriptPrecompiler.precompile(
        PrecompileSourceRequest(
          source = PrecompileSourceInfo(name = src.source.name, path = sourceAbsolute),
          config = JavaScriptCompilerConfig.DEFAULT,
        ),
        reader.readText(),
      )
    }.let { compiled ->
      write(
        src,
        requireNotNull(compiled) { "Failed to build TypeScript at src '$sourceAbsolute'" },
        asset = true,
      )
    }
  }.asExecResult()

  private suspend fun StaticSiteConfiguration.taskForSources(
    allSrcs: Map<Path, SourceTargetCode>,
    lang: SourceSetLanguage,
    taskBuilder: suspend ActionScope.(Map<Path, SourceTargetCode>) -> Task?,
  ): Task? = with(config.taskGraph) {
    allSrcs.filter { it.value.lang == lang }.ifEmpty { null }?.let { srcs ->
      taskBuilder.invoke(scope, srcs)
    }?.also { task ->
      addNode(task)
    }
  }

  private suspend fun StaticSiteConfiguration.buildWebSourcesFileWise(
    allSrcs: Map<Path, SourceTargetCode>,
    lang: SourceSetLanguage,
    tag: String = lang.formalName,
    label: Pair<String, String>? = null,
    builder: suspend StaticSiteConfiguration.(Path, SourceTargetCode) -> Result,
  ): Task? = with(scope) {
    taskForSources(allSrcs, lang) { srcs ->
      fn(name = "${name}${tag[0].uppercaseChar()}${tag.substring(1)}") {
        srcs.map { src ->
          builder(src.key, src.value)
        }.let { srcs ->
          when (srcs.any { !it.isSuccess }) {
            true -> when (val exc = srcs.firstNotNullOfOrNull { it.exceptionOrNull() }) {
              null -> Result.UnspecifiedFailure
              else -> Result.ThrowableFailure(exc)
            }
            false -> Result.Nothing
          }
        }
      }.describedBy {
        val pluralized = when (label) {
          null -> if (srcs.size == 1) "$tag file" else "$tag sources"
          else -> label.let { (singular, plural) ->
            if (srcs.size == 1) singular else plural
          }
        }
        val siteLabel = if (name != "main") " (site: $name)" else ""
        "Building ${srcs.size} ${pluralized}${siteLabel}"
      }
    }
  }

  // Create and connect tasks together to build a static site.
  private suspend fun ActionScope.build(site: StaticSiteConfiguration) = site.taskGraph.apply {
    // gather sources which compile to a single target
    val html = site.srcsOfType(SourceSetLanguage.HTML).map { (SourceSetLanguage.HTML as SourceSetLanguage) to it }
    val md = site.srcsOfType(SourceSetLanguage.Markdown).map { (SourceSetLanguage.Markdown as SourceSetLanguage) to it }
    val css = site.srcsOfType(SourceSetLanguage.CSS).map { (SourceSetLanguage.CSS as SourceSetLanguage) to it }
    val scss = site.srcsOfType(SourceSetLanguage.SCSS).map { (SourceSetLanguage.SCSS as SourceSetLanguage) to it }
    val js = site.srcsOfType(SourceSetLanguage.JavaScript).map {
      (SourceSetLanguage.JavaScript as SourceSetLanguage) to it
    }
    val ts = site.srcsOfType(SourceSetLanguage.TypeScript).map {
      (SourceSetLanguage.TypeScript as SourceSetLanguage) to it
    }
    val anythingElse = site.srcsOfUndefinedType()

    val allSrcs: PersistentMap<Path, SourceTargetCode> = listOf(
      html,
      md,
      css,
      scss,
      js,
      ts,
    ).asSequence().flatten().map { (lang, src) ->
      SourceTargetCode.of(
        lang = lang,
        source = src.second.path.absolute(),
        target = site.siteRoot.target.resolve(src.second.path.relativeTo(site.siteRoot.source)),
        sourceSet = src.first,
        file = src.second,
      )
    }.associateBy {
      it.source
    }.toSortedMap().toPersistentHashMap()

    val siteRootAbsolute = site.siteRoot.target.absolute()
    if (!siteRootAbsolute.exists()) withContext(IO) {
      Files.createDirectories(siteRootAbsolute)
    }

    val deps = buildList<Task> {
      // do we have css?
      site.buildWebSourcesFileWise(allSrcs, SourceSetLanguage.CSS, label = STYLESHEET to STYLESHEETS) { _, src ->
        buildCssFile(scss = false, src.withTarget(
          assetsRoot.target.absolute().resolve(src.source.absolute().relativeTo(assetsRoot.source.absolute()))
        ))
      }?.let {
        add(it)
      }

      // how about scss, which must be precompiled?
      site.buildWebSourcesFileWise(allSrcs, SourceSetLanguage.SCSS) { _, src ->
        buildCssFile(scss = true, src.withTarget(
          assetsRoot.target.absolute().resolve(src.source.absolute().relativeTo(assetsRoot.source.absolute()))
        ))
      }?.let {
        add(it)
      }

      // js and ts are more complex, so we handle them last and separately.
      site.buildWebSourcesFileWise(allSrcs, SourceSetLanguage.JavaScript, label = SCRIPT to SCRIPTS) { _, src ->
        buildJsFile(src.withTarget(
          assetsRoot.target.absolute().resolve(src.source.absolute().relativeTo(assetsRoot.source.absolute()))
        ))
      }?.let {
        add(it)
      }
      site.buildWebSourcesFileWise(allSrcs, SourceSetLanguage.TypeScript) { _, src ->
        buildTsFile(src.withTarget(
          assetsRoot.target.absolute().resolve(src.source.absolute().relativeTo(assetsRoot.source.absolute()))
            .parent
            .resolve(tsToJsName(src.source.name))
        ))
      }?.let {
        add(it)
      }

      // are there any other sources? if so, they are copied as-is.
      anythingElse.ifEmpty { null }?.let { extraCopiedSrcs ->
        fn(name = "${site.name}CopyExtraSrcs") {
          runCatching {
            extraCopiedSrcs.forEach { (srcSet, copiedSrc) ->
              site.copyToTarget(SourceTargetFile.of(
                copiedSrc.path,
                site.siteRoot.target.resolve(copiedSrc.path.relativeTo(site.siteRoot.source)),
                srcSet,
              ))
            }
          }.asExecResult()
        }.describedBy {
          val pluralized = when (extraCopiedSrcs.size) {
            1 -> "source file"
            else -> "sources"
          }
          "Copying ${extraCopiedSrcs.size} extra $pluralized"
        }
      }?.let {
        addNode(it)
        add(it)
      }
    }

    val primaries = buildList<Task> {
      // do we have html?
      site.buildWebSourcesFileWise(allSrcs, SourceSetLanguage.HTML) { _, src ->
        buildHtmlFile(src)
      }?.let {
        // add html to graph, build it after all other assets
        add(it)
        deps.forEach { dep -> putEdge(it, dep) }
      }

      // do we have markdown or mdx?
      site.buildWebSourcesFileWise(allSrcs, SourceSetLanguage.Markdown) { _, src ->
        buildMarkdownFile(src)
      }?.let {
        // add markdown to graph, build it after all other assets
        add(it)
        deps.forEach { dep -> putEdge(it, dep) }
      }
    }

    // then, organize a final task to assemble the site
    fn(site.name, taskDependencies(site.deps + deps + primaries)) {
      zip(site.siteRoot.target.parent.resolve("${site.name}.zip"), configure = {
        setUseZip64(Zip64Mode.AlwaysWithCompatibility)
        setComment("Static site named '${site.name}'")
        setMethod(ZipArchiveEntry.DEFLATED)
        setLevel(6) // maximum compression
      }) {
        Files.walk(site.siteRoot.target).forEach { file ->
          if (file.isDirectory()) return@forEach // skip directories
          val relativePath = site.siteRoot.target.relativize(file)
          packFile(file, relativePath.toString())
        }
      }
    }.describedBy {
      "Packing static site '${site.name}'"
    }.also { siteBuild ->
      addNode(siteBuild)
      site.deps.forEach { putEdge(siteBuild, it) }
    }
  }

  /**
   * ### Static Site Task
   *
   * Create build tasks to build a static site to a directory layout.
   *
   * @param name Name of the task to create.
   * @param state Current build state.
   * @param config Build configuration.
   * @param artifact The [StaticSite] artifact to build.
   * @param dependencies Optional list of tasks that this task depends on.
   * @return A new task that builds a static website to a target directory.
   */
  @Suppress("UNUSED_PARAMETER")
  private suspend fun ActionScope.staticSite(
    name: String,
    state: ElideBuildState,
    config: BuildConfiguration,
    artifact: StaticSite,
    dependencies: List<Task> = emptyList(),
  ) {
    WebBuilder.load()

    // build paths for outputs
    val siteRoot = state.layout.artifacts  // `./.dev/artifacts/`
      .resolve(STATIC_SITE_ARTIFACT_PATH)  // `/sites/`
      .resolve(name)  // artifact name

    // asset root where static assets will be placed
     val assetsRoot = siteRoot  // `./.dev/artifacts/sites/<name>/`
      .resolve(artifact.assets?.removePrefix("/") ?: STATIC_ASSETS_PATH_DEFAULT)  // `/assets/` or custom path

    // resolve the source root for the site, if specified
    val siteSourceRoot = artifact.srcs.let { state.layout.projectRoot.resolve(it) }
    val assetsSourceRoot = siteSourceRoot  // assets are always within the site source root for now
    // val assetsSourceRoot =
    //   (artifact.assets ?: STATIC_ASSETS_PATH_DEFAULT).let { state.layout.projectRoot.resolve(it) }

    // build a suite of tasks to compile each asset type
    val deps = LinkedList<Task>()
    val srcs = state.project.sourceSets.webSourceSets(state, siteSourceRoot).toList()

    when {
      // with no sources, we have nothing to build
      srcs.isEmpty() -> logging.warn {
        "No source sets resolved for static web artifact '$name' (nothing to be done)"
      }

      // otherwise, build a static site configuration from sources, and then wire tasks together to build it
      else -> build(staticSiteConfig(
        name = name,
        deps = deps,
        srcs = srcs,
        site = artifact,
        siteRoot = SourceTargetDirs.of(siteSourceRoot, siteRoot),
        assetsRoot = SourceTargetDirs.of(assetsSourceRoot, assetsRoot),
        state = state,
        config = config,
      ).also {
        it.check()?.let { err ->
          error("Invalid static site configuration for '$name': $err")
        }
      })
    }
  }

  override suspend fun contribute(state: ElideBuildState, config: BuildConfiguration) {
    state.manifest.artifacts.entries.filter { it.value is StaticSite }.forEach { (name, artifact) ->
      with(config.actionScope) {
        with(config.taskGraph) {
          staticSite(
            name = name,
            state = state,
            config = config,
            artifact = artifact as StaticSite,
          )
        }
      }
    }
  }
}
