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
package elide.runtime.gvm.internals.node.asserts

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.AssertStrictAPI

// Symbol where the internal module implementation is installed.
private const val ASSERT_STRICT_MODULE_SYMBOL: String = "__Elide_node_assert_strict__"

// Installs the Node assert module into the intrinsic bindings.
@Intrinsic @Factory internal class NodeAssertStrictModule : AbstractNodeBuiltinModule() {
  // Provide a compliant instance of the OS API to the DI context.
  @Singleton fun provide(): AssertStrictAPI = NodeAssertStrict.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[ASSERT_STRICT_MODULE_SYMBOL.asJsSymbol()] = provide()
  }
}

/**
 * # Node API: `assert/strict`
 */
internal class NodeAssertStrict : AssertStrictAPI {
  //

  internal companion object {
    private val SINGLETON = NodeAssertStrict()
    fun obtain(): NodeAssertStrict = SINGLETON
  }
}
