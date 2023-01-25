package elide.site.ui.components

import lib.reactSyntaxHighlighter.SyntaxHighlighterLight
import lib.reactSyntaxHighlighter.languages.bash.BashSyntax
import lib.reactSyntaxHighlighter.languages.groovy.GroovySyntax
import lib.reactSyntaxHighlighter.languages.js.JavaScriptSyntax
import lib.reactSyntaxHighlighter.languages.json.JsonSyntax
import lib.reactSyntaxHighlighter.languages.kotlin.KotlinSyntax
import lib.reactSyntaxHighlighter.languages.python.PythonSyntax
import lib.reactSyntaxHighlighter.languages.xml.XmlSyntax
import lib.reactSyntaxHighlighter.SyntaxThemeTomorrow as TomorrowTheme
import kotlin.js.Promise

/**
 * Enumeration of supported languages for syntax highlighting.
 */
@Suppress("unused")
enum class SyntaxLanguage constructor (internal val symbol: String) {
  /** Syntax highlighting for Bash. */
  BASH("bash"),

  /** Syntax highlighting for JavaScript. */
  JAVASCRIPT("js"),

  /** Syntax highlighting for JSON. */
  JSON("json"),

  /** Syntax highlighting for TypeScript. */
  TYPESCRIPT("ts"),

  /** Syntax highlighting for Kotlin. */
  KOTLIN("kotlin"),

  /** Syntax highlighting for Python. */
  PYTHON("python"),

  /** Syntax highlighting for Groovy. */
  GROOVY("groovy"),

  /** Syntax highlighting for XML. */
  XML("xml"),
}

// Default (fallback) language.
private val defaultLanguage = SyntaxLanguage.JAVASCRIPT

// Default list of languages to load.
private val defaultLanguages = listOf(
  defaultLanguage,
  SyntaxLanguage.JSON,
  SyntaxLanguage.BASH,
  SyntaxLanguage.KOTLIN,
  SyntaxLanguage.PYTHON,
  SyntaxLanguage.GROOVY,
  SyntaxLanguage.XML,
)

// Default theme to use.
private val defaultTheme = TomorrowTheme

// Active theme.
private var activeTheme = defaultTheme

// Resolve languages to their implementations.
private fun resolveLanguages(languages: List<SyntaxLanguage>): Array<Promise<Pair<SyntaxLanguage, dynamic>>> {
  return languages.map { language ->
    Promise { accept, reject ->
      when (language) {
        SyntaxLanguage.BASH -> accept(language to BashSyntax)
        SyntaxLanguage.JAVASCRIPT -> accept(language to JavaScriptSyntax)
        SyntaxLanguage.JSON -> accept(language to JsonSyntax)
        SyntaxLanguage.KOTLIN -> accept(language to KotlinSyntax)
        SyntaxLanguage.PYTHON -> accept(language to PythonSyntax)
        SyntaxLanguage.GROOVY -> accept(language to GroovySyntax)
        SyntaxLanguage.XML -> accept(language to XmlSyntax)
        else -> reject(IllegalStateException("Language not available: '$language'"))
      }
    }
  }.toTypedArray()
}

/**
 * Configure code samples for a given page render.
 *
 * This method should be called once per page-load; subsequent calls are no-ops. Languages will be configured for syntax
 * highlighting according to input parameters. Styles will be loaded. Both are loaded and held via side effects, for
 * later use by [CodeSample] component instances.
 *
 * @param languages Languages which should be loaded for highlighting.
 * @param theme Theme name to use.
 */
fun configureCodeSamples(languages: List<SyntaxLanguage>? = null, theme: dynamic = null): Promise<Unit> {
  activeTheme = theme ?: defaultTheme
  return Promise.all(resolveLanguages(languages ?: defaultLanguages)).then { langTargets ->
    langTargets.forEach {
      try {
        SyntaxHighlighterLight.asDynamic().registerLanguage(
          it.first.symbol,
          it.second,
        )
        Unit
      } catch (err: Throwable) {
        console.warn("[elide:site]", "Failed to register syntax language '${it.first.symbol}'", err)
        throw err
      }
    }
  }
}

/**
 * Code Sample: Props.
 *
 * Defines the structure of properties which can be used to configure the [CodeSample] React component.
 */
external interface CodeSampleProps : react.PropsWithChildren, react.PropsWithClassName {
  /** Active language to render for; if not provided, defaults to [SyntaxLanguage.JAVASCRIPT]. */
  var language: SyntaxLanguage?

  /** Custom style object to apply to the outer `pre` tag. */
  var customStyle: dynamic

  /** Specifies a custom style to use for this code sample. If unspecified, the default theme will be used. */
  var style: dynamic
}

/**
 * Component: Code Sample.
 *
 * Displays a sample of source-code material with features like syntax highlighting, file names, copy-to-clipboard, etc.
 * Powered by Highlight.js and React Syntax Highlighter, and typically driven from MDX.
 */
val CodeSample = react.FC<CodeSampleProps> {
  SyntaxHighlighterLight {
    language = (it.language ?: defaultLanguage).symbol
    style = it.style ?: activeTheme
    className = it.className
    children = it.children
    if (it.customStyle != null) customStyle = it.customStyle
  }
}
