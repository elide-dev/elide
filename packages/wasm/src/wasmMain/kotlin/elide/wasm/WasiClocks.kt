/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.wasm

import org.kowasm.wasi.internal.ClockId
import org.kowasm.wasi.internal.clockResGet
import org.kowasm.wasi.internal.clockTimeGet
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// Manual (for now) conversion from *.wit.md files available at https://github.com/WebAssembly/wasi-clocks/tree/64b775a99a9311cc633a83d611c11ed3d3abc634

/** A timestamp in nanoseconds. **/
typealias Instant = Long

/** A time and date in seconds plus nanoseconds. */
data class DateTime(val seconds: Long, val nanoseconds: Int)

/**
 * A monotonic clock is a clock which has an unspecified initial value, and
 * successive reads of the clock will produce non-decreasing values.
 *
 * It is intended for measuring elapsed time.
 */
interface MonotonicClock {

    /** Read the current value of the clock.
     *
     * As this the clock is monotonic, calling this function repeatedly will produce
     * a sequence of non-decreasing values.
     */
    fun now(): Instant

    /** Query the resolution of the clock. **/
    fun resolution(): Instant

    /** This creates a new [MonotonicTimer] with the given starting time. It will
     * count down from this time until it reaches zero.
     */
    fun newTimer(initial: Instant): MonotonicTimer
}

/**
 * A wall clock is a clock which measures the date and time according to some
 * external reference.
 *
 * External references may be reset, so this clock is not necessarily
 * monotonic, making it unsuitable for measuring elapsed time.
 *
 * It is intended for reporting the current date and time for humans.
 */
interface WallClock {

    /**
     * Read the current value of the clock.
     *
     * As this the clock is not monotonic, calling this function repeatedly will
     * not necessarily produce a sequence of non-decreasing values.
     *
     * The returned timestamps represent the number of seconds since
     * 1970-01-01T00:00:00Z, also known as [POSIX's Seconds Since the Epoch], also
     * known as [Unix Time].
     *
     * The nanoseconds field of the output is always less than 1000000000.
     *
     * [POSIX's Seconds Since the Epoch]: https://pubs.opengroup.org/onlinepubs/9699919799/xrat/V4_xbd_chap04.html#tag_21_04_16
     * [Unix Time]: https://en.wikipedia.org/wiki/Unix_time
     */
    fun now(): DateTime

    /**
     * Query the resolution of the clock.
     * The nanoseconds field of the output is always less than 1000000000.
     */
    fun resolution(): DateTime
}

/**
 * This is a timer that counts down from a given starting time down to zero
 * on a monotonic clock.
 */
interface MonotonicTimer {

    /** Returns the amount of time left before this timer reaches zero. **/
    fun current(): Instant

    /** Returns a future that completes when the timer reaches zero. **/
    // fun expiration(): Deferred<Unit>
}

/**
 * Default monotonic clock, suitable for general-purpose application needs.
 *
 * This allocates a new handle, so applications with frequent need of a clock
 * handle should call this function once and reuse the handle instead of
 * calling this function each time.
 */
object DefaultMonotonicClock : MonotonicClock {

    override fun now(): Instant {
        val resolution = clockResGet(ClockId.MONOTONIC)
        return clockTimeGet(ClockId.MONOTONIC, resolution)
    }

    override fun resolution(): Instant {
        return clockResGet(ClockId.MONOTONIC)
    }

    override fun newTimer(initial: Instant): MonotonicTimer {
        TODO("Not yet implemented")
    }
}

/**
 * Default wall clock, suitable for general-purpose application needs.
 */
object DefaultWallClock : WallClock {

    override fun now(): DateTime {
        val resolution = clockResGet(ClockId.REALTIME)
        val timestamp = clockTimeGet(ClockId.REALTIME, resolution)
        val duration = timestamp.toDuration(DurationUnit.NANOSECONDS)
        return duration.toComponents { seconds, nanoseconds -> DateTime(seconds, nanoseconds) }
    }

    override fun resolution(): DateTime {
        val resolution = clockResGet(ClockId.REALTIME)
        val duration = resolution.toDuration(DurationUnit.NANOSECONDS)
        return duration.toComponents { seconds, nanoseconds -> DateTime(seconds, nanoseconds) }
    }
}
