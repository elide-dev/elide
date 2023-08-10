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

@file:Suppress("MnInjectionPoints")

package elide.tool.cli

import ch.qos.logback.classic.Level
import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.ContextConfigurer
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.jansi.graalvm.AnsiConsole
import picocli.jansi.graalvm.Workaround
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess
import elide.annotations.Eager
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tool.cli.cmd.discord.ToolDiscordCommand
import elide.tool.cli.cmd.info.ToolInfoCommand
import elide.tool.cli.cmd.repl.ToolShellCommand
import elide.tool.cli.err.AbstractToolError
import elide.tool.cli.output.Counter
import elide.tool.cli.output.runJestSample
import elide.tool.cli.state.CommandState

/** Entrypoint for the main Elide command-line tool. */
@Command(
  name = ElideTool.TOOL_NAME,
  description = ["Manage, configure, and spawn Elide applications"],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
  scope = ScopeType.INHERIT,
  subcommands = [
    ToolInfoCommand::class,
    ToolShellCommand::class,
    ToolDiscordCommand::class,
  ],
  headerHeading = ("@|bold,fg(magenta)%n" +
    "   ______     __         __     _____     ______%n" +
    " /\\  ___\\   /\\ \\       /\\ \\   /\\  __-.  /\\  ___\\%n" +
    " \\ \\  __\\   \\ \\ \\____  \\ \\ \\  \\ \\ \\/\\ \\ \\ \\  __\\%n" +
    "  \\ \\_____\\  \\ \\_____\\  \\ \\_\\  \\ \\____-  \\ \\_____\\%n" +
    "   \\/_____/   \\/_____/   \\/_/   \\/____/   \\/_____/|@%n%n" +
    " @|bold,fg(magenta) " + ELIDE_TOOL_VERSION + "|@%n%n"
  )
)
@Suppress("MemberVisibilityCanBePrivate")
@Singleton class ElideTool internal constructor () :
  ToolCommandBase<CommandContext>() {
  companion object {
    init {
      System.setProperty("elide.js.vm.enableStreams", "true")
    }

    /** Name of the tool. */
    const val TOOL_NAME: String = "elide"

    // Maps exceptions to process exit codes.
    private val exceptionMapper = ExceptionMapper()

    // Tool-wide main logger.
    private val logging by lazy {
      Statics.logging
    }

    // Maybe install terminal support for Windows if it is needed.
    private fun installWindowsTerminalSupport(op: () -> Int): Int {
      return if (System.getProperty("os.name") == "Windows") {
        AnsiConsole.windowsInstall().use {
          op.invoke()
        }
      } else {
        op.invoke()
      }
    }

    // Install static classes/perform static initialization.
    private fun installStatics(op: () -> Int) {
      Workaround.enableLibraryLoad()
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()
      exitProcess(installWindowsTerminalSupport {
        op.invoke()
      })
    }

    /** CLI entrypoint and [args]. */
    @JvmStatic fun main(args: Array<String>): Unit = installStatics {
      exec(args)
    }

    /** @return Tool version. */
    @JvmStatic fun version(): String = ELIDE_TOOL_VERSION

    // Private execution entrypoint for customizing core Picocli settings.
    @JvmStatic internal fun exec(args: Array<String>): Int = ApplicationContext.builder().args(*args).start().use {
      Statics.args.set(args.toList())
      CommandLine(ElideTool::class.java, MicronautFactory(it))
        .setCommandName(TOOL_NAME)
        .setResourceBundle(ResourceBundle.getBundle("ElideTool"))
        .setAbbreviatedOptionsAllowed(true)
        .setAbbreviatedSubcommandsAllowed(true)
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setEndOfOptionsDelimiter("--")
        .setExitCodeExceptionMapper(exceptionMapper)
        .setPosixClusteredShortOptionsAllowed(true)
        .setUsageHelpAutoWidth(true)
        .setColorScheme(Help.defaultColorScheme(if (args.find { arg ->
            arg == "--no-pretty" || arg == "--pretty=false"
        } != null) {
          Help.Ansi.OFF
        } else {
          Help.Ansi.ON
        }))
        .execute(*args)
    }

    /** Configures the Micronaut binary. */
    @ContextConfigurer internal class ToolConfigurator: ApplicationContextConfigurer {
      override fun configure(context: ApplicationContextBuilder) {
        context
          .bootstrapEnvironment(false)
          .deduceEnvironment(false)
          .banner(false)
          .defaultEnvironments("cli")
          .eagerInitAnnotated(Eager::class.java)
          .eagerInitConfiguration(true)
          .eagerInitSingletons(true)
          .environmentPropertySource(false)
          .enableDefaultPropertySources(false)
          .overrideConfigLocations("classpath:elide.yml")
      }
    }

    /** Maps exceptions to exit codes. */
    private class ExceptionMapper : IExitCodeExceptionMapper {
        override fun getExitCode(exception: Throwable): Int = when (exception) {
        // user code errors arising from the repl/shell/server
        is AbstractToolError -> {
          val exitCode = exception.exitCode
          val inner = exception.cause ?: exception.exception ?: exception
          logging.error("Execution failed with code $exitCode due to ${inner.message}")
          val out = StringWriter()
          val printer = PrintWriter(out)
          inner.printStackTrace(printer)
          logging.error(StringBuilder().apply {
            append("Stacktrace:\n")
            append(out.toString())
          }.toString())
          exitCode
        }

        else -> {
          logging.error("Exiting with code -1 due to uncaught $exception")
          -1
        }
      }
    }
  }

  // Respond to logging level flags.
  private fun setLoggingLevel(level: Level) {
    ((LoggerFactory.getLogger("ROOT")) as ch.qos.logback.classic.Logger).level = level
  }

  // Bean context.
  @Inject internal lateinit var beanContext: BeanContext

  /** Verbose logging mode (wins over `--quiet`). */
  @set:Option(
    names = ["-v", "--verbose"],
    description = ["Activate verbose logging. Wins over `--quiet` when both are passed."],
    scope = ScopeType.INHERIT,
  )
  var verbose: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      setLoggingLevel(Level.INFO)
      logging.info("Verbose logging enabled.")
    }
  }

  /** Verbose logging mode. */
  @set:Option(
    names = ["-q", "--quiet"],
    description = ["Squelch most logging"],
    scope = ScopeType.INHERIT,
  )
  var quiet: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      setLoggingLevel(Level.OFF)
    }
  }

  /** Debug mode. */
  @set:Option(
    names = ["--debug"],
    description = ["Activate debugging features and extra logging"],
    scope = ScopeType.INHERIT,
  )
  var debug: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      logging.trace("Debug mode enabled.")
      setLoggingLevel(Level.TRACE)
    }
  }

  /** Whether to activate pretty logging; on by default. */
  @Option(
    names = ["--pretty"],
    negatable = true,
    description = ["Whether to colorize and animate output."],
    defaultValue = "true",
    scope = ScopeType.INHERIT,
  )
  var pretty: Boolean = false

  /** Request timeout value to apply. */
  @Option(
    names = ["--timeout"],
    description = ["Timeout to apply to application requests. Expressed in seconds."],
    defaultValue = "30",
    scope = ScopeType.INHERIT,
  )
  internal var timeout: Int = 30

  /** Whether to activate pretty logging; on by default. */
  @Option(
    names = ["--self-test"],
    negatable = true,
    description = ["Run a binary self-test"],
    defaultValue = "false",
    hidden = true,
  )
  var selftest: Boolean = false

  override suspend fun CommandContext.invoke(state: CommandState): CommandResult {
    return if (!selftest) {
      // proxy to the `shell` command for a naked run
      val cmd = beanContext.getBean(ToolShellCommand::class.java)
      cmd.call()
      cmd.commandResult.get()
    } else {
      // run output samples
      output {
        append("Running rich output self-test")
      }
      startMosaicSession {
        Counter()
        runJestSample()
      }
      success()
    }
  }
}
