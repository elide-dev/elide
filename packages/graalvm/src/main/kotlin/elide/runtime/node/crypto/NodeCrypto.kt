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

import RandomIntCallback
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.CryptoAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.vm.annotations.Polyglot
import java.security.SecureRandom
import elide.runtime.intrinsics.js.err.AbstractJsException
import elide.runtime.intrinsics.js.err.RangeError

// Internal symbol where the Node built-in module is installed.
private const val CRYPTO_MODULE_SYMBOL = "node_${NodeModuleName.CRYPTO}"

// Function name for randomUUID
private const val F_RANDOM_UUID = "randomUUID"
private const val F_RANDOM_INT = "randomInt"


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
      F_RANDOM_INT,
    ).apply { sort() }
  }

  // Implement the CryptoAPI method
  @Polyglot override fun randomUUID(options: Value?): String{
    // Note `options` parameter exists for Node.js compatibility but is currently ignored
    // It supports { disableEntropyCache: boolean } which is not applicable to our implementation
    return java.util.UUID.randomUUID().toString()
  }

 override fun randomInt(min: Int, max: Int, callback: RandomIntCallback?): Any {
   var error: AbstractJsException? = null

   if (min >= max) {
     error = RangeError.create("The value of \"max\" is out of range. It must be greater than the value of \"min\" ($min). Received $max")
   }

   val secureRandom = SecureRandom()

   val bounds = max - min
   val randomInt = secureRandom.nextInt(bounds) + min

   if (callback != null) {
     callback.invoke(error, randomInt)
   } else {
    return randomInt
   }

    return Unit
 }

  @Polyglot override fun randomInt(min: Value?, max: Value, callback: Value?): Int {
    return randomInt(min, max, callback)
  }

  override fun randomInt(max: Value, callback: Value?): Int {
    return randomInt(max, callback)
  }

  override fun randomInt(max: Value): Int {
    return randomInt(max)
  }

  @Polyglot fun randomInt(
    max: Value,
    callback: RandomIntCallback?
  ): Int {
    return randomInt(max, callback)
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
    F_RANDOM_INT -> ProxyExecutable {
      // Node.js signature: randomInt(max) OR randomInt(min, max, [callback]) OR randomInt(max, [callback])
      when (it.size) {
        1 -> randomInt(it.first())
        2 -> if (it[1].fitsInInt()) {
          randomInt(it.first(), it[1])
        } else {
          randomInt(max = it.first(), callback = it[1])
        }
        3 -> randomInt(it.first(), it[1], it.getOrNull(2))
        else -> throw JsError.typeError("Invalid number of arguments for `crypto.randomInt`")
      }
    }
    else -> null
  }
}
