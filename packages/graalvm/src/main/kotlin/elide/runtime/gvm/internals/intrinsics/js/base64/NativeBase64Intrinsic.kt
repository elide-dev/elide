@file:Suppress("UNUSED_PARAMETER")

package elide.runtime.gvm.internals.intrinsics.js.base64

import elide.annotations.core.Polyglot
import elide.core.encoding.Base64
import org.graalvm.nativeimage.Isolate

/**
 * TBD.
 */
@Suppress("unused") internal object NativeBase64Intrinsic {
  /**
   * TBD.
   */
  @JvmStatic @Polyglot fun base64Encode(input: String): String = Base64.encodeToString(input)

  /**
   * TBD.
   */
  @JvmStatic @Polyglot fun base64Decode(input: String): String = Base64.decodeToString(input)

  /**
   * TBD.
   */
  @JvmStatic @Polyglot fun base64EncodeWebsafe(input: String): String = Base64.encodeWebSafe(input)
}
