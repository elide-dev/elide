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

import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.WritableStream
import elide.runtime.intrinsics.js.stream.WritableStreamDefaultWriter

/** Implementation of writable streams (via the Web Streams standard). */
@Intrinsic(global = "WritableStream") internal class WritableStreamIntrinsic : AbstractJsIntrinsic() {
  @OptIn(DelicateElideApi::class)
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[WRITABLE_STREAM_SYMBOL.asPublicJsSymbol()] = WritableStream
    bindings[WRITER_SYMBOL.asPublicJsSymbol()] = WritableStreamDefaultWriter
  }

  private companion object {
    private const val WRITABLE_STREAM_SYMBOL = "WritableStream"
    private const val WRITER_SYMBOL = "WritableStreamDefaultWriter"
  }
}
