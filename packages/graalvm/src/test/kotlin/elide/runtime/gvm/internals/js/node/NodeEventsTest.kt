/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
@file:Suppress("MnInjectionPoints")

package elide.runtime.gvm.internals.js.node

import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.gvm.internals.node.NodeStdlib
import elide.runtime.gvm.internals.node.events.EventAware
import elide.runtime.gvm.internals.node.events.NodeEventsModule
import elide.runtime.gvm.internals.node.events.NodeEventsModuleFacade
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.EventsAPI
import elide.testing.annotations.TestCase

/** Tests for the built-in `assert` module. */
@TestCase internal class NodeEventsTest @Inject constructor(internal val events: EventsAPI)
  : NodeModuleConformanceTest<NodeEventsModule>() {
  override val moduleName: String get() = "events"
  override fun provide(): NodeEventsModule = NodeEventsModule()

  // @TODO(sgammon): Does not yet comply with all methods.
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("EventEmitter")
    yield("EventEmitterAsyncResource")
    yield("Event")
    yield("EventListener")
    yield("EventTarget")
    yield("defaultMaxListeners")
    yield("errorMonitor")
    yield("getEventListeners")
    yield("getMaxListeners")
    yield("once")
    yield("captureRejections")
    yield("captureRejectionsSymbol")
    yield("listenerCount")
    yield("on")
    yield("setMaxListeners")
    yield("addAbortListener")
  }

  @Test override fun testInjectable() {
    assertNotNull(events)
  }

  @Test fun testEventsApiSingleton() {
    assertSame(NodeEventsModuleFacade.obtain(), NodeEventsModuleFacade.obtain())
    assertSame(NodeStdlib.events, NodeEventsModuleFacade.obtain())
    assertNotSame(NodeEventsModuleFacade.obtain(), NodeEventsModuleFacade.create())
  }

  @Test fun testDefaultMaxListeners() {
    val maxListeners = assertNotNull(events.defaultMaxListeners)
    assertEquals(10, maxListeners)  // set by node as constant (unless overridden)
  }

  @Test fun testSetGlobalDefaultMaxListeners() {
    val events = NodeEventsModuleFacade.create()
    val maxListeners = assertNotNull(events.defaultMaxListeners)
    assertEquals(10, maxListeners)  // set by node as constant (unless overridden)
    events.defaultMaxListeners = 15
    assertEquals(15, events.defaultMaxListeners)
    assertEquals(10, NodeEventsModuleFacade.obtain().defaultMaxListeners)
    val events2 = NodeEventsModuleFacade.create()
    assertEquals(10, events2.defaultMaxListeners)
    events2.setMaxListeners(15)
    assertEquals(15, events2.defaultMaxListeners)
  }

  @Test fun testSetDefaultMaxListeners() {
    val events = NodeEventsModuleFacade.create()
    val aware = EventAware.create()
    assertEquals(10, events.defaultMaxListeners)
    assertEquals(10, events.getMaxListeners(aware))
    events.setMaxListeners(15)
    assertEquals(15, events.defaultMaxListeners)
    val events2 = NodeEventsModuleFacade.create()
    assertEquals(10, events2.defaultMaxListeners)
    events2.setMaxListeners(15, aware)
    assertEquals(10, events2.defaultMaxListeners)
    assertEquals(15, events.getMaxListeners(aware))
  }

  @Test fun testGetEventListenersForEmitter() {
    val aware = EventAware.create()
    assertEquals(0, aware.listenerCount(""))
    assertEquals(0, aware.listenerCount("hello"))
    assertEquals(0, events.listenerCount(aware, "hello"))
    aware.addEventListener("hello") {
      // nothing
    }
    assertEquals(1, aware.listenerCount("hello"))
    assertEquals(1, events.listenerCount(aware, "hello"))
    assertEquals(0, aware.listenerCount("other"))
    assertEquals(0, events.listenerCount(aware, "other"))
    assertEquals(1, aware.listenerCount(""))
  }

  @Test fun testGetMaxListenersForEmitter() {
    val aware = EventAware.create()
    assertEquals(10, aware.getMaxListeners())
    assertEquals(10, events.getMaxListeners(aware))
    val aware2 = EventAware.create()
    assertEquals(10, aware2.getMaxListeners())
    assertEquals(10, events.getMaxListeners(aware))
  }

  @Test fun testSetMaxListenersForEmitter() {
    val aware = EventAware.create()
    assertEquals(10, aware.getMaxListeners())
    assertEquals(10, events.getMaxListeners(aware))
    val aware2 = EventAware.create()
    assertEquals(10, aware2.getMaxListeners())
    assertEquals(10, events.getMaxListeners(aware))
    aware2.setMaxListeners(15)
    assertEquals(15, aware2.getMaxListeners())
    assertEquals(10, aware.getMaxListeners())
    assertEquals(10, events.getMaxListeners(aware))
    assertEquals(15, events.getMaxListeners(aware2))
  }
}
