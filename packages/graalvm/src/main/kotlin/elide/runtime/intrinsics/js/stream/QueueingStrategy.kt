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
package elide.runtime.intrinsics.js.stream

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.QueueingStrategy.DefaultReadStrategy.highWaterMark
import elide.runtime.intrinsics.js.stream.QueueingStrategy.DefaultWriteStrategy.highWaterMark
import elide.vm.annotations.Polyglot

/**
 * A strategy used by stream controllers to manage backpressure from compatible sources.
 *
 * This interface is meant to cover both host and guest strategies, providing a clean API for streams to use regardless
 * of the origin of the strategy.
 */
public interface QueueingStrategy {
  /**
   * A high threshold targeted by the controller; once this threshold is reached, no new values will be requested from
   * the source.
   */
  @Polyglot public fun highWaterMark(): Double

  /** Calculate the size of an arbitrary chunk of data. */
  @Polyglot public fun size(chunk: Value?): Double

  /**
   * The default queuing strategy for readable streams, using a [highWaterMark] of `0.0` and measuring every chunk with
   * size `1.0`.
   */
  public object DefaultReadStrategy : QueueingStrategy {
    @Polyglot override fun highWaterMark(): Double = 0.0
    @Polyglot override fun size(chunk: Value?): Double = 1.0
  }

  /**
   * The default queuing strategy for writable streams, using a [highWaterMark] of `1.0` and measuring every chunk with
   * size `1.0`.
   */
  public object DefaultWriteStrategy : QueueingStrategy {
    @Polyglot override fun highWaterMark(): Double = 1.0
    @Polyglot override fun size(chunk: Value?): Double = 1.0
  }
}

/**
 * A wrapper around a guest [value] that allows its use as a [QueueingStrategy]. All methods delegate to invoking
 * the corresponding member.
 */
@JvmInline public value class GuestQueueingStrategy private constructor(public val value: Value) : QueueingStrategy {
  @Polyglot override fun highWaterMark(): Double = value.invokeMember(HIGH_WATER_MARK_MEMBER).asDouble()
  @Polyglot override fun size(chunk: Value?): Double = value.invokeMember(SIZE_MEMBER).asDouble()

  public companion object {
    private const val HIGH_WATER_MARK_MEMBER = "highWaterMark"
    private const val SIZE_MEMBER = "size"

    public fun from(value: Value): GuestQueueingStrategy {
      if (!value.canInvokeMember(HIGH_WATER_MARK_MEMBER))
        throw TypeError.create("Value $value is not a valid queueing strategy: no 'highWaterMark' method found")

      if (!value.canInvokeMember(SIZE_MEMBER))
        throw TypeError.create("Value $value is not a valid queueing strategy: no 'size' method found")

      return GuestQueueingStrategy(value)
    }
  }
}

@JvmInline public value class ByteLengthQueueingStrategy(private val highWaterMark: Double) : QueueingStrategy {
  @Polyglot public constructor(options: Value?) : this(
    options?.takeIf { it.hasMember("highWaterMark") }
      ?.getMember("highWaterMark")
      ?.takeIf { it.isNumber && it.fitsInDouble() }
      ?.asDouble()
      ?: throw TypeError.create("A highWaterMark value must be supplied when creating this strategy"),
  )

  @Polyglot override fun highWaterMark(): Double = highWaterMark
  @Polyglot override fun size(chunk: Value?): Double {
    if (chunk == null) throw TypeError.create("Cannot measure a null chunk")
    return chunk.takeIf { it.hasMember("byteLength") }
      ?.getMember("byteLength")
      ?.takeIf { it.isNumber && it.fitsInDouble() }
      ?.asDouble()
      ?: throw TypeError.create("Chunk has no 'byteLength' property")
  }

  public companion object : ProxyInstantiable {
    override fun newInstance(vararg arguments: Value?): Any = ByteLengthQueueingStrategy(arguments.firstOrNull())
  }
}

@JvmInline public value class CountQueueingStrategy(private val highWaterMark: Double) : QueueingStrategy {
  @Polyglot public constructor(options: Value?) : this(
    options?.takeIf { it.hasMember("highWaterMark") }
      ?.getMember("highWaterMark")
      ?.takeIf { it.isNumber && it.fitsInDouble() }
      ?.asDouble()
      ?: throw TypeError.create("A highWaterMark value must be supplied when creating this strategy"),
  )

  @Polyglot override fun highWaterMark(): Double = highWaterMark
  @Polyglot override fun size(chunk: Value?): Double = 1.0

  public companion object : ProxyInstantiable {
    override fun newInstance(vararg arguments: Value?): Any = CountQueueingStrategy(arguments.firstOrNull())
  }
}
