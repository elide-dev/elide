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
package elide.runtime.gvm.js.node

import org.graalvm.polyglot.Value.asValue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.gvm.internals.node.events.EventAware
import elide.runtime.gvm.internals.node.events.NodeEventsModule
import elide.runtime.gvm.internals.node.events.NodeEventsModuleFacade
import elide.runtime.gvm.internals.node.events.StandardEventName
import elide.runtime.intrinsics.js.err.ValueError
import elide.runtime.intrinsics.js.node.events.AddEventListenerOptions
import elide.runtime.intrinsics.js.node.events.CustomEvent
import elide.runtime.intrinsics.js.node.events.Event
import elide.runtime.intrinsics.js.node.events.EventListener
import elide.testing.annotations.TestCase

/** Generic tests for Elide's implementation of Node-style events and event dispatch. */
@TestCase internal class NodeEventAwareTest {
  @Inject lateinit var events: NodeEventsModule

  class ExampleEventAware (private val events: EventAware = EventAware.create()) : EventAware by events {
    val eventsEmitted = mutableListOf<String>()
    val eventsSeen = mutableListOf<String>()

    init {
      // subscribe to some external event that can target this object
      events.addEventListener("externalEvent") {
        val arg = it.getOrNull(0) as? Event ?: error("Invalid event")
        respondToAnEvent(arg)
      }
    }

    // respond to an external event
    private fun respondToAnEvent(event: Event) {
      eventsSeen.add(event.type)
    }

    // function which emits an event from this object
    fun doSomethingThatEmitsAnEvent() {
      eventsEmitted.add("somethingHappened")
      events.emit("somethingHappened", CustomEvent("somethingHappened"))
    }
  }

  class ExampleRegisteredEvents (
    private val events: EventAware = EventAware.create("exampleEvent")
  ) : EventAware by events

  @Test fun testImplementEventAwareEmitter() {
    val aware = ExampleEventAware()
    val emittedSeen = mutableListOf<String>()
    aware.addEventListener("somethingHappened") {
      val arg = it.getOrNull(0) as? Event ?: error("Invalid event")
      emittedSeen.add(arg.type)
    }
    assertDoesNotThrow {
      aware.doSomethingThatEmitsAnEvent()
    }

    // should see event in our own handler
    assertEquals(1, emittedSeen.size)
    assertEquals("somethingHappened", emittedSeen[0])

    // should see event in the internal method
    val emitted = aware.eventsEmitted
    assertEquals(1, emitted.size)
    assertEquals("somethingHappened", emitted[0])
  }

  @Test fun testImplementEventAwareTarget() {
    val aware = ExampleEventAware()
    val event = CustomEvent("externalEvent")
    aware.dispatchEvent(event)
    val seen = aware.eventsSeen
    assertEquals(1, seen.size)
    assertEquals("externalEvent", seen[0])
  }

  @Test fun testOnceListener() {
    val aware = ExampleEventAware()
    val emittedSeen = mutableListOf<String>()
    aware.addEventListener("somethingHappened") {
      val arg = it.getOrNull(0) as? Event ?: error("Invalid event")
      emittedSeen.add(arg.type)
    }
    aware.addEventListener("somethingHappened", AddEventListenerOptions(once = true)) {
      val arg = it.getOrNull(0) as? Event ?: error("Invalid event")
      emittedSeen.add(arg.type)
    }
    assertDoesNotThrow {
      aware.doSomethingThatEmitsAnEvent()
    }
    assertDoesNotThrow {
      aware.doSomethingThatEmitsAnEvent()
    }
    // twice for non-once handler, once for once handler
    assertEquals(3, emittedSeen.size)
  }

  @Test fun testPrependListener() {
    val aware = ExampleEventAware()
    val emittedSeen = mutableListOf<String>()
    val initialSeen = AtomicBoolean(false)
    aware.addEventListener("somethingHappened") {
      val arg = it.getOrNull(0) as? Event ?: error("Invalid event")
      emittedSeen.add(arg.type)
    }

    // dispatch first via normal handler
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(1, emittedSeen.size)
    assertEquals("somethingHappened", emittedSeen[0])

    // then attach a prepended listener
    aware.prependListener("somethingHappened") {
      val arg = it.getOrNull(0) as? Event ?: error("Invalid event")
      emittedSeen.add(arg.type)
      initialSeen.compareAndSet(false, true)
    }
    aware.addEventListener("somethingHappened") {
      val arg = it.getOrNull(0) as? Event ?: error("Invalid event")
      emittedSeen.add(arg.type)
      assertTrue(initialSeen.get(), "prepend listener should run before other listeners")
    }

    // dispatch again
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(4, emittedSeen.size)  // three total calls: previous handler and new
    assertTrue(initialSeen.get())
  }

  @Test fun testListenerCount() {
    val aware = ExampleEventAware()
    assertEquals(0, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount("unknown"))
    aware.addEventListener("somethingHappened") { /* no-op */ }
    assertEquals(1, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount("unknown"))
    aware.addEventListener("somethingHappened") { /* no-op */ }
    assertEquals(2, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount("unknown"))
  }

  @Test fun testListenerCountRemoval() {
    val aware = ExampleEventAware()
    assertEquals(0, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount("unknown"))
    var called = false
    val listener = EventListener { /* no-op */ }
    aware.addEventListener("somethingHappened", listener)
    assertEquals(1, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount("unknown"))
    aware.addEventListener("somethingHappened") {
      called = true
    }
    assertEquals(2, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount("unknown"))
    aware.removeEventListener("somethingHappened", listener)
    assertEquals(1, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount("unknown"))
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertTrue(called)
  }

  @Test fun testMaxListeners() {
    val aware = ExampleEventAware()
    assertNotEquals(0, aware.getMaxListeners())
    assertTrue(aware.getMaxListeners() > 0)
    assertEquals(events.provide().defaultMaxListeners, aware.getMaxListeners())
    aware.setMaxListeners(10)
    assertEquals(10, aware.getMaxListeners())
    val other = ExampleEventAware()
    assertEquals(events.provide().defaultMaxListeners, other.getMaxListeners())
  }

  @Test fun testCapturePhaseListener() {
    val aware = ExampleEventAware()
    val tokens = mutableListOf<String>()
    val listenerOne = EventListener { tokens.add("one") }
    val listenerTwo = EventListener { tokens.add("two") }
    val listenerThree = EventListener { tokens.add("three") }

    aware.addEventListener("somethingHappened", AddEventListenerOptions(capture = true), listenerOne)
    aware.prependListener("somethingHappened", listenerTwo)
    aware.addEventListener("somethingHappened", AddEventListenerOptions.DEFAULTS, listenerThree)

    aware.dispatchEvent(CustomEvent("somethingHappened"))

    assertEquals(3, tokens.size)
    assertEquals("one", tokens[0])
    assertEquals("two", tokens[1])
    assertEquals("three", tokens[2])
  }

  @Test fun testEventNames() {
    val aware = ExampleEventAware()
    assertEquals(0, aware.eventNames().size)
    val registered = ExampleRegisteredEvents()
    assertEquals(1, registered.eventNames().size)
    assertEquals("exampleEvent", registered.eventNames()[0])
    val registeredByHand = EventAware.create(listOf("some", "events"))
    assertEquals(2, registeredByHand.eventNames().size)
    assertTrue(registeredByHand.eventNames().contains("some"))
    assertTrue(registeredByHand.eventNames().contains("events"))
  }

  @Test fun testCustomEventDetail() {
    val custom = object : CustomEvent("exampleEvent") {
      override val detail: String get() = "hi"
    }
    assertEquals("hi", assertNotNull(custom.detail))
    val other = object : CustomEvent("exampleEvent") {}
    assertNull(other.detail)
  }

  @Test fun testDispatchEmpty() {
    val aware = EventAware.create()
    val event = CustomEvent("somethingHappened")
    assertDoesNotThrow {
      aware.dispatchEvent(event)
    }
  }

  @Test fun testDispatchEmptyUnknown() {
    val aware = EventAware.create()
    val event = CustomEvent("someEventWhichHasNotBeenSeenBefore")
    assertDoesNotThrow {
      aware.dispatchEvent(event)
    }
  }

  @Test fun testDispatchSameEvent() {
    val event = CustomEvent("somethingHappened")
    val aware = EventAware.create()
    var counter = 0
    aware.addEventListener("somethingHappened") {
      counter += 1
    }
    assertDoesNotThrow {
      aware.dispatchEvent(event)
    }
    assertDoesNotThrow {
      aware.dispatchEvent(event)
    }
    assertDoesNotThrow {
      aware.dispatchEvent(event)
    }
    assertEquals(3, counter)
  }

  @Test fun testDispatchAfterRemove() {
    val tokens = mutableListOf<String>()
    val listenerOne = EventListener { tokens.add("one") }
    val listenerTwo = EventListener { tokens.add("two") }

    val aware = EventAware.create()
    aware.addEventListener("somethingHappened", listenerOne)
    assertEquals(1, aware.listenerCount("somethingHappened"))
    assertEquals(1, aware.listeners("somethingHappened").size)
    aware.addEventListener("somethingHappened", listenerTwo)
    assertEquals(2, aware.listenerCount("somethingHappened"))
    assertEquals(2, aware.listeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    aware.removeEventListener("somethingHappened", listenerOne)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(3, tokens.size)
    assertEquals("one", tokens[0])
    assertEquals("two", tokens[1])
    assertEquals("two", tokens[2])
  }

  @Test fun testDispatchAfterRemoveAllByName() {
    val tokens = mutableListOf<String>()
    val listenerOne = EventListener { tokens.add("one") }
    val listenerTwo = EventListener { tokens.add("two") }

    val aware = EventAware.create()
    aware.addEventListener("somethingHappened", listenerOne)
    assertEquals(1, aware.listenerCount("somethingHappened"))
    assertEquals(1, aware.listenerCount(""))
    assertEquals(1, aware.listeners("somethingHappened").size)
    assertEquals(1, aware.rawListeners("somethingHappened").size)
    aware.addEventListener("somethingHappened", listenerTwo)
    assertEquals(2, aware.listenerCount("somethingHappened"))
    assertEquals(2, aware.listenerCount(""))
    assertEquals(2, aware.listeners("somethingHappened").size)
    assertEquals(2, aware.rawListeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    aware.removeEventListener("somethingHappened", listenerOne)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(3, tokens.size)
    assertEquals("one", tokens[0])
    assertEquals("two", tokens[1])
    assertEquals("two", tokens[2])
    aware.removeAllListeners("somethingHappened")
    assertEquals(0, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount(""))
    assertEquals(0, aware.listeners("somethingHappened").size)
    assertEquals(0, aware.rawListeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(3, tokens.size)
  }

  @Test fun testDispatchAfterRemoveAllByNameUnknown() {
    val aware = EventAware.create()
    assertDoesNotThrow {
      aware.removeAllListeners("somethingHappened")
    }
    assertDoesNotThrow {
      aware.removeAllListeners("another")
    }
  }

  @Test fun testDispatchAfterRemoveAllByNameIncludesCapture() {
    val tokens = mutableListOf<String>()
    val listenerOne = EventListener { tokens.add("one") }
    val listenerTwo = EventListener { tokens.add("two") }

    val aware = EventAware.create()
    aware.addEventListener("somethingHappened", AddEventListenerOptions(capture = true), listenerOne)
    assertEquals(1, aware.listenerCount("somethingHappened"))
    assertEquals(1, aware.listenerCount(""))
    assertEquals(1, aware.listeners("somethingHappened").size)
    assertEquals(1, aware.rawListeners("somethingHappened").size)
    aware.addEventListener("somethingHappened", listenerTwo)
    assertEquals(2, aware.listenerCount("somethingHappened"))
    assertEquals(2, aware.listenerCount(""))
    assertEquals(2, aware.listeners("somethingHappened").size)
    assertEquals(2, aware.rawListeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    aware.removeAllListeners("somethingHappened")
    assertEquals(0, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount(""))
    assertEquals(0, aware.listeners("somethingHappened").size)
    assertEquals(0, aware.rawListeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(2, tokens.size)
  }

  @Test fun testSetErrorMonitor() {
    val fresh = NodeEventsModuleFacade.create()
    assertNull(fresh.errorMonitor)
    assertNull(events.provide().errorMonitor)
    fresh.errorMonitor = "hi"
    assertEquals("hi", assertNotNull(fresh.errorMonitor))
    assertNull(events.provide().errorMonitor)
    fresh.errorMonitor = null
    assertNull(fresh.errorMonitor)
  }

  @Test fun testDispatchAfterRemoveAllIncludesCapture() {
    val tokens = mutableListOf<String>()
    val listenerOne = EventListener { tokens.add("one") }
    val listenerTwo = EventListener { tokens.add("two") }

    val aware = EventAware.create()
    aware.addEventListener("somethingHappened", AddEventListenerOptions(capture = true), listenerOne)
    assertEquals(1, aware.listenerCount("somethingHappened"))
    assertEquals(1, aware.listenerCount(""))
    assertEquals(1, aware.listeners("somethingHappened").size)
    assertEquals(1, aware.rawListeners("somethingHappened").size)
    aware.addEventListener("somethingHappened", listenerTwo)
    assertEquals(2, aware.listenerCount("somethingHappened"))
    assertEquals(2, aware.listenerCount(""))
    assertEquals(2, aware.listeners("somethingHappened").size)
    assertEquals(2, aware.rawListeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    aware.removeAllListeners()
    assertEquals(0, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount(""))
    assertEquals(0, aware.listeners("somethingHappened").size)
    assertEquals(0, aware.rawListeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(2, tokens.size)
  }

  @Test fun testDispatchAfterRemoveAll() {
    val tokens = mutableListOf<String>()
    val listenerOne = EventListener { tokens.add("one") }
    val listenerTwo = EventListener { tokens.add("two") }

    val aware = EventAware.create()
    aware.addEventListener("somethingHappened", listenerOne)
    assertEquals(1, aware.listenerCount("somethingHappened"))
    assertEquals(1, aware.listenerCount(""))
    assertEquals(1, aware.listeners("somethingHappened").size)
    assertEquals(1, aware.rawListeners("somethingHappened").size)
    aware.addEventListener("somethingHappened", listenerTwo)
    assertEquals(2, aware.listenerCount("somethingHappened"))
    assertEquals(2, aware.listenerCount(""))
    assertEquals(2, aware.listeners("somethingHappened").size)
    assertEquals(2, aware.rawListeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    aware.removeEventListener("somethingHappened", listenerOne)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(3, tokens.size)
    assertEquals("one", tokens[0])
    assertEquals("two", tokens[1])
    assertEquals("two", tokens[2])
    aware.removeAllListeners()
    assertEquals(0, aware.listenerCount("somethingHappened"))
    assertEquals(0, aware.listenerCount(""))
    assertEquals(0, aware.listeners("somethingHappened").size)
    assertEquals(0, aware.rawListeners("somethingHappened").size)
    aware.dispatchEvent(CustomEvent("somethingHappened"))
    assertEquals(3, tokens.size)
  }

  @Test fun testEventsApiAsProxyObject() {
    val keys = assertNotNull(events.provide().getMemberKeys())
    assertTrue(keys.isNotEmpty())
    keys.forEach { assertTrue(events.provide().hasMember(it)) }
    assertThrows<UnsupportedOperationException> {
      events.provide().putMember("foo", asValue("bar"))
    }
    assertThrows<UnsupportedOperationException> {
      events.provide().removeMember("foo")
    }
  }

  @Test fun testEventListenerExceptionsShouldBeSuppressed() {
    val listener = EventListener {
      error("boom!")
    }
    val aware = EventAware.create()
    aware.addEventListener("somethingHappened", listener)
    assertDoesNotThrow {
      aware.dispatchEvent(CustomEvent("somethingHappened"))
    }
  }

  @Test fun testEventWithEmptyNameCausesError() {
    val event = CustomEvent()
    val aware = EventAware.create()
    assertThrows<ValueError> {
      aware.dispatchEvent(event)
    }
  }

  @Test fun testEventWithBlankNameCausesError() {
    val event = CustomEvent(" ")
    val aware = EventAware.create()
    assertThrows<ValueError> {
      aware.dispatchEvent(event)
    }
  }

  @Test fun testListenerEvents() {
    val aware = EventAware.create()
    val addedListeners = mutableListOf<String>()
    var seenCallTwo = false
    val listenerOne = EventListener {
      val eventName = it.getOrNull(0) as? String ?: error("Invalid event")
      addedListeners.add(eventName)
    }
    aware.addEventListener(StandardEventName.NEW_LISTENER, listenerOne)
    val listenerTwo = EventListener {
      seenCallTwo = true
    }
    aware.addEventListener("arbitraryEventName", listenerTwo)
    assertEquals(2, addedListeners.size)
    assertEquals(StandardEventName.NEW_LISTENER, addedListeners[0])
    assertEquals("arbitraryEventName", addedListeners[1])
    aware.dispatchEvent(CustomEvent("arbitraryEventName"))
    assertTrue(seenCallTwo)
  }

  @Test fun testEmitInvalidEventName() {
    val aware = EventAware.create()
    assertThrows<ValueError> { aware.emit("") }
    assertThrows<ValueError> { aware.emit(" ") }
    assertThrows<ValueError> { aware.emit("  ") }
  }
}
