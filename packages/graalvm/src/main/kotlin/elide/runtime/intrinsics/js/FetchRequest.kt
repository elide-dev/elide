package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * TBD.
 */
public interface FetchRequest {
  /** Default values applied to [FetchRequest] interfaces. */
  public object Defaults {
    /** Default `cache` value. */
    public const val DEFAULT_CACHE: String = "default"

    /** Default `credentials` value. */
    public const val DEFAULT_CREDENTIALS: String = "omit"

    /** Default `method` value. */
    public const val DEFAULT_METHOD: String = "GET"

    /** Default `mode` value. */
    public const val DEFAULT_MODE: String = "server"

    /** Default `priority` value. */
    public const val DEFAULT_PRIORITY: String = "auto"

    /** Default `redirect` value. */
    public const val DEFAULT_REDIRECT: String = "follow"

    /** Default `referrer` value. */
    public const val DEFAULT_REFERRER: String = "client"

    /** Default `referrerPolicy` value. */
    public const val DEFAULT_REFERRER_POLICY: String = "no-referrer"
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
  @get:Polyglot public val cache: String get() = Defaults.DEFAULT_CACHE

  /**
   * TBD.
   */
  @get:Polyglot public val credentials: String get() = Defaults.DEFAULT_CREDENTIALS

  /**
   * TBD.
   */
  @get:Polyglot public val destination: String

  /**
   * TBD.
   */
  @get:Polyglot public val headers: FetchHeaders

  /**
   * TBD.
   */
  @get:Polyglot public val integrity: String? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public val method: String get() = Defaults.DEFAULT_METHOD

  /**
   * TBD.
   */
  @get:Polyglot public val mode: String get() = Defaults.DEFAULT_MODE

  /**
   * TBD.
   */
  @get:Polyglot public val priority: String get() = Defaults.DEFAULT_PRIORITY

  /**
   * TBD.
   */
  @get:Polyglot public val redirect: String get() = Defaults.DEFAULT_REDIRECT

  /**
   * TBD.
   */
  @get:Polyglot public val referrer: String? get() = Defaults.DEFAULT_REFERRER

  /**
   * TBD.
   */
  @get:Polyglot public val referrerPolicy: String get() = Defaults.DEFAULT_REFERRER_POLICY

  /**
   * TBD.
   */
  @get:Polyglot public val url: String
}
