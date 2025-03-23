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
import org.graalvm.polyglot.proxy.ProxyInstantiable
import jakarta.inject.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.MessageChannel
import elide.runtime.intrinsics.js.MessagePort
import elide.vm.annotations.Polyglot

// Name of the `MessageChannel` class in the global scope.
private const val MESSAGE_CHANNEL_NAME = "MessageChannel"

// Name of the `MessagePort` class in the global scope.
private const val MESSAGE_PORT_NAME = "MessagePort"

// Constants for properties and methods of `MessageChannel` and `MessagePort`.
private const val MESSAGE_CHANNEL_PORT1 = "port1"
private const val MESSAGE_CHANNEL_PORT2 = "port2"

private val messageChannelProps = arrayOf(
  MESSAGE_CHANNEL_PORT1,
  MESSAGE_CHANNEL_PORT2,
)

// Public JavaScript symbol for the `MessageChannel` class.
private val MESSAGE_CHANNEL_SYMBOL = MESSAGE_CHANNEL_NAME.asPublicJsSymbol()

// Public JavaScript symbol for the `MessagePort` class.
private val MESSAGE_PORT_SYMBOL = MESSAGE_PORT_NAME.asPublicJsSymbol()

// Pair-wise message ports for the `MessageChannel` instance.
internal typealias MessagePortPair = Pair<MessagePortBuiltin, MessagePortBuiltin>

// Implements the `MessageChannel` class.
@Singleton @Intrinsic public class MessageChannelBuiltin : ProxyInstantiable, AbstractJsIntrinsic() {
  /**
   * ### Message Channel Instance
   *
   * Created when [MessageChannelBuiltin] receives a message to instantiate a new `MessageChannel` object.
   */
  public class MessageChannelInstance internal constructor (private val pair: MessagePortPair) :
    MessageChannel,
    ReadOnlyProxyObject {
    // Channel must be active to relay messages.
    @Volatile private var active = false

    @get:Polyglot override val port1: MessagePort get() = pair.first
    @get:Polyglot override val port2: MessagePort get() = pair.second

    internal fun activate() {
      check(!active) { "Message channel instance is not active" }
      active = true
      pair.first.activate()
      pair.second.activate()
    }

    internal fun target(port: MessagePortBuiltin): MessagePortBuiltin {
      return when (port) {
        pair.first -> pair.second
        pair.second -> pair.first
        else -> error("Invalid message port")
      }
    }

    override fun getMemberKeys(): Array<String> = messageChannelProps

    override fun getMember(key: String?): Any? = when (key) {
      MESSAGE_CHANNEL_PORT1 -> pair.first
      MESSAGE_CHANNEL_PORT2 -> pair.second
      else -> null
    }
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[MESSAGE_CHANNEL_SYMBOL] = this
    bindings[MESSAGE_PORT_SYMBOL] = MessagePortBuiltin::class.java
  }

  override fun newInstance(vararg arguments: Value?): MessageChannelInstance {
    val port1 = MessagePortBuiltin.create(MESSAGE_CHANNEL_PORT1)
    val port2 = MessagePortBuiltin.create(MESSAGE_CHANNEL_PORT2)
    val instance = MessageChannelInstance(port1 to port2)
    port1.bind(instance)
    port2.bind(instance)
    instance.activate()
    return instance
  }
}
