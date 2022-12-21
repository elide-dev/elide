package elide.runtime.gvm

import elide.annotations.Factory
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.internals.ContextManager
import org.graalvm.polyglot.Context as VMContext

/**
 * TBD.
 */
@Singleton internal class VMFacadeFactory @Inject internal constructor (
  // VM execution bridge.
  private val contextManager: ContextManager<VMContext, VMContext.Builder>
) {
  /**
   * TBD.
   */
  @Factory internal fun acquireVM(): VMFacade = TODO("not yet implemented")

  /**
   * TBD.
   */
  @Factory internal fun acquireContextManager(): ContextManager<VMContext, VMContext.Builder> = contextManager
}
