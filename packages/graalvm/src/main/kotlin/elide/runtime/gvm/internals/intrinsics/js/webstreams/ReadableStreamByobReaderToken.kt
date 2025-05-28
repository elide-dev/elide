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
package elide.runtime.gvm.internals.intrinsics.js.webstreams

import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableStreamBYOBReader
import elide.vm.annotations.Polyglot

internal class ReadableStreamByobReaderToken(
  private val stream: ReadableByteStream
) : ReadableStreamReaderTokenBase(), ReadableStreamBYOBReader {
  @get:Polyglot override val closed = JsPromise<Unit>()

  /** Check that the reader's [closed] promise hasn't been completed, throwing a [TypeError] if it has. */
  private fun ensureOpen() {
    if (closed.isDone) throw TypeError.create("Reader has already been released")
  }

  override fun error(reason: Any?) {
    closed.reject(reason)
  }

  override fun close() {
    closed.resolve(Unit)
  }

  @Polyglot override fun read(view: Value, options: Any?): JsPromise<ReadResult> {
    val min = (options as? Value)?.takeIf { it.hasMember("min") }?.let { opts ->
      opts.getMember("min")?.takeIf { it.isNumber }?.asLong()
    }

    return stream.readOrEnqueue(view, min ?: 1)
  }

  @Polyglot override fun releaseLock() {
    ensureOpen()
    closed.reject(TypeError.create("Reader lock was released"))
    stream.releaseReader()
  }

  @Polyglot override fun cancel() {
    stream.cancel()
  }
}
