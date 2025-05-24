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
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableByteStream
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.err.TypeError
import elide.vm.annotations.Polyglot

public interface ReadableStreamBYOBReader : ReadableStreamReader {
  @Polyglot public fun read(view: Value, options: Any? = null): JsPromise<ReadResult>

  public companion object : ProxyInstantiable {
    override fun newInstance(vararg arguments: Value?): Any {
      val stream = arguments.firstOrNull() ?: throw TypeError.create("A stream is required to create a reader")
      val unwrappedStream = runCatching { stream.asHostObject<ReadableByteStream>() }.getOrElse {
        throw TypeError.create("Value $stream is not a valid readable byte stream")
      }

      return unwrappedStream.getReaderInternal(byob = true)
    }
  }
}
