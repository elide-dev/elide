package elide.runtime.plugins.env

import java.util.concurrent.ConcurrentSkipListMap
import java.util.function.Supplier
import elide.runtime.core.DelicateElideApi
import elide.runtime.plugins.env.EnvConfig.EnvVariableSource.INLINE

/**
 * Defines configuration options for built-in managed guest access to the host environment.
 *
 * This container is meant to be used by the [Environment] plugin.
 */
@DelicateElideApi public class EnvConfig internal constructor() {
  /** Configuration for managed application environment.  */
  @DelicateElideApi public class AppEnvConfig internal constructor() {
    /**
     * Whether to enable managed environment features.
     */
    public var enabled: Boolean = true

    /**
     * Isolated suite of environment variables to provide to the guest application.
     */
    public val isolatedEnvironmentVariables: MutableMap<String, EnvVar> = ConcurrentSkipListMap()
  }

  /** Describes different sources for an environment variable. */
  @DelicateElideApi public enum class EnvVariableSource {
    /** The value is provided explicitly, inline. */
    INLINE,

    /** The value originates from a `.env` file. */
    DOTENV,

    /** The value is resolved from the host environment. */
    HOST
  }

  /** Describes configured guest application environment variables. */
  @DelicateElideApi public sealed interface EnvVar {
    /**
     * Name for the environment variable.
     */
    public val name: String

    /**
     * Value for the environment variable; if evaluated to `null`, the variable is skipped.
     */
    public val value: String?

    /**
     * Indicate whether a value can be resolved for this env var.
     */
    public val isPresent: Boolean get() = value != null

    /**
     * Source type for this environment variable; governs how a value is resolved.
     */
    public val source: EnvVariableSource

    /** Maps an inline (explicitly-provided) environment variable. */
    @DelicateElideApi @JvmRecord public data class InlineEnvVar(
      override val name: String,
      override val value: String?,
    ): EnvVar {
      override val source: EnvVariableSource get() = INLINE
    }

    /** Maps an supplied (function-resolved) environment variable. */
    @DelicateElideApi @JvmRecord public data class SuppliedEnvVar(
      override val name: String,
      private val supplier: Supplier<String?>,
    ): EnvVar {
      override val source: EnvVariableSource get() = INLINE
      override val value: String? get() = supplier.get()
    }

    /** Maps an environment variable which originates from a `.env` file. */
    @DelicateElideApi @JvmRecord public data class DotEnvVar(
      public val file: String,
      override val name: String,
      override val value: String?,
    ): EnvVar {
      override val source: EnvVariableSource get() = INLINE
    }

    /** Maps an environment variable which uses a host environment variable (usually, at the same name). */
    @DelicateElideApi @JvmRecord public data class HostMappedVar(
      public val mapped: String,
      public val defaultValue: String? = null,
      override val name: String,
    ): EnvVar {
      override val source: EnvVariableSource get() = INLINE
      override val value: String? get() = System.getenv(name) ?: defaultValue
    }

    public companion object {
      /** @return [EnvVar] which describes an explicit variable (type [EnvVariableSource.INLINE]). */
      @JvmStatic public fun of(name: String, value: String): EnvVar = InlineEnvVar(name, value)

      /** @return [EnvVar] which describes a function-resolved variable (type [EnvVariableSource.INLINE]). */
      @JvmStatic public fun provide(name: String, provider: Supplier<String?>): EnvVar =
        SuppliedEnvVar(name, provider)

      /** @return [EnvVar] which describes a dotenv-resolved variable (type [EnvVariableSource.DOTENV]). */
      @JvmStatic public fun fromDotenv(file: String, name: String, value: String?): EnvVar =
        DotEnvVar(file, name, value)

      /** @return [EnvVar] which describes a host-resolved variable (type [EnvVariableSource.HOST]). */
      @JvmStatic public fun mapToHost(name: String, atName: String = name, defaultValue: String? = null): EnvVar =
        HostMappedVar(mapped = name, name = atName, defaultValue = defaultValue)
    }
  }

  /** Environment settings which apply to the guest application. */
  internal val app: AppEnvConfig = AppEnvConfig()

  /** Configure application environment. */
  public fun environment(block: AppEnvConfig.() -> Unit) {
    app.enabled = true
    app.apply(block)
  }

  /** Inject an explicit application environment variable. */
  public fun environment(name: String, value: String?) {
    if (value == null) return
    if (!app.enabled) app.enabled = true
    app.isolatedEnvironmentVariables[name] = EnvVar.of(name, value)
  }

  /** Inject an explicit application environment variable resolved from a [callback]. */
  public fun environment(name: String, callback: () -> String?) {
    if (!app.enabled) app.enabled = true
    app.isolatedEnvironmentVariables[name] = EnvVar.provide(name, callback)
  }

  /** Expose or map a [hostVariable] to the provided [alias] (defaults to the same name). */
  public fun mapToHostEnv(hostVariable: String, alias: String = hostVariable, defaultValue: String? = null) {
    if (!app.enabled) app.enabled = true
    app.isolatedEnvironmentVariables[alias] = EnvVar.mapToHost(hostVariable, alias, defaultValue)
  }

  /** Expose or map a [hostVariable] to the provided [alias] (defaults to the same name). */
  public fun exposeHostEnv(hostVariable: String, defaultValue: String? = null) {
    mapToHostEnv(hostVariable, defaultValue = defaultValue)
  }

  /** Inject an environment variable at [name] that was loaded from a `.env` [file]. */
  public fun fromDotenv(file: String, name: String, value: String?) {
    if (!app.enabled) app.enabled = true
    app.isolatedEnvironmentVariables[name] = EnvVar.fromDotenv(file, name, value)
  }
}
