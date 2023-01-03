package elide.tool.bundler

import ch.qos.logback.classic.Level
import elide.annotations.Eager
import elide.tool.bundler.cfg.ElideBundlerTool.ELIDE_TOOL_VERSION
import elide.tool.bundler.cmd.inspect.BundleInspectCommand
import elide.tool.bundler.cmd.pack.BundlePackCommand
import elide.tool.bundler.cmd.unpack.BundleUnpackCommand
import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.annotation.ContextConfigurer
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess

/** Entrypoint for the VFS bundler tool. */
@CommandLine.Command(
  name = Bundler.CMD_NAME,
  description = ["Build and manipulate virtual file system bundles for use with Elide"],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
  scope = CommandLine.ScopeType.INHERIT,
  subcommands = [
    BundleInspectCommand::class,
    BundlePackCommand::class,
    BundleUnpackCommand::class,
  ],
)
public class Bundler : Runnable {
  public companion object {
    internal const val CMD_NAME = "bundler"

    /** Run the Bundler CLI. */
    @JvmStatic public fun main(args: Array<String>) {
      exitProcess(exec(args))
    }

    // Private execution entrypoint for customizing core Picocli settings.
    @JvmStatic internal fun exec(args: Array<String>): Int = ApplicationContext.builder().args(*args).start().use {
      CommandLine(Bundler::class.java, MicronautFactory(it))
        .setCommandName(CMD_NAME)
        .setResourceBundle(ResourceBundle.getBundle("BundlerTool"))
        .setAbbreviatedOptionsAllowed(true)
        .setAbbreviatedSubcommandsAllowed(true)
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setEndOfOptionsDelimiter("--")
        .setPosixClusteredShortOptionsAllowed(true)
        .setUsageHelpAutoWidth(true)
        .setColorScheme(CommandLine.Help.defaultColorScheme(if (args.find { arg ->
            arg == "--no-pretty" || arg == "--pretty=false"
          } != null) {
          CommandLine.Help.Ansi.OFF
        } else {
          CommandLine.Help.Ansi.ON
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
  }

  // Tool-wide main logger.
  private val logging = Statics.logging

  // Respond to logging level flags.
  private fun setLoggingLevel(level: Level) {
    ((LoggerFactory.getLogger("ROOT")) as ch.qos.logback.classic.Logger).level = level
  }

  /** Verbose logging mode (wins over `--quiet`). */
  @set:CommandLine.Option(
    names = ["-v", "--verbose"],
    description = ["Activate verbose logging. Wins over `--quiet` when both are passed."],
    scope = CommandLine.ScopeType.INHERIT,
  )
  internal var verbose: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      setLoggingLevel(Level.INFO)
      logging.info("Verbose logging enabled.")
    }
  }

  /** Verbose logging mode. */
  @set:CommandLine.Option(
    names = ["-q", "--quiet"],
    description = ["Squelch most logging"],
    scope = CommandLine.ScopeType.INHERIT,
  )
  internal var quiet: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      setLoggingLevel(Level.OFF)
    }
  }

  /** Debug mode. */
  @set:CommandLine.Option(
    names = ["--debug"],
    description = ["Activate debugging features and extra logging"],
    scope = CommandLine.ScopeType.INHERIT,
  )
  internal var debug: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      logging.trace("Debug mode enabled.")
      setLoggingLevel(Level.TRACE)
    }
  }

  /** Whether to activate pretty logging; on by default. */
  @CommandLine.Option(
    names = ["--pretty"],
    negatable = true,
    description = ["Whether to colorize and animate output."],
    defaultValue = "true",
    scope = CommandLine.ScopeType.INHERIT,
  )
  internal var pretty: Boolean = false

  override fun run() {
    // nothing to do at entry
  }
}
