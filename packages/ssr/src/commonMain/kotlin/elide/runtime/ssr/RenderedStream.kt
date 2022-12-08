package elide.runtime.ssr

import elide.annotations.core.Polyglot

/**
 * TBD
 */
public data class RenderedStream @Polyglot constructor (
  public val status: Int,
  public val html: String,
  public val headers: HeaderMap,
  public val criticalCss: String,
  public val styleChunks: Array<CssChunk>,
)
