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

import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.vm.annotations.Polyglot

/**
 * A lightweight delegating implementation of the [ReadableStreamDefaultReader] interface, wrapping the locked stream
 * instance. This wrapper can be used with both default and byte streams.
 */
internal class ReadableStreamDefaultReaderToken(
  private val stream: ReadableStreamBase
) : ReadableStreamReaderTokenBase(), ReadableStreamDefaultReader {
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

  @Polyglot override fun read(): JsPromise<ReadResult> {
    ensureOpen()
    return stream.readOrEnqueue()
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
