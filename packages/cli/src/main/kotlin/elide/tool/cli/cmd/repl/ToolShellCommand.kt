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

@file:Suppress(
  "JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE",
  "UNUSED_PARAMETER",
)

package elide.tool.cli.cmd.repl

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.core.io.IOUtils
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.management.ExecutionEvent
import org.graalvm.polyglot.management.ExecutionListener
import org.jline.builtins.ConfigurationPath
import org.jline.builtins.SyntaxHighlighter
import org.jline.console.impl.Builtins
import org.jline.console.impl.SystemHighlighter
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.*
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.slf4j.LoggerFactory
import picocli.CommandLine.*
import java.io.*
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import javax.script.ScriptEngineManager
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess
import elide.runtime.core.extensions.attach
import elide.runtime.gvm.internals.GraalVMGuest
import elide.runtime.gvm.internals.IntrinsicsManager
import elide.runtime.intrinsics.server.http.HttpServerAgent
import elide.runtime.plugins.debug.debug
import elide.runtime.plugins.env.EnvConfig.EnvVar
import elide.runtime.plugins.env.EnvConfig.EnvVariableSource.*
import elide.runtime.plugins.env.environment
import elide.runtime.plugins.js.JavaScript
import elide.runtime.plugins.vfs.vfs
import elide.tool.cli.*
import elide.tool.cli.GuestLanguage.*
import elide.tool.cli.cfg.ElideCLITool
import elide.tool.cli.err.ShellError
import elide.tool.cli.options.AccessControlOptions
import elide.tool.cli.options.EngineJavaScriptOptions
import elide.tool.cli.output.JLineLogbackAppender
import elide.tool.extensions.installIntrinsics
import elide.tool.io.WorkdirManager
import elide.tool.project.ProjectInfo
import elide.tool.project.ProjectManager


/** Interactive REPL entrypoint for Elide on the command-line. */
@Command(
  name = "run",
  aliases = ["shell", "r", "s", "serve"],
  description = ["%nRun a polyglot script, server, or interactive shell"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  synopsisHeading = "",
  customSynopsis = [
    "",
    " Usage:  elide @|bold,fg(cyan) run|shell|serve|start|@ [OPTIONS] FILE",
    "    or:  elide @|bold,fg(cyan) run|shell|serve|start|@ [OPTIONS] @|bold,fg(cyan) --stdin|@",
    "    or:  elide @|bold,fg(cyan) run|shell|serve|start|@ [OPTIONS] [@|bold,fg(cyan) -c|@|@|bold,fg(cyan) --code|@ CODE]",
    "    or:  elide @|bold,fg(cyan) run|shell|@ [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) run|shell|@ --js [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) run|shell|@ --languages",
    "    or:  elide @|bold,fg(cyan) run|shell|@ --language=[@|bold,fg(green) JS|@] [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) js|kt|jvm|python|ruby|wasm|node|deno|@ [OPTIONS]",
    "    or:  elide @|bold,fg(cyan) js|kt|jvm|python|ruby|wasm|node|deno|@ [OPTIONS] FILE",
  ],
)
@Singleton internal class ToolShellCommand @Inject constructor (
  private val projectManager: ProjectManager,
  private val workdir: WorkdirManager,
) : AbstractSubcommand<ToolState, CommandContext>() {
  internal companion object {
    private const val TOOL_LOGGER_NAME: String = "tool"
    private const val TOOL_LOGGER_APPENDER: String = "CONSOLE"
    private const val CONFIG_PATH_APP = "/etc/elide"
    private const val CONFIG_PATH_USR = "~/.elide"
    private const val VERSION_INSTRINSIC_NAME = "__Elide_version__"

    private val logging: Logger by lazy {
      Logging.of(ToolShellCommand::class)
    }
  }

  /** [SystemRegistryImpl] that filters for special REPL commands. */
  private class ReplSystemRegistry(
    parser: Parser,
    terminal: Terminal,
    workDir: Supplier<Path>,
    configPath: ConfigurationPath,
  ) : SystemRegistryImpl(parser, terminal, workDir, configPath) {
    override fun isCommandOrScript(command: String): Boolean {
      return command.startsWith("!") || super.isCommandOrScript(command)
    }
  }

  /** Allows selecting a language by name. */
  @Introspected @ReflectiveAccess class LanguageSelector {
    /** Specifies the guest language(s) to support. */
    @Option(
      names = ["--language", "-l"],
      description = ["Specify language by name. Options: \${COMPLETION-CANDIDATES}."],
    )
    internal var language: EnumSet<GuestLanguage>? = null

    /** Flag for a JavaScript VM. */
    @Option(
      names = ["--js", "--javascript", "-js"],
      description = ["Equivalent to passing '--language=JS'."],
    )
    internal var javascript: Boolean = true

    /** Flag for JVM support. */
    @Option(
      names = ["--jvm", "--java", "-java"],
      description = ["Equivalent to passing '--language=JVM'."],
    )
    internal var jvm: Boolean = false

    /** Flag for Kotlin support. */
    @Option(
      names = ["--kotlin", "--kt", "-kt"],
      description = ["Equivalent to passing '--language=KOTLIN'."],
    )
    internal var kotlin: Boolean = jvm

    /** Flag for Ruby support. */
    @Option(
      names = ["--ruby", "--rb", "-rb"],
      description = ["Equivalent to passing '--language=RUBY'."],
    )
    internal var ruby: Boolean = false

    /** Flag for Python support. */
    @Option(
      names = ["--python", "--py", "-py"],
      description = ["Equivalent to passing '--language=PYTHON'."],
    )
    internal var python: Boolean = false

    /** Flag for WebAssembly support. */
    @Option(
      names = ["--wasm"],
      description = ["Equivalent to passing '--language=WASM'."],
    )
    internal var wasm: Boolean = false

    /** Flag for LLVM support. */
    @Option(
      names = ["--llvm"],
      description = ["Equivalent to passing '--language=LLVM'."],
    )
    internal var llvm: Boolean = false

    // Calculated and cached suite of supported languages loaded into the VM space.
    private val langs: EnumSet<GuestLanguage> by lazy {
      when {
        // if we're provided an explicit list, use that
        language != null -> language!!

        // otherwise, use provided flags
        else -> EnumSet.noneOf(GuestLanguage::class.java).apply {
          if (javascript) add(JS)
          if (jvm) add(JVM)
          if (kotlin) add(KOTLIN)
          if (ruby) add(RUBY)
          if (python) add(PYTHON)
          if (wasm) add(WASM)
        }.let { flags ->
          // if we have no languages enabled, use JS, which is the default.
          if (flags.isEmpty()) EnumSet.of(JS) else flags
        }
      }
    }

    // Resolve the primary interactive language.
    internal fun primary(project: ProjectInfo? = null): GuestLanguage =
      langs.first { it.id != GraalVMGuest.JAVASCRIPT.symbol } ?: JS

    // Resolve the specified language.
    internal fun resolve(project: ProjectInfo? = null): EnumSet<GuestLanguage> = langs
  }

  /** Specifies settings for the Chrome DevTools inspector. */
  @Introspected @ReflectiveAccess class InspectorConfig {
    @Option(
      names = ["--inspect"],
      description = ["Whether to enable the Chrome Devtools inspector"],
      defaultValue = "false",
    )
    internal var enabled: Boolean = false

    /** Specifies whether the inspector should suspend immediately at execution start. */
    @Option(
      names = ["--inspect:suspend"],
      description = ["Whether the inspector should suspend execution immediately."],
      defaultValue = "false",
    )
    internal var suspend: Boolean = false

    /** Specifies whether the inspector should suspend for internal (facade) sources. */
    @Option(
      names = ["--inspect:internal"],
      description = ["Specifies whether the inspector should suspend for internal (facade) sources"],
      defaultValue = "false",
      hidden = false,
    )
    internal var `internal`: Boolean = false

    /** Specifies whether the inspector should suspend for internal (facade) sources. */
    @Option(
      names = ["--inspect:wait"],
      description = ["Whether to wait for the inspector to attach before executing any code at all."],
      defaultValue = "false",
    )
    internal var wait: Boolean = false

    /** Specifies the port the inspector should bind to. */
    @Option(
      names = ["--inspect:port"],
      description = ["Set the port the inspector binds to"],
      defaultValue = "4200",
    )
    internal var port: Int = 0

    /** Specifies the host the inspector should bind to. */
    @Option(
      names = ["--inspect:host"],
      description = ["Set the host the inspector binds to"],
      defaultValue = "localhost",
    )
    internal var host: String = ""

    /** Specifies the path the inspector should bind to. */
    @Option(
      names = ["--inspect:path"],
      description = ["Set a custom path for the inspector"],
    )
    internal var path: String? = null

    /** Specifies paths where sources are available. */
    @Option(
      names = ["--inspect:sources"],
      arity = "0..N",
      description = ["Add a source directory to the inspector path. Specify 0-N times."],
    )
    internal var sources: List<String> = emptyList()

    /** Apply these settings to the root engine configuration container. */
    internal fun apply(config: PolyglotEngineConfiguration) {
      if (!enabled) return

      // install and configure the Debug plugin
      config.debug {
        chromeInspector {
          enabled = this@InspectorConfig.enabled

          suspend = this@InspectorConfig.suspend
          internal = this@InspectorConfig.internal
          waitAttached = wait

          host = this@InspectorConfig.host
          port = this@InspectorConfig.port

          path = this@InspectorConfig.path
          sourcePaths = sources
        }
      }
    }
  }

  /** Specifies settings for the Debug Adapter Protocol host. */
  @Introspected @ReflectiveAccess class DebugConfig {
    /** Specifies whether the debugger should suspend immediately at execution start. */
    @Option(
      names = ["--debug:suspend"],
      description = ["Whether the debugger should suspend execution immediately."],
      defaultValue = "false",
    )
    internal var suspend: Boolean = false

    /** Specifies whether the debugger should suspend for internal (facade) sources. */
    @Option(
      names = ["--debug:wait"],
      description = ["Whether to wait for the debugger to attach before executing any code at all."],
      defaultValue = "false",
    )
    internal var wait: Boolean = false

    /** Specifies the port the debugger should bind to. */
    @Option(
      names = ["--debug:port"],
      description = ["Set the port the debugger binds to"],
      defaultValue = "4711",
    )
    internal var port: Int = 0

    /** Specifies the host the debugger should bind to. */
    @Option(
      names = ["--debug:host"],
      description = ["Set the host the debugger binds to"],
      defaultValue = "localhost",
    )
    internal var host: String = ""

    /** Apply these settings to the root engine configuration container. */
    internal fun apply(config: PolyglotEngineConfiguration) {
      // install and configure the Debug plugin
      config.debug {
        debugAdapter {
          suspend = this@DebugConfig.suspend
          waitAttached = wait

          host = this@DebugConfig.host
          port = this@DebugConfig.port
        }
      }
    }
  }

  /** Specifies settings for application environment. */
  @Introspected @ReflectiveAccess class EnvironmentConfig {
    /** Specifies whether the runtime should honor dotenv files. */
    @Option(
      names = ["--env:dotenv"],
      description = ["Whether to honor .env files; defaults to `true`"],
      defaultValue = "true",
    )
    internal var dotenv: Boolean = true

    /** Specifies whether the runtime should honor dotenv files. */
    @Option(
      names = ["--env"],
      description = ["Additional environment variables to set, in x=y format"],
      arity = "0..N",
    )
    internal var envVars: Map<String, String> = emptyMap()

    /** Apply these settings to created execution contexts. */
    @Suppress("KotlinConstantConditions")
    internal fun apply(project: ProjectInfo?, config: PolyglotEngineConfiguration) = config.environment {
      val effectiveInjectedEnv = TreeMap<String, EnvVar>()

      // inject `NODE_ENV`
      effectiveInjectedEnv["NODE_ENV"] = EnvVar.of("NODE_ENV", if (ElideCLITool.ELIDE_RELEASE_TYPE == "DEV") {
        "development"
      } else {
        "production"
      })

      // apply project-level environment variables first (if applicable)
      project?.env?.vars?.forEach {
        if (it.value.isPresent) {
          if (it.value.source == DOTENV && !dotenv) {
            return@forEach  // skip .env vars if so instructed
          }
          effectiveInjectedEnv[it.key] = requireNotNull(it.value)
        }
      }

      // apply manually-installed environment variables
      envVars.forEach {
        if (it.value.isNotBlank() && it.value.isNotBlank()) {
          effectiveInjectedEnv[it.key] = EnvVar.of(it.key, it.value)
        }
      }

      effectiveInjectedEnv.forEach {
        when (it.value.source) {
          INLINE -> environment(it.key, it.value.value)

          HOST -> (it.value as EnvVar.HostMappedVar).let { hostMappedVar ->
            mapToHostEnv(
              hostMappedVar.mapped,
              hostMappedVar.name,
              defaultValue = hostMappedVar.defaultValue,
            )
          }

          DOTENV -> (it.value as EnvVar.DotEnvVar).let { dotEnvVar ->
            fromDotenv(
              dotEnvVar.file,
              it.key,
              it.value.value,
            )
          }
        }
      }
    }
  }

  internal sealed class LanguageSettings

  /** Settings which apply to Python only. */
  @Introspected @ReflectiveAccess class PythonSettings : LanguageSettings() {
    /** Whether to activate Python debug mode. */
    @Option(
      names = ["--py:debug"],
      description = ["Activate Python debug mode"],
      defaultValue = "false",
    )
    internal var debug: Boolean = false
  }

  /** Settings which apply to Ruby only. */
  @Introspected @ReflectiveAccess class RubySettings : LanguageSettings() {
    /** Whether to activate Ruby debug mode. */
    @Option(
      names = ["--rb:debug"],
      description = ["Activate Ruby debug mode"],
      defaultValue = "false",
    )
    internal var debug: Boolean = false
  }

  /** Settings which apply to JVM languages only. */
  @Introspected @ReflectiveAccess class JvmSettings : LanguageSettings() {
    /** Whether to activate JVM debug mode. */
    @Option(
      names = ["--jvm:debug"],
      description = ["Activate JVM debug mode"],
      defaultValue = "false",
    )
    internal var debug: Boolean = false
  }

  /** Settings which apply to Kotlin. */
  @Introspected @ReflectiveAccess class KotlinSettings : LanguageSettings() {
    /** Whether to activate Kotlin debug mode. */
    @Option(
      names = ["--kt:debug"],
      description = ["Activate Kotlin debug mode"],
      defaultValue = "false",
    )
    internal var debug: Boolean = false
  }

  /** Settings which apply to Groovy. */
  @Introspected @ReflectiveAccess class GroovySettings : LanguageSettings() {
    /** Whether to activate Groovy debug mode. */
    @Option(
      names = ["--groovy:debug"],
      description = ["Activate Groovy debug mode"],
      defaultValue = "false",
    )
    internal var debug: Boolean = false
  }

  /** Settings which apply to WASM. */
  @Introspected @ReflectiveAccess class WasmSettings : LanguageSettings() {
    /** Whether to activate WASM debug mode. */
    @Option(
      names = ["--wasm:debug"],
      description = ["Activate WASM debug mode"],
      defaultValue = "false",
    )
    internal var debug: Boolean = false
  }

  // Whether the last-seen command was a user exit.
  private val exitSeen = AtomicBoolean(false)

  // Last-seen statement executed by the VM.
  private val allSeenStatements = LinkedList<String>()

  // Count of statement lines seen; used as an offset for the length of `allSeenStatements`.
  private val statementCounter = AtomicInteger(0)

  // Language-specific syntax highlighter.
  private val langSyntax: AtomicReference<SyntaxHighlighter?> = AtomicReference(null)

  // Main operating terminal.
  private val terminal: AtomicReference<Terminal?> = AtomicReference(null)

  // Active line reader.
  private val lineReader: AtomicReference<LineReader?> = AtomicReference(null)

  // Intrinsics manager
  @Inject internal lateinit var intrinsicsManager: IntrinsicsManager

  // Server manager
  private val serverAgent: HttpServerAgent = HttpServerAgent()

  // Synchronization primitive used to coordinate server behavior
  private val phaser: AtomicReference<Phaser> = AtomicReference(Phaser(1))

  // Whether a server is running.
  private val serverRunning: AtomicBoolean = AtomicBoolean(false)

  // Active project configuration.
  private val activeProject: AtomicReference<ProjectInfo> = AtomicReference(null)

  // Main script engine manager.
  private val scriptEngineManager: ScriptEngineManager by lazy {
    ScriptEngineManager(this::class.java.classLoader)
  }

  /** Specifies the guest language to run. */
  @Option(
    names = ["--languages"],
    description = ["Query supported languages"],
    defaultValue = "false",
  )
  internal var languages: Boolean = false

  /** Specifies that `stdin` should be used to read the script. */
  @Option(
    names = ["--stdin"],
    description = ["Read a script to execute from stdin"],
    defaultValue = "false",
  )
  internal var useStdin: Boolean = false

  /** Specifies that a single statement should be run from the command line. */
  @Option(
    names = ["--code", "-c"],
    description = ["Execute a single statement"],
    defaultValue = "false",
  )
  internal var executeLiteral: Boolean = false

  /** Specifies that a server is started within guest code; equivalent to calling `serve` or variants. */
  @Option(
    names = ["--serve", "-s", "--server"],
    description = ["Start in server mode"],
    defaultValue = "false",
  )
  internal var executeServe: Boolean = false

  /** Specifies the file-system to mount and use for guest VM access. */
  @Option(
    names = ["--filesystem", "-fs"],
    description = ["Mount a virtual filesystem bundle for guest VM use"],
    arity = "0..N",
    paramLabel = "FILE|URI",
  )
  internal var filesystems: List<String> = emptyList()

  /** Host access settings. */
  @ArgGroup(
    validate = false,
    exclusive = false,
    heading = "%nAccess Control:%n",
  ) internal lateinit var accessControl: AccessControlOptions

  /** App environment settings. */
  @ArgGroup(
    exclusive = false,
    heading = "%nEnvironment:%n",
  ) internal var appEnvironment: EnvironmentConfig = EnvironmentConfig()

  /** Chrome inspector settings. */
  @ArgGroup(
    exclusive = false,
    heading = "%nInspector:%n",
  ) internal var inspector: InspectorConfig = InspectorConfig()

  /** DAP host settings. */
  @ArgGroup(
    exclusive = false,
    heading = "%nDebugger:%n",
  ) internal var debugger: DebugConfig = DebugConfig()

  /** Language selector. */
  @ArgGroup(
    exclusive = false,
    heading = "%nLanguage Selection:%n",
  ) internal var language: LanguageSelector? = null

  /** Settings specific to JavaScript. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: JavaScript%n",
  ) internal var jsSettings: EngineJavaScriptOptions = EngineJavaScriptOptions()

  /** Settings specific to Python. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: Python%n",
  ) internal var pythonSettings: PythonSettings = PythonSettings()

  /** Settings specific to Ruby. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: Ruby%n",
  ) internal var rubySettings: RubySettings = RubySettings()

  /** Settings specific to Ruby. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: JVM%n",
  ) internal var jvmSettings: JvmSettings = JvmSettings()

  /** Settings specific to Kotlin. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: Kotlin%n",
  ) internal var kotlinSettings: KotlinSettings = KotlinSettings()

  /** Settings specific to Groovy. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: Groovy%n",
  ) internal var groovySettings: GroovySettings = GroovySettings()

  /** Settings specific to Groovy. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: WASM%n",
  ) internal var wasmSettings: WasmSettings = WasmSettings()

  /** File to run within the VM. */
  @Parameters(
    index = "0",
    arity = "0..1",
    paramLabel = "FILE|CODE",
    description = [
      "File or snippet to run. If `-c|--code` is passed, interpreted " +
              "as a snippet. If not specified, an interactive shell is started.",
    ],
  )
  internal var runnable: String? = null

  // Executed when a guest statement is entered.
  private fun onStatementEnter(event: ExecutionEvent) {
    if (verbose) {
      val highlighter = langSyntax.get()
      val lineReader = lineReader.get()

      val txt: CharSequence? = event.location.characters
      if (!txt.isNullOrBlank()) {
        if (highlighter != null && lineReader != null) {
          lineReader.printAbove(highlighter.highlight(txt.toString()))
        } else {
          println(txt)
        }
      }
    }
  }

  private fun printHighlightedResult(txt: String) {
    val highlighter = langSyntax.get()
    val lineReader = lineReader.get()

    if (!txt.isNullOrBlank()) {
      if (highlighter != null && lineReader != null) {
        lineReader.printAbove(highlighter.highlight(txt))
      } else {
        println(txt)
      }
    }
  }

  // Execute a single chunk of code, or literal statement.
  @Suppress("SameParameterValue") private fun executeOneChunk(
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    ctx: PolyglotContext,
    origin: String,
    code: String,
    interactive: Boolean = false,
    literal: Boolean = false,
  ): Value {
    // fix: python fails in interactive mode without special characters
    val actullyInteractive = if (primaryLanguage.engine == "python") {
      false
    } else interactive

    // build a source code chunk from the line @TODO(sgammon): resolve source from all languages
    val chunk = Source.newBuilder(primaryLanguage.engine, code, origin)
      .interactive(actullyInteractive)
      .internal(false)

    val source = if (literal) {
      chunk.buildLiteral()
    } else {
      chunk.build()
    }

    logging.trace("Code chunk built. Evaluating")
    val result = try {
      ctx.evaluate(source)
    } catch (exc: PolyglotException) {
      if (interactive) {
        throw exc  // don't capture exceptions during interactive sessions; they are handled separately
      }
      when (val throwable = processUserCodeError(primaryLanguage, exc)) {
        null -> {
          logging.trace("Caught exception from code statement ${statementCounter.get()}", exc)
          throw exc
        }

        else -> throw throwable
      }
    }
    logging.trace("Code chunk evaluation complete")
    return result
  }

  // Execute a single chunk of code as a literal statement.
  private fun executeSingleStatement(
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    ctx: PolyglotContext,
    code: String,
  ) {
    if (serveMode()) {
      serverRunning.set(true)
      readExecuteCode(
        "stdin",
        languages,
        primaryLanguage,
        ctx,
        Source.newBuilder(primaryLanguage.symbol, code, "stdin")
          .encoding(StandardCharsets.UTF_8)
          .internal(false)
          .buildLiteral(),
      )
    } else executeOneChunk(
      languages,
      primaryLanguage,
      ctx,
      "stdin",
      code,
      interactive = false,
      literal = false,
    )
  }

  // Build or detect a terminal to use for interactive REPL use.
  private fun buildTerminal(): Terminal {
    return TerminalBuilder.builder()
      .jansi(true)
      .jna(false)  // not supported on M1 (crashes)
      .color(pretty)
      .encoding(StandardCharsets.UTF_8).build().apply {
        val executeThread = Thread.currentThread()
        if (width == 0 || height == 0) {
          size = Size(120, 40)  // hard coded terminal size when redirecting
        }
        handle(Terminal.Signal.INT) {
          executeThread.interrupt()
        }
        terminal.set(this)
      }
  }

  // Build a parser for use by the line reader.
  private fun buildParser(): Parser = DefaultParser().apply {
    escapeChars = null
    isEofOnUnclosedQuote = true
    setRegexVariable(null)
    setEofOnUnclosedBracket(DefaultParser.Bracket.CURLY, DefaultParser.Bracket.ROUND, DefaultParser.Bracket.SQUARE)
    setRegexCommand("[:]{0,1}[a-zA-Z!]{1,}\\S*")  // change default regex to support shell commands
  }

  // Build a command history manager for use by the line reader.
  private fun buildHistory(): History = DefaultHistory()

  // Resolve the current guest language to a named syntax highlighting package.
  private fun syntaxHighlightName(language: GuestLanguage): String = language.label

  // Build a highlighting manager for use by the line reader.
  private fun buildHighlighter(language: GuestLanguage, jnanorc: Path): Pair<SystemHighlighter, SyntaxHighlighter> {
    logging.debug("Building highlighter with config path '$jnanorc'")
    val commandHighlighter = SyntaxHighlighter.build(jnanorc, "COMMAND")
    val argsHighlighter = SyntaxHighlighter.build(jnanorc, "ARGS")
    val langHighlighter = SyntaxHighlighter.build(jnanorc, syntaxHighlightName(language))
    return SystemHighlighter(commandHighlighter, argsHighlighter, langHighlighter) to langHighlighter
  }

  // Build a line reader for interactive REPL use.
  private fun initCLI(
    root: String,
    language: GuestLanguage,
    jnanorcFile: Path?,
    workDir: Supplier<Path>,
    configPath: ConfigurationPath,
    op: LineReader.(SystemRegistryImpl) -> Unit,
  ) {
    val parser = buildParser()
    val terminal = buildTerminal()

    // tweak unsupported commands for native images
    val commands: MutableSet<Builtins.Command> = EnumSet.copyOf(Builtins.Command.entries)
    commands.remove(Builtins.Command.TTOP)
    val builtins = Builtins(commands, workDir, configPath, null)

    val systemRegistry = ReplSystemRegistry(parser, terminal, workDir, configPath)
    systemRegistry.setCommandRegistries(builtins)

    // order matters: initialize these last, because they depend on `ReplSystemRegistry` being registered as a
    // singleton, which happens in `setCommandRegistries`.
    val (highlighter, languageHighlighter) = if (jnanorcFile != null) {
      buildHighlighter(language, jnanorcFile)
    } else {
      SystemHighlighter(null, null, null) to null
    }
    val history = buildHistory()

    val reader = LineReaderBuilder.builder()
      .appName("elide")
      .terminal(terminal)
      .parser(parser)
      .history(history)
      .completer(systemRegistry.completer())
      .highlighter(highlighter)
      .variable(LineReader.HISTORY_FILE, Paths.get(root, "history"))
      .option(LineReader.Option.INSERT_BRACKET, true)
      .option(LineReader.Option.EMPTY_WORD_OPTIONS, false)
      .option(LineReader.Option.AUTO_FRESH_LINE, true)
      .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
      .option(LineReader.Option.CASE_INSENSITIVE, true)
      .option(LineReader.Option.CASE_INSENSITIVE_SEARCH, true)
      .option(LineReader.Option.USE_FORWARD_SLASH, true)
      .option(LineReader.Option.INSERT_TAB, false)
      .build()

    lineReader.set(reader)
    langSyntax.set(languageHighlighter)
    builtins.setLineReader(reader)
    history.attach(reader)
    reader.apply {
      op.invoke(this, systemRegistry)
    }
  }

  // Initialize nano-rc syntax highlighting configurations.
  private fun initNanorcConfig(rootPath: Path, userHome: String): File? {
    if (!rootPath.exists() || Statics.noColor) {
      logging.debug("Syntax highlighting disabled by flags, missing root, or NO_COLOR")
      return null
    }
    val jnanorcDir = rootPath.resolve("nanorc")
    val jnanorcFile = Paths.get(
      userHome,
      "jnanorc",
    ).toFile()

    logging.debug("Checking nanorc root at path '${jnanorcFile.toPath()}'")
    var jnanocDirReady = false
    if (!jnanorcDir.exists()) {
      logging.debug("Nano config directory does not exist. Creating...")

      try {
        File(jnanorcDir.absolutePathString()).mkdirs()
        jnanocDirReady = true
      } catch (e: Exception) {
        logging.debug("Failed to create nanorc directory: ${e.message}")
      }
    } else {
      jnanocDirReady = true
    }
    if (jnanocDirReady) {
      // copy syntax files
      listOf(
        "args.nanorc",
        "command.nanorc",
        "dark.nanorctheme",
        "javascript.nanorc",
        "json.nanorc",
        "kotlin.nanorc",
        "light.nanorctheme",
        "nanorctheme.template",
        "python.nanorc",
        "ruby.nanorc",
        "java.nanorc",
        "ts.nanorc",
      ).parallelStream().forEach {
        val target = jnanorcDir.resolve(it)
        logging.debug("- Initializing syntax file '$it' ($target)")
        if (target.exists()) {
          logging.debug("Syntax file '$it' already exists. Skipping...")
          return@forEach
        }
        val fileStream = ToolShellCommand::class.java.getResourceAsStream("/nanorc/$it") ?: return@forEach
        val contents = IOUtils.readText(fileStream.bufferedReader(StandardCharsets.UTF_8))

        try {
          File(target.absolutePathString()).bufferedWriter(StandardCharsets.UTF_8).use { outbuf ->
            outbuf.write(contents)
          }
        } catch (ioe: IOException) {
          logging.debug("Failed to write nanorc config file.", ioe)
        }
      }
    }

    logging.debug("Checking syntax config at path '${jnanorcFile.toPath()}'")
    if (!jnanorcFile.exists()) {
      logging.debug("Syntax config does not exist. Writing...")
      FileWriter(jnanorcFile).use { fw ->
        fw.write(
          """
          theme ${rootPath.absolutePathString()}/nanorc/dark.nanorctheme
          """.trimIndent(),
        )
        fw.write("\n")
        fw.write(
          """
          include ${rootPath.absolutePathString()}/nanorc/*.nanorc
          """.trimIndent(),
        )
        fw.write("\n")
      }
    }
    return jnanorcFile
  }

  // Redirect logging calls to JLine for output.
  private fun redirectLoggingToJLine(lineReader: LineReader) {
    val rootLogger = LoggerFactory.getLogger(TOOL_LOGGER_NAME) as ch.qos.logback.classic.Logger
    val current = rootLogger.getAppender(TOOL_LOGGER_APPENDER) as ConsoleAppender<ILoggingEvent>
    val ctx = current.context
    val appender = JLineLogbackAppender(ctx, lineReader)
    rootLogger.detachAndStopAllAppenders()
    rootLogger.addAppender(appender)
    appender.start()
  }

  // Given an error location (in interactive mode), fetch the source, plus `contextLines` on each side of the error.
  private fun errorContextLines(exc: PolyglotException, contextLines: Int = 1): Pair<Int, List<String>> {
    val startLine = maxOf(0, (exc.sourceLocation?.startLine ?: 0) - contextLines)
    val endLine = exc.sourceLocation?.endLine ?: 0
    val errorBase = minOf(0, startLine)

    // bail: no lines to show
    if (startLine == 0 && endLine == 0) return errorBase to emptyList()

    // otherwise, calculate lines
    val totalLines = endLine - startLine + (contextLines * 2)
    val ctxLines = ArrayList<String>(totalLines)
    val baseOnHand = minOf(0, statementCounter.get())
    val errorTail = errorBase + (exc.sourceLocation.endLine - exc.sourceLocation.startLine)
    val topLine = maxOf(statementCounter.get(), errorTail)

    return when {
      // cannot resolve: we don't have those lines (they are too early for our buffer)
      errorBase < baseOnHand -> -1 to emptyList()

      // otherwise, resolve from seen statements
      else -> {
        ctxLines.addAll(allSeenStatements.subList(errorBase, minOf(topLine, allSeenStatements.size)))
        (errorBase + 1) to ctxLines
      }
    }
  }

  // Given an error, render a table explaining the error, along with any source context if we have it.
  private fun displayFormattedError(
    exc: Throwable,
    message: String,
    advice: String? = null,
    internal: Boolean = false,
    stacktrace: Boolean = internal,
  ) {
    val term = terminal.get()
    val reader = lineReader.get()
    if (exc !is PolyglotException) return

    // begin calculating with source context
    val middlePrefix = "║ "
    val errPrefix = "→ "
    val stopPrefix = "✗ "
    val lineContextTemplate = "$middlePrefix%lineNum┊ %lineText"
    val errContextPrefix = "$errPrefix%lineNum┊ %lineText"
    val stopContextPrefix = "$stopPrefix%lineNum┊ %lineText"

    val (startingLineNumber, errorContext) = errorContextLines(exc)
    val startLine = exc.sourceLocation?.startLine ?: 0
    val endLine = exc.sourceLocation?.endLine ?: 0
    val lineDigits = endLine.toString().length

    val errRange = startLine..endLine
    val lineContextRendered = errorContext.mapIndexed { index, line ->
      val lineNumber = startingLineNumber + index

      (if (lineNumber in errRange) {
        // it's an error line
        errContextPrefix
      } else if (index == errorContext.lastIndex) {
        stopContextPrefix
      } else {
        // it's a context line
        lineContextTemplate
      }).replace("%lineNum", lineNumber.toString().padStart(lineDigits + 1, ' '))
        .replace("%lineText", line)
    }

    // if requested, build a stacktrace for this error
    val stacktraceContent = if (stacktrace) {
      val stackString = StringWriter()
      val stackPrinter = PrintWriter(stackString)
      exc.printStackTrace(stackPrinter)
      stackPrinter.flush()
      stackString.toString()
    } else {
      ""
    }
    val stacktraceLines = if (stacktrace) {
      stacktraceContent.lines()
    } else {
      emptyList()
    }

    val pad = 2 * 2
    val maxErrLineSize = if (lineContextRendered.isNotEmpty()) lineContextRendered.maxOf { it.length } + pad else 0

    // calculate the maximum width needed to display the error box, but don't exceed the width of the terminal.
    val width = minOf(
      term?.width ?: 120,
      maxOf(
        // message plus padding
        message.length + pad,

        // error context lines
        maxErrLineSize,

        // advice
        (advice?.length ?: 0) + pad,

      // stacktrace
      if (stacktrace) stacktraceLines.maxOf { it.length + pad + 2 } else 0,
    ))

    val textWidth = width - (pad / 2) + if (stacktrace) {
      "      ".length
    } else 0

    val top = ("╔" + "═".repeat(textWidth) + "╗")
    val bottom = ("╚" + "═".repeat(textWidth) + "╝")
    val divider = ("╟" + "─".repeat(textWidth) + "╢")
    val blankLine = ("║" + " ".repeat(textWidth) + "║")

    // render error string
    val rendered = StringBuilder().apply {
      append("\n")
      appendLine(top)
      // ╔══════════════════════╗
      // ║ 1┊ (code)            ║
      // → 2┊ (err)             ║
      lineContextRendered.forEach {
        appendLine(it.padEnd(textWidth + 1, ' ') + "║")
      }
      // ╟──^───────────────────╢
      if (lineContextRendered.isNotEmpty()) appendLine(divider)

      if (message.isNotBlank()) {
        // ║ SomeError: A message ║
        append(middlePrefix).append(message.padEnd(textWidth - 1, ' ')).append("║\n")
      }

      if (stacktrace || advice?.isNotBlank() == true) {
        // ╟──────────────────────╢
        if (lineContextRendered.isNotEmpty() && message.isNotBlank()) appendLine(divider)
        else if (message.isNotBlank()) appendLine(divider)

        // append advice next
        if (advice?.isNotBlank() == true) {
          // ║ Advice:              ║
          append(middlePrefix).append("Advice:".padEnd(textWidth - 1, ' ') + "║\n")
          // ║ Example advice.      ║
          append(middlePrefix).append(advice.padEnd(textWidth - 1, ' ') + "║\n")
          // ╟──────────────────────╢
          if (stacktrace) appendLine(divider)
        }

        // append stacktrace next
        if (stacktrace) {
          // ║ Stacktrace:          ║
          append(middlePrefix).append("Stacktrace:".padEnd(textWidth - 1, ' ') + "║\n")
          appendLine(blankLine)

          // ║ ...                  ║
          stacktraceLines.forEach {
            if (it.startsWith(" ") || it.startsWith('\t')) {
              // if it's a spaced line, don't add additional end-spacing
              append(middlePrefix).append(it.padEnd(textWidth - pad - middlePrefix.length, ' ') + "║\n")
            } else {
              append(middlePrefix).append(it.padEnd(textWidth - 1, ' ') + "║\n")
            }
          }
        }
      }

      appendLine(bottom)
      // ╚══════════════════════╝
      append("\n")
    }.toString()


    if (reader != null) {
      // format error string
      val style = AttributedStyle.BOLD.foreground(AttributedStyle.RED)
      if (pretty) {
        val formatted = AttributedString(rendered, style)
        reader.printAbove(formatted)
      } else {
        reader.printAbove(rendered)
      }
    } else System.err.use {
      it.writeBytes(rendered.toByteArray(StandardCharsets.UTF_8))
      it.flush()
    }
  }

  // Handle an error which occurred within guest code.
  private fun processUserCodeError(language: GuestLanguage, exc: PolyglotException): Throwable? {
    when {
      exc.isSyntaxError -> displayFormattedError(
        exc,
        exc.message ?: "Syntax error",
        "Check ${language.label} syntax",
      )

      exc.isIncompleteSource -> displayFormattedError(
        exc,
        exc.message ?: "Syntax error",
        "${language.label} syntax is incomplete",
      )

      exc.isHostException || exc.message?.contains("HostException: ") == true -> displayFormattedError(
        exc,
        exc.message ?: "An runtime error was thrown",
        advice = "This is an error in Elide. Please report this to the Elide Team with `elide bug`",
        stacktrace = true,
        internal = true,
      )

      exc.isGuestException -> displayFormattedError(
        exc,
        exc.message ?: "An error was thrown",
        stacktrace = !interactive.get(),
      )

      // @TODO(sgammon): interrupts
      exc.isInterrupted -> {}

      // @TODO(sgammon): resource exhaustion
      exc.isResourceExhausted -> {}

      exc.isInternalError -> displayFormattedError(
        exc,
        exc.message ?: "An error was thrown",
        internal = true,
      )
    }
    // in interactive sessions, return `null` if the error is non-fatal; this tells the outer execution loop to ignore
    // the exception (since it has been printed to the user), and continue with the interactive session.
    return if (interactive.get()) {
      null
    } else {
      ShellError.USER_CODE_ERROR.raise(exc)
    }
  }

  private fun showValue(value: Value) {
    printHighlightedResult(value.toString())
  }

  // Wrap an interactive REPL session in exit protection.
  private fun beginInteractiveSession(
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    engine: PolyglotEngine,
    ctx: PolyglotContext,
  ) {
    // resolve working directory
    val workDir = Supplier { Paths.get(System.getProperty("user.dir")) }
    val userHome = Paths.get(System.getProperty("user.home")).absolutePathString()
    val userConfigPath = workdir.configRoot().toPath()
    val root = userConfigPath.absolutePathString()
    val rootPath = Path(root)

    // calculate config path
    val configPath = ConfigurationPath(
      Paths.get(CONFIG_PATH_APP),
      userConfigPath,
    )

    // initialize syntax highlighting configurations
    val jnanorcFile = initNanorcConfig(rootPath, userHome)

    // execution step listener
    ExecutionListener.newBuilder().onEnter(::onStatementEnter).statements(true).attach(engine).use {
      initCLI(root, primaryLanguage, jnanorcFile?.toPath(), workDir, configPath) { registry ->
        // before beginning execution loop, redirect logging to a new appender, which will add logs *above* the user
        // input section of an interactive session.
        redirectLoggingToJLine(this)

        while (true) {
          try {
            // clean up from potential previous execution
            registry.cleanUp()

            // read line and execute
            val line = readLine("elide (${primaryLanguage.id})> ") ?: break  // exit on empty line
            logging.debug("Source line received; executing as statement")
            logging.trace { "Code provided: '$line'" }

            // build a source code chunk from the line
            val result = executeOneChunk(
              languages,
              primaryLanguage,
              ctx,
              "<shell:${primaryLanguage.symbol}>",
              line,
              interactive = true,
              literal = true,
            )

            // only add to statement counter and index if we didn't experience an error
            allSeenStatements.add(line)
            val statement = statementCounter.incrementAndGet()
            logging.trace { "Statement counter: $statement" }
            if (statement > 1000L) {
              allSeenStatements.drop(1)  // drop older lines at the 1000-statement mark
            }

            // if the user had broken with ctrl+c before, they've now processed code, so we should reset the `exitSeen`
            // cookie state for the next exit opportunity.
            if (exitSeen.get()) exitSeen.set(false)

            // if we are running in modes which show the result, print it
            if (!quiet && primaryLanguage.id != RUBY.id) showValue(result)

          } catch (exc: NoSuchElementException) {
            logging.debug("User expressed no input: exiting.")
            break
          } catch (exc: PolyglotException) {
            logging.trace("Caught polyglot exception", exc)
            if (exc.isExit) {
              logging.debug("Exception received is exit: finishing interactive session")
              break  // exit on guest exit
            } else {
              logging.debug("Exception received is not exit: printing stack trace")
              when (val throwable = processUserCodeError(primaryLanguage, exc)) {
                null -> continue
                else -> throw throwable
              }
            }
          } catch (userInterrupt: UserInterruptException) {
            if (exitSeen.get()) {
              break  // we've already seen the exit, so we need to break
            } else try {
              exitSeen.compareAndExchange(false, true)
              println("ctrl+c caught; do it again to exit")
              continue
            } catch (ioe: Exception) {
              break  // just in case
            }
          } catch (eof: EndOfFileException) {
            continue
          } catch (interrupt: InterruptedException) {
            println("Interrupted (sys)")
            logging.debug("Session interrupted; concluding")
            break  // exit on interrupt
          }
        }
      }
    }
  }

  // Read an executable script file and return it as a `File` and a `Source`.
  private fun readExecutableScript(languages: EnumSet<GuestLanguage>, language: GuestLanguage, script: File): Source {
    logging.debug("Reading executable user script at path '${script.path}' (language: ${language.id})")
    if (!script.exists()) {
      logging.debug("Script file does not exist")
      throw ShellError.FILE_NOT_FOUND.asError()
    } else {
      logging.trace("Script check: File exists")
    }
    if (!script.canRead()) {
      logging.debug("Script file cannot be read")
      throw ShellError.FILE_NOT_READABLE.asError()
    } else {
      logging.trace("Script check: File is readable")
    }
    if (!script.isFile) {
      logging.debug("Script file is not a regular file")
      throw ShellError.NOT_A_FILE.asError()
    } else {
      logging.trace("Script check: File is a regular file")
    }

    // type check: first, check file extension
    val allowedMimeTypes = languages.flatMap { it.mimeTypes }.toSortedSet()
    val allowedExtensions = languages.flatMap { it.extensions }.toSortedSet()
    val openMimeMode = languages.any { it.mimeTypes.isEmpty() }

    // @TODO(sgammon): less searching here
    val targetLang = if (!allowedExtensions.contains(script.extension)) {
      logging.debug("Script fail: Extension is not present in language allowed extensions")

      val contentType = Files.probeContentType(Path(script.path))
      if (!openMimeMode && !allowedMimeTypes.contains(contentType)) {
        logging.debug("Script fallback fail: Mime type is also not in allowed set for language. Rejecting.")
        throw ShellError.FILE_TYPE_MISMATCH.asError()
      } else {
        logging.trace("Script check: File mimetype matches")
      }
      languages.find {
        it.mimeTypes.contains(contentType)
      }
    } else {
      logging.trace("Script check: File extension matches")
      languages.find {
        it.extensions.contains(script.extension)
      }
    }
    requireNotNull(targetLang) {
      "Failed to resolve target language for source file"
    }

    return Source.newBuilder(targetLang.symbol, script)
      .encoding(StandardCharsets.UTF_8)
      .internal(false)
      .build()
  }

  private val commandSpecifiesServer: Boolean by lazy {
    Statics.args.get().let {
      it.contains("serve") || it.contains("start") || it.contains("node")
    }
  }

  // Detect whether we are running in `serve` mode (with alias `start`).
  private fun serveMode(): Boolean = commandSpecifiesServer || executeServe

  // Read an executable script, and then execute the script and keep it started as a server.
  private fun readStartServer(label: String, language: GuestLanguage, ctx: PolyglotContext, source: Source) {
    try {
      // enter VM context
      logging.trace("Entered VM for server application (language: ${language.id}). Consuming script from: '$label'")

      // initialize the server intrinsic and run using the provided source
      serverAgent.run(entrypoint = source, acquireContext = ::resolvePolyglotContext)
      phaser.get().register()

      serverRunning.set(true)
    } catch (exc: PolyglotException) {
      processUserCodeError(language, exc)?.let { throw it }
    }
  }

  // Read an executable script, and then execute the script; if it's a server, delegate to `readStartServer`.
  private fun readExecuteCode(
    label: String,
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    ctx: PolyglotContext,
    source: Source,
  ) {
    if (serveMode()) readStartServer(
      label,
      primaryLanguage,
      ctx,
      source,
    ) else try {
      // enter VM context
      logging.trace("Entered VM for script execution (language: ${primaryLanguage.id}). Consuming script from: '$label'")

      // parse the source
      val parsed = try {
        ctx.parse(source)
      } catch (exc: Exception) {
        logging.error("Failed to parse user script $label", exc)
        throw exc
      }

      // check that it is executable
      if (!parsed.canExecute()) {
        logging.error("Failed to execute user script $label: not executable")
        return
      } else {
        logging.trace("Script is executable. Here goes nothing")
      }

      // execute the script
      parsed.execute()

    } catch (exc: PolyglotException) {
      when (val throwable = processUserCodeError(primaryLanguage, exc)) {
        null -> {}
        else -> throw throwable
      }
    }
  }

  // Make sure the file-system bundle specified by `bundleSpec` is usable.
  private fun checkFsBundle(bundleSpec: String?): URI? = when {
    bundleSpec.isNullOrBlank() -> null
    bundleSpec.startsWith("classpath:") -> URI.create(bundleSpec)
    else -> File(bundleSpec).toURI()
  }

  // Resolve the primary interactive language for the provided `file`.
  private fun primaryFromFile(file: File): GuestLanguage? {
    return when (val engine = scriptEngineManager.getEngineByExtension(file.extension)) {
      null -> null
      else -> TODO("")
    }
  }

  // Resolve the default language to use when interpreting a given `fileInput`, or use a sensible fallback from the set
  // of supported languages. If JS is disabled, there can only be one language; otherwise the default language is JS. If
  // a file is provided with a specific matching file extension for a given language, that language is used.
  private fun resolvePrimaryLanguage(
    project: ProjectInfo?,
    languageSelector: LanguageSelector?,
    languages: EnumSet<GuestLanguage>,
    fileInput: File?,
  ): GuestLanguage = when {
    // if we have a file input, the extension for the file takes next precedence
    fileInput != null && fileInput.exists() -> {
      val ext = fileInput.extension
      languages.find { it.extensions.contains(ext) } ?: primaryFromFile(fileInput) ?: JS
    }

    // if there is only one language, that's the result
    languages.size == 1 -> languages.first()

    // an explicit flag from a user takes top precedence
    languageSelector != null -> languageSelector.primary(project)

    // we have to have at least one language
    languages.size == 0 -> error("Cannot start VM with no enabled guest languages")

    // otherwise, if JS is included in the set of languages, that is the default.
    else -> if (languages.contains(JS)) JS else languages.first()
  }

  override fun PolyglotEngineConfiguration.configureEngine() {
    // grab project configurations, if available
    val project = activeProject.get()
    if (project != null) logging.debug("Resolved project info: $project")

    // conditionally apply debugging settings
    if (debug) debugger.apply(this)
    inspector.apply(this)

    // configure host access rules
    hostAccess = when {
      accessControl.allowAll -> HostAccess.ALLOW_ALL
      accessControl.allowIo -> HostAccess.ALLOW_IO
      accessControl.allowEnv -> HostAccess.ALLOW_ENV
      else -> HostAccess.ALLOW_NONE
    }

    // configure environment variables
    appEnvironment.apply(project, this)

    // configure VFS with user-specified bundles
    vfs {
      // resolve the file-system bundles to use
      val userBundles = filesystems.mapNotNull { checkFsBundle(it) }
      if (userBundles.isNotEmpty() && logging.isEnabled(LogLevel.DEBUG)) {
        logging.debug("File-system bundle(s) specified: ${userBundles.joinToString(",")}")
      } else {
        logging.debug("No file-system bundles specified")
      }

      userBundles.forEach { uri ->
        // check the bundle URI
        if (uri.scheme == "classpath:") {
          logging.warn("Rejecting `classpath:`-prefixed bundle: not supported by CLI")
          throw ShellError.BUNDLE_NOT_FOUND.asError()
        } else {
          // make sure the file can be read
          val file = try {
            logging.trace("Checking bundle at URI '$uri'")
            File(uri)
          } catch (err: IOException) {
            throw ShellError.BUNDLE_NOT_FOUND.asError()
          }

          logging.trace("Checking existence of '$uri'")
          if (!file.exists()) throw ShellError.BUNDLE_NOT_FOUND.asError()

          logging.trace("Checking readability of '$uri'")
          if (!file.canRead()) throw ShellError.BUNDLE_NOT_ALLOWED.asError()

          logging.debug("Mounting guest filesystem at URI: '$uri'")
          include(uri)
        }
      }
    }

    // configure support for guest languages
    val versionProp = VERSION_INSTRINSIC_NAME to ElideTool.version()
    val intrinsics = intrinsicsManager.resolver()

    install(JavaScript) {
      logging.debug("Configuring JS VM")
      installIntrinsics(intrinsics, GraalVMGuest.JAVASCRIPT, versionProp)
      jsSettings.apply(this)
    }

    install(elide.runtime.plugins.python.Python) {
      logging.debug("Configuring Python VM")
      installIntrinsics(intrinsics, GraalVMGuest.PYTHON, versionProp)
    }
    install(elide.runtime.plugins.ruby.Ruby) {
      logging.debug("Configuring Ruby VM")
      installIntrinsics(intrinsics, GraalVMGuest.RUBY, versionProp)
    }

    (language ?: LanguageSelector()).resolve().forEach { lang ->
      when (lang) {
        // Secondary Engines: JVM
        JVM -> {
          install(elide.runtime.plugins.jvm.Jvm) {
            logging.debug("Configuring JVM")
            multithreading = false
          }

          install(elide.runtime.plugins.java.Java) {
            logging.debug("Configuring Java")
          }
        }

        GROOVY -> logging.warn("Groovy runtime plugin is not yet implemented")

        KOTLIN -> install(elide.runtime.plugins.kotlin.Kotlin) {
          val classpathDir = workdir.cacheDirectory()
            .resolve("elide-kotlin-runtime")
            .absolutePath

          logging.debug("Configuring Kotlin with classpath root $classpathDir")
          guestClasspathRoot = classpathDir
        }

        SCALA -> logging.warn("Scala runtime plugin is not yet implemented")
        else -> {}
      }
    }
  }

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    logging.debug("Shell/run command invoked")

    // resolve project configuration
    val projectConfigJob = projectManager.resolveProjectAsync()

    // begin resolving language support
    val supported = determineSupportedLanguages()
    val allSupported = EnumSet.copyOf(supported.map { it.first })
    if (languages) {
      logging.debug("User asked for a list of supported languages. Printing and exiting.")
      out.line("Supported languages: ${allSupported.joinToString(", ")}")
      return success()
    }

    // resolve the language to use
    val project = projectConfigJob.await()
    val selector = language ?: LanguageSelector()
    val langs = selector.resolve(project)
    logging.trace("All supported languages: ${allSupported.joinToString(", ") { it.id }}")
    supported.find { langs.contains(it.first) }?.second ?: throw ShellError.LANGUAGE_NOT_SUPPORTED.asError()
    logging.debug("Initializing language contexts (${langs.joinToString(", ") { it.id }})")

    if (!executeLiteral && !useStdin && runnable == null) {
      // if no script inputs are specified, we are entering an interactive session. in this case, we should notify the
      // JS engine, so that it can apply relevant options.
      System.setProperty("vm.interactive", "true")
    }
    val experimentalLangs = langs.filter {
      it.experimental && !it.suppressExperimentalWarning
    }
    val primaryLang: (File?) -> GuestLanguage = { target ->
      resolvePrimaryLanguage(
        project,
        language,
        langs,
        when {
          // can't guess the language of a script from `stdin`
          useStdin || executeLiteral -> null

          // in `serve` mode, or with a `runnable`, we return `runnable` (script is required)
          else -> target
        },
      )
    }

    // apply project configurations to context, if needed
    project?.let { prj ->
      activeProject.set(prj)
    }

    withContext {
      // activate interactive behavior
      interactive.compareAndSet(false, true)

      // warn about experimental status, as applicable
      if (experimentalLangs.isNotEmpty()) {
        logging.warn(
          "Caution: Support for ${experimentalLangs.joinToString(", ") { i -> i.formalName }} " +
                  "considered experimental.",
        )
      }

      when (val scriptTargetOrCode = runnable) {
        // run in interactive mode
        null, "-" -> if (useStdin || runnable == "-") {
          // consume from stdin
          primaryLang.invoke(null).let { lang ->
            input.buffer.use { buffer ->
              readExecuteCode(
                "from stdin",
                langs,
                lang,
                it,
                Source.create(lang.symbol, buffer.readText()),
              )
            }
          }
        } else if (!serveMode()) {
          logging.debug("Beginning interactive guest session")
          beginInteractiveSession(
            langs,
            primaryLang.invoke(null),
            engine,
            it,
          )
        } else {
          logging.error("To run a server, pass a file, or code via stdin or `-c`")
        }

        // run a script as a file, or perhaps a string literal
        else -> if (executeLiteral) {
          logging.trace("Interpreting runnable parameter as code")
          executeSingleStatement(
            langs,
            primaryLang.invoke(null),
            it,
            scriptTargetOrCode,
          )
        } else {
          // no literal execution flag = we need to parse `runnable` as a file path
          logging.trace("Interpreting runnable parameter as file path (`--code` was not passed)")
          File(scriptTargetOrCode).let { scriptFile ->
            primaryLang.invoke(scriptFile).let { lang ->
              logging.debug("Beginning script execution")
              readExecuteCode(
                scriptFile.name,
                langs,
                lang,
                it,
                readExecutableScript(
                  langs,
                  lang,
                  scriptFile,
                ),
              )
            }
          }
        }
      }
    }

    // don't exit if we have a running server
    if (serverRunning.get()) {
      // wait for all tasks to arrive
      logging.debug("Waiting for long-lived tasks to arrive")
      phaser.get().arriveAndAwaitAdvance()
      logging.debug("Exiting")
    }
    return success()
  }
}
