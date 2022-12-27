package elide.runtime.gvm

import elide.annotations.Factory
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.internals.AbstractVMEngine
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.js.JsRuntime
import io.micronaut.context.BeanContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.reflect.KClass
import org.graalvm.polyglot.Context as VMContext

/**
 * TBD.
 */
@Singleton public class VMFacadeFactory @Inject internal constructor (
  // VM execution bridge.
  private val contextManager: ContextManager<VMContext, VMContext.Builder>,

  // Active bean context.
  private val beanContext: BeanContext,
) {
  // Resolve the VM implementation class to use for the provided `language`.
  private fun resolveVMFactoryImpl(language: GuestLanguage): KClass<*> = when (language.symbol) {
    "js" -> JsRuntime::class
    else -> throw IllegalArgumentException("Unsupported guest language: ${language.symbol}")
  }

  /**
   * TBD.
   */
  public fun acquireVM(vararg languages: GuestLanguage): VMFacade {
    require(languages.size == 1) {
      "Please acquire a guest VM with a minimum and maximum of one supported `GuestLanguage`"
    }

    val language = languages.first()
    val impl = resolveVMFactoryImpl(language)
    val vm = beanContext.getBean(impl.java) as AbstractVMEngine<*, *>
    return object: VMFacade by vm {
      /* No overrides at this time. */
    }
  }

  /**
   * TBD.
   */
  @Factory internal fun acquireVM(): VMFacade = TODO("not yet implemented")

  /**
   * TBD.
   */
  @Factory internal fun acquireContextManager(): ContextManager<VMContext, VMContext.Builder> = contextManager
}
