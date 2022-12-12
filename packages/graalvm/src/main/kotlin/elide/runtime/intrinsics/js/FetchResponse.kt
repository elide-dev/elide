package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * TBD.
 */
public interface FetchResponse {
  /** Default values applied to [FetchResponse] interfaces. */
  public object Defaults {
    /** Default value for `type`. */
    public const val DEFAULT_TYPE: String = "basic"

    /** Default value for `redirected`. */
    public const val DEFAULT_REDIRECTED: Boolean = false

    /** Default value for `status`. */
    public const val DEFAULT_STATUS: Int = 200

    /** Default value for `statusText`. */
    public const val DEFAULT_STATUS_TEXT: String = "OK"

    /** Default value for `url`. */
    public const val DEFAULT_URL: String = ""
  }

  /**
   * TBD.
   */
  public interface Factory {
    /**
     * TBD.
     */
    public fun error(): FetchResponse

    /**
     * TBD.
     */
    public fun redirect(): FetchResponse
  }

  /**
   * TBD.
   */
  @get:Polyglot public val body: ReadableStream

  /**
   * TBD.
   */
  @get:Polyglot public val bodyUsed: Boolean

  /**
   * TBD.
   */
  @get:Polyglot public val headers: FetchHeaders

  /**
   * TBD.
   */
  @get:Polyglot public val ok: Boolean get() = status in 200..299

  /**
   * TBD.
   */
  @get:Polyglot public val redirected: Boolean get() = Defaults.DEFAULT_REDIRECTED

  /**
   * TBD.
   */
  @get:Polyglot public val status: Int get() = Defaults.DEFAULT_STATUS

  /**
   * TBD.
   */
  @get:Polyglot public val statusText: String get() = Defaults.DEFAULT_STATUS_TEXT

  /**
   * TBD.
   */
  @get:Polyglot public val type: String get() = Defaults.DEFAULT_TYPE

  /**
   * TBD.
   */
  @get:Polyglot public val url: String get() = Defaults.DEFAULT_URL
}
