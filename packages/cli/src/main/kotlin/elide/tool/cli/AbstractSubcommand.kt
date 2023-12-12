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

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import lukfor.progress.TaskService
import lukfor.progress.TaskServiceBuilder
import lukfor.progress.executors.ITaskExecutor
import lukfor.progress.tasks.ITaskRunnable
import lukfor.progress.tasks.Task
import lukfor.progress.tasks.TaskFailureStrategy.IGNORE_FAILURES
import org.graalvm.polyglot.Language
import picocli.CommandLine.Mixin
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Spec
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStream
import java.io.PrintStream
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import elide.annotations.Inject
import elide.runtime.Logger
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration
import elide.tool.cli.err.AbstractToolError
import elide.tool.cli.options.CommonOptions
import elide.tool.cli.state.CommandState
import elide.tool.err.DefaultErrorHandler
import elide.tool.err.ErrorHandler
import org.graalvm.polyglot.Engine as VMEngine


/**
 * # Sub-command
 *
 * Abstract class which provides baseline logic for sub-commands which are part of the Elide CLI. This class provides
 * facilities for logging, resource management, input/output control, managed execution, and more; all sub-commands
 * inherit.
 *
 * @param State Structure used for tooling state, which can be specific to this sub-command type.
 * @param Context Command execution context shape, which can be specific to this sub-command type.
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
    private const val enableVirtualThreads = true
    private const val enableFixedThreadPool = false
    private const val enableFlexibleThreadPool = true

    private val _stdout = System.out
    private val _stderr = System.err
    private val _stdin = System.`in`
    private val _inbuf = _stdin.bufferedReader()
    private val _engine = VMEngine.create()

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

  private val _cpus = Runtime.getRuntime().availableProcessors()

  private val threadFactory: ToolThreadFactory = ToolThreadFactory(
    enableVirtualThreads,
    DefaultErrorHandler.acquire(),
  )
  private val threadedExecutor: ListeningExecutorService = when {
    enableVirtualThreads -> MoreExecutors.listeningDecorator(Executors.newThreadPerTaskExecutor(threadFactory))
    enableFixedThreadPool -> MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(_cpus, threadFactory))
    enableFlexibleThreadPool -> MoreExecutors.listeningDecorator(
      MoreExecutors.getExitingScheduledExecutorService(ScheduledThreadPoolExecutor(_cpus, threadFactory))
    )
    else -> MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(threadFactory))
  }
  private val dispatcher: CoroutineDispatcher = threadedExecutor.asCoroutineDispatcher()

  private val logging: Logger by lazy {
    Statics.logging
  }

  /** Implements a thread factory for tool execution operations. */
  private class ToolThreadFactory (
    private val virtualThreads: Boolean,
    private val errorHandler: ErrorHandler,
  ) : ThreadFactory {
    override fun newThread(target: Runnable): Thread = when (virtualThreads) {
      true -> Thread
        .ofVirtual()
        .inheritInheritableThreadLocals(true)
        .name("elide-vt")
        .unstarted(target)

      false -> Thread
        .ofPlatform()
        .inheritInheritableThreadLocals(true)
        .name("elide")
        .priority(Thread.MAX_PRIORITY)
        .unstarted(target)
        .apply { isDaemon = true }

    }.apply {
      uncaughtExceptionHandler = errorHandler
    }
  }

  /** Output controller base surface. */
  internal sealed interface OutputController : Logger {
    /** Current output settings. */
    val settings: ToolState.OutputSettings

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

  /** Central task service interface. */
  internal interface ExecutionService {
    /** Task executor. */
    val taskExecutor: ITaskExecutor

    /** Run tasks against the main service. */
    fun run(vararg tasks: ITaskRunnable)

    /** Run tasks against the main service. */
    fun run(tasks: List<ITaskRunnable>)
  }

  /** Controller for parallel execution. */
  internal sealed interface ExecutionController {
    /** Co-routine scope of the current execution. */
    val scope: CoroutineScope

    /** Co-routine context of the current execution. */
    val context: CoroutineContext

    /** Active dispatcher. */
    val dispatcher: CoroutineDispatcher

    /** Central task execution service and orchestration. */
    val service: ExecutionService
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
    val backgroundExecutor: ITaskExecutor,
    val runner: (List<ITaskRunnable>) -> Unit,
    override val dispatcher: CoroutineDispatcher,
    override val scope: CoroutineScope,
    override val context: CoroutineContext,
  ) : ExecutionController {
    override val service: ExecutionService get() = object: ExecutionService {
      override val taskExecutor: ITaskExecutor get() = backgroundExecutor

      override fun run(vararg tasks: ITaskRunnable) {
        runner.invoke(tasks.toList())
      }

      override fun run(tasks: List<ITaskRunnable>) {
        runner.invoke(tasks)
      }
    }
  }

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

  // Common options shared by all commands.
  @Mixin internal lateinit var commons: CommonOptions

  // Command specification from Picocli.
  @Spec internal lateinit var commandSpec: CommandSpec

  /** A thread-local [PolyglotContext] instance acquired from the [engine]. */
  private val contextHandle: ThreadLocal<PolyglotContext> = ThreadLocal()

  /**
   * A lazily initialized [PolyglotEngine] instance, customized by the [configureEngine] event.
   *
   * @see createEngine
   */
  protected val engine: PolyglotEngine by lazy(::createEngine)

  /** Controller for tool output. */
  protected lateinit var out: OutputController

  /** Controller for tool input. */
  protected lateinit var input: InputController

  /** Controller for tool execution. */
  protected lateinit var exec: ExecutionController

  /** Execution context for the current tool run. */
  protected lateinit var context: ToolContext<State>

  /** Whether this is an interactive session. */
  val interactive: AtomicBoolean = AtomicBoolean(false)

  /** Debug flag status. */
  val debug: Boolean get() = commons.debug

  /** Verbose output flag status. */
  val verbose: Boolean get() = false  // @TODO(sgammon): fix mixin injection

  /** Quiet output flag status. */
  val quiet: Boolean get() = commons.quiet

  /** Pretty output flag status. */
  val pretty: Boolean get() = commons.pretty

  // Base execution context.
  private val baseExecContext: CoroutineContext = Dispatchers.Default + CoroutineName("elide")
  override val coroutineContext: CoroutineContext get() = baseExecContext

  /**
   * Configure and create a new [PolyglotEngine], invoking the [configureEngine] event, and applying all relevant
   * constraints. This method is meant to be used by the lazy [engine] property.
   *
   * @return A new, exclusive [PolyglotEngine] instance.
   */
  private fun createEngine(): PolyglotEngine = PolyglotEngine {
    // allow subclasses to customize the engine
    configureEngine()
  }

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

      initializeVM(state)
      op.invoke(toolContext)
    }
  }

  // Attach a VM shutdown hook which cleans up and emits a shutdown message.
  private fun attachShutdownHook() {
    Runtime.getRuntime().addShutdownHook(
      Thread {
        logging.debug("Cleaning up tool resources")
        close()

        // @TODO(sgammon): base injection bug
        if (!Statics.args.get().contains("--quiet") && interactive.get()) runBlocking {
          out.pretty(
            {
              line("Exiting session. Have a great day! \uD83D\uDC4B")
            },
            {
              line("Exited session")
            },
          )
        }
      },
    )
  }

  /**
   * Resolve a thread-local [PolyglotContext], acquiring a new one from the [engine] if necessary. That the returned
   * context _must not_ be shared with other threads to avoid exceptions related to concurrent usage.
   *
   * Subclasses should prefer [withContext] as it provides a limited scope in which the context can be used.
   */
  protected fun resolvePolyglotContext(): PolyglotContext {
    logging.debug("Resolving context for current thread")

    // already intialized on the current thread
    contextHandle.get()?.let { cached ->
      logging.debug("Reusing cached context for current thread")
      return cached
    }

    // not initialized yet, acquire a new one and store it
    logging.debug("No cached context found for current thread, acquiring new context")
    return engine.acquire().also { created ->
      contextHandle.set(created)

      // always explicitly enter the context on the current thread
      created.enter()
    }
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
    sharedResources.addAll(initialize())

    // build initial state
    val toolState = state() ?: materializeInitialState()
    val ctx = context(state, coroutineContext)

    @Suppress("UNCHECKED_CAST")
    initializeToolResources(toolState as State) {
      // finally, call the sub-command entrypoint
      ctx.invoke(this)
    }
  }

  /**
   * Run a [block] of code using a [PolyglotContext] configured by the current [engine]. The context is guaranteed to
   * be exclusive to this thread, but it may be reused after this operation completes.
   *
   * The first invocation of this method will cause the [engine] to be initialized, triggering the
   * [configureEngine] event.
   */
  protected open fun withContext(block: (PolyglotContext) -> Unit) {
    logging.debug("Acquiring context for CLI tool")
    with(resolvePolyglotContext()) {
      logging.debug("Context acquired")
      runCatching(block).onFailure { cause ->
        // it's not a known tool error, log it
        if (cause !is AbstractToolError) logging.error(
          "Uncaught exception within VM context. Please catch and handle all VM execution exceptions.",
          cause,
        )

        // always rethrow
        throw cause
      }
    }
  }

  /**
   * ## Output Controller
   *
   * Create a wrapped [OutputController] which can manage receiving and emitting output from the sub-command implemented
   * by this class.
   *
   * @param state Tool state to use for output configuration.
   * @return Wrapped output controller.
   */
  protected open fun outputController(state: ToolState): OutputController =
    DefaultOutputController(state, Statics.logging)

  /**
   * ## Input Controller
   *
   * Create a wrapped [InputController] which can manage receiving input from the user and providing it to the sub-
   * command implemented by this class.
   *
   * @param state Tool state to use for input configuration.
   * @param input Input stream to consume.
   * @param buffer Buffered reader of input.
   * @return Wrapped input controller.
   */
  @Suppress("SameParameterValue")
  protected open fun inputController(state: ToolState, input: InputStream, buffer: BufferedReader): InputController =
    DefaultInputController.DEFAULT

  /**
   * ## Execution Controller
   *
   * Create a wrapped [ExecutionController] which can manage the execution of tasks on behalf of the sub-command
   * implemented by this class.
   *
   * @param state Tool state to use for execution configuration.
   * @param scope Co-routine scope to use for execution.
   * @param context Co-routine context to use for execution.
   * @return Execution controller.
   */
  protected open fun execController(
    state: ToolState,
    scope: CoroutineScope,
    context: CoroutineContext,
  ): ExecutionController {
    // prepare background task service executor
    val taskExecutor = object: ITaskExecutor {
      override fun setThreads(threads: Int) {
        TODO("Not yet implemented")
      }

      override fun getThreads(): Int {
        TODO("Not yet implemented")
      }

      override fun waitForAll() {
        TODO("Not yet implemented")
      }

      override fun run(vararg tasks: Task?) {
        TODO("Not yet implemented")
      }

      override fun run(tasks: MutableList<out Task>?) {
        TODO("Not yet implemented")
      }
    }

    val taskService = TaskServiceBuilder()
      // @TODO(sgammon): customize these defaults
      .animated(true)
      .threads(1)
      .target(System.out)
      .onFailure(IGNORE_FAILURES)
      .executor(taskExecutor)

    return DefaultExecutionController(
      taskExecutor,
      taskService::run,
      dispatcher,
      scope,
      context,
    )
  }

  /**
   * ## VM Initialization
   *
   * Given the provided [base] [State], initialize a VM context; this gives sub-commands a chance to customize a guest
   * VM before it is used.
   *
   * @param base Baseline state/configuration for the VM.
   */
  protected open fun initializeVM(base: State): Boolean = false

  /**
   * ## Initialize Resources
   *
   * Initialize any resources which need to be available for the sub-command implemented by this class; such resources
   * (all [AutoCloseable]) are held by the sub-command for the entire lifecycle of the CI run, and are closed/disposed
   * of afterward automatically.
   *
   * @return List of resources which should be managed on behalf of the sub-command implemented by this class.
   */
  protected open fun initialize(): List<AutoCloseable> = emptyList()

  /**
   * TBD.
   */
  protected open fun state(): State? = null

  /** Configure the [PolyglotEngine] that will be used to acquire contexts used by the [withContext] function. */
  protected open fun PolyglotEngineConfiguration.configureEngine(): Unit = Unit

  /**
   * TBD.
   */
  protected abstract suspend fun CommandContext.invoke(state: ToolContext<State>): CommandResult
}
