package elide.runtime.core.internals.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.EnvironmentAccess
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent
import elide.runtime.core.EngineLifecycleEvent.EngineCreated
import elide.runtime.core.EngineLifecycleEvent.EngineInitialized
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.*
import elide.runtime.core.internals.MutableEngineLifecycle
import elide.runtime.core.internals.graalvm.GraalVMEngine.Companion.create

/**
 * A [PolyglotEngine] implementation built around a GraalVM [engine]. This class allows plugins to configure the
 * [Context] builders before they are returned by the [acquire] method.
 *
 * @see acquire
 * @see create
 */
@DelicateElideApi internal class GraalVMEngine private constructor(
  private val lifecycle: MutableEngineLifecycle,
  private val config: GraalVMConfiguration,
  private val engine: Engine,
) : PolyglotEngine {
  /** Select an [EnvironmentAccess] configuration from the [HostAccess] settings for this engine. */
  private fun HostAccess.toEnvAccess(): EnvironmentAccess? = when (this) {
    ALLOW_ENV, ALLOW_ALL -> EnvironmentAccess.INHERIT
    ALLOW_IO, ALLOW_NONE -> EnvironmentAccess.NONE
  }

  /**
   * Returns the underlying GraalVM [Engine] used by this instance.
   *
   * This method is considered delicate even for internal use within the Elide runtime, since it breaks the
   * encapsulation provided by the core API; it should be used only in select cases where accessing the GraalVM engine
   * directly is less complex than implementing a new abstraction for the desired feature.
   */
  internal fun unwrap(): Engine {
    return engine
  }

  /** Create a new [GraalVMContext], triggering lifecycle events to allow customization. */
  private fun createContext(): GraalVMContext {
    // build a new context using the shared engine
    val builder = Context.newBuilder()
      .allowExperimentalOptions(true)
      .allowEnvironmentAccess(config.hostAccess.toEnvAccess())
      .engine(engine)

    // allow plugins to customize the context on creation
    lifecycle.emit(EngineLifecycleEvent.ContextCreated, builder)

    // build the context and notify event listeners
    val context = GraalVMContext(builder.build())
    lifecycle.emit(EngineLifecycleEvent.ContextInitialized, context)

    return context
  }

  override fun acquire(): PolyglotContext {
    return createContext()
  }

  internal companion object {
    /**
     * Creates a new [GraalVMEngine] using the provided [configuration]. This method triggers the [EngineCreated] event
     * for registered plugins.
     */
    fun create(
      configuration: GraalVMConfiguration,
      lifecycle: MutableEngineLifecycle,
    ): GraalVMEngine {
      val languages = configuration.languages.map { it.languageId }.toTypedArray()
      val builder = Engine.newBuilder(*languages).allowExperimentalOptions(true)

      // allow plugins to customize the engine builder
      lifecycle.emit(EngineCreated, builder)

      // build the engine
      val engine = GraalVMEngine(lifecycle, configuration, builder.build())

      // one more event for initialization plugins
      lifecycle.emit(EngineInitialized, engine)

      return engine
    }
  }
}
