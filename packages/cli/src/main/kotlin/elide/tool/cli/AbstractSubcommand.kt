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

package elide.tool.cli

import org.graalvm.polyglot.Language
import java.io.*
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import elide.annotations.Inject
import elide.runtime.Logger
import elide.runtime.gvm.ContextFactory
import elide.runtime.gvm.VMFacadeFactory
import elide.runtime.gvm.internals.VMProperty
import elide.runtime.gvm.vfs.EmbeddedGuestVFS
import elide.runtime.gvm.vfs.HostVFS
import elide.tool.cli.err.AbstractToolError
import elide.tool.cli.err.ShellError
import elide.tool.cli.state.CommandState
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Engine as VMEngine

/**
 * TBD.
 */
@Suppress("MemberVisibilityCanBePrivate") internal abstract class AbstractSubcommand<
  State: ToolState,
  Context: CommandContext,
> :
  CoroutineScope,
  Closeable,
  AutoCloseable,
  ToolCommandBase<Context>() {
  protected companion object {
    private val _stdout = System.out
    private val _stderr = System.err
    private val _stdin = System.`in`
    private val _inbuf = _stdin.bufferedReader()
    private val _engine = VMEngine.create()
    private val threadFactory: ToolThreadFactory = ToolThreadFactory()
    private val threadedExecutor: ExecutorService = Executors.newCachedThreadPool(threadFactory)
    private val dispatcher: CoroutineDispatcher = threadedExecutor.asCoroutineDispatcher()
    private val engineInitialized: AtomicBoolean = AtomicBoolean(false)

    // Determine the set of supported guest languages.
    internal fun determineSupportedLanguages(): List<Pair<GuestLanguage, Language>> {
      val logging = Statics.logging
      logging.trace("Retrieving supported guest languages")
      return _engine.languages.values.mapNotNull {
        val supported = GuestLanguage.resolveFromEngine(it.id)
        if (supported == null) {
          logging.debug("Language '${it.name}' not supported for CLI use: no support in Elide")
          null
        } else if (!it.isInteractive) {
          logging.debug("Language '${it.name}' not supported for CLI use: no REPL support")
          null
        } else {
          logging.trace("Language '${it.name}' is supported")
          supported to it
        }
      }
    }
  }

  private val logging: Logger by lazy {
    Statics.logging
  }

  /** Implements a thread factory for tool execution operations. */
  private class ToolThreadFactory : ThreadFactory {
    override fun newThread(target: Runnable): Thread {
//      Thread
//        .ofPlatform()
//        .allowSetThreadLocals(true)
//        .inheritInheritableThreadLocals(true)
//        .name("elide", 1337L)
//        .priority(Thread.MAX_PRIORITY)
//        .unstarted(target)
      return Thread().apply {
        priority = Thread.MAX_PRIORITY
        isDaemon = true
//        isUncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
//          e.printStackTrace()
//        }
      }
    }
  }

  /** Output controller base surface. */
  internal sealed interface OutputController : Logger {
    /** Current output settings. */
    val settings: ToolState.OutputSettings

    /** Active output session. */
//    val session: Int?

    /** Direct access to standard-out. */
    val stdout: PrintStream get() = _stdout

    /**
     * TBD.
     */
    fun emit(text: CharSequence)

    /**
     * TBD.
     */
    fun line(text: CharSequence)

    /**
     * TBD.
     */
    suspend fun pretty(operation: OutputCallable, fallback: OutputCallable)

    /**
     * TBD.
     */
    suspend fun pretty(operation: OutputCallable) = pretty(operation) {
      // Do nothing as a fallback.
    }

    /**
     * TBD.
     */
    fun verbose(vararg args: Any)
  }

  /** Input controller base surface. */
  @Suppress("unused") internal sealed interface InputController {
    /** Direct access to standard-input. */
    val stdin: InputStream get() = _stdin

    /** Buffered access to `stdin`. */
    val buffer: BufferedReader get() = _inbuf

    /**
     * TBD.
     */
    fun readLineBlocking(): String?

    /**
     * TBD.
     */
    suspend fun readLine(): String?

    /**
     * TBD.
     */
    suspend fun readLineAsync(): Deferred<String?>
  }

  /** Controller for parallel execution. */
  internal sealed interface ExecutionController {
    /** Co-routine scope of the current execution. */
    val scope: CoroutineScope

    /** Co-routine context of the current execution. */
    val context: CoroutineContext

    /** Active dispatcher. */
    val dispatcher: CoroutineDispatcher
  }

  /** Execution context for a run of the Elide tool. */
  internal sealed interface ToolContext<State: ToolState> {
    /** Output settings and controls. */
    val output: OutputController

    /** Input settings and controls. */
    val input: InputController

    /** Settings and controls for task execution. */
    val exec: ExecutionController

    /** Active state for this tool run. */
    val state: State
  }

  /** Default input controller implementation. */
  protected open class DefaultInputController (
    private val inbuf: BufferedReader? = null,
  ) : InputController {
    override suspend fun readLine(): String? = readLineAsync().await()

    override suspend fun readLineAsync(): Deferred<String?> = coroutineScope {
      async {
        withContext(Dispatchers.IO) {
          readLineBlocking()
        }
      }
    }

    override fun readLineBlocking(): String? = (inbuf ?: buffer).readLine()

    internal companion object {
      /** Singleton default input controller. */
      val DEFAULT = DefaultInputController()
    }
  }

  /** Default output controller implementation. */
  protected open class DefaultOutputController<State: ToolState> (
    private val _state: State,
    private val _logger: Logger,
    private val _settings: ToolState.OutputSettings = _state.output
  ) : OutputController, Logger by _logger {
    override val settings: ToolState.OutputSettings get() = _settings

    override fun emit(text: CharSequence) = (if (_settings.stderr) _stderr else _stdout).print(text)

    override fun line(text: CharSequence) = (if (_settings.stderr) _stderr else _stdout).println(text)

    override suspend fun pretty(operation: OutputCallable, fallback: OutputCallable) {
      if (_settings.pretty) {
        operation.invoke(_state, this)
      }
    }

    override fun verbose(vararg args: Any) {
      if (_settings.verbose && !_settings.quiet) {
        _logger.info(*args)
      }
    }
  }

  /** Default (wrapped) execution controller. */
  protected open class DefaultExecutionController(
    override val dispatcher: CoroutineDispatcher,
    override val scope: CoroutineScope,
    override val context: CoroutineContext,
  ) : ExecutionController

  /** Private implementation of tool execution context. */
  protected abstract class ToolExecutionContextImpl<T: ToolState> constructor (private val _state: T) : ToolContext<T> {
    internal companion object {
      @JvmStatic fun <T: ToolState> forSuite(
        state: T,
        output: OutputController,
        input: InputController,
        exec: ExecutionController,
      ): ToolExecutionContextImpl<T> {
        return object : ToolExecutionContextImpl<T>(state) {
          override val output: OutputController get() = output
          override val input: InputController get() = input
          override val exec: ExecutionController get() = exec
        }
      }
    }

    /** Return the calculated tool state. */
    override val state: T get() = _state
  }

  // Shared resources which should be closed at the conclusion of processing.
  private val sharedResources: MutableList<AutoCloseable> = LinkedList()

  // Main top-level tool.
  @Inject private lateinit var base: ElideTool

  // Context factory for guest VMs.
  @Inject private lateinit var vmContextFactory: ContextFactory<VMContext, VMContext.Builder>

  // Factory to acquire VM execution facades.
  @Inject protected lateinit var vmFactory: VMFacadeFactory

  /** Controller for tool output. */
  protected lateinit var out: OutputController

  /** Controller for tool input. */
  protected lateinit var input: InputController

  /** Controller for tool execution. */
  protected lateinit var exec: ExecutionController

  /** Execution context for the current tool run. */
  protected lateinit var context: ToolContext<State>

  /** Debug flag status. */
  val debug: Boolean get() = base.debug

  /** Verbose output flag status. */
  val verbose: Boolean get() = base.verbose

  /** Quiet output flag status. */
  val quiet: Boolean get() = base.quiet

  /** Pretty output flag status. */
  val pretty: Boolean get() = base.pretty

  // Base execution context.
  private val baseExecContext: CoroutineContext = Dispatchers.Default + CoroutineName("elide")
  override val coroutineContext: CoroutineContext get() = baseExecContext

  // Build an initial `ToolState` instance from the main tool.
  private fun materializeInitialState(): ToolState {
    return ToolState.EMPTY
  }

  // Build a new context for execution of a sub-command.
  private fun buildToolContext(
    scope: CoroutineScope,
    context: CoroutineContext,
    state: State,
  ): ToolContext<State> {
    return ToolExecutionContextImpl.forSuite(
      state,
      outputController(state),
      inputController(state, _stdin, _inbuf),
      execController(state, scope, context),
    )
  }

  // Initialize shared streams, sessions, and any other tool resources.
  private suspend fun <R> initializeToolResources(
    state: State,
    op: suspend ToolContext<State>.() -> R,
  ): R {
    logging.debug("Prepping tool resources")
    attachShutdownHook()

    return use {
      val toolContext = buildToolContext(
        this,
        coroutineContext,
        state,
      )

      // mount context targets
      out = toolContext.output
      input = toolContext.input
      exec = toolContext.exec
      context = toolContext

      // call into the subclass to initialize the VM, as needed
      if (initializeVM(state)) {
        // activate the context, having been configured by now via `configureVM`
        try {
          vmContextFactory.activate()
        } catch (err: Throwable) {
          logging.error("Failed to activate VM context factory", err)
          throw err
        }
      }
      op.invoke(toolContext)
    }
  }

  // Attach a VM shutdown hook which cleans up and emits a shutdown message.
  private fun attachShutdownHook() {
    Runtime.getRuntime().addShutdownHook(Thread {
      logging.debug("Cleaning up tool resources")
      close()

      runBlocking {
        out.pretty({
          line("Exiting session. Have a great day! \uD83D\uDC4B")
        }, {
          line("Exited session")
        })
      }
    })
  }

  /**
   * TBD.
   */
  override fun close() {
    sharedResources.forEach {
      try {
        logging.trace("Cleaning shared resource", it)
        it.close()
      } catch (err: Throwable) {
        logging.error("Caught exception while closing shared resource '$it' (ignored)", err)
      }
    }
  }

  /**
   * Run the target sub-command, managing state internally for resources which are expected to be shared, and which may
   * need to be closed at the conclusion of the command's execution lifecycle.
   *
   * This method is responsible for translating a zero-arg sub-command call into an [invoke] call, with a filled out
   * [ToolState] instance. It does this by:
   *
   * 1. Querying the sub-class instance for any [state] that it wants to decode. This preempts default state and is
   *    optional. The default implementation returns `null` in order to opt-out.
   * 2. If no state is provided by the sub-command implementation, one is calculated from the arguments provided to the
   *    root tool ([base]).
   *
   * After state has been acquired, resources are initialized and held open via the [initialize] method, which may also
   * be overridden by sub-commands to customize initialization.
   */
  override suspend fun Context.invoke(state: CommandState): CommandResult = use {
    // allow the subclass to register its own shared resources
    sharedResources.addAll(initialize(base))

    // build initial state
    val toolState = state(base) ?: materializeInitialState()
    val ctx = context(state)

    @Suppress("UNCHECKED_CAST")
    initializeToolResources(toolState as State) {
      // finally, call the sub-command entrypoint
      ctx.invoke(this)
    }
  }

  /**
   * TBD.
   */
  protected fun configureVM(props: Stream<VMProperty>) {
    vmContextFactory.configureVM(props)
  }

  /**
   * TBD.
   */
  @Suppress("DEPRECATION")
  protected open fun withVM(
    context: ToolContext<State>,
    userBundles: List<URI>,
    systemBundles: List<URI>,
    hostIO: Boolean,
    contextBuilder: (VMContext.Builder) -> Unit = {},
    op: VMCallable<State>,
  ) {
    require(!engineInitialized.get()) {
      "Cannot re-initialize CLI guest VM"
    }
    engineInitialized.set(true)

    logging.debug("Acquiring VM context for CLI tool")
    val wrappedBuilder: (VMContext.Builder) -> Unit = {
      // configure the VM as normal
      contextBuilder.invoke(it)

      // if we have a virtualized FS, mount it
      if (userBundles.isNotEmpty() && !hostIO) {
        val bundles = userBundles.map { fsBundleUri ->
          // check the bundle URI
          if (fsBundleUri.scheme == "classpath:") {
            logging.debug("Rejecting `classpath:`-prefixed bundle: not supported by CLI")
            throw ShellError.BUNDLE_NOT_FOUND.asError()
          } else {
            // make sure the file can be read
            val file = try {
              logging.trace("Checking bundle at URI '$fsBundleUri'")
              File(fsBundleUri)
            } catch (err: IOException) {
              throw ShellError.BUNDLE_NOT_FOUND.asError()
            }
            logging.trace("Checking existence of '$fsBundleUri'")
            if (!file.exists()) throw ShellError.BUNDLE_NOT_FOUND.asError()
            logging.trace("Checking readability of '$fsBundleUri'")
            if (!file.canRead()) throw ShellError.BUNDLE_NOT_ALLOWED.asError()
            logging.debug("Mounting guest filesystem at URI: '$fsBundleUri'")
            fsBundleUri
          }
        }

        it.fileSystem(
          EmbeddedGuestVFS.forBundle(
            *systemBundles.plus(bundles).toTypedArray(),
          )
        )
      } else if (systemBundles.isNotEmpty() && !hostIO) {
        logging.debug { "No user bundles, but ${systemBundles.size} system bundles present; mounting embedded" }
        it.fileSystem(
          EmbeddedGuestVFS.forBundle(
            *systemBundles.toTypedArray(),
          )
        )
      } else if (hostIO) {
        // if we're doing host I/O, mount that instead
        logging.debug("Command-line flags indicate host I/O; mounting host filesystem")
        it.fileSystem(HostVFS.acquire())
      }
    }
    vmContextFactory.acquire(wrappedBuilder) {
      try {
        enter()
        op.invoke(context, this)
      } catch (err: AbstractToolError) {
        // it's a known tool error. re-throw.
        throw err
      } catch (err: Throwable) {
        logging.error(
          "Uncaught exception within VM context. Please catch and handle all VM execution exceptions.",
          err,
        )
        throw err
      } finally {
        leave()
      }
    }
  }

  /**
   * TBD.
   */
  protected open fun outputController(
    state: ToolState,
//    session: OutputSession?,
  ): OutputController =
    DefaultOutputController(state, Statics.logging)

  /**
   * TBD.
   */
  @Suppress("SameParameterValue")
  protected open fun inputController(state: ToolState, input: InputStream, buffer: BufferedReader): InputController =
    DefaultInputController.DEFAULT

  /**
   * TBD.
   */
  protected open fun execController(
    state: ToolState,
    scope: CoroutineScope,
    context: CoroutineContext,
  ): ExecutionController = DefaultExecutionController(
    dispatcher,
    scope,
    context,
  )

  /**
   * TBD.
   */
  protected open fun initializeVM(base: State): Boolean = false

  /**
   * TBD.
   */
  protected open fun initialize(base: ElideTool): List<AutoCloseable> = emptyList()

  /**
   * TBD.
   */
  protected open fun state(base: ElideTool): State? = null

  /**
   * TBD.
   */
  protected abstract suspend fun CommandContext.invoke(state: ToolContext<State>): CommandResult
}
