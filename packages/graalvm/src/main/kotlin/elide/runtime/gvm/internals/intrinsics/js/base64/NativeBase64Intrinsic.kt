@file:Suppress("UNUSED_PARAMETER")

package elide.runtime.gvm.internals.intrinsics.js.base64

import elide.core.encoding.Base64
import org.graalvm.nativeimage.Isolate
import org.graalvm.nativeimage.c.function.CEntryPoint

/**
 * TBD.
 */
@Suppress("unused") internal object NativeBase64Intrinsic {
  /**
   * TBD.
   */
  @CEntryPoint(name = "base64_encode")
  @JvmStatic fun base64Encode(thread: Isolate, input: String): String {
    return Base64.encodeToString(input)
  }

  /**
   * TBD.
   */
  @CEntryPoint(name = "base64_decode")
  @JvmStatic fun base64Decode(thread: Isolate, input: String): String {
    return Base64.decodeToString(input)
  }

  /**
   * TBD.
   */
  @CEntryPoint(name = "base64_encode_websafe")
  @JvmStatic fun base64EncodeWebsafe(thread: Isolate, input: String): String {
    return Base64.encodeWebSafe(input)
  }
}
