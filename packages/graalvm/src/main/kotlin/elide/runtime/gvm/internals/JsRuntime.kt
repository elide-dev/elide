package elide.runtime.gvm.internals

import elide.runtime.gvm.internals.GraalVMGuest.JAVASCRIPT
import elide.annotations.Factory
import elide.runtime.gvm.ExecutableScript
import elide.runtime.gvm.StreamingReceiver
import elide.runtime.gvm.VMEngineFactory
import elide.runtime.gvm.VMEngineImpl
import elide.runtime.gvm.cfg.JsRuntimeConfig
import elide.runtime.gvm.intrinsics.GuestRuntime
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

/**
 * TBD.
 */
@GuestRuntime internal class JsRuntime : AbstractVMEngine<JsRuntimeConfig>(JAVASCRIPT) {
  /**
   * TBD.
   */
  @Factory internal companion object : VMEngineFactory<JsRuntime> {
    /** @inheritDoc */
    override fun acquire(): JsRuntime {
      return JsRuntime()
    }
  }

  /** @inheritDoc */
  override suspend fun prewarmScript(script: ExecutableScript) {
    super.prewarmScript(script)
  }

  /** @inheritDoc */
  override suspend fun executeStreaming(script: ExecutableScript, vararg args: Any?, receiver: StreamingReceiver): Job {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override suspend fun <R> executeAsync(
    script: ExecutableScript,
    returnType: Class<R>,
    vararg args: Any?
  ): Deferred<R?> {
    TODO("Not yet implemented")
  }
}
