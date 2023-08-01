package elide.runtime.gvm.internals.intrinsics.js

import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.internals.GraalVMGuest
import elide.runtime.intrinsics.GuestIntrinsic

/** Abstract base class for all intrinsic implementations. */
internal abstract class AbstractJsIntrinsic : GuestIntrinsic {
  /** @inheritDoc */
  override fun language(): GuestLanguage = GraalVMGuest.JAVASCRIPT

  /** @inheritDoc */
  override fun displayName(): String = "native code"

  /** @inheritDoc */
  override fun toString(): String = "[${displayName()}]"
}
