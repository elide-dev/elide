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
    /** @inheritDoc */
    override var seconds: Long
      get() = builder.seconds
      set(value) { builder.seconds = value }

    /** @inheritDoc */
    override var nanos: Int
      get() = builder.nanos
      set(value) { builder.nanos = value }

    /** @inheritDoc */
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

    /** @inheritDoc */
    override fun empty(): ProtoTimestamp = TimestampBuilder.newBuilder().build()

    /** @inheritDoc */
    override fun copy(model: ProtoTimestamp): ProtoTimestamp = model.toBuilder().build()

    /** @inheritDoc */
    override fun defaultInstance(): ProtoTimestamp = DEFAULT_INSTANCE

    /** @inheritDoc */
    override fun builder() = TimestampBuilder.newBuilder()
  }

  /** @inheritDoc */
  override fun factory() = Factory

  /** @inheritDoc */
  override fun toBuilder(): TimestampBuilder = TimestampBuilder.of(timestamp.toBuilder())

  /** @inheritDoc */
  override val seconds: Long get() = timestamp.seconds

  /** @inheritDoc */
  override val nanos: Int get() = timestamp.nanos

  /** @inheritDoc */
  override fun toInstant(): Instant = timestamp.toKotlinInstant()
}
