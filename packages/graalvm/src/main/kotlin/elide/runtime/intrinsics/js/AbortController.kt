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
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * # Abort Controller
 *
 * Manages abort control through a tandem [AbortSignal]; typically used to control abortion of pending or in-flight
 * async operations.
 *
 * From MDN:
 *
 * The AbortController interface represents a controller object that allows you to abort one or more Web requests as and
 * when desired.
 *
 * You can create a new AbortController object using the AbortController() constructor. Communicating with an
 * asynchronous operation is done using an AbortSignal object.
 *
 * [`AbortController` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortController)
 * @see AbortSignal `AbortSignal` tandem event type
 */
@API @HostAccess.Implementable public interface AbortController {
  /**
   * Abort Signal
   *
   * Provides the [AbortSignal] instance paired with, and managed by, this abort controller. From MDN:
   *
   * Returns an [AbortSignal] object instance, which can be used to communicate with, or to abort, an asynchronous
   * operation.
   *
   * [`AbortController.signal` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortController/signal)
   */
  @get:Polyglot public val signal: AbortSignal

  /**
   * Mark this abort controller as transferable, enabling it for use across threaded contexts, and with `postMessage`
   * and similar mechanisms.
   *
   * This method is meant for host-side use only.
   */
  public fun markTransferable()

  /**
   * Indicate whether [markTransferable] has been called on this controller, and, therefore, whether it is eligible for
   * use within the context of `postMessage` and similar mechanisms.
   *
   * @return `true` if this controller is transferable, `false` otherwise.
   */
  public fun canBeTransferred(): Boolean

  /**
   * Abort
   *
   * Triggers this controller to use the paired [AbortSignal] to abort any relying operation. From MDN:
   *
   * Aborts an asynchronous operation before it has completed. This is able to abort fetch requests, consumption of a
   * response bodies, and streams.
   *
   * [`AbortController.abort()` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortController/abort)
   *
   * @param reason Optional reason parameter to provide
   */
  @Polyglot public fun abort(reason: Any? = null)

  /**
   * ## Abort Controller - Factory
   *
   * Models the structure expected/provided for constructors and static factories of an [AbortController].
   *
   * [`AbortController` on MDN](https://developer.mozilla.org/en-US/docs/Web/API/AbortController#constructor)
   */
  @API public interface Factory : ProxyInstantiable {}
}
