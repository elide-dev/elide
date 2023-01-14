package elide.core

/**
 * # Core: Defaults
 *
 * Specifies sensible defaults for well-known or commonly-used types or values. These configurations may change over
 * time, but they should change very slowly, and represent the best practices for the current time, with a focus on
 * interoperability.
 */
public object Defaults : PlatformDefaults {
  /**
   * ## Defaults: Charset.
   *
   * Default character set when interpreting string data, or converting string data to and from raw bytes. The default
   * character set should be overridable in almost every circumstance; this value is merely a reasonable default.
   */
  public override val charset: String = "UTF-8"
}
