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

import elide.annotations.API
import elide.runtime.interop.ReadOnlyProxyObject
import elide.vm.annotations.Polyglot

/**
 * ## Message Channel
 *
 * Implements the `MessageChannel` global function for JavaScript, which is used to communicate between multiple
 * browsing or execution contexts.
 *
 * ### Standards Compliance
 *
 * The `MessageChannel` class is defined as part of the WinterTC Minimum Common API.
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/MessageChannel)
 */
@API public interface MessageChannel : ReadOnlyProxyObject {
  /**
   * The first port created for this channel; initialized when the channel is constructed.
   *
   * Message ports are event-aware types. Events are delivered to the message port, and received via event handlers. See
   * [MessagePort] for usage.
   */
  @get:Polyglot public val port1: MessagePort

  /**
   * The second port created for this channel; initialized when the channel is constructed.
   *
   * Message ports are event-aware types. Events are delivered to the message port, and received via event handlers. See
   * [MessagePort] for usage.
   */
  @get:Polyglot public val port2: MessagePort
}
