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
    /** @inheritDoc */
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

    /** @inheritDoc */
    override fun empty(): KxTimestamp = TimestampBuilder.newBuilder().build()

    /** @inheritDoc */
    override fun copy(model: KxTimestamp): KxTimestamp = TimestampBuilder.of(
      model.timestamp.first,
      model.timestamp.second,
    ).build()

    /** @inheritDoc */
    override fun defaultInstance(): KxTimestamp = DEFAULT_INSTANCE

    /** @inheritDoc */
    override fun builder(): TimestampBuilder = TimestampBuilder.newBuilder()
  }

  /** @inheritDoc */
  override fun factory() = Factory

  override fun toBuilder(): TimestampBuilder = TimestampBuilder.of(timestamp.first, timestamp.second)

  /** @inheritDoc */
  override fun toInstant(): Instant = Instant.fromEpochSeconds(timestamp.first, timestamp.second)
}
