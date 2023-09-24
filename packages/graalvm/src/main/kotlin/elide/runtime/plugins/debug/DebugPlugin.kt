package elide.runtime.plugins.debug

import elide.runtime.core.*
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.EngineCreated
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.extensions.enableOption
import elide.runtime.core.extensions.setOption

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
  private fun configureDap(builder: PolyglotEngineBuilder): Unit = with(builder) {
    option("dap", "${config.debugger.host}:${config.debugger.port}")
    setOption("dap.WaitAttached", config.debugger.waitAttached)
    setOption("dap.Suspend", config.debugger.suspend)
  }

  private fun configureChromeInspector(builder: PolyglotEngineBuilder): Unit = with(builder) {
    option("inspect", "${config.inspector.host}:${config.inspector.port}")

    config.inspector.path?.let { path -> option("inspect.Path", path) }

    // source path delimiters are platform-specific
    config.inspector.sourcePaths?.let { paths ->
      option(
        /* key = */ "inspect.SourcePath",
        /* value = */ paths.joinToString(if (platform.os.isUnix) UNIX_SOURCE_DELIMITER else WINDOWS_SOURCE_DELIMITER),
      )
    }

    setOption("inspect.WaitAttached", config.inspector.waitAttached)
    setOption("inspect.Suspend", config.inspector.suspend)
    setOption("inspect.Internal", config.inspector.internal)
  }

  /** Apply debug [config] to a context [builder] during the [EngineCreated] event. */
  internal fun onEngineCreated(builder: PolyglotEngineBuilder) {
    if (config.inspector.enabled) configureChromeInspector(builder)
    if (config.debugger.enabled) configureDap(builder)
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
      scope.lifecycle.on(EngineCreated, instance::onEngineCreated)

      return instance
    }
  }
}

/** Configure the [Debug] plugin, installing it if not already present. */
@DelicateElideApi public fun PolyglotEngineConfiguration.debug(configure: DebugConfig.() -> Unit) {
  plugin(Debug)?.config?.apply(configure) ?: install(Debug, configure)
}
