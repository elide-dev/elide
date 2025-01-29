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
import elide.annotations.API
import elide.vm.annotations.Polyglot

// Default timeout or delay value for JavaScript timers.
private const val DEFAULT_TIMEOUT: Long = 0L

/**
 * ID type for timers.
 */
public typealias TimerId = Long

/**
 * # JavaScript Timers
 *
 * Implements intrinsic JavaScript timers, at the familiar symbols `setTimeout` and `setInterval`; each method starts a
 * timer task which is dispatched after a specified delay. Interval timers repeat their execution until canceled, while
 * regular one-shot timers execute only once.
 *
 * Note that parameters are reversed for the host-side versions of these methods. JavaScript `setTimeout` takes the
 * callback value first, for example, and the delay second; [setTimeout] takes the delay first so that the callback can
 * use Kotlin's lambda syntax.
 *
 * &nbsp;
 */
@API public interface Timers {
  /**
   * ## Set Timeout
   *
   * Schedules a callback to be called after a delay. This method takes a JVM [callback].
   *
   * @param delay The delay in milliseconds.
   * @param callback The callback to be called.
   * @return The timer ID.
   */
  @Polyglot public fun setTimeout(delay: Long? = DEFAULT_TIMEOUT, callback: () -> Unit): TimerId

  /**
   * ## Set Timeout
   *
   * Schedules a callback to be called after a delay. This method variant takes a polyglot [Value].
   *
   * @param delay The delay in milliseconds.
   * @param arg Arguments to pass to the callback. Optional.
   * @param callback The callback to be called.
   * @return The timer ID.
   */
  @Polyglot public fun setTimeout(delay: Long? = DEFAULT_TIMEOUT, vararg arg: Any?, callback: Value): TimerId

  /**
   * ## Clear Timeout
   *
   * Cancels a one-shot timer task previously scheduled with [setTimeout].
   *
   * @param id The timer ID.
   */
  @Polyglot public fun clearTimeout(id: TimerId)

  /**
   * ## Set Interval
   *
   * Schedules a callback to be called repeatedly, each time after a delay. This method takes a JVM [callback].
   *
   * @param delay The delay in milliseconds.
   * @param callback The callback to be called.
   * @return The timer ID.
   */
  @Polyglot public fun setInterval(delay: Long? = DEFAULT_TIMEOUT, callback: () -> Unit): TimerId

  /**
   * ## Set Interval
   *
   * Schedules a callback to be called repeatedly, each time after a delay. This method variant takes a polyglot
   * [Value].
   *
   * @param delay The delay in milliseconds.
   * @param arg Arguments to pass to the callback. Optional.
   * @param callback The callback to be called.
   * @return The timer ID.
   */
  @Polyglot public fun setInterval(delay: Long? = DEFAULT_TIMEOUT, vararg arg: Any?, callback: Value): TimerId

  /**
   * ## Clear Interval
   *
   * Cancels a repeated timer task previously scheduled with [setTimeout].
   *
   * @param id The timer ID.
   */
  @Polyglot public fun clearInterval(id: TimerId)
}
