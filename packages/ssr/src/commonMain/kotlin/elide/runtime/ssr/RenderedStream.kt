package elide.runtime.ssr

import elide.annotations.core.Polyglot

/**
 * # SSR: Rendered Stream
 *
 * Data class which represents a rendered and streamed SSR response. Such responses are composed of headers (as all HTTP
 * responses are), front matter (`<head>` content), and chunked body content.
 *
 * @param status HTTP return status.
 * @param html HTML code to be emitted.
 * @param headers HTTP headers to be emitted in the response.
 * @param criticalCss Critical CSS to be emitted as front-matter.
 * @param styleChunks Additional style chunks to emit.
 */
public data class RenderedStream @Polyglot constructor (
  public val status: Int,
  public val html: String,
  public val headers: Map<String, String>,
  public val criticalCss: String,
  public val styleChunks: Array<CssChunk>,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as RenderedStream

    if (status != other.status) return false
    if (html != other.html) return false
    if (headers != other.headers) return false
    if (criticalCss != other.criticalCss) return false
    if (!styleChunks.contentEquals(other.styleChunks)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = status
    result = 31 * result + html.hashCode()
    result = 31 * result + headers.hashCode()
    result = 31 * result + criticalCss.hashCode()
    result = 31 * result + styleChunks.contentHashCode()
    return result
  }
}
