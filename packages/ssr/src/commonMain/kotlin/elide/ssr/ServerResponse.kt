package elide.ssr

/**
 * TBD
 */
public interface ServerResponse {
  /** */
  public val status: Int? get() = null

  /** */
  public val headers: Map<String, String>? get() = null

  /** */
  public val content: String get() = ""

  /** */
  public val css: String get() = ""

  /** */
  public val hasContent: Boolean get() = content.isNotBlank()

  /** */
  public val fin: Boolean get() = false
}
