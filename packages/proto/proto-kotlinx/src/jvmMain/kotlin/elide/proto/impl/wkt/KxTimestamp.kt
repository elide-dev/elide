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

package elide.proto.impl.wkt

import kotlinx.datetime.Instant
import elide.proto.api.wkt.Timestamp as ITimestamp


@JvmInline public value class KxTimestamp private constructor (private val timestamp: Pair<Long, Int>) :
  ITimestamp<KxTimestamp, KxTimestamp.TimestampBuilder> {
  /** Builder for pure-Kotlin timestamps. */
  public class TimestampBuilder (
    override var seconds: Long,
    override var nanos: Int,
  ) : ITimestamp.IBuilder<KxTimestamp> {
    override fun build(): KxTimestamp = KxTimestamp(seconds to nanos)

    // Internal helpers.
    internal companion object {
      // Coming soon.
      @JvmStatic fun of(seconds: Long, nanos: Int) = TimestampBuilder(seconds, nanos)

      // Coming soon.
      @JvmStatic fun newBuilder() = of(0, 0)
    }
  }

  /** Factory implementation for pure-Kotlin timestamps. */
  public companion object Factory : ITimestamp.Factory<KxTimestamp, TimestampBuilder> {
    /** Default singleton (empty) instance. */
    @JvmStatic private val DEFAULT_INSTANCE: KxTimestamp = KxTimestamp(
      0L to 0
    )

    override fun empty(): KxTimestamp = TimestampBuilder.newBuilder().build()

    override fun copy(model: KxTimestamp): KxTimestamp = TimestampBuilder.of(
      model.timestamp.first,
      model.timestamp.second,
    ).build()

    override fun defaultInstance(): KxTimestamp = DEFAULT_INSTANCE

    override fun builder(): TimestampBuilder = TimestampBuilder.newBuilder()
  }

  override fun factory() = Factory

  override fun toBuilder(): TimestampBuilder = TimestampBuilder.of(timestamp.first, timestamp.second)

  override fun toInstant(): Instant = Instant.fromEpochSeconds(timestamp.first, timestamp.second)
}
