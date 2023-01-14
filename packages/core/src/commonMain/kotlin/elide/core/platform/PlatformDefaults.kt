package elide.core.platform

/**
 * # Core: Platform-specific Defaults
 *
 * It is expected that a set of platform defaults are defined for each platform that Elide supports. These defaults can
 * reference the global set of universal defaults ([elide.core.Defaults]), or override with their own values. All
 * defaults must be defined at compile time.
 */
public expect class PlatformDefaults : elide.core.PlatformDefaults
