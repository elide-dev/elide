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
import elide.runtime.intrinsics.js.node.crypto.RandomIntCallback

// Internal symbol where the Node built-in module is installed.
private const val CRYPTO_MODULE_SYMBOL = "node_${NodeModuleName.CRYPTO}"

// Function name for randomUUID
private const val F_RANDOM_UUID = "randomUUID"
private const val F_RANDOM_INT = "randomInt"

// Cached Int generator to ensure we don't create multiple instances.
private val cryptoRandomGenerator by lazy { SecureRandom() }


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
     throw RangeError.create("The value of \"max\" is out of range. It must be greater than the value of \"min\" ($min). Received $max")
   }

   val randomInt = cryptoRandomGenerator.nextInt(min,   max)

   if (callback != null) {
     callback.invoke(error, randomInt)
     return Unit
   } else {
      return randomInt
   }
 }

  @Polyglot override fun randomInt(min: Value, max: Value, callback: Value?): Any {
    return randomInt(min, max, callback)
  }

  @Polyglot override fun randomInt(max: Value, callback: Value?): Any {
    return randomInt(0, max.asInt(), callback as? RandomIntCallback)
  }

  @Polyglot override fun randomInt(max: Value): Any {
    return randomInt(0, max.asInt())
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
    F_RANDOM_INT -> ProxyExecutable { args ->
      // Node.js signature: randomInt(max) OR randomInt(max, [callback]) OR randomInt(min, max) OR randomInt(min, max, [callback])
      when (args.size) {
        1 -> randomInt(0, args.first().asInt())
        2 -> if (args[1].fitsInInt()) {
          randomInt(min = args.first().asInt(), max = args[1].asInt())
        } else {
          randomInt(max = args.first(), callback = args[1])
        }
        3 -> randomInt(args.first().asInt(), args[1].asInt(), args.getOrNull(2) as? RandomIntCallback)
        else -> throw JsError.typeError("Invalid number of arguments for `crypto.randomInt`, ${args.size} arguments were provided.")
      }
    }
    else -> null
  }
}
