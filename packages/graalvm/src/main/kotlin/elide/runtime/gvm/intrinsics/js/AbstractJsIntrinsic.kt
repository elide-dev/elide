package elide.runtime.gvm.intrinsics.js

import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.internals.GraalVMGuest
import elide.runtime.gvm.intrinsics.GuestIntrinsic

/** Abstract base class for all intrinsic implementations. */
internal abstract class AbstractJsIntrinsic : GuestIntrinsic {
  /** @inheritDoc */
  override fun language(): GuestLanguage = GraalVMGuest.JAVASCRIPT
}
