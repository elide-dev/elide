package elide.runtime

import kotlin.reflect.KClass

/** Describes an expected class which is able to produce [Logger] instances as a factory. */
@Suppress("MemberVisibilityCanBePrivate", "unused") public actual class Logging {
  public companion object {
    // Singleton logging manager instance.
    private val singleton = Logging()

    /** @return Generic root logger. */
    public fun acquire(): elide.runtime.wasm.Logger = root()

    /** @return Logger created, or resolved, for the [target] Kotlin class. */
    public fun of(target: KClass<*>): elide.runtime.wasm.Logger = named(
      target.simpleName ?: ""
    )

    /** @return Logger resolved at the root name. */
    public fun root(): elide.runtime.wasm.Logger = named(
      ""
    )

    /** @return Logger created for the specified [name]. */
    public fun named(name: String): elide.runtime.wasm.Logger {
      return if (name.isEmpty() || name.isBlank()) {
        singleton.logger() as elide.runtime.wasm.Logger
      } else {
        singleton.logger(name) as elide.runtime.wasm.Logger
      }
    }

    // @TODO(sgammon): logging control JS-side
    @Suppress("UNUSED_PARAMETER")
    internal fun isEnabled(level: LogLevel) = true

    // Static log sender.
    internal fun sendLog(level: LogLevel, messages: List<Any>, levelChecked: Boolean) {
      val enabled = levelChecked || isEnabled(level)
      if (enabled) {
        print(messages.toTypedArray())
      }
    }
  }

  /**
   * Acquire a [Logger] for the given logger [name], which can be any identifying string; in JVM circumstances, the full
   * class name of the subject which is sending the logs is usually used.
   *
   * @param name Name of the logger to create and return.
   * @return Desired logger.
   */
  public actual fun logger(name: String): Logger {
    TODO("not yet implemented")
  }

  /**
   * Acquire a root [Logger] which is unnamed, or uses an empty string value (`""`) for the logger name.
   *
   * @return Root logger.
   */
  public actual fun logger(): Logger {
    TODO("not yet implemented")
  }
}
