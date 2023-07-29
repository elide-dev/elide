package elide.runtime.gvm.internals.intrinsics.js.crypto

import elide.vm.annotations.Polyglot
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.internals.intrinsics.js.typed.UUIDValue
import elide.runtime.intrinsics.js.SubtleCrypto
import elide.runtime.intrinsics.js.err.QuotaExceededError
import elide.runtime.intrinsics.js.err.ValueError
import elide.runtime.intrinsics.js.typed.UUID
import java.security.SecureRandom
import elide.runtime.intrinsics.js.Crypto.Companion.MAX_RANDOM_BYTES_SIZE
import org.graalvm.polyglot.Value as GuestValue
import elide.runtime.intrinsics.js.Crypto as WebCryptoAPI

/** Intrinsic implementation of the [WebCryptoAPI]. */
@Intrinsic(global = WebCryptoIntrinsic.GLOBAL_CRYPTO)
internal class WebCryptoIntrinsic : WebCryptoAPI, AbstractJsIntrinsic() {
  internal companion object {
    /** Injected name of the Base64 global. */
    const val GLOBAL_CRYPTO = "crypto"

    /** Base64 symbol. */
    private val CRYPTO_SYMBOL = GLOBAL_CRYPTO.asJsSymbol()
  }

  // Lazy-initialized secure random generator.
  private val secureRandom: SecureRandom by lazy {
    SecureRandom.getInstanceStrong()
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[CRYPTO_SYMBOL] = this
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

  @get:Polyglot override val subtle: SubtleCrypto get() = error("SubtleCrypto is not supported yet in Elide.")
}
