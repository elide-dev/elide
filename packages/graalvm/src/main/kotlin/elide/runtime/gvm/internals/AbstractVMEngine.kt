package elide.runtime.gvm.internals

import elide.runtime.gvm.*
import elide.runtime.gvm.VMEngineImpl
import elide.runtime.gvm.cfg.GuestRuntimeConfiguration
import elide.runtime.gvm.cfg.JsRuntimeConfig
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

/**
 * TBD.
 */
internal abstract class AbstractVMEngine<C : GuestRuntimeConfiguration> constructor (
  protected val language: GraalVMGuest
) : VMEngineImpl<C> {
  /** @inheritDoc */
  override fun language(): GuestLanguage = language

  /** @inheritDoc */
  override suspend fun prewarmScript(script: ExecutableScript) {
    // no-op (by default)
  }

  /** @inheritDoc */
  override suspend fun <R> execute(script: ExecutableScript, returnType: Class<R>, vararg args: Any?): R? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun <R> executeBlocking(script: ExecutableScript, returnType: Class<R>, vararg args: Any?): R? {
    TODO("Not yet implemented")
  }
}
