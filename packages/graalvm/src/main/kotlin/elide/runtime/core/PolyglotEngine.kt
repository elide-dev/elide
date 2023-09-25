package elide.runtime.core

/**
 * The Polyglot Engine is responsible for creating new [PolyglotContext] instances, as well as triggering
 * [events][EngineLifecycleEvent] that allow [plugins][EnginePlugin] to extend the runtime.
 *
 * Engine instances can be created using the [PolyglotEngine][elide.runtime.core.PolyglotEngine] DSL, which provides
 * methods to configure and extend the engine by installing plugins.
 *
 * The [acquire] function can be used to obtain a new [PolyglotContext] configured by the engine.
 */
@DelicateElideApi public interface PolyglotEngine {
  /** Acquire a new [PolyglotContext]. The returned context has all plugins applied on creation. */
  public fun acquire(): PolyglotContext
}