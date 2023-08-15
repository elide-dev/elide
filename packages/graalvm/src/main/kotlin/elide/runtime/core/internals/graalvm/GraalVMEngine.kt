package elide.runtime.core.internals.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent
import elide.runtime.core.EngineLifecycleEvent.EngineCreated
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.internals.MutableEngineLifecycle
import elide.runtime.core.internals.graalvm.GraalVMEngine.Companion.create

/**
 * A [PolyglotEngine] implementation built around a GraalVM [engine]. This class allows plugins to configure the
 * [Context] builders before they are returned by the [acquire] method.
 *
 * @see acquire
 * @see create
 */
@DelicateElideApi internal class GraalVMEngine private constructor(
  private val lifecycle: MutableEngineLifecycle,
  private val engine: Engine,
) : PolyglotEngine {
  /** Create a new [GraalVMContext], triggering lifecycle events to allow customization. */
  private fun createContext(): GraalVMContext {
    // build a new context using the shared engine
    val builder = Context.newBuilder().engine(engine)

    // allow plugins to customize the context on creation
    lifecycle.emit(EngineLifecycleEvent.ContextCreated, builder)

    // build the context and notify event listeners
    val context = GraalVMContext(builder.build())
    lifecycle.emit(EngineLifecycleEvent.ContextInitialized, context)

    return context
  }

  override fun acquire(): PolyglotContext {
    return createContext()
  }

  internal companion object {
    /**
     * Creates a new [GraalVMEngine] using the provided [configuration]. This method triggers the [EngineCreated] event
     * for registered plugins.
     */
    fun create(configuration: GraalVMConfiguration): GraalVMEngine {
      val languages = configuration.languages.map { it.languageId }.toTypedArray()
      val builder = Engine.newBuilder(*languages)

      // allow plugins to customize the engine builder
      configuration.lifecycle.emit(EngineCreated, builder)

      // build the engine
      val engine = GraalVMEngine(configuration.lifecycle, builder.build())

      return engine
    }
  }
}
