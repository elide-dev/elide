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
package elide.runtime.node.readline

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ReadlineAPI
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable

// Installs the Node readline module into the intrinsic bindings.
@Intrinsic internal class NodeReadlineModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeReadline.create() }

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.READLINE)) { singleton }
  }
}

/**
 * # Node API: `readline`
 */
internal class NodeReadline private constructor () : ReadOnlyProxyObject, ReadlineAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeReadline = NodeReadline()
  }

  override fun getMemberKeys(): Array<String> = arrayOf(
    "InterfaceConstructor","Interface","clearLine","clearScreenDown","createInterface","cursorTo","moveCursor","emitKeypressEvents"
  )
  override fun getMember(key: String?): Any? = when (key) {
    "createInterface" -> ProxyExecutable { _ ->
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("question","close")
        override fun getMember(k: String?): Any? = when (k) {
          "question" -> ProxyExecutable { argv: Array<Value> ->
            val cb = argv.getOrNull(1)
            cb?.takeIf { it.canExecute() }?.execute(argv.firstOrNull())
            null
          }
          "close" -> ProxyExecutable { _: Array<Value> -> null }
          else -> null
        }
      }
    }
    "clearLine", "clearScreenDown", "cursorTo", "moveCursor", "emitKeypressEvents" -> ProxyExecutable { _: Array<Value> -> null }
    else -> null
  }
}
