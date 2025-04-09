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

@file:Suppress("MnInjectionPoints", "MaxLineLength", "unused", "NOTHING_TO_INLINE")

package elide.tool.cli

import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.ContextConfigurer
import io.micronaut.core.annotation.Introspected
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.ProcessProperties
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Help
import picocli.CommandLine.Parameters
import picocli.CommandLine.ScopeType
import java.util.*
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
import elide.tool.cli.cmd.pkl.ToolPklCommand
import elide.tool.cli.cmd.project.ToolProjectCommand
import elide.tool.cli.cmd.tool.ToolInvokeCommand
import elide.tool.cli.cmd.repl.ToolShellCommand
import elide.tool.cli.state.CommandState
import elide.tool.engine.NativeEngine
import elide.tool.err.DefaultErrorHandler
import elide.tool.io.RuntimeWorkdirManager

// Default timeout to apply to non-server commands.
private const val DEFAULT_CMD_TIMEOUT = 30

// Pre-initialized application builder.
internal val applicationContextBuilder = ApplicationContext
  .builder()
  .environments("cli", if (ImageInfo.inImageCode()) "native" else "jvm")
  .defaultEnvironments("cli")
  .eagerInitAnnotated(Eager::class.java)
  .eagerInitSingletons(false)
  .eagerInitConfiguration(true)
  .deduceEnvironment(false)
  .deduceCloudEnvironment(false)
  .environmentPropertySource(false)
  .enableDefaultPropertySources(false)
  .bootstrapEnvironment(false)
  .banner(false)

/** Entrypoint for the main Elide command-line tool. */
@Command(
  name = Elide.TOOL_NAME,
  description = ["", "Manage, configure, and run polyglot applications with Elide"],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
  scope = ScopeType.INHERIT,
  subcommands = [
    ToolInfoCommand::class,
    ToolInvokeCommand::class,
    HelpCommand::class,
    ToolShellCommand::class,
    ToolPklCommand::class,
    ToolDiscordCommand::class,
    ToolProjectCommand::class,
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
    "    or:  elide @|bold,fg(yellow) srcfile.<js|ts|jsx|tsx|py...>|@ [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) js|node|deno|@ [OPTIONS] [FILE] [ARG...]",
    "    or:  elide @|bold,fg(cyan) js|node|deno|@ [OPTIONS] [@|bold,fg(cyan) --code|@ CODE]",
    "    or:  elide @|bold,fg(cyan) run|repl|serve|@ [OPTIONS] [FILE] [ARG...]",
    "    or:  elide @|bold,fg(cyan) run|repl|serve|@ [OPTIONS] [@|bold,fg(cyan) --code|@ CODE]",
    "    or:  elide @|bold,fg(cyan) run|repl|@ [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) run|repl|@ --js [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) run|repl|@ --language=[@|bold,fg(green) JS|@] [OPTIONS] [FILE] [ARG...]",
    "    or:  elide @|bold,fg(cyan) run|repl|@ --languages=[@|bold,fg(green) JS|@,@|bold,fg(green) PYTHON|@,...] [OPTIONS] [FILE] [ARG...]",
  ],
)
@Suppress("MemberVisibilityCanBePrivate")
@Introspected
@Context @Singleton class Elide : ToolCommandBase<CommandContext>() {
  companion object {
    /** Name of the tool. */
    const val TOOL_NAME: String = "elide"

    // Whether to log early init messages.
    private const val INIT_LOGGING: Boolean = false

    // Global default locale.
    private val globalLocale = Locale.of("en", "US")

    // Application context; initialized early for CLI use.
    @Volatile private lateinit var applicationContext: ApplicationContext

    private const val PITCH_LINK = "https://elide.dev/pitch"
    private const val PLAYGROUND_LINK = "https://elide.dev/playground"

    private val bundle = ResourceBundle.getBundle("ElideTool", Locale.US)
    private val errHandler = DefaultErrorHandler.acquire()

    // Initialization timestamp.
    private val initTime by lazy { System.currentTimeMillis() }

    // Builder for the CLI entrypoint.
    private val cliBuilder get() = CommandLine(Elide::class.java, object: CommandLine.IFactory {
      override fun <K : Any?> create(cls: Class<K?>?): K? {
        return MicronautFactory(applicationContext).create<K>(cls)
      }
    }).apply {
      setResourceBundle(bundle)
      setExitCodeExceptionMapper(errHandler)
      setCommandName(TOOL_NAME)
      setAbbreviatedOptionsAllowed(true)
      setAbbreviatedSubcommandsAllowed(true)
      setCaseInsensitiveEnumValuesAllowed(true)
      setEndOfOptionsDelimiter("--")
      setPosixClusteredShortOptionsAllowed(true)
      setUsageHelpAutoWidth(true)
    }

    // Whether native libraries have loaded.
    @Volatile private var nativeLibsLoaded: Boolean = false

    // Properties which cannot be set by users.
    private val blocklistedProperties = sortedSetOf(
      "elide.js.vm.enableStreams",
    )

    // Init logging.
    @JvmStatic private fun initLog(message: String) {
      if (INIT_LOGGING) {
        val now = System.currentTimeMillis()
        val delta = now - initTime
        System.err.println("[entry] [${delta}ms] $message")
      }
    }

    @JvmStatic fun requestNatives(server: Boolean = false, tooling: Boolean = false) {
      if (!nativeLibsLoaded) {
        nativeLibsLoaded = true
        initializeNatives(server, tooling)
      }
    }

    @JvmStatic @Synchronized private fun initializeNatives(server: Boolean, tooling: Boolean) {
      // load natives
      initLog("Initializing natives")

      NativeEngine.boot(
        { RuntimeWorkdirManager.acquire() },
        server = server,
        tooling = tooling,
      ) {
        emptyList()
      }.also {
        if (tooling) {
          dev.elide.cli.bridge.CliNativeBridge.initialize()
        }
        initLog("Natives ready")
      }
    }

    @JvmStatic private fun initializeTerminal() {
      if (Statics.disableStreams) {
        return
      }
      assert(!ImageInfo.inImageBuildtimeCode())
      initLog("Initializing ANSI system console")
      org.fusesource.jansi.AnsiConsole.systemInstall()
    }

    @JvmStatic private fun initializeTerminalNative() {
      if (Statics.disableStreams) {
        return
      }
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
    fun installStatics(bin: String, args: Array<String>, cwd: String?) {
      initLog("Initializing statics")
      ProcessManager.initializeStatic(bin, args, cwd ?: "")
      if (ImageInfo.inImageCode()) try {
        ProcessProperties.setArgumentVectorProgramName("elide")
      } catch (_: UnsupportedOperationException) {
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
          ImageInfo.inImageBuildtimeCode() -> initializeTerminalNative()

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

    /** CLI entrypoint and [args]. */
    @JvmStatic fun entry(args: Array<String>): Int {
      initLog("Firing entrypoint")
      return exec(args)
    }

    /** Unwind and clean up shared resources on exit. */
    @JvmStatic @Synchronized fun close() {
      // nothing yet
    }

    /** @return Tool version. */
    @JvmStatic fun version(): String = ELIDE_TOOL_VERSION

    // Private execution entrypoint for customizing core Picocli settings.
    @Suppress("SpreadOperator")
    @JvmStatic internal inline fun exec(args: Array<String>): Int {
      // special case: exit immediately with `0` if `--exit` is provided as the first and only argument.
      if (args.size == 1 && args[0] == "--exit") {
        return 0
      }
      initLog("Preparing context")
      return applicationContextBuilder
        .args(*args)
        .also { initLog("Starting application context") }
        .start()
        .also { initLog("Application context started; loading tool entrypoint") }
        .use {
          Locale.setDefault(globalLocale)
          initLog("Preparing CLI configuration (locale: $globalLocale)")

          applicationContext = it
          cliBuilder.apply {
            setColorScheme(
              Help.defaultColorScheme(
                if (args.find { arg -> arg == "--no-pretty" || arg == "--pretty=false" } != null ||
                  System.getenv("NO_COLOR") != null) {
                  Help.Ansi.OFF
                } else {
                  Help.Ansi.ON
                },
              )
            )
          }.let {
            initLog("Entering application context")

            // special case: print `--help` directly to the statically-assigned out-stream; needed for testing and
            // other monkeypatch-oriented use cases
            if (args.isNotEmpty() && args[0] == "--help") {
              it.usage(Statics.err)
              return 0
            }
            it.execute(*args).also {
              initLog("Finished execution")
            }
          }
        }.also {
          initLog("Exiting application context")
        }
    }

    /** Configures the Micronaut binary. */
    @ContextConfigurer internal class ToolConfigurator: ApplicationContextConfigurer {
      override fun configure(context: ApplicationContextBuilder) {
        context
          .bootstrapEnvironment(false)
          .deduceEnvironment(false)
          .deduceCloudEnvironment(false)
          .banner(false)
          .environments("cli", if (ImageInfo.inImageCode()) "native" else "jvm")
          .defaultEnvironments("cli")
          .eagerInitAnnotated(Eager::class.java)
          .eagerInitConfiguration(true)
          .eagerInitSingletons(false)
          .environmentPropertySource(false)
          .enableDefaultPropertySources(false)
      }
    }
  }

  // Bean context.
  @Inject internal lateinit var beanContext: BeanContext

  @CommandLine.Option(
    names = ["--pitch"],
    hidden = true,
  )
  internal var pitch: Boolean = false

  @CommandLine.Option(
    names = ["--playground"],
    hidden = true,
  )
  internal var playground: Boolean = false

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

  private suspend fun CommandContext.openElidePitch() {
    promptForLink(
      forThing = "the Elide pitch",
      promptMessage = "Open Elide's pitch?",
      redirectTarget = PITCH_LINK,
      force = true,
    )
  }

  private suspend fun CommandContext.openElidePlayground() {
    promptForLink(
      forThing = "the Elide playground",
      promptMessage = "Open the Elide Playground in a codespace?",
      redirectTarget = PITCH_LINK,
    )
  }

  override suspend fun CommandContext.invoke(state: CommandState): CommandResult {
    return when {
      pitch -> CommandResult.success().also { openElidePitch() }
      playground -> CommandResult.success().also { openElidePlayground() }
      else -> if (srcfile == "discord" || Statics.args.firstOrNull() == "discord") {
        beanContext.getBean(ToolDiscordCommand::class.java).let {
          it.call()
          return it.commandResult.get()
        }
      } else {
        // proxy to the `shell` command for a naked run
        val bean = beanContext.getBean(ToolShellCommand::class.java).apply {
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
        }
        bean.call()
        bean.commandResult.get()
      }
    }
  }
}
