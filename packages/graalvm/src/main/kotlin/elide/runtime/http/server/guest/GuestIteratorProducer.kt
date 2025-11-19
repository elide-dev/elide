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
package elide.runtime.http.server.guest

import io.netty.buffer.ByteBuf
import org.graalvm.polyglot.Value
import elide.runtime.exec.ContextAware
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.PinnedContext
import elide.runtime.http.server.HttpResponseBody
import elide.runtime.http.server.source

/**
 * Attach a source to this response body stream that yields values from a guest [iterator] value using the [executor]
 * to dispatch pulls.
 *
 * The [mapChunk] function is used to convert values from the guest iterator to chunks that are written to the response
 * stream.
 *
 * This function must be called from within the [executor] while the context that produced the [iterator] is active to
 * ensure it can be pinned correctly.
 */
@ContextAware public fun HttpResponseBody.sourceIterator(
  iterator: Value,
  executor: ContextAwareExecutor,
  mapChunk: (chunk: Value) -> ByteBuf,
) {
  require(iterator.isIterator) { "Source guest value must be an iterator" }
  val pinned = PinnedContext.current()

  source { writer ->
    executor.execute(pinned) {
      if (!iterator.hasIteratorNextElement()) writer.end()
      else writer.write(mapChunk(iterator.iteratorNextElement))
    }
  }
}
