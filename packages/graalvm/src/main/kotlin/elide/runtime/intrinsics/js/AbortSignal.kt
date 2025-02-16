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

import org.graalvm.polyglot.HostAccess
import elide.annotations.API
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.vm.annotations.Polyglot

/**
 * # Abort Signal
 *
 * Abort signals are used to trigger an abort of an operation; from MDN:
 *
 * The AbortSignal interface represents a signal object that allows you to communicate with an asynchronous operation
 * (such as a fetch request) and abort it if required via an AbortController object.
 *
 * Note that [AbortSignal] is also an [EventTarget]; the event `abort` will be emitted when the signal is aborted.
 *
 * [`AbortSignal` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal)
 * [`AbortController` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortController)
 * @see AbortController Companion `AbortController` class
 */
@API @HostAccess.Implementable public interface AbortSignal : EventTarget {
  /**
   * ## Aborted
   *
   * Read-only property indicating whether the signal has been aborted; from MDN:
   *
   * A Boolean that indicates whether the request(s) the signal is communicating with is/are aborted (true) or not
   * (false).
   *
   * [`AbortSignal.aborted` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal/aborted)
   */
  @get:Polyglot public val aborted: Boolean

  /**
   * ## Reason
   *
   * Read-only property indicating the reason the operation was aborted; from MDN:
   *
   * The reason read-only property returns a JavaScript value that indicates the abort reason. The property is
   * undefined when the signal has not been aborted. It can be set to a specific value when the signal is aborted, using
   * AbortController.abort() or AbortSignal.abort(). If not explicitly set in those methods, it defaults to "AbortError"
   * DOMException.
   *
   * [`AbortSignal.reason` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal/reason)
   */
  @get:Polyglot public val reason: Any?

  /**
   * ## Throw If Aborted
   *
   * Throws the [AbortSignal]'s [reason] for aborting, if aborted; from MDN:
   *
   * Throws the signal's abort reason if the signal has been aborted; otherwise it does nothing.
   *
   * [`AbortSignal.throwIfAborted` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal/throwIfAborted)
   */
  @Polyglot public fun throwIfAborted()

  /**
   * ## Abort Signal - Factory
   *
   * Models the structure expected/provided for constructors of an [AbortSignal].
   *
   * [`AbortSignal` static methods on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal#static_methods)
   */
  @API public interface Factory {
    /**
     * Returns an [AbortSignal] instance that is already set as aborted.
     *
     * [MDN Reference](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal/abort_static)
     *
     * @return An aborted AbortSignal instance.
     */
    @Polyglot public fun abort(): AbortSignal

    /**
     * The `any` static method takes an iterable of abort signals and returns an [AbortSignal]. The returned abort
     * signal is aborted when any of the input iterable abort signals are aborted.
     *
     * [MDN Reference](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal/any_static)
     *
     * @param iterable Iterable of instances expected to comply with [AbortSignal] to use as delegates
     * @return Configured [AbortSignal] instance.
     */
    @Polyglot public fun any(iterable: Iterable<AbortSignal>): AbortSignal

    /**
     * The `timeout` static method returns an [AbortSignal] that will automatically abort after a specified time.
     *
     * [MDN Reference](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal/timeout_static)
     *
     * @param time Timeout value; must be between `0` and maximum safe integer value
     * @return Configured [AbortSignal] instance.
     */
    @Polyglot public fun timeout(time: Int): AbortSignal
  }
}
