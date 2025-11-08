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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.intrinsics.js.crypto

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.security.SecureRandom
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.gvm.internals.intrinsics.js.typed.UUIDValue
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.Crypto.Companion.MAX_RANDOM_BYTES_SIZE
import elide.runtime.intrinsics.js.SubtleCrypto
import elide.runtime.intrinsics.js.err.AbstractJsException
import elide.runtime.intrinsics.js.err.QuotaExceededError
import elide.runtime.intrinsics.js.err.ValueError
import elide.runtime.intrinsics.js.typed.UUID
import elide.vm.annotations.Polyglot
import org.graalvm.polyglot.Value as GuestValue
import elide.runtime.intrinsics.js.Crypto as WebCryptoAPI

private val WEB_CRYPTO_PROPS_AND_METHODS = arrayOf(
  "getRandomValues",
  "randomUUID",
  "subtle",
)

/** Intrinsic implementation of the [WebCryptoAPI]. */
@Intrinsic(global = WebCryptoIntrinsic.GLOBAL_CRYPTO)
internal class WebCryptoIntrinsic : WebCryptoAPI, ProxyObject, AbstractJsIntrinsic() {
  internal companion object {
    /** Injected name of the Base64 global. */
    const val GLOBAL_CRYPTO = "crypto"
  }

  // Lazy-initialized secure random generator.
  private val secureRandom: SecureRandom by lazy {
    SecureRandom.getInstanceStrong()
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[GLOBAL_CRYPTO.asPublicJsSymbol()] = this
  }

  private fun throwRandomValuesOverflow(): QuotaExceededError {
    return QuotaExceededError("Exceeded maximum byte size for `getRandomValues` (${MAX_RANDOM_BYTES_SIZE})")
  }

  @Suppress("UNCHECKED_CAST")
  @Polyglot override fun getRandomValues(typedArray: Any) {
    when (typedArray) {
      is ByteArray -> {
        if (typedArray.size > MAX_RANDOM_BYTES_SIZE) throw throwRandomValuesOverflow()
        secureRandom.nextBytes(typedArray)
      }
      is GuestValue -> {
        // has to be an array
        if (!typedArray.hasArrayElements()) throw ValueError.create("Not an array")
        getRandomValues(typedArray.`as`(List::class.java))
      }
      is MutableList<*> -> {
        // resolve size (under cap of MAX_RANDOM...)
        val size = if (typedArray.size > MAX_RANDOM_BYTES_SIZE) throw throwRandomValuesOverflow() else {
          typedArray.size
        }

        // generate equal sized array of bytes
        val generatedBytes = ByteArray(size)
        getRandomValues(generatedBytes)

        // try for each type
        val setAs: (Triple<MutableList<Any>, Int, Byte>) -> Any = when (typedArray[0]) {
          is Int -> {
            { it.first.set(it.second, it.third.toInt()) }
          }
          else -> throw ValueError.create("Cannot call `getRandomValues` with non-numeric value")
        }
        for (i in 0 until size) {
          // copy bytes into typed array
          setAs(Triple(typedArray as MutableList<Any>, i, generatedBytes[i]))
        }
      }
      else -> throw ValueError.create("Cannot call `getRandomValues` with non-array value")
    }
  }

  @Polyglot override fun randomUUID(): UUID = UUIDValue.random()

  @Polyglot override fun randomInt(min: Int, max: Int, callback: (AbstractJsException?, Int) -> Unit?): Any {
    TODO("Not yet implemented")
  }

  @get:Polyglot override val subtle: SubtleCrypto get() = error("SubtleCrypto is not supported yet in Elide.")

  override fun getMemberKeys(): Array<String> = WEB_CRYPTO_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in WEB_CRYPTO_PROPS_AND_METHODS
  override fun putMember(key: String?, value: Value?) {
    // no-op
  }

  override fun removeMember(key: String?): Boolean = false

  override fun getMember(key: String?): Any? = when (key) {
    "getRandomValues" -> ProxyExecutable {
      getRandomValues(
        it.getOrNull(0) ?: throw JsError.typeError("Missing argument for getRandomValues")
      )
    }
    "randomUUID" -> ProxyExecutable { randomUUID() }
    "subtle" -> this.subtle
    else -> null
  }
}
