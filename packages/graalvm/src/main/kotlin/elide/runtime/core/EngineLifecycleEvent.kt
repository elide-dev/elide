package elide.runtime.core

/**
 * Represents a type-safe event key that can be dispatched by [EngineLifecycle] implementations, and consumed by
 * [plugins][EnginePlugin]. Each event indicates the type of the subject provided to consumers using a generic type
 * parameter.
 *
 * @see EngineCreated
 * @see ContextCreated
 */
@DelicateElideApi public sealed interface EngineLifecycleEvent<@Suppress("unused") T> {
  /**
   * Lifecycle event triggered when a [PolyglotEngine] is being created, providing a [PolyglotEngineBuilder] that can
   * be customized before the engine is built. This event is typically received only once. 
   */
  public data object EngineCreated : EngineLifecycleEvent<PolyglotEngineBuilder>

  /**
   * Lifecycle event triggered when a [PolyglotEngine] is initialized after construction. This event is typically
   * received only once, after [EngineCreated].
   */
  public data object EngineInitialized : EngineLifecycleEvent<PolyglotEngine>
  
  /**
   * Lifecycle event triggered when a [PolyglotContext] is being created, providing a [PolyglotContextBuilder] that can
   * be customized before the context is built. This event is typically received whenever [PolyglotEngine.acquire] is
   * called, to allow intercepting the newly built context.
   */
  public data object ContextCreated : EngineLifecycleEvent<PolyglotContextBuilder>

  /**
   * Lifecycle event triggered when a [PolyglotContext] is initialized after construction. This event is typically
   * received after [ContextCreated], once the context has been built, allowing plugins to run guest init code.
   */
  public data object ContextInitialized : EngineLifecycleEvent<PolyglotContext>
}
