package elide.tool.cli

import com.jakewharton.mosaic.MosaicScope
import com.jakewharton.mosaic.runMosaic
import elide.tool.cli.err.AbstractToolError
import elide.tool.cli.state.CommandOptions
import elide.tool.cli.state.CommandState
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Abstract base for all Elide Tool commands, including the root command. */
abstract class AbstractToolCommand<Context>: Callable<Int>, Runnable, CommandApi where Context: CommandContext {
  companion object {
    /** Status of colorized / formatted output support. */
    internal val pretty: AtomicBoolean = AtomicBoolean(true)

    /** Status of debug mode. */
    internal val debug: AtomicBoolean = AtomicBoolean(false)

    /** Status of verbose output mode. */
    internal val verbose: AtomicBoolean = AtomicBoolean(false)

    /** Status of quiet output mode. */
    internal val quiet: AtomicBoolean = AtomicBoolean(false)
  }

  // Initialization state.
  private val initialized: AtomicBoolean = AtomicBoolean(false)

  /** Observed exit code value; defaults to `0`. */
  override val commandResult: AtomicReference<CommandResult> = AtomicReference(CommandResult.Success)

  /**
   * Deferred operation wrapper.
   *
   * Wrap the provided [op] in an asynchronous execution context, returning a [Deferred] value for the return value [R]
   * of the provided [op].
   *
   * @param op Operation to wrap in a deferred context.
   * @return Deferred operation of type [R].
   */
  suspend fun <R> deferred(op: suspend () -> R): Deferred<R> = coroutineScope {
    async {
      op.invoke()
    }
  }

  /**
   * Protect user code operations.
   *
   * Wrap the provided [op] in exception protection which allows through [AbstractToolError] instances, but logs for all
   * other errors as "unknown".
   *
   * @param op Operation to perform.
   */
  protected open suspend fun protect(op: suspend () -> Unit) {
    try {
      op.invoke()
    } catch (err: AbstractToolError) {
      // it's a known tool error. re-throw.
      throw err
    } catch (err: Throwable) {
      Statics.logging.error(
        "Uncaught exception. Please catch and handle all exceptions within the scope of a sub-command",
        err,
      )
      throw err
    }
  }

  /**
   * Execution entrypoint for this command.
   *
   * The provided [op] callable is used to actually run the command's implementation; if a fatal exception occurs, it is
   * interrogated for an exit code, otherwise `0` is provided.
   *
   * @param op Operation which implements this command.
   * @return Exit code.
   */
  private fun execute(op: suspend Context.(CommandState) -> CommandResult): Int = runBlocking {
    val exit = AtomicInteger(0)
    val logging = Statics.logging
    val options = CommandOptions.of(
      args = listOf(),
      debug = debug.get(),
      verbose = verbose.get(),
      quiet = quiet.get(),
      pretty = pretty.get(),
    )
    logging.trace {
      "Resolved options: \n$options"
    }

    logging.trace {
      "Initializing Mosaic scope"
    }

    // build state, context
    val state = CommandState.resolve() ?: CommandState.of(options).register()
    logging.trace {
      "Built command state: \n$state"
    }
    val ctx = context(state)
    logging.trace {
      "Built command context: \n$ctx"
    }

    // invoke and set exit code
    logging.trace {
      "Invoking command implementation"
    }
    op.invoke(ctx, state).let { result ->
      exit.set(when (result) {
        is CommandResult.Success -> 0
        is CommandResult.Error -> result.exitCode
      }.also {
        logging.trace {
          "Command implementation returned exit code: $it"
        }
        commandResult.set(result)
      })
    }

    // return exit code
    exit.get()
  }

  /**
   * Callable entrypoint for this command as a [Callable].
   *
   * @return Exit code from running the command.
   */
  override fun call(): Int {
    return execute {
      invoke(it).also(commandResult::set)  // enter context and invoke
    }
  }

  /**
   * Callable entrypoint for this command as a [Runnable].
   *
   * The return value can be consulted by querying the [commandResult] or [exitCode].
   */
  override fun run() {
    call()  // delegate to `call`
  }

  /**
   * Build execution context for this command implementation.
   *
   * The execution context may be customized on top of the baseline [CommandContext]. Various APIs are provided for
   * resolving configurations, accessing services, and so on, via the context.
   *
   * @return Execution context to use for this command.
   */
  open fun context(state: CommandState): Context {
    @Suppress("UNCHECKED_CAST")
    return CommandContext.default(state) as Context
  }

  /**
   * Start a Mosaic rich output session and run the provided [op].
   *
   * @param op Operation to run.
   */
  suspend inline fun <reified V> startMosaicSession(crossinline op: suspend MosaicScope.() -> V): V {
    val container: AtomicReference<V> = AtomicReference(null)
    runMosaic {
      container.set(op.invoke(this))
    }
    return container.get()
  }

  /**
   * Run the implementation for this command.
   *
   * This method is overridden by sub-classes in order to implement the actual command logic. The receiver [Context] is
   * created by the [context] function, and this entrypoint is called, by the [call] method, by the outer framework.
   *
   * @receiver Execution context for this command.
   * @param state Early command state; specifies
   * @return Command execution result.
   */
  abstract suspend fun Context.invoke(state: CommandState): CommandResult
}
