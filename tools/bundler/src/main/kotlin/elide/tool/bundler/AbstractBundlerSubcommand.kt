@file:Suppress("RedundantVisibilityModifier")

package elide.tool.bundler

import elide.runtime.Logger
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Mixin
import picocli.CommandLine.ParentCommand
import java.io.Closeable
import java.io.File

/**
 * # Bundler: Sub-command
 *
 * Defines a sub-command for the bundler tool. Sub-commands implement the abstract interface defined by this class, and
 * can then seamlessly receive the input parameters they need to operate. This class is also in charge of providing the
 * sub-command implementations with execution, logging, output, and I/O tools.
 */
public abstract class AbstractBundlerSubcommand : Runnable, Closeable, AutoCloseable {
  /** Context in which sub-commands run. */
  interface CommandContext {
    /** Indicate whether debug mode is active. */
    val debug: Boolean

    /** Indicate whether verbose-mode is active. */
    val verbose: Boolean

    /** Indicate whether quiet-mode is active. */
    val quiet: Boolean

    /** File to work with (a bundle). */
    val file: File?

    /** Whether we should wait/consume from `stdin`. */
    val stdin: Boolean

    /** Indicate whether pretty-mode is active. */
    val pretty: Boolean
  }

  /** Interface which bundler parent commands are expected to provide. */
  interface BundlerParentCommand {
    /** Indicate whether debug mode is active. */
    val debug: Boolean

    /** Indicate whether verbose-mode is active. */
    val verbose: Boolean

    /** Indicate whether quiet-mode is active. */
    val quiet: Boolean

    /** Indicate whether pretty-mode is active. */
    val pretty: Boolean
  }

  // Logger for all sub-commands.
  protected val logging: Logger = Statics.logging

  // Top-level `Bundler` instance which is hosting this sub-command action.
  @ParentCommand protected lateinit var top: BundlerParentCommand

  // Common options for all bundle-handling sub-commands.
  @Mixin(name = "Common bundle options") protected lateinit var bundle: CommonBundleOptions

  // Prepare tooling resources and context, and then invoke the `operation`, with the provided `context`.
  private fun prepareAndInvoke(operation: BundlerOperation, context: CommandContext) = use {
    runBlocking {
      operation(context)
    }
  }

  /** Runnable entrypoint for the sub-command. */
  override fun run() = prepareAndInvoke(invoke(), object: CommandContext {
    override val debug: Boolean get() = top.debug
    override val verbose: Boolean get() = top.verbose
    override val quiet: Boolean get() = top.quiet
    override val file: File? get() = bundle.file
    override val stdin: Boolean get() = bundle.stdin
    override val pretty: Boolean get() = top.pretty
  })

  /** Close any command resources. */
  override fun close() {
    // no-op
  }

  /**
   * Sub-command: Operation.
   *
   * Sugar function which defines a [BundlerOperation] from the provided inputs; this function is meant to be called
   * immediately upon entering [invoke] from a sub-command class.
   */
  protected fun operation(op: BundlerOperation): BundlerOperation {
    return op  // no-op for now
  }

  /**
   * Sub-command: Invoke.
   *
   * This method is invoked by the [run] method when a sub-command is run via the CLI. This method's job is to return an
   * operation which can be executed to satisfy the user's command.
   *
   * The resulting function, a [BundlerOperation], is invoked with a [CommandContext] which provides configuration and
   * input parameters to the command. The operation is not executed if the tool is operating in dry-run mode.
   *
   * ## Co-routine context
   *
   * The returned [BundlerOperation] may opt to suspend when performing I/O or other expensive operations. The entire
   * operation is executed in a blocking context. Jobs which perform I/O, compression, or other expensive operations
   * which *are not on the blocking path* should use the worker provided by the [CommandContext] to spawn new
   * co-routines.
   *
   * @return Bundler operation to execute.
   */
  internal abstract fun invoke(): BundlerOperation
}
