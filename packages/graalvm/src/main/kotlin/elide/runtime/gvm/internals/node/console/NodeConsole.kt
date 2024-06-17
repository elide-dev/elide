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
package elide.runtime.gvm.internals.node.console

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ConsoleAPI

// Internal symbol where the Node built-in module is installed.
private const val CONSOLE_MODULE_SYMBOL = "node_console"

// Installs the Node console module into the intrinsic bindings.
@Intrinsic @Factory internal class NodeConsoleModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): ConsoleAPI = NodeConsole.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[CONSOLE_MODULE_SYMBOL.asJsSymbol()] = provide()
  }
}

/**
 * # Node API: `console`
 */
internal class NodeConsole : ConsoleAPI {
  //

  internal companion object {
    private val SINGLETON = NodeConsole()
    fun obtain(): NodeConsole = SINGLETON
  }
}
