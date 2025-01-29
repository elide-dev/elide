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
package elide.runtime.intrinsics.js.node.events

import elide.annotations.API
import elide.runtime.intrinsics.js.JsSymbol

/**
 * ## Node API: Event
 *
 * The [Event] object is an adaptation of the [Event Web API](https://dom.spec.whatwg.org/#event). Instances are created
 * internally by Elide.
 *
 * The root [Event] interface describes properties which are made available on all event types, regardless of how they
 * are created or used; application-level events should extend [CustomEvent] to gain [Event] implementation access.
 *
 * &nbsp;
 *
 * ### Event Properties
 *
 * Events, by nature, are always expected to _at least_ define a string [type] as their "event name," or "event type."
 * Listeners typically subscribe to events based on the [type] value.
 *
 * The event [type] can be a [String] or a [JsSymbol].
 *
 * &nbsp;
 *
 * ### Custom Events
 *
 * Applications that wish to dispatch or emit their own events can do so by either (1) extending [CustomEvent] and
 * overriding properties as needed; or (2) creating an instance of [CustomEvent] via the provided public constructor.
 *
 * Custom events can be initialized via their constructor, or via the [CustomEvent.initEvent] method.
 */
@API public sealed interface Event {
  /**
   * TBD.
   */
  public val bubbles: Boolean

  /**
   * TBD.
   */
  public val cancelable: Boolean

  /**
   * TBD.
   */
  public val composed: Boolean

  /**
   * TBD.
   */
  public val currentTarget: EventTarget?

  /**
   * TBD.
   */
  public val defaultPrevented: Boolean

  /**
   * TBD.
   */
  public val eventPhase: Int

  /**
   * TBD.
   */
  public val isTrusted: Boolean

  /**
   * TBD.
   */
  public val returnValue: Boolean

  /**
   * TBD.
   */
  public val srcElement: EventTarget?

  /**
   * TBD.
   */
  public val target: EventTarget?

  /**
   * TBD.
   */
  public val timeStamp: Double

  /**
   * TBD.
   */
  public val type: String

  /**
   * TBD.
   */
  public fun composedPath(): Array<EventTarget>

  /**
   * TBD.
   */
  public fun initEvent(type: String, bubbles: Boolean, cancelable: Boolean)

  /**
   * TBD.
   */
  public fun preventDefault()

  /**
   * TBD.
   */
  public fun stopImmediatePropagation()

  /**
   * TBD.
   */
  public fun stopPropagation()
}
