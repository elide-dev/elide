/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.tool.cli.cmd.tool

import picocli.CommandLine
import java.util.TreeSet
import elide.tool.Arguments
import elide.tool.Environment
import elide.tool.MutableArguments
import elide.tool.MutableEnvironment
import elide.tool.Tool
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.Statics
import elide.tool.cli.ToolState

interface ToolCommandBuilder<T> where T: AbstractTool {
  /** Arguments to pass to the tool. */
  val args: MutableArguments

  /** Environment to set for the tool invocation. */
  val env: MutableEnvironment
}

fun interface ToolCommandConfigurator<T> where T: AbstractTool {
  fun configure(builder: ToolCommandBuilder<T>)
}

private val defaultHelpIndicators = sortedSetOf("--help", "-h", "help")
private val defaultVersionIndicators = sortedSetOf("--version", "-version", "-v")

/**
 * ## Delegated Tool Command
 *
 * A "delegated tool command" is a top-level extension point to the Elide command-line interface, typically mounted at
 * the same name as the tool itself; for example, `elide javac` for the Java compiler.
 *
 * @param T Tool adapter implementation
 */
abstract class DelegatedToolCommand<T> (
  private val info: Tool.CommandLineTool,
  private val configurator: ToolCommandConfigurator<T>? = null,
): AbstractSubcommand<ToolState, CommandContext>() where T: AbstractTool {
  abstract var spec: CommandLine.Model.CommandSpec

  @CommandLine.Option(
    names = ["-help", "--help"],
  )
  var showHelp: Boolean = false

  @CommandLine.Option(
    names = ["-version", "--version"],
  )
  var showVersion: Boolean = false

  /**
   * Prepares an instance of the tool adapter, using the provided [args] and [environment].
   *
   * @param args Arguments to pass to the tool.
   * @param environment Environment to set for the tool invocation.
   * @return A configured instance of the tool adapter.
   */
  abstract fun configure(args: Arguments, environment: Environment): T

  /** @return Arguments which should trigger help text to print. */
  open fun helpIndicators(): TreeSet<String> = defaultHelpIndicators

  /** @return Arguments which should trigger the tool's version to show. */
  open fun versionIndicators(): TreeSet<String> = defaultVersionIndicators

  /** @return Version string for this tool. */
  open fun renderVersion(): StringBuilder = StringBuilder("${info.name} ${info.version}")

  /** @return Version string for this tool. */
  open fun renderHelp(): StringBuilder = StringBuilder().apply {
    append(
      StringBuilder(CommandLine.Help.Ansi.AUTO.string(info.help().toString()))
    )
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    logging.debug { "Invoked delegated tool command; preparing tool state" }
    val args = Statics.args.toList()

    // `--help` and `--version` support
    val isHelp = showHelp || helpIndicators().let { helpFlags ->
      args.find { it in helpFlags } != null
    }
    val isVersion = if (isHelp) false else showVersion || versionIndicators().let { versionFlags ->
      args.find { it in versionFlags } != null
    }
    return when {
      // `--help` and `--version` are mutually exclusive with any other arguments
      isHelp || isVersion -> if (args.size > 2) {
        error("Cannot specify '--help' or '--version' or similar arguments with other options")
      } else when {
        // `--help` is handled on behalf of the embedded tool, and implemented directly
        isHelp -> success().also {
          logging.debug { "Rendering usage for delegated command '${info.name}'" }

          output {
            append(renderHelp())
          }
        }

        else -> success().also {
          logging.debug { "Rendering version for delegated command '${info.name}'" }

          output {
            append(renderVersion())
          }
        }
      }

      // otherwise, we're invoking the tool normally
      else -> {
        logging.debug { "Parsing args for '${info.name}' invocation" }

        var startingPoint = -1
        for (argI in args.indices) {
          val arg = args[argI]
          if (arg == "--") {
            startingPoint = argI + 1
            break
          }
        }
        if (startingPoint < 0) {
          error("Tool command not found: ${info.name}")
        }
        val allToolArgs = args.slice(startingPoint..args.lastIndex)
        val effectiveToolArgs = Arguments.from(allToolArgs).toMutable()
        val effectiveToolEnv = Environment.host().toMutable()

        configurator?.configure(object: ToolCommandBuilder<T> {
          override val args: MutableArguments = effectiveToolArgs
          override val env: MutableEnvironment = effectiveToolEnv
        })
        var errExitCode = 1
        var exitMessage = "Execution of '${info.name}' failed"

        runCatching {
          logging.debug { "Configuring tool '${info.name}'" }

          when (val result = configure(
            args = effectiveToolArgs.build(),
            environment = effectiveToolEnv.build(),
          ).delegateToTool(this@invoke, state)) {
            Tool.Result.Success -> success()
            else -> error("Failed to call tool: $result") // @TODO: delegated error handling
          }
        }.onFailure {
          when (val exc = it) {
            is AbstractTool.EmbeddedToolError -> exc.render(outputController(state.state)).also {
              errExitCode = exc.exitCode
              exc.message?.let { exitMessage = it }
            }
            else -> {
              logging.error("Failed to call tool: ${info.name}", exc)
              errExitCode = 1
              exitMessage = it.message ?: "Unknown error"
            }
          }
        }.getOrElse {
          err(exitMessage, exitCode = errExitCode)
        }
      }
    }
  }
}
