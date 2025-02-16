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

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.runtime.intrinsics.js.AbortSignal

/**
 * ## Node: Events Once Options
 *
 * Options which can be passed to the `events.once` method.
 */
@API public data class EventsOnceOptions(
  /**
   * Can be used to cancel waiting for the event.
   */
  public val signal: AbortSignal? = null,
)

/**
 * ## Node: Add Listener Options
 *
 * Options which can be passed to the `target.addEventListener` method.
 */
@API public data class AddEventListenerOptions(
  /**
   * Can be used to cancel waiting for the event.
   */
  public val signal: AbortSignal? = null,

  /**
   * When `true`, serves as a hint that the listener will not call the Event object's `preventDefault()` method.
   * Default: `false`.
   */
  public val passive: Boolean = false,

  /**
   * If `true`, the listener is removed after the next time a type event is dispatched.
   */
  public val once: Boolean = false,

  /**
   * If `true`, the listener is invoked when the event is dispatched in the capture phase.
   */
  public val capture: Boolean = false,
) {
  public companion object {
    /** Default option set. */
    public val DEFAULTS: AddEventListenerOptions = AddEventListenerOptions()

    /**
     * Create an [AddEventListenerOptions] instance from a guest value, which can be a hash-map style object or an
     * object with members.
     *
     * @param value The value to convert.
     * @return The converted options.
     */
    @JvmStatic public fun fromGuest(value: Value): AddEventListenerOptions {
      return when {
        value.hasMembers() -> AddEventListenerOptions(
          // abort signals are not supported yet
          signal = null,
          passive = value.getMember("passive").takeIf { it.isBoolean }?.asBoolean() ?: false,
          once = value.getMember("once").takeIf { it.isBoolean }?.asBoolean() ?: false,
          capture = value.getMember("capture").takeIf { it.isBoolean }?.asBoolean() ?: false,
        )

        value.hasHashEntries() -> AddEventListenerOptions(
          // abort signals are not supported yet
          signal = null,
          passive = value.getHashValue("passive").takeIf { it.isBoolean }?.asBoolean() ?: false,
          once = value.getHashValue("once").takeIf { it.isBoolean }?.asBoolean() ?: false,
          capture = value.getHashValue("capture").takeIf { it.isBoolean }?.asBoolean() ?: false,
        )

        else -> error("Cannot convert options from guest value")
      }
    }
  }
}

/**
 * ## Node: Remove Listener Options
 *
 * Options which can be passed to the `target.addEventListener` method.
 */
@API public data class RemoveEventListenerOptions(
  /**
   * If `true`, the removed from the capture phase.
   */
  public val capture: Boolean = false,
) {
  public companion object {
    /** Default option set. */
    public val DEFAULTS: RemoveEventListenerOptions = RemoveEventListenerOptions()

    /**
     * Create an [RemoveEventListenerOptions] instance from a guest value, which can be a hash-map style object or an
     * object with members.
     *
     * @param value The value to convert.
     * @return The converted options.
     */
    @JvmStatic public fun fromGuest(value: Value): RemoveEventListenerOptions {
      return when {
        value.hasMembers() -> RemoveEventListenerOptions(
          capture = value.getMember("capture").takeIf { it.isBoolean }?.asBoolean() ?: false,
        )

        value.hasHashEntries() -> RemoveEventListenerOptions(
          capture = value.getHashValue("capture").takeIf { it.isBoolean }?.asBoolean() ?: false,
        )

        else -> error("Cannot convert options from guest value")
      }
    }
  }
}
