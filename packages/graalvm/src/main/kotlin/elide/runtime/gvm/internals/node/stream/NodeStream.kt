/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.gvm.internals.node.stream

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.StreamAPI

// Internal symbol where the Node built-in module is installed.
private const val STREAM_MODULE_SYMBOL = "__Elide_node_stream__"

// Installs the Node stream module into the intrinsic bindings.
@Intrinsic @Factory internal class NodeStreamModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): StreamAPI = NodeStream.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[STREAM_MODULE_SYMBOL.asJsSymbol()] = provide()
  }
}

/**
 * # Node API: `stream`
 */
internal class NodeStream : StreamAPI {
  //

  internal companion object {
    private val SINGLETON = NodeStream()
    fun obtain(): NodeStream = SINGLETON
  }
}