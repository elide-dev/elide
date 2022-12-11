package elide.runtime.gvm.internals

import elide.runtime.gvm.internals.GraalVMGuest.JAVASCRIPT
import elide.annotations.Factory
import elide.runtime.gvm.VMEngineFactory
import elide.runtime.gvm.VMEngineImpl
import elide.runtime.gvm.intrinsics.GuestRuntime

/**
 * TBD.
 */
@GuestRuntime internal abstract class JsRuntime : VMEngineImpl, AbstractVMEngine(JAVASCRIPT) {
  /**
   * TBD.
   */
  @Factory internal companion object : VMEngineFactory<JsRuntime> {
    /** @inheritDoc */
    override fun acquire(): JsRuntime {
      TODO("Not yet implemented")
    }
  }
}
