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

import com.jakewharton.mosaic.MosaicScope
import elide.runtime.Logger
import elide.tool.cli.state.CommandState
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * # Command Context
 *
 * Describes the structure of application context for executable CLI command code; this context is made available to
 * access flags, options, configurations, clients, and other shared resources. Command context is required for all CLI
 * command implementations.
 *
 * ## Specialized Context
 *
 * Subclasses may extend the [SpecializedCommandContext] marker interface, which is not sealed; this type includes all
 * base defaults defined by the [DefaultCommandContext]. When a command implementation defines a specialized context, it
 * must provide an implementation that acts as a factory for the context; [CommandState] is the type provided at this
 * early stage of initialization.
 *
 * ## Context Features
 *
 * The context provides access to several APIs which enable easy access to shared resources and logic.
 *
 * ## Rendering & Execution
 *
 * The [CommandContext] also constitutes a [MosaicScope], which itself constitutes a [CoroutineScope]; these are the
 * active scopes/contexts for a given command execution. Rendering can safely occur within contexts and within command
 * implementations, as can async/suspending execution.
 *
 * To switch execution contexts, use the built-in `Dispatchers` tools that ship with KotlinX co-routines.
 *
 * ### Rendering with Mosaic
 *
 * APIs provided here for rendering call into Mosaic. Mosaic's own functions can be imported and used, transparently, as
 * well. It's up to you.
 */
sealed interface CommandContext : CoroutineScope {
  companion object {
    /** @return Default command context implementation. */
    @JvmStatic @Suppress("unused_parameter")
    fun default(state: CommandState): CommandContext = object : DefaultCommandContext {
      override val coroutineContext: CoroutineContext get() = TODO("Not yet implemented")
      override val logging: Logger get() = Statics.logging
      override val serverLogging: Logger get() = Statics.serverLogger
      override val accessLogging: Logger get() = Statics.serverLogger
    }
  }

  /**
   * Default command context; provides implementation defaults.
   */
  interface DefaultCommandContext : CommandContext

  /**
   * Specialized command context; extension point for command implementations with custom context structure.
   */
  interface SpecializedCommandContext : DefaultCommandContext

  /**
   * Build a success result for a command execution.
   *
   * @return Successful command execution result.
   */
  fun success(): CommandResult = CommandResult.success()

  /**
   * Build an error result for a command execution.
   *
   * @param message Error message to show.
   * @param exitCode Exit code for the error.
   * @return Error command execution result.
   */
  fun err(message: String? = "An unknown error occurred", exitCode: Int = -1): CommandResult =
    CommandResult.err(exitCode, message)

  // -- Configuration -- //

  /**
   * Main logger which is used for non-primary CLI output. Logging should be used for debug/trace messages, and for more
   * verbose output, info-level logs should be used. Logs are not emitted at all unless `--verbose` is passed.
   */
  val logging: Logger

  /**
   * Server-specific logger, which operates in async mode in order to prevent blocking the application. Server operation
   * logs pass through this logger.
   */
  val serverLogging: Logger

  /**
   * Server-specific access logger, which operates in async mode in order to prevent blocking the application. Server
   * requests pass through this logger.
   */
  val accessLogging: Logger

  /**
   * Emit regular output back to the user; this kind of output is considered primary output.
   *
   * @param out String builder to emit.
   */
  fun output(out: StringBuilder) {
    println(out.toString())
  }

  /**
   * Emit regular output back to the user; this kind of output is considered primary output.
   *
   * @param out String builder to emit.
   */
  suspend fun output(out: suspend StringBuilder.() -> Unit) {
    output(StringBuilder().apply {
      out.invoke(this)
    })
  }
}
