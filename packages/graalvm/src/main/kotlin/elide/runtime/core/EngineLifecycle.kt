package elide.runtime.core

/**
 * The lifecycle allows [plugins][EnginePlugin] to subscribe to [events][EngineLifecycleEvent] such as
 * [engine][EngineLifecycleEvent.EngineCreated] and [context][EngineLifecycleEvent.ContextCreated] configuration.
 *
 * [Lifecycle events][EngineLifecycleEvent] are part of a sealed hierarchy of singletons that act as type-safe event
 * keys, and can be used as follows:
 *
 * ```kotlin
 * lifecycle.on(EngineLifecycleEvent.ContextCreated) { it: ContextBuilder ->
 *  // update the context builder before it is handed back to the engine
 *  it.option(...)
 * }
 * ```
 */
@DelicateElideApi public interface EngineLifecycle {
  public fun <T> on(event: EngineLifecycleEvent<T>, consume: (T) -> Unit)
}