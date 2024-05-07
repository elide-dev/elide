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
package elide.runtime.intrinsics.js.node.events

import elide.annotations.API

/**
 * ## Node API: Event
 */
@API public interface Event {
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
  public val currentTarget: EventTarget

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
  public val srcElement: EventTarget

  /**
   * TBD.
   */
  public val target: EventTarget

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
