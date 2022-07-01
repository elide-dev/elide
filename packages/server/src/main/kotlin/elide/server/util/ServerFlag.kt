package elide.server.util

import elide.AppEnvironment


/** Static server flags, which may be set via Java system properties or environment variables. */
public object ServerFlag {
  /** Operating environment for the application. */
  public val appEnv: AppEnvironment = resolve("elide.appEnv", AppEnvironment.LIVE.name) {
    AppEnvironment.valueOf(it.trim().uppercase())
  }

  /** Whether to enable VM inspection. */
  public val inspect: Boolean = resolve("elide.vm.inspect", "false") {
    it.trim().toBoolean()
  }

  // Resolve an enumerated flag value.
  fun <R> resolve(name: String, defaultValue: String, then: (String) -> R): R {
    val value = System.getProperty(name, System.getenv(name))
    return if (value?.isNotBlank() == true) {
      then.invoke(value)
    } else {
      then.invoke(defaultValue)
    }
  }
}
