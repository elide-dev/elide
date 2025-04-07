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
  public fun highWaterMark(): Double

  /** Calculate the size of an arbitrary chunk of data. */
  public fun size(chunk: Any?): Double

  /** The default queuing strategy, using a [highWaterMark] of `0.0` and measuring every chunk with size `1.0`. */
  public object Default : QueueingStrategy {
    override fun highWaterMark(): Double = 0.0
    override fun size(chunk: Any?): Double = 1.0
  }
}
