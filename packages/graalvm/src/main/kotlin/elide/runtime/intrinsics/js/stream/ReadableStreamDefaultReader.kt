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
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.err.TypeError
import elide.vm.annotations.Polyglot

/**
 * A reader used by streams in default mode, used to read arbitrary chunks of data. Default readers do not support
 * BYOB operations.
 */
public interface ReadableStreamDefaultReader : ReadableStreamReader {
  /** Read a chunk from the stream, returning a promise that is fulfilled with the result. */
  @Polyglot public fun read(): JsPromise<ReadResult>

  public companion object : ProxyInstantiable {
    override fun newInstance(vararg arguments: Value?): Any {
      val stream = arguments.firstOrNull() ?: throw TypeError.create("A stream is required to create a reader")
      val unwrappedStream = runCatching { stream.asProxyObject<ReadableStream>() }.getOrElse {
        throw TypeError.create("Value $stream is not a valid readable stream")
      }

      return unwrappedStream.getReader()
    }
  }
}
