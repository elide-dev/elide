package elide.runtime.gvm.internals

import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.VMEngine

/**
 * TBD.
 */
internal abstract class AbstractVMEngine constructor (protected val language: GraalVMGuest) : VMEngine {
  /** @inheritDoc */
  override fun language(): GuestLanguage = language
}
