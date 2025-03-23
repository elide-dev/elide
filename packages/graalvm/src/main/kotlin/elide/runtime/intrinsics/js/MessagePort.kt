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
package elide.runtime.intrinsics.js

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import java.lang.AutoCloseable
import elide.annotations.API
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.messaging.PostMessageOptions
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.vm.annotations.Polyglot

/**
 * ## Message Port
 *
 * Implements the `MessagePort` global type for JavaScript, which represents one port in a two-sided channel which is
 * used to communicate between multiple browsing or execution contexts.
 *
 * ### Standards Compliance
 *
 * The `MessagePort` class is defined as part of the WinterTC Minimum Common API.
 *
 * ### Usage
 *
 * From MDN:
 * The MessagePort interface of the Channel Messaging API represents one of the two ports of a MessageChannel, allowing
 * messages to be sent from one port and listening out for them arriving at the other.
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/MessagePort)
 */
@API public interface MessagePort : ProxyObject, EventTarget, AutoCloseable {
  /**
   * ### Start
   *
   * Start services on this message port. Messages will not be delivered until `start` is called at least once; affixing
   * a message handler implies a call to `start`.
   *
   * From MDN:
   * The start() method of the MessagePort interface starts the sending of messages queued on the port. This method is
   * only needed when using [EventTarget.addEventListener]; it is implied when using `onmessage`.
   *
   * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/MessagePort/start)
   */
  @Polyglot public fun start()

  /**
   * ### Close Port
   *
   * From MDN:
   * The close() method of the MessagePort interface disconnects the port, so it is no longer active. This stops the
   * flow of messages to that port.
   *
   * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/MessagePort/close)
   */
  @Polyglot override fun close()

  /**
   * ### Post Message
   *
   * Post a message to this port, causing any listeners to be dispatched with the message and any given parameters. This
   * method has multiple overloads to support different types of messages and options.
   *
   * This particular method variant is designed for host-side dispatch with guest [Value] objects.
   *
   * @param values The values to post to the port.
   *
   * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/MessagePort/postMessage)
   */
  public fun postMessage(vararg values: Value): Unit = when (values.size) {
    0 -> throw JsError.typeError("`postMessage` requires at least one argument.")
    1 -> postMessage(values.first(), null, PostMessageOptions.empty())
    2 -> postMessage(values.first(), null, PostMessageOptions.from(values[1]))
    else -> postMessage(
      values.first(),
      values[1].asHostObject<Collection<Transferable>>(),
      PostMessageOptions.from(values[2])
    )
  }

  /**
   * ### Post Message
   *
   * Post a message to this port, causing any listeners to be dispatched with the message and any given parameters. This
   * method has multiple overloads to support different types of messages and options.
   *
   * This method is designed for guest dispatch in the form:
   * `postMessage(message, [ transfer_obj ])`
   *
   * @param message Message value to post.
   * @param transfer A list of transferable objects to post with the message.
   *
   * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/MessagePort/postMessage)
   */
  @Polyglot public fun postMessage(message: Any?, transfer: Collection<Transferable>?): Unit =
    postMessage(message, transfer, PostMessageOptions.empty())

  /**
   * ### Post Message
   *
   * Post a message to this port, causing any listeners to be dispatched with the message and any given parameters. This
   * method has multiple overloads to support different types of messages and options.
   *
   * This method is designed for guest dispatch in the form:
   * `postMessage(message, { options })`
   *
   * @param message Message value to post.
   * @param options Options to apply to this message posting.
   *
   * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/MessagePort/postMessage)
   */
  @Polyglot public fun postMessage(message: Any?, options: PostMessageOptions?): Unit =
    postMessage(message, null, options)

  /**
   * ### Post Message
   *
   * Post a message to this port, causing any listeners to be dispatched with the message and any given parameters. This
   * method has multiple overloads to support different types of messages and options.
   *
   * This method is designed for guest dispatch in the form:
   * `postMessage(message, [ transfer_obj ] { options })`
   *
   * @param message Message value to post.
   * @param transfer A list of transferable objects to post with the message.
   * @param options Options to apply to this message posting.
   *
   * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/MessagePort/postMessage)
   */
  @Polyglot public fun postMessage(message: Any?, transfer: Collection<Transferable>?, options: PostMessageOptions?)
}
