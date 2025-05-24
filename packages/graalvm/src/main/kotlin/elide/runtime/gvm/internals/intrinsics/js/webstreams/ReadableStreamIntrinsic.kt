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

import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.stream.ByteLengthQueueingStrategy
import elide.runtime.intrinsics.js.stream.CountQueueingStrategy
import elide.runtime.intrinsics.js.stream.ReadableStreamBYOBReader
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader

/** Implementation of readable streams (via the Web Streams standard). */
@Intrinsic(global = "ReadableStream") @Singleton internal class ReadableStreamIntrinsic : AbstractJsIntrinsic() {
  @OptIn(DelicateElideApi::class)
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[READABLE_STREAM_SYMBOL.asPublicJsSymbol()] = ReadableStream
    bindings[BYTE_LENGTH_STRATEGY_SYMBOL.asPublicJsSymbol()] = ByteLengthQueueingStrategy
    bindings[COUNT_STRATEGY_SYMBOL.asPublicJsSymbol()] = CountQueueingStrategy
    bindings[DEFAULT_READER_SYMBOL.asPublicJsSymbol()] = ReadableStreamDefaultReader
    bindings[BYOB_READER_SYMBOL.asPublicJsSymbol()] = ReadableStreamBYOBReader
  }

  private companion object {
    private const val DEFAULT_READER_SYMBOL = "ReadableStreamDefaultReader"
    private const val BYOB_READER_SYMBOL = "ReadableStreamBYOBReader"
    private const val READABLE_STREAM_SYMBOL = "ReadableStream"
    private const val BYTE_LENGTH_STRATEGY_SYMBOL = "ByteLengthQueuingStrategy"
    private const val COUNT_STRATEGY_SYMBOL = "CountQueuingStrategy"
  }
}
