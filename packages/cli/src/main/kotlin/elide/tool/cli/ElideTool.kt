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

import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.ContextConfigurer
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.bridge.SLF4JBridgeHandler
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.jansi.graalvm.AnsiConsole
import java.security.Security
import java.util.*
import kotlin.system.exitProcess
import elide.annotations.Context
import elide.annotations.Eager
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.HostPlatform
import elide.runtime.core.HostPlatform.OperatingSystem
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tool.cli.cmd.discord.ToolDiscordCommand
import elide.tool.cli.cmd.help.HelpCommand
import elide.tool.cli.cmd.info.ToolInfoCommand
import elide.tool.cli.cmd.repl.ToolShellCommand
import elide.tool.cli.cmd.selftest.SelfTestCommand
import elide.tool.cli.cmd.update.SelfUpdateCommand
import elide.tool.cli.output.Counter
import elide.tool.cli.output.runJestSample
import elide.tool.cli.state.CommandState
import elide.tool.engine.NativeEngine
import elide.tool.err.DefaultErrorHandler
import elide.tool.io.RuntimeWorkdirManager


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
    HelpCommand::class,
    SelfUpdateCommand::class,
    SelfTestCommand::class,
  ],
  headerHeading = ("@|bold,fg(magenta)%n" +
    "   ______     __         __     _____     ______%n" +
    " /\\  ___\\   /\\ \\       /\\ \\   /\\  __-.  /\\  ___\\%n" +
    " \\ \\  __\\   \\ \\ \\____  \\ \\ \\  \\ \\ \\/\\ \\ \\ \\  __\\%n" +
    "  \\ \\_____\\  \\ \\_____\\  \\ \\_\\  \\ \\____-  \\ \\_____\\%n" +
    "   \\/_____/   \\/_____/   \\/_/   \\/____/   \\/_____/|@%n%n" +
    " @|bold,fg(magenta) " + ELIDE_TOOL_VERSION + "|@%n%n"
  ),
)
@Suppress("MemberVisibilityCanBePrivate")
@Context @Singleton class ElideTool : ToolCommandBase<CommandContext>() {
  companion object {
    init {
//      Security.insertProviderAt(OpenSSLProvider(), 0)
//      Security.insertProviderAt(BouncyCastleProvider(), 1)

      // load natives
      NativeEngine.boot(RuntimeWorkdirManager.acquire()) {
        listOf(
          "elide.js.vm.enableStreams" to "true",
          "io.netty.allocator.maxOrder" to "3",
          "io.netty.serviceThreadPrefix" to "elide-svc",
          "io.netty.native.deleteLibAfterLoading" to "true",  // reversed bc of bug (actually does not delete)
          "io.netty.buffer.bytebuf.checkAccessible" to "false",
          org.fusesource.jansi.AnsiConsole.JANSI_MODE to org.fusesource.jansi.AnsiConsole.JANSI_MODE_FORCE,
          org.fusesource.jansi.AnsiConsole.JANSI_GRACEFUL to "false",
        )
      }
    }

    /** Name of the tool. */
    const val TOOL_NAME: String = "elide"

    // Maps exceptions to process exit codes.
    private val exceptionMapper = DefaultErrorHandler.acquire()

    @JvmStatic private fun initializeTerminal() {
      org.fusesource.jansi.AnsiConsole.systemInstall()
    }

    // Maybe install terminal support for Windows if it is needed.
    private fun installWindowsTerminalSupport(op: () -> Int): Int {
      return AnsiConsole.windowsInstall().use {
        op.invoke()
      }
    }

    // Install static classes/perform static initialization.
    private fun installStatics(op: () -> Int): Int {
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()
      val runner = {
        if (!org.fusesource.jansi.AnsiConsole.isInstalled()) {
          initializeTerminal()
        }
        op.invoke()
      }

      return when {
        // on windows, provide native terminal support fix
        HostPlatform.resolve().os == OperatingSystem.WINDOWS -> installWindowsTerminalSupport(runner)

        // otherwise, install terminal and begin execution
        else -> runner.invoke()
      }
    }

    /** CLI entrypoint and [args]. */
    @JvmStatic fun main(args: Array<String>): Unit = exitProcess(
      installStatics {
        exec(args)
      },
    )

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
        .setColorScheme(
          Help.defaultColorScheme(
            if (args.find { arg -> arg == "--no-pretty" || arg == "--pretty=false" } != null ||
                System.getenv("NO_COLOR") != null) {
              Help.Ansi.OFF
            } else {
              Help.Ansi.ON
            },
          ),
        )
        .execute(*args)
    }

    /** Configures the Micronaut binary. */
    @ContextConfigurer internal class ToolConfigurator: ApplicationContextConfigurer {
      override fun configure(context: ApplicationContextBuilder) {
        context
          .bootstrapEnvironment(false)
          .deduceEnvironment(false)
          .deduceCloudEnvironment(false)
          .banner(false)
          .defaultEnvironments("cli")
          .eagerInitAnnotated(Eager::class.java)
          .eagerInitConfiguration(true)
          .eagerInitSingletons(false)
          .environmentPropertySource(false)
          .enableDefaultPropertySources(false)
          .overrideConfigLocations("classpath:elide.yml")
      }
    }
  }

  // Bean context.
  @Inject internal lateinit var beanContext: BeanContext

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
