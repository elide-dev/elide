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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.javascript

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.lang.ref.WeakReference
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.MessagePort
import elide.runtime.intrinsics.js.Transferable
import elide.runtime.intrinsics.js.messaging.PostMessageOptions
import elide.runtime.intrinsics.js.node.events.CustomEvent
import elide.runtime.intrinsics.js.node.events.Event
import elide.runtime.intrinsics.js.node.events.EventListener
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.runtime.node.events.EventAwareProxy
import elide.runtime.node.events.StandardEventName
import elide.vm.annotations.Polyglot

// Properties and methods on a `MessagePort`.
private const val MESSAGE_EV = StandardEventName.MESSAGE
private const val ONMESSAGE_PROP = "on${MESSAGE_EV}"
private const val POST_MESSAGE_FN = "postMessage"
private const val CLOSE_FN = "close"
private const val START_FN = "start"

private val messagePortProps = arrayOf(
  ONMESSAGE_PROP,
  POST_MESSAGE_FN,
  CLOSE_FN,
  START_FN,
)

private sealed interface OnMessageHandler {
  fun dispatch(message: Any?)

  @JvmInline private value class FunctionHandler(private val fn: (Any?) -> Unit) : OnMessageHandler {
    override fun dispatch(message: Any?) {
      fn(message)
    }
  }

  @JvmInline private value class GuestHandler(private val fn: Value) : OnMessageHandler {
    override fun dispatch(message: Any?) {
      fn.executeVoid(message)
    }
  }

  @JvmInline private value class ProxyHandler(private val fn: ProxyExecutable) : OnMessageHandler {
    override fun dispatch(message: Any?) {
      fn.execute(Value.asValue(message))
    }
  }

  companion object {
    fun create(fn: (Any?) -> Unit): OnMessageHandler = FunctionHandler(fn)
    fun create(fn: Value): OnMessageHandler = GuestHandler(fn)
    fun create(fn: ProxyExecutable): OnMessageHandler = ProxyHandler(fn)
  }
}

// Implements the `MessagePort` class.
public class MessagePortBuiltin private constructor (
  private val portName: String,
  private val events: EventAwareProxy = EventAwareProxy.create(),
) : MessagePort, EventTarget by events, ProxyObject {
  // Held as weak because a channel may be closed and the port should be garbage collected.
  private lateinit var channel: WeakReference<MessageChannelBuiltin.MessageChannelInstance>

  // Whether this message port is still open for messages.
  @Volatile private var open = false

  // Whether this message port has started listening.
  @Volatile private var listening = false

  // Currently assigned main message handler. Responds to `onmessage`.
  @Volatile private var handler: OnMessageHandler? = null

  internal fun bind(channel: MessageChannelBuiltin.MessageChannelInstance) {
    assert(!this::channel.isInitialized) { "Message port $portName is already bound to a channel." }
    this.channel = WeakReference(channel)
  }

  internal fun activate() {
    assert(this::channel.isInitialized) { "Message channel instance is not active" }
    open = true
  }

  // Event listener for incoming messages to this port.
  private val listener = EventListener { it ->
    handler?.dispatch(it)
  }

  @Polyglot override fun start() {
    check(open) { "MessagePort is closed" }
    if (!listening) {
      listening = true
      events.addEventListener(StandardEventName.MESSAGE, listener)
    }
  }

  @Polyglot override fun postMessage(message: Any?, transfer: Collection<Transferable>?, options: PostMessageOptions?) {
    check(open) { "MessagePort is closed" }
    val ev = CustomEvent(MESSAGE_EV, mapOf("data" to message))
    channel.get()
      ?.target(this)
      ?.dispatchEvent(ev)
  }

  override fun dispatchEvent(event: Event): Boolean = when (event.type) {
    StandardEventName.MESSAGE -> {
      handler?.dispatch(event)
      events.dispatchEvent(event)
    }
    else -> events.dispatchEvent(event)
  }

  @Polyglot override fun close() {
    if (open) {
      open = false
    }
    if (listening) {
      events.removeEventListener(StandardEventName.MESSAGE, listener)
      listening = false
    }
  }

  override fun toString(): String {
    return "MessagePort($portName, active=$open)"
  }

  override fun getMemberKeys(): Array<String> = messagePortProps.plus(events.memberKeys)

  @Suppress("SpreadOperator")
  override fun getMember(key: String): Any? = when (key) {
    ONMESSAGE_PROP -> handler
    POST_MESSAGE_FN -> ProxyExecutable { postMessage(*it) }
    CLOSE_FN -> ProxyExecutable { close() }
    START_FN -> ProxyExecutable { start() }
    else -> events.getMember(key)
  }

  override fun hasMember(key: String): Boolean = key in messagePortProps || (
    key == ONMESSAGE_PROP && handler != null
  ) || (
    events.hasMember(key)
  )

  override fun removeMember(key: String): Boolean = when (key) {
    // clear handler if instructed
    ONMESSAGE_PROP -> true.also { handler = null }
    else -> events.removeMember(key)
  }

  override fun putMember(key: String, value: Value?): Unit = when (key) {
    ONMESSAGE_PROP -> when {
      // handler can be a host object
      value != null && value.isProxyObject -> value.asProxyObject<ProxyExecutable>().let {
        this.handler = OnMessageHandler.create(it)
        start()
      }

      // handler must be non-null and executable
      value == null || value.isNull || !value.canExecute() ->
        throw JsError.typeError("Invalid handler for `$ONMESSAGE_PROP`")

      else -> handler = OnMessageHandler.create(value)
    }
    else -> events.putMember(key, value)
  }

  internal companion object {
    /** @return New [MessagePortBuiltin] bound weakly to the parent. */
    @JvmStatic fun create(name: String) = MessagePortBuiltin(name)
  }
}
