/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

@file:Suppress("MnInjectionPoints", "MaxLineLength")

package elide.tool.cli

import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.ContextConfigurer
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.ProcessProperties
import org.slf4j.bridge.SLF4JBridgeHandler
import picocli.CommandLine
import picocli.CommandLine.*
import java.security.Security
import java.util.*
import kotlin.system.exitProcess
import elide.annotations.Context
import elide.annotations.Eager
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.HostPlatform
import elide.runtime.core.HostPlatform.OperatingSystem
import elide.runtime.gvm.internals.ProcessManager
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tool.cli.cmd.discord.ToolDiscordCommand
import elide.tool.cli.cmd.help.HelpCommand
import elide.tool.cli.cmd.info.ToolInfoCommand
import elide.tool.cli.cmd.lint.ToolLintCommand
import elide.tool.cli.cmd.repl.ToolShellCommand
import elide.tool.cli.cmd.selftest.SelfTestCommand
import elide.tool.cli.state.CommandState
import elide.tool.engine.NativeEngine
import elide.tool.err.DefaultErrorHandler
import elide.tool.io.RuntimeWorkdirManager


/** Entrypoint for the main Elide command-line tool. */
@Command(
  name = Elide.TOOL_NAME,
  description = ["", "Manage, configure, and run polyglot applications with Elide"],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
  scope = ScopeType.INHERIT,
  subcommands = [
    ToolInfoCommand::class,
    ToolDiscordCommand::class,
    ToolLintCommand::class,
    HelpCommand::class,
    ToolShellCommand::class,
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
  synopsisHeading = "",
  customSynopsis = [
    "",
    " Usage:  ",
    "    or:  elide @|bold,fg(cyan) info|help|discord|bug...|@ [OPTIONS]",
    "    or:  elide @|bold,fg(yellow) srcfile.<js|py|rb|kt|java|wasm|...>|@ [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) js|kt|jvm|python|ruby|wasm|node|deno|@ [OPTIONS] [FILE] [ARG...]",
    "    or:  elide @|bold,fg(cyan) js|kt|jvm|python|ruby|wasm|node|deno|@ [OPTIONS] [@|bold,fg(cyan) --code|@ CODE]",
    "    or:  elide @|bold,fg(cyan) run|repl|serve|@ [OPTIONS] [FILE] [ARG...]",
    "    or:  elide @|bold,fg(cyan) run|repl|serve|@ [OPTIONS] [@|bold,fg(cyan) --code|@ CODE]",
    "    or:  elide @|bold,fg(cyan) run|repl|@ [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) run|repl|@ --js [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) run|repl|@ --language=[@|bold,fg(green) JS|@] [OPTIONS] [FILE] [ARG...]",
    "    or:  elide @|bold,fg(cyan) run|repl|@ --languages=[@|bold,fg(green) JS|@,@|bold,fg(green) PYTHON|@,...] [OPTIONS] [FILE] [ARG...]",
  ],
)
@Suppress("MemberVisibilityCanBePrivate")
@Context @Singleton class Elide : ToolCommandBase<CommandContext>() {
  companion object {
    /** Name of the tool. */
    const val TOOL_NAME: String = "elide"

    // Maps exceptions to process exit codes.
    private val exceptionMapper = DefaultErrorHandler.acquire()

    // Properties which cannot be set by users.
    private val blocklistedProperties = sortedSetOf(
      "elide.js.vm.enableStreams",
    )

    @JvmStatic private fun initializeNatives() {
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

    @JvmStatic private fun initializeTerminal() {
      assert(!ImageInfo.inImageBuildtimeCode())
      org.fusesource.jansi.AnsiConsole.systemInstall()
    }

    @JvmStatic private fun initializeTerminalNative(win32: Boolean = false) {
      // this trick is necessary to capture the out/err streams set up by `AnsiConsole`, which performs those steps in
      // private code; after the streams are captured, they are uninstalled and instead mounted via the native image
      // I/O stream wrappers, which will delegate to the assigned streams anyway.
      //
      // streams for stdout and stderr are forbidden to be replaced during native image building, and this class must
      // initialize at build time for the compiler to effectively optimize the rest of the runtime; thus, this routine
      // replaces `initializeTerminal` only for build-time execution.
      try {
        org.fusesource.jansi.AnsiConsole.systemInstall().also {
          // capture assigned streams
          Statics.assignStreams(
            out = System.out,
            err = System.err,
            `in` = System.`in`,
          )
        }
      } finally {
        // thou shall not pass
        org.fusesource.jansi.AnsiConsole.systemUninstall()
        org.fusesource.jansi.AnsiConsole.systemUninstall()
        org.fusesource.jansi.AnsiConsole.systemUninstall()
        org.fusesource.jansi.AnsiConsole.systemUninstall()
      }
    }

    // Maybe install terminal support for Windows if it is needed.
    private fun installWindowsTerminalSupport() {
      picocli.jansi.graalvm.AnsiConsole.windowsInstall()
    }

    // Install static classes/perform static initialization.
    private fun installStatics(args: Array<String>, cwd: String?) {
      ProcessManager.initializeStatic(args, cwd ?: "")
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()
      initializeNatives()

      if (ImageInfo.inImageCode()) try {
        ProcessProperties.setArgumentVectorProgramName("elide")
      } catch (uoe: UnsupportedOperationException) {
        // no-op
      }

      // set system-level properties passed via command line (this must be done early)
      val sysPropArgs = args.filter { it.startsWith("-D") }
      sysPropArgs.forEach { arg ->
        val parts = arg.split("=", limit = 2)
        val key = parts.first().substring(2)
        if (blocklistedProperties.contains(key)) {
          throw IllegalArgumentException("Cannot set blocklisted property: $key")
        }
        val value = parts.getOrNull(1) ?: ""
        System.setProperty(key, value)
      }

      // prep runner and exec
      val os = HostPlatform.resolve().os
      val isWindows = os == OperatingSystem.WINDOWS

      val runner = {
        when {
          // build-time code cannot swap the out/err streams, but instead must delegate via the native I/O wrapper.
          ImageInfo.inImageBuildtimeCode() -> initializeTerminalNative(isWindows)

          // otherwise, we can simply swap the streams so that the ANSI terminal takes over.
          !org.fusesource.jansi.AnsiConsole.isInstalled() -> initializeTerminal()
        }
      }

      when {
        // on windows, provide native terminal support fix
        isWindows -> installWindowsTerminalSupport()

        // otherwise, install terminal and begin execution
        else -> runner.invoke()
      }
    }

    private fun cleanup() {
      // no-op
    }

    /** CLI entrypoint and [args]. */
    @JvmStatic fun entry(args: Array<String>) {
      // load and install libraries
      installStatics(args, System.getProperty("user.dir"))

      val exitCode = try {
        exec(args)
      } finally {
        cleanup()
      }
      exitProcess(exitCode)
    }

    /** @return Tool version. */
    @JvmStatic fun version(): String = ELIDE_TOOL_VERSION

    // Private execution entrypoint for customizing core Picocli settings.
    @JvmStatic internal fun exec(args: Array<String>): Int = ApplicationContext
      .builder()
      .eagerInitAnnotated(Eager::class.java)
      .args(*args)
      .start().use {
      CommandLine(Elide::class.java, MicronautFactory(it))
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
       ).execute(*args)
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

  /** Source file shortcut alias. */
  @Parameters(
    index = "0",
    description = ["Source file to run."],
    scope = ScopeType.INHERIT,
    arity = "0..1",
    paramLabel = "FILE",
  )
  internal var srcfile: String? = null

  /** Script file arguments. */
  @Parameters(
    index = "1",
    description = ["Arguments to pass"],
    scope = ScopeType.INHERIT,
    arity = "0..*",
    paramLabel = "ARG",
  )
  internal var args: List<String>? = null

  override suspend fun CommandContext.invoke(state: CommandState): CommandResult {
    // proxy to the `shell` command for a naked run
    return beanContext.getBean(ToolShellCommand::class.java).apply {
      this@Elide.srcfile?.let {
        if (!ToolShellCommand.languageAliasToEngineId.contains(it)) {
          runnable = it
          if (this@Elide.args?.isNotEmpty() == true) {
            arguments = this@Elide.args!!
          }
        } else {
          languageHint = it
        }
      }
      call()
    }.commandResult.get()
  }
}

fun main(args: Array<String>) {
  Elide.entry(args)
}
