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
package elide.runtime.node.crypto

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.CryptoAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.vm.annotations.Polyglot

// Internal symbol where the Node built-in module is installed.
private const val CRYPTO_MODULE_SYMBOL = "node_${NodeModuleName.CRYPTO}"

// Functiopn name for randomUUID
private const val F_RANDOM_UUID = "randomUUID"
private val F_CREATE_HASH = "createHash"

// Installs the Node crypto module into the intrinsic bindings.
@Intrinsic internal class NodeCryptoModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeCrypto.create() }
  internal fun provide(): NodeCrypto = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[CRYPTO_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { singleton }
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.CRYPTO)) { singleton }
  }
}

/**
 * # Node API: `crypto`
 */
internal class NodeCrypto private constructor () : ReadOnlyProxyObject, CryptoAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeCrypto = NodeCrypto()

    // Module members
    private val moduleMembers = arrayOf(
      F_RANDOM_UUID,
      F_CREATE_HASH,
    ).apply { sort() }
  }

  // Implement the CryptoAPI method
  @Polyglot override fun randomUUID(options: Value?): String{
    // Note `options` parameter exists for Node.js compatibility but is currently ignored
    // It supports { disableEntropyCache: boolean } which is not applicable to our implementation
    return java.util.UUID.randomUUID().toString()
  }
  
  @Polyglot override fun createHash(algorithm: String): NodeHash {
    return NodeHash(algorithm)
  }

  // ProxyObject implementation
  override fun getMemberKeys(): Array<String> = moduleMembers

  override fun hasMember(key: String): Boolean = 
    moduleMembers.binarySearch(key) >= 0

  override fun getMember(key: String): Any? = when (key) {
    F_RANDOM_UUID -> ProxyExecutable { args ->
      // Node.js signature: randomUUID([options])
      val options = args.getOrNull(0)
      randomUUID(options)
    }
    F_CREATE_HASH -> ProxyExecutable { args ->
      val algorithm = args.getOrNull(0)?.asString() ?: throw IllegalArgumentException("Algorithm required")
      createHash(algorithm)
    }
    else -> null
  }
}
