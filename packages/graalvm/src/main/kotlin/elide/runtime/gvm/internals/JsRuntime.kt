package elide.runtime.gvm.internals

import elide.runtime.gvm.VMEngine
import elide.runtime.gvm.VMEngineFactory

/**
 * TBD.
 */
internal abstract class JsRuntime : AbstractVMEngine(GraalVMGuest.JAVASCRIPT) {
  /**
   * TBD.
   */
  internal companion object : VMEngineFactory<JsRuntime> {
    /** @inheritDoc */
    override fun acquire(): JsRuntime {
      TODO("Not yet implemented")
    }
  }
}
