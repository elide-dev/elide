package elide.runtime.ssr

import elide.annotations.core.Polyglot

/**
 * TBD
 */
public data class CssChunk @Polyglot constructor (
  @field:Polyglot public val ids: Array<String>,
  @field:Polyglot public val key: String,
  @field:Polyglot public val css: String,
)
