/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.gvm

import io.micronaut.context.BeanContext
import io.micronaut.http.HttpRequest
import java.util.ServiceLoader
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.reflect.KClass
import elide.annotations.Factory
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.api.GuestRuntime
import elide.runtime.gvm.internals.AbstractVMEngine
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.js.JsRuntime
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
  /**
   * TBD.
   */
  internal class CompoundVMFacade private constructor (private val engines: Array<out VMFacade>) : VMFacade {
    internal companion object {
      /**
       * TBD.
       */
      @JvmStatic fun withEngines(engines: List<VMFacade>) = CompoundVMFacade(engines.toTypedArray())
    }

    override fun language(): GuestLanguage {
      TODO("Not yet implemented")
    }

    override suspend fun prewarmScript(script: ExecutableScript) {
      TODO("Not yet implemented")
    }

    override suspend fun executeStreaming(
      script: ExecutableScript,
      args: ExecutionInputs,
      receiver: StreamingReceiver
    ): Job {
      TODO("Not yet implemented")
    }

    override suspend fun executeRender(
      script: ExecutableScript,
      request: HttpRequest<*>,
      context: Any?,
      receiver: StreamingReceiver
    ): Job {
      TODO("Not yet implemented")
    }

    override suspend fun <R> execute(script: ExecutableScript, returnType: Class<R>, args: ExecutionInputs?): R? {
      TODO("Not yet implemented")
    }

    override suspend fun <R> executeAsync(
      script: ExecutableScript,
      returnType: Class<R>,
      args: ExecutionInputs?
    ): Deferred<R?> {
      TODO("Not yet implemented")
    }

    override fun <R> executeBlocking(script: ExecutableScript, returnType: Class<R>, args: ExecutionInputs?): R? {
      TODO("Not yet implemented")
    }
  }

  // Lazy resolution of VM engine implementations.
  private val installedEngines by lazy {
    ServiceLoader.load(VMEngineImpl::class.java)
  }

  // Resolve the VM implementation class to use for the provided `language`.
  private fun resolveStaticVMFactoryImpl(language: GuestLanguage): KClass<*>? = when (language.symbol) {
    GuestLanguage.JAVASCRIPT.symbol -> JsRuntime::class
    else -> null
  }

  // Load a VM implementation via a service-loader.
  private fun resolveDynamicVMFactoryImpl(language: GuestLanguage): KClass<*>? {
    return installedEngines.stream().filter {
      it.type().annotations.find { anno ->
        anno.annotationClass.qualifiedName == GuestRuntime::class.qualifiedName
      }?.let { anno ->
        val runtime = anno as GuestRuntime
        runtime.engine == language.symbol
      } ?: false
    }.findFirst().let { candidate ->
      if (candidate.isEmpty) {
        null
      } else {
        candidate.get().type().kotlin
      }
    }
  }

  // Initialize a VM engine after loading it from the service loader; this is done by acquiring an instance through the
  // active bean context.
  private fun initializeVMEngine(impl: KClass<*>): AbstractVMEngine<*, *, *> {
    return (beanContext.getBean(impl.java) as AbstractVMEngine<*, *, *>).apply {
      this@apply.contextManager = this@VMFacadeFactory.contextManager
      initialize()
    }
  }

  /**
   * TBD.
   */
  public fun acquireVM(vararg languages: GuestLanguage): VMFacade {
    val engines = languages.map {
      val impl = resolveStaticVMFactoryImpl(it) ?: resolveDynamicVMFactoryImpl(it) ?: error(
        "Failed to resolve VM implementation for language: ${it.label}. Is it supported and installed?"
      )
      val vm = initializeVMEngine(impl)
      object: VMFacade by vm {
        /* No overrides at this time. */
      }
    }
    return CompoundVMFacade.withEngines(
      engines
    )
  }

  /**
   * TBD.
   */
  @Factory internal fun acquireContextManager(): ContextManager<VMContext, VMContext.Builder> = contextManager
}
