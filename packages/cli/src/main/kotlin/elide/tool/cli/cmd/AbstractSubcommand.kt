package elide.tool.cli.cmd

import elide.annotations.Inject
import elide.runtime.Logger
import elide.runtime.gvm.ContextFactory
import elide.runtime.gvm.VMFacadeFactory
import elide.runtime.gvm.internals.VMProperty
import elide.runtime.gvm.vfs.EmbeddedGuestVFS
import elide.runtime.gvm.vfs.HostVFS
import elide.tool.bundler.AbstractBundlerSubcommand
import elide.tool.cli.*
import elide.tool.cli.GuestLanguage
import elide.tool.cli.OutputCallable
import elide.tool.cli.Statics
import elide.tool.cli.ToolState
import elide.tool.cli.VMCallable
import elide.tool.cli.err.AbstractToolError
import elide.tool.cli.err.ShellError
import kotlinx.coroutines.*
import org.graalvm.polyglot.Language
import java.io.BufferedReader
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Engine as VMEngine
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.lang.Runnable
import java.net.URI
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.stream.Stream
import kotlin.coroutines.CoroutineContext

/**
 * TBD.
 */
@Suppress("MemberVisibilityCanBePrivate") internal abstract class AbstractSubcommand<State: ToolState> :
  CoroutineScope,
  Closeable,
  AutoCloseable,
  AbstractBundlerSubcommand.BundlerParentCommand,
  AbstractToolCommand() {
  protected companion object {
    private val _stdout = System.out
    private val _stderr = System.err
    private val _stdin = System.`in`
    private val _inbuf = _stdin.bufferedReader()
    private val _engine = VMEngine.create()
    private val logging: Logger = Statics.logging
    private val threadFactory: ToolThreadFactory = ToolThreadFactory()
    private val threadedExecutor: ExecutorService = Executors.newCachedThreadPool(threadFactory)
    private val dispatcher: CoroutineDispatcher = threadedExecutor.asCoroutineDispatcher()

    // Determine the set of supported guest languages.
    internal fun determineSupportedLanguages(): List<Pair<GuestLanguage, Language>> {
      logging.trace("Retrieving supported guest languages")
      return _engine.languages.values.mapNotNull {
        val supported = GuestLanguage.resolveFromId(it.id)
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

  /** Implements a thread factory for tool execution operations. */
  private class ToolThreadFactory : ThreadFactory {
    /** @inheritDoc */
    override fun newThread(target: Runnable): Thread = Thread
      .ofPlatform()
      .allowSetThreadLocals(true)
      .inheritInheritableThreadLocals(true)
      .name("elide", 1337L)
      .priority(Thread.MAX_PRIORITY)
      .unstarted(target)
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
    /** @inheritDoc */
    override suspend fun readLine(): String? = readLineAsync().await()

    /** @inheritDoc */
    override suspend fun readLineAsync(): Deferred<String?> = coroutineScope {
      async {
        withContext(Dispatchers.IO) {
          readLineBlocking()
        }
      }
    }

    /** @inheritDoc */
    override fun readLineBlocking(): String? = (inbuf ?: buffer).readLine()

    internal companion object {
      /** Singleton default input controller. */
      val DEFAULT = DefaultInputController()
    }
  }

  /** Default output controller implementation. */
  protected open class DefaultOutputController<State: ToolState> (
    private val _state: State,
//    private val _session: OutputSession?,
    private val _logger: Logger,
    private val _settings: ToolState.OutputSettings = _state.output
  ) : OutputController, Logger by _logger {
    /** @inheritDoc */
    override val settings: ToolState.OutputSettings get() = _settings

    /** @inheritDoc */
//    override val session: Session? get() = _session

    /** @inheritDoc */
    override fun emit(text: CharSequence) = (if (_settings.stderr) _stderr else _stdout).print(text)

    /** @inheritDoc */
    override fun line(text: CharSequence) = (if (_settings.stderr) _stderr else _stdout).println(text)

    /** @inheritDoc */
    override suspend fun pretty(operation: OutputCallable, fallback: OutputCallable) {
      if (_settings.pretty) {
        operation.invoke(_state, this)
      }
    }

    /** @inheritDoc */
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

  /** @inheritDoc */
  override val debug: Boolean get() = base.debug

  /** @inheritDoc */
  override val verbose: Boolean get() = base.verbose

  /** @inheritDoc */
  override val quiet: Boolean get() = base.quiet

  /** @inheritDoc */
  override val pretty: Boolean get() = base.pretty

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
  private fun <R> initializeToolResources(
    state: State,
    op: ToolContext<State>.() -> R,
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
  @Suppress("UNCHECKED_CAST")
  override fun run() = protect {
//    use {
      // allow the subclass to register its own shared resources
      sharedResources.addAll(initialize(base))

      // build initial state
      val state = state(base) ?: materializeInitialState()

      // pretty-format output session
//      if (terminal != null) {
//        outputSession(terminal) {
//          bootAndExecute(this)
//        }
//        initializeToolResources(state as State) {
          // finally, call the sub-command entrypoint
//          invoke(this)
//        }
//      } else {
        // unable to bind to system terminal; fall back to non-pretty output.
//        bootAndExecute(null)
//      }
    initializeToolResources(state as State) {
      // finally, call the sub-command entrypoint
      invoke(this)
    }

//    }
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
    fsBundleUris: List<URI>,
    hostIO: Boolean,
    contextBuilder: (VMContext.Builder) -> Unit = {},
    op: VMCallable<State>,
  ) {
    logging.debug("Acquiring VM context for CLI tool")
    val wrappedBuilder: (VMContext.Builder) -> Unit = {
      // configure the VM as normal
      contextBuilder.invoke(it)

      // if we have a virtualized FS, mount it
      if (fsBundleUris.isNotEmpty() && !hostIO) {
        val bundles = fsBundleUris.map { fsBundleUri ->
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

        it.fileSystem(EmbeddedGuestVFS.forBundle(
          *bundles.toTypedArray(),
        ))
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
  protected open fun protect(op: () -> Unit) {
    try {
      op.invoke()
    } catch (err: AbstractToolError) {
      // it's a known tool error. re-throw.
      throw err
    } catch (err: Throwable) {
      logging.error("Uncaught exception. Please catch and handle all exceptions within the scope of a sub-command", err)
      throw err
    }
  }

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
  protected abstract fun invoke(context: ToolContext<State>)
}
