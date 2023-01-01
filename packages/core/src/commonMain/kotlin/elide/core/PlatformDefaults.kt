package elide.core

/**
 * # Core: Platform Defaults
 *
 * Specifies the interface which defaults-objects are expected to comply with, and which the platform-specific defaults
 * for a given architecture or OS target must implement.
 */
public interface PlatformDefaults {
  /**
   * ## Defaults: Charset.
   *
   * Default character set when interpreting string data, or converting string data to and from raw bytes. The default
   * character set should be overridable in almost every circumstance; this value is merely a reasonable default.
   */
  public val charset: String get() = Defaults.charset
}
