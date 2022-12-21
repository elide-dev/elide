package elide.runtime.gvm.internals

import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.EventProcessor
import elide.annotations.Context
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.cfg.GuestVMConfiguration
import elide.runtime.gvm.internals.VMStaticProperty as StaticProperty
import org.graalvm.polyglot.Engine
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import org.graalvm.polyglot.Context as VMContext

/** TBD. */
@Context @Singleton internal class NativeContextManagerImpl @Inject internal constructor (
  config: GuestVMConfiguration,
) : ContextManager<VMContext, VMContext.Builder> {
  private companion object {
    // Static options which are supplied to the engine.
    private val staticEngineOptions = listOf(
      StaticProperty.active("engine.BackgroundCompilation"),
      StaticProperty.active("engine.UsePreInitializedContext"),
      StaticProperty.active("engine.Compilation"),
      StaticProperty.active("engine.Inlining"),
      StaticProperty.active("engine.MultiTier"),
      StaticProperty.active("engine.Splitting"),
      StaticProperty.inactive("engine.SpawnIsolate"),
      StaticProperty.inactive("engine.IsolateMemoryProtection"),
      StaticProperty.of("engine.Mode", "latency"),
      StaticProperty.of("engine.PreinitializeContexts", "js"),
    )
  }

  /** Stubbed output stream. */
  private class StubbedOutputStream : OutputStream() {
    companion object {
      /** Singleton instance for internal use. */
      internal val SINGLETON = StubbedOutputStream()
    }

    override fun write(b: Int): Unit = error(
      "Cannot write to stubbed stream from inside the JS VM."
    )
  }

  /** Stubbed input stream. */
  private class StubbedInputStream : InputStream() {
    companion object {
      /** Singleton instance for internal use. */
      internal val SINGLETON = StubbedInputStream()
    }

    override fun read(): Int = error(
      "Cannot read from stubbed stream from inside the JS VM."
    )
  }

  /** Implements a thread local which binds a [NativeVMThread] to an exclusive VM context. */
  private class VMThreadLocal : ThreadLocal<VMContext?>() {
    override fun initialValue(): VMContext? = null
  }

  /** Thread implementation for a unit of guest VM work. */
  private inner class NativeVMThread private constructor (
    initial: VMContext,
    private val operation: VMContext.() -> Unit,
  ) : Thread() {
    /** Execution lock for this thread. */
    private val mutex = ReentrantLock()

    /** Last-seen exception. */
    private val lastException: AtomicReference<Throwable?> = AtomicReference(null)

    /** Thread-local VM reference, initialized with cold context. */
    private val context: VMThreadLocal = VMThreadLocal().apply {
      set(initial)
    }

    // Prepare to enter the VM context.
    private fun preExecute(context: VMContext) {
      context.enter()
    }

    // Clean up after exiting the VM context.
    private fun postExecute(context: VMContext) {
      context.leave()
    }

    // Lock for execution.
    private fun withLock(op: (VMContext) -> Unit) {
      val ctx = context.get() ?: error("Failed to acquire VM context")
      mutex.lock()
      try {
        op.invoke(ctx)
      } catch (err: Throwable) {
        lastException.set(err)
      } finally {
        mutex.unlock()
      }
    }

    /**
     *
     */
    override fun run(): Unit = withLock {
      try {
        preExecute(it)
        operation.invoke(it)
      } finally {
        postExecute(it)
      }
    }
  }

  private class NativeVMInvocation<Inputs: ExecutionInputs> {

  }

  private class NativeVMDispatcher<I: ExecutionInputs> : EventHandler<NativeVMInvocation<I>> {
    override fun onEvent(event: NativeVMInvocation<I>, sequence: Long, endOfBatch: Boolean) {
      TODO("Not yet implemented")
    }
  }

  // Atomic reference to the globally-active Engine.
  private val engine: AtomicReference<Engine> = AtomicReference(null)

  // Whether the engine has initialized.
  private val initialized: AtomicReference<Boolean> = AtomicReference(false)

  // Context factory function.
  private val contextFactory: AtomicReference<(Engine) -> VMContext.Builder> = AtomicReference(null)

  // Context configuration function.
  private val contextConfigure: AtomicReference<(VMContext.Builder) -> VMContext> = AtomicReference(null)

  init {
    // Acquire a global engine singleton.
    engine.set(
      Engine.newBuilder(*config.languages.toTypedArray())
        .`in`(StubbedInputStream.SINGLETON)
        .out(StubbedOutputStream.SINGLETON)
        .err(StubbedOutputStream.SINGLETON)
        .useSystemProperties(false)
        .allowExperimentalOptions(true).let {
          // Apply static engine options.
          staticEngineOptions.fold(it) { builder, property ->
            builder.option(property.symbol, property.value())
          }
        }.build()
    )
    initialized.compareAndSet(
      false,
      true
    )
  }

  /** @inheritDoc */
  override fun installContextFactory(factory: (Engine) -> VMContext.Builder) {
    contextFactory.set(factory)
  }

  override fun installContextSpawn(factory: (VMContext.Builder) -> VMContext) {
    contextConfigure.set(factory)
  }

  /** @inheritDoc */
  override fun engine(): Engine = if (initialized.get()) {
    engine.get()
  } else error(
    "Engine is not initialized"
  )

  override fun <R> executeAsync(operation: VMContext.() -> R): CompletableFuture<R> {
    TODO("Not yet implemented")
  }
}
