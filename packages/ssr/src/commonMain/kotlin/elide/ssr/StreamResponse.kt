package elide.ssr

import elide.vm.annotations.Polyglot

/**
 * TBD.
 */
public sealed class StreamResponse @Polyglot constructor (
  override val status: Int = 200,
  override val headers: Map<String, String> = emptyMap(),
  override val content: String = "",
  override val hasContent: Boolean = false,
  override val fin: Boolean = true,
) : ServerResponse {
  public companion object {
    /**
     * TBD
     */
    @Polyglot
    public fun success(
      content: String,
      headers: Map<String, String> = emptyMap(),
      status: Int = 200,
      criticalCss: String = "",
      styleChunks: Array<CssChunk> = emptyArray(),
    ): StreamResponse {
      return Success(
        RenderedStream(
        status = status,
        html = content,
        headers = headers,
        criticalCss = criticalCss,
        styleChunks = styleChunks,
      ))
    }

    /**
     * TBD
     */
    @Polyglot
    public fun error(
      thr: Throwable
    ): StreamResponse {
      return Error(thr)
    }
  }

  /** */
  public class Success(
    public val stream: RenderedStream,
  ) : StreamResponse(
    status = stream.status,
    headers = stream.headers,
    content = stream.html,
    hasContent = true,
    fin = true,
  )

  /** */
  public class Error(
    public val err: Any,
    status: Int = 500,
    headers: Map<String, String> = emptyMap(),
  ) : StreamResponse(
    status = status,
    headers = headers,
    hasContent = false,
    fin = true,
  )
}
