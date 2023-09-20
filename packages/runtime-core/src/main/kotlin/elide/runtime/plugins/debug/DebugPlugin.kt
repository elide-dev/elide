package elide.runtime.plugins.debug

import elide.runtime.core.*
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.extensions.enableOption

/**
 * Engine plugin providing debug and inspection features for guest code.
 *
 * @see debug
 * @see DebugConfig 
 */
@DelicateElideApi public class Debug private constructor(
  public val config: DebugConfig,
  private val platform: HostPlatform,
) {
  /** Apply debug [config] to a context [builder] during the [ContextCreated] event. */
  internal fun onContextCreated(builder: PolyglotContextBuilder): Unit = with(builder) {
    config.path?.let { path -> option("inspect.Path", path) }

    // source path delimiters are platform-specific
    config.sourcePaths?.let { paths ->
      option(
        /* key = */ "inspect.SourcePath",
        /* value = */ paths.joinToString(if (platform.os.isUnix) UNIX_SOURCE_DELIMITER else WINDOWS_SOURCE_DELIMITER),
      )
    }

    if (config.waitAttached) enableOption("inspect.WaitAttached")
    if (config.suspend) enableOption("inspect.Suspend")
  }

  /** Identifier for the [Debug] plugin, which provides debugging options for embedded languages. */
  public companion object Plugin : EnginePlugin<DebugConfig, Debug> {
    /** Delimiter used to separate source paths in UNIX-based systems. */
    private const val UNIX_SOURCE_DELIMITER = ":"

    /** Delimiter used to separate source paths in Windows systems. */
    private const val WINDOWS_SOURCE_DELIMITER = ";"

    override val key: Key<Debug> = Key("Debug")

    override fun install(scope: InstallationScope, configuration: DebugConfig.() -> Unit): Debug {
      // apply the configuration and create the plugin instance
      val config = DebugConfig().apply(configuration)
      val instance = Debug(config, scope.configuration.hostPlatform)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::onContextCreated)

      return instance
    }
  }
}

/** Configure the [Debug] plugin, installing it if not already present. */
@DelicateElideApi public fun PolyglotEngineConfiguration.debug(configure: DebugConfig.() -> Unit) {
  plugin(Debug)?.config?.apply(configure) ?: install(Debug, configure)
}
