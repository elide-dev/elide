package elide.runtime.gvm

/**
 *
 */
public interface GuestLanguage {
  /**
   *
   */
  public val symbol: String

  /**
   *
   */
  public val label: String

  /**
   *
   */
  public val supportsSSR: Boolean get() = true

  /**
   *
   */
  public val supportsStreamingSSR: Boolean get() = true
}
