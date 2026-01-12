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
import java.math.BigInteger
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
import kotlin.concurrent.thread
import elide.runtime.intrinsics.js.err.RangeError
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.crypto.RandomIntCallback

// Internal symbol where the Node built-in module is installed.
private const val CRYPTO_MODULE_SYMBOL = "node_${NodeModuleName.CRYPTO}"

// Function name for randomUUID
private const val F_RANDOM_UUID = "randomUUID"
private const val F_RANDOM_INT = "randomInt"

// Cached Int generator to ensure we don't create multiple instances.
private val cryptoRandomGenerator by lazy { SecureRandom() }

// The maximum range (max - min) allowed is 2^48 in Node.js.
private val MAX_48_BIT_LIMIT = BigInteger.valueOf(2L).pow(48)

// Generates a cryptographically secure random integer between the specified `min` (inclusive) and `max` (exclusive) values.
private fun genRandomInt(min: Long, max: Long): Long {
  try {
    return cryptoRandomGenerator.nextLong(min, max)
  } catch (e: Throwable) {
    throw TypeError.create("Error generating random bytes for randomInt: ${e.message}")
  }
}

// Safely converts a Value to a BigInteger, ensuring it is a safe integer within JS limits.
private fun safeValueToBigInt(value: Value, name: String): BigInteger {
  if (value.isNumber) {
    val bigIntValue: BigInteger? = when {
      value.fitsInLong() -> {
        BigInteger.valueOf(value.asLong())
      }
      // Reject integers that exceed Long.MAX_VALUE or are less than Long.MIN_VALUE
      value.fitsInBigInteger() -> throw RangeError.create("The \"$name\" argument must be a safe integer. Received an integer that exceeds the max bounds ${MAX_48_BIT_LIMIT}.")
      // Reject non-integer numbers
      value.fitsInDouble() -> {
        throw TypeError.create("The \"$name\" argument must be a safe integer. Received a non-integer number: ${value.asDouble()}.")
      }
      else -> null // Reject non-integer (e.g. Infinity, NaN, very large BigInts)
    }

    // Define JS safe integer bounds
    val jsMaxSafeInt = BigInteger("9007199254740991") // 2^53 - 1
    val jsMinSafeInt = BigInteger("-9007199254740991") // -(2^53 - 1)

    // Final check: even if conversion works, ensure it falls within JS safe limits
    if (bigIntValue != null && bigIntValue >= jsMinSafeInt && bigIntValue <= jsMaxSafeInt) {
      return bigIntValue
    }
  }
  // Invalid value type, we don't want it
  throw TypeError.create("The \"$name\" argument must be a safe integer. Received ${value}.")
}

// Validates that the provided min and max values are safe integers and that the range difference does not exceed 2^48.
private fun genSafeRange(min: Value, max: Value): Pair<Long, Long> {
  // Safely convert both inputs to BigInteger
  val minBigInt = safeValueToBigInt(min, "min")
  val maxBigInt = safeValueToBigInt(max, "max")

  // Enforce the Min <= Max rule otherwise we throw a RangeError
  if (minBigInt >= maxBigInt) {
    throw RangeError.create("The value of \"max\" is out of range. It must be greater than the value of \"min\" (${minBigInt}). Received ${maxBigInt}.")
  }

  val rangeDifference = maxBigInt.subtract(minBigInt)

  // If the range difference exceeds 2^48, we throw a RangeError. Node.js has a range limit of 2^48 for randomInt.
  if (rangeDifference > MAX_48_BIT_LIMIT) {
    println("Range difference exceeds 2^48 limit: $rangeDifference")
    throw RangeError.create("The value of \"max - min\" is out of range. It must be <= 281474976710655. Received ${rangeDifference}.")
  }

  // Return the validated safe Long values
  return Pair(minBigInt.toLong(), maxBigInt.toLong())
}

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

  @Polyglot override fun randomInt(min: Long, max: Long): Long {
    return genRandomInt(min, max)
  }

  @Polyglot override fun randomInt(min: Long, max: Long, callback: RandomIntCallback) {
    val randomValue = genRandomInt(min, max)

    thread {
      try {
        callback.invoke(null, randomValue)
      } catch (e: Throwable) {
        callback.invoke(TypeError.create(e.message ?: "Unknown error"), randomValue)
      }
    }
  }

  @Polyglot override fun randomInt(min: Value, max: Value, callback: Value) {
    val (safeMin, safeMax) = genSafeRange(min, max)

    val safeCallback: RandomIntCallback = callback.let { cb ->
      { err: Throwable?, value: Long? ->
        cb.execute(
          err?.let { Value.asValue(it) },
          value?.let { Value.asValue(it) }
        )
      }
    }

    return randomInt(safeMin, safeMax, safeCallback)
  }

  @Polyglot override fun randomInt(min: Value, max: Value): Long {
    val (safeMin, safeMax) = genSafeRange(min, max)
    return randomInt(safeMin, safeMax)
  }

  @Polyglot override fun randomInt(max: Value): Long {
    val (safeMin, safeMax) = genSafeRange(Value.asValue(0), max)
    return randomInt(safeMin, safeMax)
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
      // Check if last argument is a callback function
      val lastIsCb = args.lastOrNull()?.canExecute() == true

      when (args.size) {
        1 -> {
          // randomInt(max)
          this.randomInt(args[0])
        }
        2 -> {
          if (lastIsCb) {
            // randomInt(max, callback)
            this.randomInt(Value.asValue(0), args[0], args.last())
          } else {
            // randomInt(min, max)
            this.randomInt(args[0], args[1])
          }
        }
        3 -> {
          // randomInt(min, max, callback)
          this.randomInt(args[0], args[1], args.last())
        }
        else -> throw JsError.typeError("Invalid number of arguments for crypto.randomInt: ${args.size}")
      }
    }
    else -> null
  }
}
