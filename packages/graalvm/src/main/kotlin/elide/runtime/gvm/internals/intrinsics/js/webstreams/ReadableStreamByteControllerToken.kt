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
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableByteStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamBYOBRequest
import elide.vm.annotations.Polyglot

internal class ReadableStreamByteControllerToken(
  private val stream: ReadableByteStream
) : ReadableByteStreamController {
  @get:Polyglot override val desiredSize: Double? get() = stream.desiredSize()
  @get:Polyglot override val byobRequest: ReadableStreamBYOBRequest? get() = stream.getRequest()

  @Polyglot override fun enqueue(chunk: Value?) {
    if (chunk == null) throw TypeError.create("Cannot enqueue null or undefined chunk")
    stream.fulfillOrEnqueue(chunk)
  }

  @Polyglot override fun error(reason: Any?) = stream.error(reason)
  @Polyglot override fun close() = stream.close()
}
