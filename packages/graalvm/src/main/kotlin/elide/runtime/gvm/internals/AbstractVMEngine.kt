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
import elide.util.RuntimeFlag
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Value as GuestValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import org.graalvm.polyglot.*
import org.graalvm.polyglot.proxy.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream

/**
 * # Guest VM Engine
 *
 * Abstract concept of a "guest" VM engine, which implements a given guest language for use with Elide. The abstract VM
 * engine brings together configuration ([Config]), executable code container ([Code]), and invocation bindings
 * ([Bindings]) types which are implemented for a given language context.
 *
 * These types, together, describe the dispatch flow for the VM to the compiler, and with enough flexibility that each
 * guest language feels natural on the receiving end. Read on for an explanation of each type and what behavior is
 * introduced by this base.
 *
 * ## Configuration
 *
 * Implementations provide a concrete configuration type ([Config]) which describes basic configurations applicable to
 * all guest runtimes (but in the case of this one), as well as any additional configuration parameters which relate to
 * the specific guest VM being configured.
 *
 * Depending on circumstance, this configuration can come from different places:
 * - For servers, **Micronaut-style configuration** is used, with injection sourcing property values from all installed
 *   property sources.
 * - For the CLI, a configuration is built from **command-line flags**.
 *
 * ## Executable scripts
 *
 * Implementations provide a concrete executable script type, compliant with [ExecutableScript] ([Code]), which models
 * the script source API surface for this guest language. MIME types are declared here, as well as some other source
 * related nuances.
 *
 * ## Invocation bindings
 *
 * Finally, implementations provide a concrete invocation binding surface, which explains how [ExecutableScript] types
 * can be dispatched for this guest language. Depending on the different execution modes supported for a given script,
 * these bindings may provide different methods.
 *
 * @param Config Concrete configuration type associated with this VM implementation.
 * @param Code Concrete executable script type associated with this VM implementation.
 * @param Bindings Invocation bindings surface definition for use with this VM implementation.
 */
internal abstract class AbstractVMEngine<
  Config : GuestRuntimeConfiguration,
  Code: ExecutableScript,
  Bindings: InvocationBindings,
> constructor (
  private val contextManager: ContextManager<VMContext, VMContext.Builder>,
  protected val language: GraalVMGuest,
  protected val config: Config,
) : VMEngineImpl<Config>/*, ServerInitializer*/ {
  internal companion object {
    /** Manifest name for runtime info. */
    internal const val runtimeManifest = "runtime.json"
  }

  /**
   * ## Runtime Info.
   *
   * Describes the structure of configuration which is expected to be embedded with a given guest runtime bundle located
   * within the application classpath. The runtime info payload (usually at `runtime.json` under the guest language root
   * path) describes things like:
   *
   * - The version of the runtime
   * - The language supported by the runtime
   * - VFS bundles to load for the runtime
   * - Individual code artifacts to load for the runtime
   *
   * All of these steps occur for a given guest language VM before any user code is executed. This allows embedded
   * runtime code to establish a consistent environment, load polyfills, load and configure intrinsics, and so on, ahead
   * of user code execution.
   *
   * **Note:** Code listed in the [artifacts] section of the configuration are loaded as "internal" sources; by default,
   * such sources are hidden from debugging and show as `native code` in stack traces and console logs. Special flags
   * are available to suppress this behavior if needed (i.e. when debugging the runtime itself).
   *
   * @param engine Version of this engine.
   * @param language Language supported by this engine.
   * @param vfs Virtual File System (VFS) bundles to load and make available (as well as any user VFS bundles).
   * @param artifacts Set of individual code artifacts to load ahead of any user code.
   * @param entry Entrypoint overlay information, which establishes entry-point bindings. Loaded last, after any user
   *   code, if specified. Deprecated (new VMs should not use unless no alternative exists).
   */
  @Serializable
  internal data class RuntimeInfo(
    val engine: String,
    val language: tools.elide.meta.GuestLanguage,
    val vfs: List<RuntimeVFS> = emptyList(),
    val artifacts: List<RuntimeArtifact> = emptyList(),
    val entry: String? = null,
  )

  /**
   * ## Runtime Info: VFS.
   *
   * Describes virtual file system (VFS) bundles which should be loaded along with user data, and placed into the guest
   * accessible file system. Bundles specified in the [RuntimeInfo] configuration are loaded at VM construction time and
   * written to the in-memory FS before any user data, so they can still be overridden by user data, and thus function
   * as polyfills or sensible defaults.
   *
   * VFS bundles can be expressed as compressed or un-compressed tarballs, or in Elide's internal format. The latter is
   * implemented through Elide's own bundler CLI tool.
   *
   * Multiple bundle files can be specified, in which case they are loaded and initialized into the in-memory filesystem
   * with preserved order. After runtime-listed VFS bundles are loaded, any user-provided VFS bundles are loaded.
   *
   * The internal format and compression state of a bundle is inferred from its file-name. If the file name ends in
   * `.tar.gz`, it is interpreted as a compressed tarball; `.tar`, it is interpreted as an un-compressed tarball; the
   * special extension `.evfs` is interpreted as an Elide VFS bundle.
   *
   * @param name Name of the bundle file to load.
   */
  @Serializable
  internal data class RuntimeVFS(
    val name: String,
  )

  /**
   * ## Runtime Info: Artifacts.
   *
   * Describes a single code artifact which should be loaded and evaluated in each VM context before any user code is
   * loaded or executed. Artifacts are loaded in the order specified in the configuration, and are loaded as "internal"
   * sources, which means they are hidden from debugging and show as `native code` in stack traces and console logs.
   *
   * Code listed as runtime artifacts needs to remain as small as possible, and as quick as possible to execute, because
   * all user code waits for it to complete before execution begins. To mitigate this cost, runtime init code is only
   * loaded at the construction of each VM context.
   *
   * Listed files must be directly evaluable code within the context of the guest language implemented by a VM. During
   * initialization of the VM, intrinsics for that language are mounted and available.
   *
   * @param name Name of the file to load and evaluate.
   */
  @Serializable
  internal data class RuntimeArtifact(
    val name: String,
  )

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
   * ## VM: Configure.
   *
   * This method applies baseline configurations consistent with how Elide guarantees guest VMs to work. This includes
   * isolated access to the host environment and code, unless otherwise declared. Because Elide's core makes use of the
   * strict [HostAccess.SCOPED] policy provided by GraalVM, callback types are additionally scoped to the lifecycle of
   * the calls they relate to.
   *
   * At this time, [conditionalOptions] declared by the VM implementation are also consulted, and applied, where
   * enabled, to the resulting configuration.
   *
   * @param builder VM context builder which should receive the configuration.
   */
  @Suppress("DEPRECATION")
  private fun configureVM(builder: VMContext.Builder) {
    // set strong secure baseline for context guest access
    builder
      .allowEnvironmentAccess(EnvironmentAccess.NONE)
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
      .allowHostAccess(HostAccess.newBuilder(HostAccess.SCOPED)
        .allowImplementations(Proxy::class.java)
        .allowAccessAnnotatedBy(HostAccess.Export::class.java)
        .allowArrayAccess(true)
        .allowBufferAccess(true)
        .allowAccessInheritance(true)
        .allowIterableAccess(true)
        .allowIteratorAccess(true)
        .allowListAccess(true)
        .allowMapAccess(true)
        .build())

    // allow the guest VM implementation to configure the builder with language-specific options
    Stream.concat(conditionalOptions.stream(), configure(contextManager.engine(), builder)).filter {
      it.active()
    }.forEach {
      builder.option(it.symbol, it.value()!!)  // no null values at this stage
    }
  }

  /**
   * ## VM: Intrinsics.
   *
   * Collects the set of [GuestIntrinsic] values which should be present for the guest language implemented by this VM;
   * the language which is used to resolve such intrinsics is provided by [language].
   *
   * @see GuestIntrinsic for more information about the structure of intrinsics.
   * @return Collection of intrinsics for the active language.
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
   * ## Implementation: Configure.
   *
   * Provide a stream of [VMProperty] entries which should be applied to the active [context], based on the provided VM
   * [engine]; conditional properties which declare an inactive value are skipped.
   *
   * If the order of the applied properties matters, then the stream should be ordered (and should avoid parallelism).
   *
   * @param engine VM engine which is spawning a context that needs to be configured.
   * @param context Context builder which is in the process of being configured.
   * @return Stream of [VMProperty] instances, or adherents, to test and conditionally apply to the resulting context.
   */
  protected abstract fun configure(engine: Engine, context: VMContext.Builder): Stream<VMProperty>

  /**
   * ## Implementation: Prepare bindings.
   *
   * TBD.
   */
  protected abstract fun prepare(context: VMContext, bindings: GuestValue)

  /**
   * ## Implementation: Execution.
   *
   * TBD.
   */
  protected abstract fun <Inputs: ExecutionInputs> execute(
    context: VMContext,
    script: Code,
    bindings: Bindings,
    inputs: Inputs,
  ): GuestValue
}
