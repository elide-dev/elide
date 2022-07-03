package elide.server.util

import elide.AppEnvironment
import elide.util.UUID
import java.util.concurrent.atomic.AtomicReference


/** Static server flags, which may be set via Java system properties or environment variables. */
@Suppress("unused", "MemberVisibilityCanBePrivate", "RedundantVisibilityModifier") public object ServerFlag {
  // Server arguments provided by the boot entrypoint.
  private val args: AtomicReference<Array<String>> = AtomicReference(emptyArray())

  /** Operating environment for the application. */
  public val appEnv: AppEnvironment get() = resolve("elide.appEnv", AppEnvironment.LIVE.name) {
      AppEnvironment.valueOf(it.trim().uppercase())
  }

  /** Whether to enable VM inspection. */
  public val inspect: Boolean get() = resolve("elide.vm.inspect", "false") {
    it.trim().toBoolean()
  }

  /** Host where inspection should mount. */
  public val inspectHost: String get() = resolve("elide.vm.inspect.host", "localhost") {
    it.trim().ifBlank {
      "localhost"
    }
  }

  /** Port where inspection should mount. */
  public val inspectPort: Int get() = resolve("elide.vm.inspect.port", "4242") {
    it.trim().toIntOrNull() ?: 4242
  }

  /** Path where inspection should mount. */
  public val inspectPath: String get() = resolve("elide.vm.inspect.path", "") {
    it.trim().ifBlank {
      UUID.random().uppercase()
    }
  }

  /** Whether to enable VM inspection secure mode (TLS). */
  public val inspectSecure: Boolean get() = resolve("elide.vm.inspect.secure", "false") {
    it.trim().toBoolean()
  }

  // Resolve an enumerated flag value.
  public fun <R> resolve(name: String, defaultValue: String, then: (String) -> R): R {
    val value = System.getProperty(name, System.getenv(name) ?: resolveArg(name))
    return if (value?.isNotBlank() == true) {
      then.invoke(value)
    } else {
      then.invoke(defaultValue)
    }
  }

  // Resolve the named argument or return `null`.
  private fun resolveArg(name: String): String? {
    val args = args.get()
    for (arg in args) {
      if (arg.startsWith("--$name=")) {
        return arg.substring("--$name=".length)
      }
    }
    return null
  }

  /** Install server flag value state. */
  @JvmStatic public fun setArgs(args: Array<String>) {
    this.args.set(
      args
    )
  }
}
