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
package elide.runtime.node.http2

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.HTTP2API
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

// Installs the Node `http2` module into the intrinsic bindings.
@Intrinsic internal class NodeHttp2Module : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeHttp2.create() }
  internal fun provide(): HTTP2API = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.HTTP2)) { provide() }
  }
}

/**
 * # Node API: `http2`
 */
internal class NodeHttp2 private constructor () : ReadOnlyProxyObject, HTTP2API {
  //

  internal companion object {
    @JvmStatic fun create(): NodeHttp2 = NodeHttp2()
  }

  private class ReadOnlyTypeObject(private val name: String) : ReadOnlyProxyObject {
    override fun getMemberKeys(): Array<String> = emptyArray()
    override fun getMember(key: String?): Any? = null
    override fun toString(): String = "[object $name]"
  }

  private val ALL_MEMBERS = arrayOf(
    "Http2Session","ServerHttp2Session","ClientHttp2Session","Http2Stream","ClientHttp2Stream",
    "ServerHttp2Stream","Http2Server","Http2SecureServer","Http2ServerRequest","Http2ServerResponse",
    "connect","createServer","createSecureServer","constants"
  )

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS
  override fun getMember(key: String?): Any? = when (key) {
    "Http2Session","ServerHttp2Session","ClientHttp2Session","Http2Stream","ClientHttp2Stream",
    "ServerHttp2Stream","Http2Server","Http2SecureServer","Http2ServerRequest","Http2ServerResponse" -> ReadOnlyTypeObject(key!!)
    "constants" -> ProxyObject.fromMap(emptyMap<String, Any>())
    "connect","createServer","createSecureServer" -> ProxyExecutable { _: Array<Value> ->
      object : ReadOnlyProxyObject {
        private var started = false
        override fun getMemberKeys(): Array<String> = arrayOf("listen","close","on")
        override fun getMember(k: String?): Any? = when (k) {
          "listen" -> ProxyExecutable { argv: Array<Value> -> if (!started) started = true; argv.lastOrNull()?.takeIf { it.canExecute() }?.execute(); this }
          "close" -> ProxyExecutable { _: Array<Value> -> this }
          "on" -> ProxyExecutable { _: Array<Value> -> this }
          else -> null
        }
      }
    }
    else -> null
  }
}
