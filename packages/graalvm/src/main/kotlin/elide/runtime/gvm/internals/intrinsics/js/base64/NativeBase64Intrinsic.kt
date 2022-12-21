@file:Suppress("UNUSED_PARAMETER")

package elide.runtime.gvm.internals.intrinsics.js.base64

import elide.core.encoding.Base64
import org.graalvm.nativeimage.Isolate

/**
 * TBD.
 */
@Suppress("unused") internal object NativeBase64Intrinsic {
  /**
   * TBD.
   */
  @JvmStatic fun base64Encode(thread: Isolate, input: String): String {
    return Base64.encodeToString(input)
  }

  /**
   * TBD.
   */
  @JvmStatic fun base64Decode(thread: Isolate, input: String): String {
    return Base64.decodeToString(input)
  }

  /**
   * TBD.
   */
  @JvmStatic fun base64EncodeWebsafe(thread: Isolate, input: String): String {
    return Base64.encodeWebSafe(input)
  }
}
