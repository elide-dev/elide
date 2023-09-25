package elide.runtime.core.internals

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycle
import elide.runtime.core.EngineLifecycleEvent

/**
 * An internal implementation of the [EngineLifecycle] interface that allows emitting events.
 * @see emit
 */
@DelicateElideApi internal class MutableEngineLifecycle : EngineLifecycle {
  /** A map associating event keys with a stack of listeners. */
  private val listeners = mutableMapOf<Any, MutableList<Any>>()

  override fun <T> on(event: EngineLifecycleEvent<T>, consume: (T) -> Unit) {
    listeners.compute(event) { _, present ->
      // add the consumer to the list if the entry exists, otherwise create a new list
      present?.apply { add(consume) } ?: mutableListOf(consume)
    }
  }

  @Suppress("unchecked_cast")
  fun <T> emit(event: EngineLifecycleEvent<T>, payload: T) {
    // trigger the event for each listener in this entry
    listeners[event]?.forEach { (it as (T) -> Unit).invoke(payload) }
  }
}
