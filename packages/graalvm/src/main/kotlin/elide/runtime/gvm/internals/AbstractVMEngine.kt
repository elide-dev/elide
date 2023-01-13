package elide.runtime.gvm.internals

import elide.annotations.Inject
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.*
import elide.runtime.gvm.cfg.GuestRuntimeConfiguration
import elide.runtime.gvm.cfg.GuestVMConfiguration
import elide.runtime.gvm.internals.GVMInvocationBindings.DispatchStyle
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchRequestIntrinsic
import elide.runtime.gvm.internals.js.JsInvocationBindings
import elide.runtime.ssr.HeaderMap
import elide.runtime.ssr.ServerResponse
import elide.util.RuntimeFlag
import io.micronaut.http.HttpRequest
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.graalvm.polyglot.*
import org.graalvm.polyglot.proxy.Proxy
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.stream.Stream
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Value as GuestValue

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
) : VMEngineImpl<Config> {
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
  override suspend fun executeStreaming(
    script: ExecutableScript,
    args: ExecutionInputs,
    receiver: StreamingReceiver,
  ): Job {
    require(script.language().symbol == language.symbol) {
      "Cannot execute script of type '${script.language().label}' with VM '${this::class.simpleName}'"
    }
    require(script.invocation().contains(InvocationMode.STREAMING)) {
      "Cannot execute script '$script' via streaming interface: It does not support streaming"
    }
    return coroutineScope {
      launch {
        contextManager {
          // "initialize" the script, which includes parsing, compiling, and resolving exports, etc.
          val initialized = initializeScript(this, script)

          // resolve in vocation bindings adapters for this script
          val bindings = resolve(
            this,
            initialized,
            DispatchStyle.SERVER,
          )

          // execute the script and obtain an output value
          val out = execute(
            this,
            initialized,
            bindings,
            args,
          )
          logging.info("Server out", out)
          TODO("server response handling not yet implemented")
        }
      }
    }
  }

  /** @inheritDoc */
  override suspend fun <R> executeAsync(
    script: ExecutableScript,
    returnType: Class<R>,
    args: ExecutionInputs?,
  ): Deferred<R?> {
    require(script.language().symbol == language.symbol) {
      "Cannot execute script of type '${script.language().label}' with VM '${this::class.simpleName}'"
    }
    require(script.invocation().contains(InvocationMode.ASYNC)) {
      "Cannot execute script '$script' via async interface: It does not support async invocation"
    }
    return coroutineScope {
      async {
        contextManager {
          // "initialize" the script, which includes parsing, compiling, and resolving exports, etc.
          val initialized = initializeScript(this, script)

          // resolve in vocation bindings adapters for this script
          val bindings = resolve(this, initialized)

          // execute the script and obtain an output value
          val out = execute(
            this,
            initialized,
            bindings,
            args ?: ExecutionInputs.EMPTY,
          )
          out.`as`(returnType)
        }
      }
    }
  }

  // Decode a `ServerResponse` value from a guest script.
  private fun decodeServerResponse(hasMembers: Boolean, value: GuestValue): ServerResponse {
    return object: ServerResponse {
      override val status: Int? get() = (
        if (hasMembers && value.hasMember("status")) {
          value.getMember("status").asInt()
        } else {
          null
        }
      )

      override val headers: HeaderMap? get() = (
        if (hasMembers && value.hasMember("headers")) {
          val headersData = value.getMember("headers")
          if (!headersData.isNull && headersData.hasMembers()) {
            headersData.memberKeys.associateWith { key ->
              headersData.getMember(key).asString()
            }
          } else {
            null
          }
        } else {
          null
        }
      )

      override val content: String get() = (
        if (hasMembers && value.hasMember("content")) {
          value.getMember("content").asString()
        } else {
          ""
        }
      )

      override val hasContent: Boolean get() = (
        hasMembers && value.getMember("hasContent")?.asBoolean() ?: content.isNotBlank()
      )

      override val fin: Boolean get() = (
        hasMembers && value.getMember("fin")?.asBoolean() ?: false
      )
    }
  }

  // Build a callable function which can receive streamed render output.
  private fun buildRenderProxy(receiver: StreamingReceiver) : ProxyExecutable {
    return ProxyExecutable { arguments ->
      val chunk = arguments.firstOrNull()
      if (chunk != null) {
        val hasMembers = chunk.hasMembers()
        receiver.invoke(decodeServerResponse(
          hasMembers,
          chunk,
        ))
      }
      null  // no return value
    }
  }

  // Handle a render output value, which should either be a `String`, `ServerResponse`, or one of those wrapped in a
  // `Promise`.
  private fun handleRenderOutput(out: GuestValue, receiver: StreamingReceiver) {
    when (val meta = out.metaObject) {
      null -> error("Failed to resolve meta-object for `render` output")
      else -> when {
        out.isNull -> logging.warn("Received `null` from `render` in guest script; skipping")

        // if the function returned a string directly, emit it as the final chunk
        out.isString -> receiver.invoke(object: ServerResponse {
          override val content: String get() = meta.asString()
          override val hasContent: Boolean get() = true
          override val fin: Boolean get() = true
        })

        // otherwise, if the function returns a promise, we should wait on the output and re-dispatch.
        meta.metaSimpleName == "Promise" -> {
          val resultReceiver: Consumer<GuestValue> = Consumer {
            handleRenderOutput(it, receiver)
          }
          out.invokeMember("then", resultReceiver)
        }

        // otherwise, consider it a `ServerResponse`.
        out.hasMembers() -> receiver.invoke(decodeServerResponse(
          true,
          out,
        ))

        else -> error("Failed to resolve `render` output: Unrecognized value $out")
      }
    }
  }

  /** @inheritDoc */
  @Suppress("UNCHECKED_CAST", "IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
  override suspend fun executeRender(
    script: ExecutableScript,
    request: HttpRequest<*>,
    context: Any?,
    receiver: StreamingReceiver
  ): Job {
    require(script.language().symbol == language.symbol) {
      "Cannot execute script of type '${script.language().label}' with VM '${this::class.simpleName}'"
    }
    require(script.invocation().contains(InvocationMode.STREAMING)) {
      "Cannot execute script '$script' via streaming render interface: It does not support streaming invocation"
    }
    return coroutineScope {
      async {
        contextManager {
          // "initialize" the script, which includes parsing, compiling, and resolving exports, etc.
          val initialized = initializeScript(this, script)

          // resolve in vocation bindings adapters for this script

          // execute the script and obtain an output value
          when (val bindings = resolve(
            this,
            initialized,
            DispatchStyle.RENDER,
          )) {
            // we are executing a sidecar render call
            is JsInvocationBindings.JsRender -> {
              val out = try {
                bindings.mapped.values.first().value.execute(
                  FetchRequestIntrinsic.forRequest(request),
                  context,
                  buildRenderProxy(receiver),
                )
              } catch (exc: PolyglotException) {
                logging.error("Failed to dispatch `render` method for SSR script", exc)
                throw exc
              }

              handleRenderOutput(
                out,
                receiver,
              )
            }

            // the binding supports multiple interfaces
            is JsInvocationBindings.JsCompound -> {
              // we should be dealing with a render function
              if (!bindings.supported().contains(DispatchStyle.RENDER))
                error("Cannot invoke embedded SSR script: `render` function not exported")

              val entry = bindings.mapped.entries.find {
                it.key.type == JsInvocationBindings.JsEntrypointType.RENDER
              } ?: error(
                "Entrypoint unresolved: `render` (for embedded script binding $bindings)"
              )

              val out = try {
                entry.value.value.execute(
                  FetchRequestIntrinsic.forRequest(request),
                  context,
                  receiver,
                )
              } catch (exc: PolyglotException) {
                logging.error("Failed to dispatch `render` method for SSR script", exc)
                throw exc
              }

              handleRenderOutput(
                out,
                receiver,
              )
            }

            else -> error("Unsupported JS invocation binding: $bindings")
          }
        }
      }
    }
  }

  /** @inheritDoc */
  override suspend fun <R> execute(
    script: ExecutableScript,
    returnType: Class<R>,
    args: ExecutionInputs?,
  ): R? = executeAsync(
    script,
    returnType,
    args,
  ).await()

  /** @inheritDoc */
  override fun <R> executeBlocking(
    script: ExecutableScript,
    returnType: Class<R>,
    args: ExecutionInputs?,
  ): R? = runBlocking {
    execute(script, returnType, args)
  }

  /**
   * Initialize a guest [script] within the provided [context].
   *
   * @param context VM context with which to initialize the script.
   * @param script Guest script to initialize.
   * @return Casted, checked, initialized script.
   */
  @Suppress("UNUSED_PARAMETER", "UNCHECKED_CAST")
  private fun initializeScript(context: VMContext, script: ExecutableScript): Code {
    return when (script) {
      is AbstractGVMScript -> {
        when (script.state()) {
          // if the script is not even initialized, we may need to load it from disk, or from the classpath.
          ExecutableScript.State.UNINITIALIZED -> {
            try {
              script.load()
            } catch (exc: Throwable) {
              logging.error(
                "Failed to call `load` on script '$script'",
                exc,
              )
              throw exc
            }

            // if no error was encountered, recurse to next step
            initializeScript(context, script)
          }

          // if the script has not been `EVALUATED` yet, we must evaluate it before execution
          ExecutableScript.State.PARSED -> {
            try {
              script.evaluate(context)  // primes a value cache
              script as Code
            } catch (exc: PolyglotException) {
              logging.error(
                "Failed to evaluate guest script '$script'",
                exc,
              )
              throw exc
            }
          }

          // otherwise, we are ready to execute
          ExecutableScript.State.EVALUATED, ExecutableScript.State.EXECUTED -> script as Code
        }
      }
      else -> error(
        "Cannot execute script of type '${script::class.java.simpleName}' via GraalVM engine"
      )
    }
  }

  // -- VM: Configuration -- //

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
   * This method offers the runtime implementation an opportunity to prepare the provided [context] for use with guest
   * code. The provided [globals] are made available after registration of any intrinsics, as applicable, and the
   * [context] provided is configured with the expected set of [VMProperty] instances, according to [configure].
   *
   * @param context Context to prepare for use with guest code.
   * @param globals Global language bindings which are active for this context.
   */
  protected abstract fun prepare(context: VMContext, globals: GuestValue)

  // -- VM: Execution -- //

  /**
   * ## Implementation: Resolve.
   *
   * This method is dispatched by the abstract VM implementation in order to resolve a concrete set of invocation
   * [Bindings] for the provided [script] and dispatch [mode] (as applicable); if no [mode] is available, a sensible
   * default entry-point should be chosen (or an error thrown).
   *
   * @param context Context with which to resolve bindings for the provided [script].
   * @param script Guest code script to resolve bindings for.
   * @param mode Desired dispatch style for this script, if known.
   * @return Set of initialized invocation [Bindings].
   */
  protected abstract fun resolve(context: VMContext, script: Code, mode: DispatchStyle? = null): Bindings

  /**
   * ## Implementation: Execution.
   *
   * Called from the abstract VM implementation when execution of a given [script] and [bindings] pair should actually
   * take place; the provided [inputs] should be understood by the implementation and translated as needed.
   *
   * @param context Context within which this execution should take place.
   * @param script Guest code script under evaluation/execution.
   * @param bindings Resolved bindings from [resolve] for the provided [script].
   * @param inputs Inputs to the script, if any.
   * @return Result of the execution, if any.
   */
  protected abstract fun <Inputs: ExecutionInputs> execute(
    context: VMContext,
    script: Code,
    bindings: Bindings,
    inputs: Inputs,
  ): GuestValue
}
