package elide.runtime.gvm.internals.intrinsics.js.crypto

import elide.annotations.core.Polyglot
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.internals.intrinsics.js.typed.UUIDValue
import elide.runtime.intrinsics.js.SubtleCrypto
import elide.runtime.intrinsics.js.typed.UUID
import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.Crypto as WebCryptoAPI

/** Intrinsic implementation of the [WebCryptoAPI]. */
@Intrinsic(global = WebCryptoIntrinsic.GLOBAL_CRYPTO)
internal class WebCryptoIntrinsic : WebCryptoAPI, AbstractJsIntrinsic() {
  internal companion object {
    /** Injected name of the Base64 global. */
    const val GLOBAL_CRYPTO = "__elide_crypto"

    /** Base64 symbol. */
    private val CRYPTO_SYMBOL = GLOBAL_CRYPTO.asJsSymbol()
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[CRYPTO_SYMBOL] = this
  }

  @Polyglot override fun getRandomValues(array: Value): ByteArray {
    TODO("Not yet implemented")
  }

  @Polyglot override fun randomUUID(): UUID = UUIDValue.random()

  @get:Polyglot override val subtle: SubtleCrypto get() = error("SubtleCrypto is not supported yet in Elide.")
}
