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

@file:Suppress("RedundantVisibilityModifier")

package elide.proto.api.wkt

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import elide.proto.api.Record
import java.time.Instant as JavaInstant

/**
 * TBD.
 */
public interface Timestamp<Concrete, Builder> :
  Comparable<Timestamp<*, *>>,
  Record<Timestamp<Concrete, Builder>, Builder> where Builder: Timestamp.IBuilder<Concrete> {
  /**
   * TBD.
   */
  public interface IBuilder<Timestamp> {
    /**
     * TBD.
     */
    public var seconds: Long

    /**
     * TBD.
     */
    public var nanos: Int

    /**
     * TBD.
     */
    public fun build(): Timestamp
  }

  /**
   * TBD.
   */
  public interface Factory<Concrete, B: IBuilder<Concrete>> : Record.Factory<Concrete, B> {
    /** @InheritDoc */
    override fun create(op: B.() -> Unit): Concrete = builder().let { b ->
      op.invoke(b)
      b.build()
    }

    // -- Timestamp Factory API: Type Conversion -- //

    /**
     * TBD.
     */
    public fun at(instantValue: Instant): Concrete = atSecond(
      instantValue.epochSeconds,
      instantValue.nanosecondsOfSecond,
    )

    /**
     * TBD.
     */
    public fun from(dateValue: java.util.Date): Concrete = from(dateValue.toInstant())

    /**
     * TBD.
     */
    public fun from(dateValue: java.sql.Date): Concrete = from(dateValue.toInstant())

    /**
     * TBD.
     */
    public fun from(instantValue: JavaInstant): Concrete = at(instantValue.toKotlinInstant())

    // -- Timestamp Factory API: Seconds -- //

    /**
     * TBD.
     */
    public fun atSecond(secondsValue: Long): Concrete = create {
      seconds = secondsValue
    }

    /**
     * TBD.
     */
    public fun atSecond(secondsValue: Long, nanosValue: Int): Concrete = create {
      seconds = secondsValue
      nanos = nanosValue
    }

    // -- Timestamp Factory API: Milliseconds -- //

    /**
     * TBD.
     */
    public fun atMilli(millisValue: Long): Concrete = millisValue.milliseconds.toComponents { secs, nans ->
      create {
        seconds = secs
        nanos = nans
      }
    }

    // -- Timestamp Factory API: Now -- //

    /**
     * TBD.
     */
    public fun now(): Concrete = at(Clock.System.now())

    // -- Timestamp Factory API: From-Now -- //

    /**
     * TBD.
     */
    public fun fromNow(durationRelative: Duration): Concrete = at(Clock.System.now().plus(
      durationRelative
    ))

    /**
     * TBD.
     */
    public fun secondsFromNow(secondsRelative: Long): Concrete = at(Clock.System.now().plus(
      secondsRelative.seconds
    ))

    /**
     *
     */
    public fun millisFromNow(millisRelative: Long): Concrete = at(Clock.System.now().plus(
      millisRelative.milliseconds
    ))
  }

  /**
   * TBD.
   */
  public val seconds: Long get() = toInstant().epochSeconds

  /**
   * TBD.
   */
  public val millis: Long get() = toInstant().toEpochMilliseconds()

  /**
   * TBD.
   */
  public val nanos: Int get() = toInstant().nanosecondsOfSecond

  /**
   * TBD.
   */
  public fun toInstant(): Instant

  /**
   * TBD.
   */
  public fun toJavaInstant(): JavaInstant = toInstant().toJavaInstant()

  /**
   * TBD.
   */
  override fun compareTo(other: Timestamp<*, *>): Int = other.toInstant().compareTo(toInstant())
}
