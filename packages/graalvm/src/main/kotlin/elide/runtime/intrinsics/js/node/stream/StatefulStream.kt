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
package elide.runtime.intrinsics.js.node.stream

import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.io.Closeable
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * # Node API: Stateful Stream
 */
@HostAccess.Implementable
@API public interface StatefulStream : Closeable, AutoCloseable {
  /**
   * Is true after `'close'` has been emitted.
   */
  @get:Polyglot public val closed: Boolean

  /**
   * Is true after `destroy()` has been called.
   */
  @get:Polyglot public val destroyed: Boolean


  /**
   * Destroy the stream. Optionally emit an `'error'` event, and emit a `'close'` event (unless `emitClose` is set to
   * `false`).
   *
   * After this call, the writable stream has ended and subsequent calls to `write()` or `end()` will result in an
   * `ERR_STREAM_DESTROYED` error. This is a destructive and immediate way to destroy a stream. Previous calls to
   * `write()` may not have drained, and may trigger an `ERR_STREAM_DESTROYED` error.
   *
   * Use `end()` instead of `destroy` if data should flush before close, or wait for the `'drain'` event before
   * destroying the stream.
   */
  @Polyglot public fun destroy()

  /**
   * Destroy the stream. Optionally emit an `'error'` event, and emit a `'close'` event (unless `emitClose` is set to
   * `false`).
   *
   * See docs for [Readable] and [Writable] for specific behavior of this method.
   *
   * @param error Error to set for this destruction
   */
  @Polyglot public fun destroy(error: Value)

  /**
   * Destroy the stream. Optionally emit an `'error'` event, and emit a `'close'` event (unless `emitClose` is set to
   * `false`).
   *
   * See docs for [Readable] and [Writable] for specific behavior of this method.
   *
   * @param error Error to set for this destruction
   */
  @Polyglot public fun destroy(error: Throwable)
}
