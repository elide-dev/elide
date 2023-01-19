@file:Suppress("unused")
@file:JsModule("react-syntax-highlighter")
@file:JsNonModule

package lib.reactSyntaxHighlighter

/**
 * Properties supported by React Syntax Highlighter.
 */
external interface SyntaxHighlighterProps : react.PropsWithChildren, react.PropsWithClassName {
  /** Registered language to load for syntax highlighting. */
  var language: String

  /** JS theme to use for syntax highlighting. */
  var style: dynamic

  /** Custom style object to apply to the outer `pre` tag. */
  var customStyle: dynamic
}

/** Parent class for syntax highlighter components. */
sealed external interface SyntaxHighlighterComponent<P: SyntaxHighlighterProps> : react.FC<P>

/**
 * Main syntax highlighter, with the full set of themes and languages.
 */
@JsName("default")
external val SyntaxHighlighter: react.FC<SyntaxHighlighterProps>

/**
 * "Light" syntax highlighter, with no built-in themes or languages.
 */
@JsName("Light")
external val SyntaxHighlighterLight: SyntaxHighlighterComponent<SyntaxHighlighterProps>

/**
 * "Light" syntax highlighter operating in async mode.
 */
@JsName("LightAsync")
external val SyntaxHighlighterLightAsync: SyntaxHighlighterComponent<SyntaxHighlighterProps>
