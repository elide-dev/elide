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

import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * ## Count Queuing Strategy
 *
 * The `CountQueuingStrategy` interface of the Streams API provides a built-in chunk counting queuing strategy that can
 * be used when constructing streams.
 *
 * [`CountQueuingStrategy` on MDN](https://developer.mozilla.org/docs/Web/API/CountQueuingStrategy)
 */
@API public interface CountQueuingStrategy : QueuingStrategy, ProxyObject {
  // Always returns `1` by spec.
  @Polyglot override fun size(): Int = 1

  /**
   * ### High-water mark
   *
   * Provided at construction time, if any. Defined as: The total number of chunks that can be contained in the internal
   * queue before backpressure is applied.
   */
  @get:Polyglot public val highWaterMark: Int
}
