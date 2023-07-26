package elide.tool.cli

import ch.qos.logback.classic.Level
import elide.annotations.Eager
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.tool.bundler.AbstractBundlerSubcommand
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tool.cli.cmd.bundle.ToolBundleCommand
import elide.tool.cli.cmd.express.ToolExpressCommand
import elide.tool.cli.err.ToolError
import elide.tool.cli.cmd.info.ToolInfoCommand
import elide.tool.cli.cmd.repl.ToolShellCommand
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
import java.util.ResourceBundle
import kotlin.properties.Delegates
import kotlin.system.exitProcess

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
    ToolBundleCommand::class,
    ToolExpressCommand::class,
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
@Singleton public class ElideTool internal constructor () :
  AbstractToolCommand(), AbstractBundlerSubcommand.BundlerParentCommand {
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
      /** @inheritDoc */
      override fun getExitCode(exception: Throwable): Int {
        val exitCode = if (exception is ToolError) {
          exception.exitCode
        } else {
          1
        }
        logging.error("Exiting with code $exitCode due to uncaught $exception")
        return exitCode
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
  override var verbose: Boolean by Delegates.observable(false) { _, _, active ->
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
  override var quiet: Boolean by Delegates.observable(false) { _, _, active ->
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
  override var debug: Boolean by Delegates.observable(false) { _, _, active ->
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
  override var pretty: Boolean = false

  /** Request timeout value to apply. */
  @Option(
    names = ["--timeout"],
    description = ["Timeout to apply to application requests. Expressed in seconds."],
    defaultValue = "30",
    scope = ScopeType.INHERIT,
  )
  internal var timeout: Int = 30

  // Nothing here (an empty tool run cannot occur anyway).
  override fun run() {
    // proxy to the `shell` command for a naked run
    beanContext.getBean(ToolShellCommand::class.java).run()
  }
}
