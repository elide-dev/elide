/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.runtime.node

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.intrinsics.js.node.EventsAPI
import elide.runtime.intrinsics.js.node.events.CustomEvent
import elide.runtime.intrinsics.js.node.events.Event
import elide.runtime.node.events.EventAware
import elide.runtime.node.events.NodeEventsModule
import elide.runtime.node.events.NodeEventsModuleFacade
import elide.testing.annotations.TestCase

/** Tests for the built-in `assert` module. */
@TestCase internal class NodeEventsTest @Inject constructor(internal val events: EventsAPI) :
  NodeModuleConformanceTest<NodeEventsModule>() {
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

  @Test fun testCustomEvent() {
    val event = CustomEvent("sample")
    assertIs<Event>(event)
    assertEquals("sample", event.type)
    assertNull(event.target)
    assertNull(event.detail)
    val eventWithDetail = CustomEvent("sample", "hello")
    assertEquals("sample", eventWithDetail.type)
    assertNull(eventWithDetail.target)
    assertEquals("hello", eventWithDetail.detail)
    val overridden = object : CustomEvent("sample") {
      override val detail: Any get() = "hi"
    }
    assertEquals("hi", overridden.detail)
    val fresh = CustomEvent()
    assertEquals("", fresh.type)
    fresh.initEvent("someType", bubbles = false, cancelable = false)
    assertEquals("someType", fresh.type)
    assertFalse(fresh.bubbles)
    assertFalse(fresh.cancelable)
    val cancellable = CustomEvent()
    assertEquals("", cancellable.type)
    cancellable.initEvent("someType", bubbles = false, cancelable = true)
    assertEquals("someType", cancellable.type)
    assertFalse(cancellable.bubbles)
    assertTrue(cancellable.cancelable)
    assertThrows<IllegalStateException> {
      // re-initializing an event should fail
      cancellable.initEvent("reinit", bubbles = false, cancelable = false)
    }
  }

  @Test fun testCustomEventPreventDefault() {
    val notPrevented = CustomEvent("sample")
    val prevented = CustomEvent("sample")
    assertFalse(notPrevented.defaultPrevented)
    assertFalse(prevented.defaultPrevented)
    prevented.preventDefault()
    assertFalse(notPrevented.defaultPrevented)
    assertTrue(prevented.defaultPrevented)
  }

  @Test fun testCustomEventStopPropagation() {
    val notStopped = CustomEvent("sample")
    val stopped = CustomEvent("sample")
    assertTrue(notStopped.propagates)
    assertTrue(stopped.propagates)
    stopped.stopPropagation()
    assertFalse(stopped.propagates)
  }

  @Test fun testCustomEventStopPropagationImmediate() {
    val notStopped = CustomEvent("sample")
    val stopped = CustomEvent("sample")
    assertTrue(notStopped.propagates)
    stopped.stopImmediatePropagation()
    assertFalse(stopped.propagates)
  }

  @Test fun testCustomEventMembers() {
    val event = CustomEvent("sample")
    val members = event.memberKeys
    members.forEach {
      assertTrue(event.hasMember(it))

      assertDoesNotThrow {
        event.getMember(it)
      }
    }
  }

  @Test fun testCustomEventTarget() {
    val event = CustomEvent("sample")
    val target = EventAware.create()
    assertTrue(event.bubbles)
    assertTrue(event.cancelable)
    assertTrue(event.canDispatch())
    assertNull(event.target)
    assertNull(event.currentTarget)
    assertNull(event.srcElement)
    assertNull(event.detail)
    assertEquals(0, event.eventPhase)
    assertEquals("sample", event.type)
    assertDoesNotThrow {
      event.notifyDispatch(target)
    }
    assertEquals(2, event.eventPhase)
    assertTrue(event.bubbles)
    assertTrue(event.cancelable)
    assertNotNull(event.currentTarget)
    assertNotNull(event.target)
    assertNotNull(event.srcElement)
    assertSame(target, event.currentTarget)
    assertSame(target, event.target)
    assertSame(target, event.srcElement)
    val path = event.composedPath()
    assertEquals(1, path.size)
    assertSame(target, path[0])
    val next = EventAware.create()
    event.notifyTarget(next)
    assertEquals(2, event.eventPhase)
    assertSame(next, event.currentTarget)
    assertSame(target, event.target)
    assertSame(target, event.srcElement)
    val path2 = event.composedPath()
    assertEquals(2, path2.size)
    assertSame(next, path2[0])
    assertSame(target, path2[1])
  }

  @Test fun testCustomEventTimestamp() {
    val event = CustomEvent("sample")
    assertNotNull(event.timeStamp)
    assertTrue(event.timeStamp > 0)
    while (System.currentTimeMillis().toDouble() == event.timeStamp) {
      // wait for time to tick
    }
    val event2 = CustomEvent("sample")
    assertNotNull(event2.timeStamp)
    assertTrue(event2.timeStamp > 0)
    assertTrue(event2.timeStamp > event.timeStamp)
  }

  @Test fun testCustomEventTrusted() {
    CustomEvent("sample").let {
      assertTrue(it.canDispatch())
      assertFalse(it.isTrusted)
    }
    CustomEvent("sample").apply { notifyTrusted() }.let {
      assertTrue(it.canDispatch())
      assertTrue(it.isTrusted)
    }
  }

  @Test fun testEmptyComposedPath() {
    val empty = CustomEvent("sample")
    val path = empty.composedPath()
    assertTrue(path.isEmpty())
    assertEquals(0, path.size)
  }

  @Test fun testCannotDispatchIfStopImmediate() {
    val event = CustomEvent("sample")
    assertTrue(event.canDispatch())
    event.stopImmediatePropagation()
    assertFalse(event.canDispatch())
  }

  @Test fun testCreateCustomEvent() = dual {
    assertNotNull(CustomEvent("sample"))
  }.guest {
    // language=JavaScript
    """
      test(new CustomEvent("sample")).isNotNull();
    """
  }

  @Test fun testCreateCustomEventWithDetail() = dual {
    assertNotNull(CustomEvent("sample", mapOf("detail" to true)))
  }.guest {
    // language=JavaScript
    """
      test(new CustomEvent("sample", { detail: true })).isNotNull();
    """
  }

  @Test fun testCreateCustomEventRequiresType() {
    executeGuest {
      // language=JavaScript
      """
      test(new CustomEvent())
    """
    }.fails()
  }
}
