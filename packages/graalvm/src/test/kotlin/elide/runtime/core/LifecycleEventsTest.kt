/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.core

import org.junit.jupiter.api.Test
import elide.runtime.core.EngineLifecycleEvent.*
import elide.runtime.core.EnginePlugin.InstallationScope

@OptIn(DelicateElideApi::class)
internal class LifecycleEventsTest {
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
