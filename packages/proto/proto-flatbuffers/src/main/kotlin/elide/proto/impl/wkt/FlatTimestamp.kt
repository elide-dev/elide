@file:Suppress("RedundantVisibilityModifier", "unused")

package elide.proto.impl.wkt

import com.google.flatbuffers.FlatBufferBuilder
import elide.proto.api.Record
import google.protobuf.Timestamp
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import elide.proto.api.wkt.Timestamp as ITimestamp

/** Flatbuffers-backed model timestamp. */
@JvmInline value class FlatTimestamp private constructor (
  private val timestamp: Timestamp
) : ITimestamp<FlatTimestamp, FlatTimestamp.TimestampBuilder> {
  /** Thin builder for a [FlatTimestamp]. */
  public class TimestampBuilder private constructor (
    private var _seconds: Long = 0L,
    private var _nanos: Int = 0,
    private var bufferBuilder: FlatBufferBuilder? = null,
  ) : ITimestamp.IBuilder<FlatTimestamp> {
    /** @inheritDoc */
    override var seconds: Long
      get() = _seconds
      set(value) { _seconds = value }

    /** @inheritDoc */
    override var nanos: Int
      get() = _nanos
      set(value) { _nanos = value }

    /** Set the Flatbuffers builder to use. */
    public fun setBuffer(bufferBuilder: FlatBufferBuilder): TimestampBuilder {
      this.bufferBuilder = bufferBuilder
      return this
    }

    /** @inheritDoc */
    override fun build(): FlatTimestamp {
      val buf = when (val bufferBuilder = this.bufferBuilder) {
        null -> FlatBufferBuilder()
        else -> bufferBuilder
      }
      val ts = Timestamp.createTimestamp(
        buf,
        _seconds,
        _nanos,
      )
      buf.finish(ts)

      return FlatTimestamp(
        Timestamp.getRootAsTimestamp(buf.dataBuffer())
      )
    }

    // Internal helpers.
    internal companion object {
      /** @return Fresh builder. */
      @JvmStatic internal fun newBuilder() = TimestampBuilder()

      /** @return Builder wrapping the provided proto-[builder]. */
      @JvmStatic internal fun of(builder: TimestampBuilder) = TimestampBuilder(
        builder._seconds,
        builder._nanos,
      )
    }
  }

  /** Factory implementation for Flatbuffers-backed timestamps. */
  public companion object Factory : ITimestamp.Factory<FlatTimestamp, TimestampBuilder> {
    /** Default singleton (empty) instance. */
    @JvmStatic private val DEFAULT_INSTANCE: FlatTimestamp = empty()

    /** @return Kotlin [Instant] from a regular Protocol Buffers [Timestamp]. */
    public fun Timestamp.toKotlinInstant() = Instant.fromEpochSeconds(seconds, nanos)

    /** @return Java [java.time.Instant] from a regular Protocol Buffers [Timestamp]. */
    public fun Timestamp.toJavaInstant() = toKotlinInstant().toJavaInstant()

    /** @return [ProtoTimestamp] instance from a regular Protocol Buffers [Timestamp]. */
    public fun Timestamp.toModel() = FlatTimestamp(this)

    /** @inheritDoc */
    override fun empty(): FlatTimestamp = TimestampBuilder.newBuilder().build()

    /** @inheritDoc */
    override fun copy(model: FlatTimestamp): FlatTimestamp = at(model.toInstant())

    /** @inheritDoc */
    override fun defaultInstance(): FlatTimestamp = DEFAULT_INSTANCE

    /** @inheritDoc */
    override fun builder(): TimestampBuilder = TimestampBuilder.newBuilder()
  }

  /** @inheritDoc */
  override fun factory(): Record.Factory<out elide.proto.api.wkt.Timestamp<FlatTimestamp, TimestampBuilder>, TimestampBuilder> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun toBuilder(): TimestampBuilder = TimestampBuilder.newBuilder().let { b ->
    b.seconds = this.seconds
    b.nanos = this.nanos
    b
  }

  /** @inheritDoc */
  override val seconds: Long get() = timestamp.seconds

  /** @inheritDoc */
  override val nanos: Int get() = timestamp.nanos

  /** @inheritDoc */
  override fun toInstant(): Instant = timestamp.toKotlinInstant()
}
