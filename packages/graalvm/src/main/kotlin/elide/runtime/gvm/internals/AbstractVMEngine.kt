package elide.runtime.gvm.internals

import elide.annotations.Inject
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.*
import elide.runtime.gvm.VMEngineImpl
import elide.runtime.gvm.cfg.GuestRuntimeConfiguration
import elide.runtime.gvm.cfg.GuestVMConfiguration
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
//import elide.server.ServerInitializer
import elide.util.RuntimeFlag
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Value as GuestValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.EnvironmentAccess
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.io.IOAccess
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream

/**
 * TBD.
 */
internal abstract class AbstractVMEngine<Config : GuestRuntimeConfiguration, Code: ExecutableScript> constructor (
  internal val contextManager: ContextManager<VMContext, VMContext.Builder>,
  protected val language: GraalVMGuest,
  protected val config: Config,
) : VMEngineImpl<Config>/*, ServerInitializer*/ {
  // Abstract VM engine logger.
  private val logging: Logger = Logging.of(AbstractVMEngine::class)

  /** Whether this VM engine has initialized. */
  private val initialized = AtomicBoolean(false)

  /** Top-level guest VM configuration. */
  @Inject internal lateinit var guestConfig: GuestVMConfiguration

  /** VM intrinsics manager. */
  @Inject internal lateinit var intrinsicsManager: IntrinsicsManager

  /** VM filesystem manager. */
  @Inject internal lateinit var filesystem: GuestVFS

  // Abstract VM options which must be evaluated at the time a context is created.
  private val conditionalOptions : List<VMProperty> = listOf(
    VMConditionalMultiProperty(main = VMConditionalProperty("vm.inspect", "inspect", {
      RuntimeFlag.inspect || guestConfig.inspector?.enabled == true
    }), properties = listOf(
      // Inspection: Path.
      VMRuntimeProperty.ofConfigurable("vm.inspect.path", "inspect.Path") {
        RuntimeFlag.inspectPath ?: guestConfig.inspector?.path
      },

      // Inspection: Suspend.
      VMRuntimeProperty.ofBoolean("vm.inspect.suspend", "inspect.Suspend") {
        RuntimeFlag.inspectSuspend || guestConfig.inspector?.suspend == true
      },

      // Inspection: Secure.
      VMRuntimeProperty.ofBoolean("vm.inspect.secure", "inspect.Secure") {
        RuntimeFlag.inspectSecure || guestConfig.inspector?.secure == true
      },

      // Inspection: Wait for debugger.
      VMRuntimeProperty.ofBoolean("vm.inspect.wait", "inspect.WaitAttached") {
        (
          RuntimeFlag.inspectSuspend &&
          RuntimeFlag.inspectWait
        ) || (
          guestConfig.inspector?.suspend == true &&
          guestConfig.inspector?.wait == true
        )
      },

      // Inspection: Runtime sources.
      VMRuntimeProperty.ofBoolean("vm.inspect.internal", "inspect.Internal") {
        RuntimeFlag.inspectInternal
      },
    )),

    // Sandbox: Max CPU time. Limits CPU time of guest executions.
    VMRuntimeProperty.ofConfigurable("vm.sandbox.maxCpuTime", "sandbox.MaxCPUTime") {
      when (val limit = guestConfig.enterprise?.sandbox?.maxCpuTime?.toMillis()) {
        null -> null
        else -> if (limit > 0) {
          limit.toString() + "ms"
        } else {
          null
        }
      }
    },

    // Sandbox: Max AST depth. Limits the syntax tree depth of parsed functions.
    VMRuntimeProperty.ofConfigurable("vm.sandbox.maxAstDepth", "sandbox.MaxASTDepth") {
      when (val limit = guestConfig.enterprise?.sandbox?.maxAstDepth) {
        null -> null
        else -> if (limit > 0) {
          limit.toString()
        } else {
          null
        }
      }
    },

    // Sandbox: Max heap. Limits the amount of memory available to the guest.
    VMRuntimeProperty.ofConfigurable("vm.sandbox.maxHeapMemory", "sandbox.MaxHeapMemory") {
      guestConfig.enterprise?.sandbox?.maxHeapMemory?.ifBlank { null }
    },

    // Sandbox: Max threads. Limits the number of threads that can be active in a given guest context.
    VMRuntimeProperty.ofConfigurable("vm.sandbox.maxThreads", "sandbox.MaxThreads") {
      when (val limit = guestConfig.enterprise?.sandbox?.maxThreads) {
        null -> null
        else -> if (limit > 0) {
          limit.toString()
        } else {
          null
        }
      }
    }
  )

  init {
    // install context factory
    contextManager.installContextFactory {
      builder(it)
    }

    // install context spawn
    contextManager.installContextSpawn {
      spawn(it)
    }

    initialized.compareAndSet(false, true)
  }

  /** @inheritDoc */
  override fun language(): GuestLanguage = language

  // Install resolved intrinsics into the target `ctx` `bindings`.
  private fun installIntrinsic(bindings: MutableIntrinsicBindings): (GuestIntrinsic) -> Unit = {
    it.install(bindings)
  }

  // Context builder factory. Provided to the context manager.
  internal fun builder(engine: Engine): VMContext.Builder = VMContext.newBuilder(
    *guestConfig.languages.toTypedArray()
  ).engine(engine).apply {
    // configure baseline settings for the builder according to the implemented VM
    configureVM(this)
  }

  // Context configuration function. Provided to the context manager.
  internal fun spawn(builder: VMContext.Builder): VMContext {
    // 2: build the context
    return builder.build().apply {
      // 3: resolve target language bindings
      val globals = getBindings(language().symbol)
      val overlay = MutableIntrinsicBindings.Factory.create()

      // 4: resolve and install intrinsics into global overlay
      intrinsics().forEach(installIntrinsic(overlay))

      // 5: flush global overlay to bindings
      overlay.forEach { name, target ->
        globals.putMember(name.symbol, target)
      }

      // 6: prepare runtime with language-specific init
      prepare(this, globals)
    }
  }

  /**
   * TBD.
   */
  @Suppress("DEPRECATION")
  private fun configureVM(builder: VMContext.Builder) {
    // set strong secure baseline for context guest access
    builder
      .allowEnvironmentAccess(EnvironmentAccess.NONE)
      .allowHostAccess(HostAccess.SCOPED)
      .allowPolyglotAccess(PolyglotAccess.NONE)
      .allowInnerContextOptions(false)
      .allowCreateThread(false)
      .allowCreateProcess(false)
      .allowAllAccess(false)
      .allowHostClassLoading(false)
      .allowNativeAccess(false)
      .allowExperimentalOptions(true)
      .allowValueSharing(true)
      .fileSystem(filesystem)
      .allowIO(true)

    // allow the guest VM implementation to configure the builder with language-specific options
    Stream.concat(conditionalOptions.stream(), configure(contextManager.engine(), builder)).filter {
      it.active()
    }.forEach {
      builder.option(it.symbol, it.value()!!)  // no null values at this stage
    }
  }

  /**
   * TBD.
   */
  protected open fun intrinsics(): Collection<GuestIntrinsic> = intrinsicsManager.resolver().resolve(
    language()
  )

  /** @inheritDoc */
  override suspend fun prewarmScript(script: ExecutableScript) {
    // no-op (by default)
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

  /** @inheritDoc */
  override suspend fun <R> execute(script: ExecutableScript, returnType: Class<R>, vararg args: Any?): R? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun <R> executeBlocking(script: ExecutableScript, returnType: Class<R>, vararg args: Any?): R? {
    TODO("Not yet implemented")
  }

  /**
   * TBD.
   */
  protected abstract fun configure(engine: Engine, context: VMContext.Builder): Stream<VMProperty>

  /**
   * TBD.
   */
  protected abstract fun prepare(context: VMContext, bindings: GuestValue)

  /**
   * TBD.
   */
  protected abstract fun execute(context: VMContext, script: Code, bindings: InvocationBindings): GuestValue
}
