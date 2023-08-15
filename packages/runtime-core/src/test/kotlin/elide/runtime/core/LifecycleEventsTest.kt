package elide.runtime.core

import org.junit.jupiter.api.Test
import elide.runtime.core.EngineLifecycleEvent.*
import elide.runtime.core.EnginePlugin.InstallationScope

@OptIn(DelicateElideApi::class) class LifecycleEventsTest {
  /** Instrumented test plugin that allows using the [EngineLifecycle] without a full implementation. */
  private class InstrumentedLifecyclePlugin<T>(
    val event: EngineLifecycleEvent<T>,
  ) : MockEnginePlugin(id = "LifecycleTestPlugin") {
    /** Whether the [event] was received by this plugin at least once. */
    val eventReceived: Boolean get() = eventsReceived >= 0

    /** Number of times the [event] was received by this plugin. */
    var eventsReceived: Int = 0
      private set

    override fun install(scope: InstallationScope, configuration: Unit.() -> Unit) {
      scope.lifecycle.on(event) { eventsReceived++ }
    }
  }

  private fun InstrumentedLifecyclePlugin<*>.assertEventReceived() {
    assert(eventReceived) { "should receive '$event' event" }
  }

  private fun <T> testLifecycleEvent(event: EngineLifecycleEvent<T>, prepare: (PolyglotEngine) -> Unit = { }) {
    val plugin = InstrumentedLifecyclePlugin(event)
    val engine = PolyglotEngine { install(plugin) }

    prepare(engine)

    plugin.assertEventReceived()
  }

  @Test fun testEvents() {
    testLifecycleEvent(EngineCreated)
    testLifecycleEvent(EngineInitialized)

    testLifecycleEvent(ContextCreated) { engine -> engine.acquire() }
    testLifecycleEvent(ContextInitialized) { engine -> engine.acquire() }
  }
}
