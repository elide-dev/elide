package elide.runtime.plugins.debug

import elide.runtime.core.DelicateElideApi

/**
 * Defines configuration options for the built-in implementation of the Chrome DevTools Protocol, and the
 * Debug Adapter Protocol.
 *
 * This container is meant to be used by the [Debug].
 */
@DelicateElideApi public class DebugConfig internal constructor() {
  /** A custom path to be used as connection URL for the debugger. */
  public var path: String? = null

  /**
   * A list of directories or ZIP/JAR files representing the source path, used to resolve relative references in
   * inspected code.
   */
  public var sourcePaths: List<String>? = null

  /** Whether to suspend execution at the first source line. Defaults to `true` */
  public var suspend: Boolean = true

  /** Whether to wait until the debugger is attached before executing any code. Defaults to `false`. */
  public var waitAttached: Boolean = false
}