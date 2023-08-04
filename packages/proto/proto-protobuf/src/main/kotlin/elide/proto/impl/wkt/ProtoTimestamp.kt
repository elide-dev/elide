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

@file:Suppress("RedundantVisibilityModifier", "unused")

package elide.proto.impl.wkt

import com.google.protobuf.Timestamp
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import elide.proto.api.wkt.Timestamp as ITimestamp

/** Implements a universal model timestamp, backed by a well-known-type Protocol Buffers [Timestamp]. */
public class ProtoTimestamp private constructor (private val timestamp: Timestamp) :
  ITimestamp<ProtoTimestamp, ProtoTimestamp.TimestampBuilder> {
  /** Builder interface for proto-based timestamps. */
  interface Builder : ITimestamp.IBuilder<ProtoTimestamp>

  /** Implementation of timestamp builder context backed by a proto-timestamp builder. */
  public class TimestampBuilder (private val builder: Timestamp.Builder) : Builder {
    override var seconds: Long
      get() = builder.seconds
      set(value) { builder.seconds = value }

    override var nanos: Int
      get() = builder.nanos
      set(value) { builder.nanos = value }

    override fun build(): ProtoTimestamp = ProtoTimestamp(builder.build())

    // Internal helpers.
    internal companion object {
      /** @return Fresh builder. */
      @JvmStatic internal fun newBuilder() = TimestampBuilder(Timestamp.newBuilder())

      /** @return Builder wrapping the provided proto-[builder]. */
      @JvmStatic internal fun of(builder: Timestamp.Builder) = TimestampBuilder(builder)
    }
  }

  /** Factory implementation for proto-backed timestamps. */
  public companion object Factory : ITimestamp.Factory<ProtoTimestamp, TimestampBuilder> {
    /** Default singleton (empty) instance. */
    @JvmStatic private val DEFAULT_INSTANCE: ProtoTimestamp = ProtoTimestamp(
      Timestamp.getDefaultInstance()
    )

    /** @return Kotlin [Instant] from a regular Protocol Buffers [Timestamp]. */
    public fun Timestamp.toKotlinInstant() = Instant.fromEpochSeconds(seconds, nanos)

    /** @return Java [java.time.Instant] from a regular Protocol Buffers [Timestamp]. */
    public fun Timestamp.toJavaInstant() = toKotlinInstant().toJavaInstant()

    /** @return [ProtoTimestamp] instance from a regular Protocol Buffers [Timestamp]. */
    public fun Timestamp.toModel() = ProtoTimestamp(this)

    override fun empty(): ProtoTimestamp = TimestampBuilder.newBuilder().build()

    override fun copy(model: ProtoTimestamp): ProtoTimestamp = model.toBuilder().build()

    override fun defaultInstance(): ProtoTimestamp = DEFAULT_INSTANCE

    override fun builder() = TimestampBuilder.newBuilder()
  }

  override fun factory() = Factory

  override fun toBuilder(): TimestampBuilder = TimestampBuilder.of(timestamp.toBuilder())

  override val seconds: Long get() = timestamp.seconds

  override val nanos: Int get() = timestamp.nanos

  override fun toInstant(): Instant = timestamp.toKotlinInstant()
}
