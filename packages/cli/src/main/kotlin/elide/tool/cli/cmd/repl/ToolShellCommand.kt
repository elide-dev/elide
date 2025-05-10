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

@file:Suppress(
  "UNUSED_PARAMETER",
  "MaxLineLength",
  "LargeClass",
)

package elide.tool.cli.cmd.repl

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import io.micronaut.context.BeanContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.core.io.IOUtils
import org.graalvm.nativeimage.ImageInfo
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
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeoutException
import java.util.function.Supplier
import java.util.stream.Stream
import javax.tools.ToolProvider
import jakarta.inject.Provider
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.math.max
import kotlin.streams.asSequence
import kotlin.streams.asStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import elide.annotations.Inject
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess
import elide.runtime.core.extensions.attach
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.GuestError
import elide.runtime.gvm.internals.IntrinsicsManager
import elide.runtime.gvm.kotlin.KotlinCompilerConfig
import elide.runtime.gvm.kotlin.KotlinJarBundleInfo
import elide.runtime.gvm.kotlin.KotlinLanguage
import elide.runtime.gvm.kotlin.KotlinPrecompiler
import elide.runtime.gvm.kotlin.KotlinScriptCallable
import elide.runtime.intrinsics.server.http.HttpServerAgent
import elide.runtime.intrinsics.testing.TestingRegistrar
import elide.runtime.plugins.kotlin.shell.GuestKotlinEvaluator
import elide.runtime.plugins.vfs.VfsListener
import elide.runtime.plugins.vfs.vfs
import elide.runtime.precompiler.Precompiler
import elide.tool.Classpath
import elide.tool.ClasspathSpec
import elide.tool.Environment
import elide.tool.MultiPathUsage
import elide.tool.cli.*
import elide.tool.cli.GuestLanguage.*
import elide.tool.cli.cmd.builder.emitCommand
import elide.tool.cli.cmd.runner.DebugConfig
import elide.tool.cli.cmd.runner.DelegatedRunner.Companion.delegatedRunner
import elide.tool.cli.cmd.runner.EnvironmentConfig
import elide.tool.cli.cmd.runner.InspectorConfig
import elide.tool.cli.cmd.runner.LanguageSelector
import elide.tool.cli.cmd.runner.ShellRunnable
import elide.tool.cli.err.AbstractToolError
import elide.tool.cli.err.ShellError
import elide.tool.cli.options.AccessControlOptions
import elide.tool.cli.options.EngineJavaOptions
import elide.tool.cli.options.EngineJavaScriptOptions
import elide.tool.cli.options.EngineJvmOptions
import elide.tool.cli.options.EngineKotlinOptions
import elide.tool.cli.options.EnginePythonOptions
import elide.tool.cli.output.JLineLogbackAppender
import elide.tool.cli.output.TOOL_LOGGER_APPENDER
import elide.tool.cli.output.TOOL_LOGGER_NAME
import elide.tool.err.ErrPrinter
import elide.tool.exec.SubprocessRunner.delegateTask
import elide.tool.exec.SubprocessRunner.stringToTask
import elide.tool.exec.SubprocessRunner.subprocess
import elide.tool.exec.allProjectPaths
import elide.tool.extensions.installIntrinsics
import elide.tool.io.WorkdirManager
import elide.tool.project.PackageManifestService
import elide.tool.project.ProjectManager
import elide.tooling.builder.BuildDriver
import elide.tooling.builder.BuildDriver.dependencies
import elide.tooling.builder.BuildDriver.resolve
import elide.tooling.builder.TestDriver
import elide.tooling.builder.TestDriver.discoverTests
import elide.tooling.jvm.resolver.MavenAetherResolver
import elide.tooling.project.ElideProject
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.NodePackageManifest
import elide.tooling.project.manifest.PackageManifest
import elide.tooling.runner.ProcessRunner

/**
 * Type alias for an accessor method which allows an optional builder amendment.
 */
private typealias ContextAccessor = () -> PolyglotContext

/** Interactive REPL entrypoint for Elide on the command-line. */
@Command(
  name = "run",
  description = ["Run a polyglot script, server, or interactive shell"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  synopsisHeading = "",
  aliases = [
    "r",
    "serve",
    "repl",
    "test",
  ],
  customSynopsis = [
    "",
    " Usage:  elide @|bold,fg(yellow) srcfile.<js|ts|jsx|tsx|py|kts...>|@ [OPTIONS] [--] [ARGS]",
    "    or:  elide @|bold,fg(yellow) <script>|@ [OPTIONS] [--] [ARGS]",
    "    or:  elide @|bold,fg(cyan) run|repl|serve|@ [OPTIONS] [@|bold,fg(cyan) --code|@ CODE] " +
            "[@|bold,fg(cyan) --stdin|@] [FILE] [ARGS]",
    "    or:  elide @|bold,fg(cyan) js|node|python|...|@ [OPTIONS] [@|bold,fg(cyan) --code|@ CODE] [FILE] [ARGS]",
    "",
  ],
)
@Introspected
@ReflectiveAccess
internal class ToolShellCommand @Inject constructor(
  private val beanContext: BeanContext,
  private val projectManagerProvider: Provider<ProjectManager>,
  private val manifestsProvider: Provider<PackageManifestService>,
  private val workdirProvider: Provider<WorkdirManager>,
  private val guestExecProvider: Provider<GuestExecutorProvider>,
) : ProjectAwareSubcommand<ToolState, CommandContext>() {
  internal companion object {
    private const val CONFIG_PATH_APP = "/etc/elide"
    private const val VERSION_INSTRINSIC_NAME = "__Elide_version__"

    // Whether to enable extended language plugins.
    private val ENABLE_JVM = System.getProperty("elide.lang.jvm") == "true"
    private val ENABLE_RUBY = System.getProperty("elide.lang.ruby") == "true"
    private val ENABLE_PYTHON = System.getProperty("elide.lang.python") == "true"
    private val ENABLE_TYPESCRIPT = System.getProperty("elide.lang.typescript") == "true"

    private val logging: Logger by lazy {
      Logging.of(ToolShellCommand::class)
    }

    private val tsAliases = setOf("ts", "typescript")
    private val pyAliases = setOf("py", "python")
    private val rbAliases = setOf("rb", "ruby")
    private val jvmAliases = setOf("jvm", "java")
    private val ktAliases = setOf("kt", "kotlin")

    // Maps language aliases to the engine they should invoke.
    internal val languageAliasToEngineId: SortedMap<String, String> = sortedMapOf(
      "py" to "python",
      "python" to "python",
      "node" to "js",
      "js" to "js",
      "javascript" to "js",
      "ruby" to "ruby",
      "jvm" to "jvm",
      "java" to "jvm",
      "kotlin" to "jvm",
      "kt" to "jvm",
    )

    // Whether the last-seen command was a user exit.
    private val exitSeen = atomic(false)

    // Count of statement lines seen; used as an offset for the length of `allSeenStatements`.
    private val statementCounter = atomic(0)

    // Language-specific syntax highlighter.
    private val langSyntax = atomic<SyntaxHighlighter?>(null)

    // Main operating terminal.
    private val terminal = atomic<Terminal?>(null)

    // Active line reader.
    private val lineReader = atomic<LineReader?>(null)

    // Server manager
    private val serverAgent: HttpServerAgent by lazy { HttpServerAgent() }

    // Synchronization primitive used to coordinate server behavior
    private val phaser = atomic(Phaser(1))

    // Whether a server is running.
    private val serverRunning = atomic(false)

    // Active project configuration.
    private val activeProject = atomic<ElideProject?>(null)
  }

  private val projectManager: ProjectManager by lazy {
    projectManagerProvider.get()
  }

  private val manifests: PackageManifestService by lazy {
    manifestsProvider.get()
  }

  private val workdir: WorkdirManager by lazy {
    workdirProvider.get()
  }

  private val guestExec: GuestExecutorProvider by lazy {
    guestExecProvider.get()
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

  // Resolve the specified language.
  @Suppress("ComplexCondition")
  internal fun resolveLangs(alias: String? = null): EnumSet<GuestLanguage> {
    return EnumSet.noneOf(GuestLanguage::class.java).apply {
      add(JS)
      add(WASM)
      if (ENABLE_TYPESCRIPT && (language.typescript || (alias != null && tsAliases.contains(alias)))) add(TYPESCRIPT)
      if (ENABLE_PYTHON && (language.python || (alias != null && pyAliases.contains(alias)))) add(PYTHON)
      if (ENABLE_RUBY && (language.ruby || (alias != null && rbAliases.contains(alias)))) add(RUBY)
      if (ENABLE_JVM && (language.jvm || language.kotlin || (alias != null && jvmAliases.contains(alias)))) add(JVM)
      if (ENABLE_JVM && (language.kotlin || (alias != null && ktAliases.contains(alias)))) add(KOTLIN)
    }
  }

  private lateinit var threadFactory: ThreadFactory

  // Last-seen statement executed by the VM.
  private val allSeenStatements = LinkedList<String>()

  // Intrinsics manager
  @Inject internal lateinit var mainIntrinsicsManager: Stream<IntrinsicsManager>

  // Event listeners for the vfs
  @Inject internal lateinit var registeredVfsListeners: Stream<VfsListener>

  // Intrinsics manager
  private val intrinsicsManager: Supplier<IntrinsicsManager>
    get() = Supplier {
      mainIntrinsicsManager.findFirst().orElseThrow()
    }

  // Event listeners for the vfs
  private val vfsListeners: Supplier<List<VfsListener>>
    get() = Supplier {
      registeredVfsListeners.toList()
    }

  /** Specifies the guest language to run. */
  @Option(
    names = ["--languages"],
    description = ["Query supported languages"],
    defaultValue = "false",
  )
  internal var languages: Boolean = false

  /** Enables installation before run. */
  @Option(
    names = ["--no-install"],
    description = ["Install dependencies before running"],
    defaultValue = "false",
  )
  internal var skipInstall: Boolean = false

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
  )
  var accessControl: AccessControlOptions = AccessControlOptions()

  /** App environment settings. */
  @ArgGroup(
    exclusive = false,
    heading = "%nEnvironment:%n",
  )
  internal var appEnvironment: EnvironmentConfig = EnvironmentConfig()

  /** Chrome inspector settings. */
  @ArgGroup(
    exclusive = false,
    heading = "%nInspector:%n",
  )
  internal var inspector: InspectorConfig = InspectorConfig()

  /** DAP host settings. */
  @ArgGroup(
    exclusive = false,
    heading = "%nDebugger:%n",
  )
  internal var debugger: DebugConfig = DebugConfig()

  /** Language selector. */
  @ArgGroup(
    exclusive = false,
    heading = "%nLanguage Selection:%n",
  )
  internal var language: LanguageSelector = LanguageSelector()

  /** Settings specific to JavaScript. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: JavaScript%n",
  )
  internal var jsSettings: EngineJavaScriptOptions = EngineJavaScriptOptions()

  /** Settings specific to Python. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: Python%n",
  )
  internal var pySettings: EnginePythonOptions = EnginePythonOptions()

  /** Settings specific to JVM. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: JVM%n",
  )
  internal var jvmSettings: EngineJvmOptions = EngineJvmOptions()

  /** Settings specific to Java. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: Java%n",
  )
  internal var javaSettings: EngineJavaOptions = EngineJavaOptions()

  /** Settings specific to Kotlin. */
  @ArgGroup(
    validate = false,
    heading = "%nEngine: Kotlin%n",
  )
  internal var kotlinSettings: EngineKotlinOptions = EngineKotlinOptions()

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

  /** Script arguments to pass. */
  @Parameters(
    index = "1",
    arity = "0..*",
    paramLabel = "ARG",
    description = [
      "Arguments to pass to the script to run; must be positioned " +
              "after the file to run.",
    ],
  )
  internal var arguments: List<String>? = null

  // Language hint passed in from outer tools, like when the user calls `elide python`.
  internal var languageHint: String? = null

  // Action hint passed in from outer tools, like when the user calls `elide serve`.
  internal var actionHint: String? = null

  // Executed when a guest statement is entered.
  private fun onStatementEnter(event: ExecutionEvent) {
    if (verbose) {
      val highlighter = langSyntax.value
      val lineReader = lineReader.value

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

  private fun printHighlighted(txt: String) {
    val highlighter = langSyntax.value
    val lineReader = lineReader.value

    if (txt.isNotBlank()) {
      if (highlighter != null && lineReader != null) {
        lineReader.printAbove(highlighter.highlight(txt))
      } else {
        println(txt)
      }
    }
  }

  @Suppress("unused")
  private fun onUserInteractiveSource(code: String, source: Source) {
    printHighlighted(code)
  }

  // Execute a single chunk of code, or literal statement.
  @Suppress("SameParameterValue", "unused") private fun executeOneChunk(
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    ctxAccessor: ContextAccessor,
    origin: String,
    code: String,
    interactive: Boolean = false,
    literal: Boolean = false,
  ): Value {
    val chunk = Source.newBuilder(primaryLanguage.engine, code, origin)
      .interactive(false)
      .internal(false)
      .cached(true)

    val source = if (literal) {
      chunk.buildLiteral()
    } else {
      chunk.build()
    }

    // if we are executing interactively, we need to "print above" with the user's code
    if (interactive) onUserInteractiveSource(
      code,
      source,
    )

    val ctx = ctxAccessor.invoke()
    logging.trace("Code chunk built. Evaluating")
    ctx.enter()
    val result = try {
      ctx.evaluate(source)
    } catch (exc: PolyglotException) {
      if (interactive) {
        throw exc  // don't capture exceptions during interactive sessions; they are handled separately
      }
      when (val throwable = processUserCodeError(primaryLanguage, exc)) {
        null -> {
          logging.trace("Caught exception from code statement ${statementCounter.value}", exc)
          throw exc
        }

        else -> throw throwable
      }
    } finally {
      ctx.leave()
    }
    logging.trace("Code chunk evaluation complete")
    return result
  }

  // Execute a single chunk of code as a literal statement.
  private fun executeSingleStatement(
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    ctxAccessor: ContextAccessor,
    code: String,
  ) {
    if (serveMode()) {
      assert(!serverRunning.value) { "Server is already running" }
      serverRunning.value = true
      executeSource(
        "stdin",
        languages,
        primaryLanguage,
        ctxAccessor,
        Source.newBuilder(primaryLanguage.symbol, code, "stdin")
          .encoding(StandardCharsets.UTF_8)
          .internal(false)
          .buildLiteral(),
      )
    } else executeOneChunk(
      languages,
      primaryLanguage,
      ctxAccessor,
      "stdin",
      code,
      interactive = false,
      literal = false,
    )
  }

  // Build or detect a terminal to use for interactive REPL use.
  private fun buildTerminal(): Terminal {
    return TerminalBuilder.builder()
      .ffm(true)  // prefer ffm
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
        terminal.value = this
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
    logging.debug("Building highlighter with config path '{}'", jnanorc)
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
      .option(LineReader.Option.ERASE_LINE_ON_FINISH, true)
      .build()

    lineReader.value = reader
    langSyntax.value = languageHighlighter
    builtins.setLineReader(reader)
    history.attach(reader)
    reader.apply {
      op.invoke(this, systemRegistry)
    }
  }

  // Initialize nano-rc syntax highlighting configurations.
  @Suppress("TooGenericExceptionCaught")
  private fun initNanorcConfig(rootPath: Path, userHome: String): File? {
    if (!rootPath.exists() || Statics.noColor) {
      logging.debug("Syntax highlighting disabled by flags, missing root, or NO_COLOR")
      return null
    }
    val jnanorcDir = rootPath.resolve("nanorc")
    val jnanorcFile = Paths.get(userHome, "jnanorc").toFile()

    logging.debug("Checking nanorc root at path '{}'", jnanorcFile.toPath())
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
        logging.debug("- Initializing syntax file '{}' ({})", it, target)
        if (target.exists()) {
          logging.debug("Syntax file '$it' already exists. Skipping...")
          return@forEach
        }
        val fileStream = ToolShellCommand::class.java.getResourceAsStream("/nanorc/$it")
        if (fileStream == null) {
          logging.warn("Failed to locate nanorc config file from resources: $it")
          return@forEach
        }

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

    logging.debug("Checking syntax config at path '{}'", jnanorcFile.toPath())
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
    val rootLogger = LoggerFactory.getLogger(TOOL_LOGGER_NAME) as? ch.qos.logback.classic.Logger
      ?: return
    val current = rootLogger.getAppender(TOOL_LOGGER_APPENDER) as? ConsoleAppender<ILoggingEvent>
      ?: return
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
    val baseOnHand = minOf(0, statementCounter.value)
    val errorTail = errorBase + (exc.sourceLocation.endLine - exc.sourceLocation.startLine)
    val topLine = maxOf(statementCounter.value, errorTail)

    return when {
      // cannot resolve: we don't have those lines (they are too early for our buffer)
      errorBase < baseOnHand -> -1 to emptyList()

      // otherwise, resolve from seen statements
      else -> {
        if (allSeenStatements.isNotEmpty()) {
          ctxLines.addAll(allSeenStatements.subList(errorBase, minOf(topLine, allSeenStatements.size)))
          (errorBase + 1) to ctxLines
        } else when (runnable?.ifBlank { null }) {
          // @TODO implement
          null -> (errorBase + 1) to emptyList()
          else -> (-1) to emptyList()
        }
      }
    }
  }

  // Determine error printer settings for this run.
  private fun errPrinterSettings(): ErrPrinter.ErrPrinterSettings = ErrPrinter.ErrPrinterSettings(
    enableColor = pretty,
    maxLength = terminal.value?.width ?: ErrPrinter.DEFAULT_MAX_WIDTH,
  )

  // Given an error, render a table explaining the error, along with any source context if we have it.
  private fun displayFormattedError(
    exc: Throwable,
    message: String,
    advice: String? = null,
    internal: Boolean = false,
    stacktrace: Boolean = internal,
    withCause: Boolean = true,
  ) {
    if (exc !is PolyglotException) {
      exc.printStackTrace()
      return
    }
    if (debug || verbose) {
      // print full stack traces raw in debug or verbose mode mode
      exc.printStackTrace()
    }
    val term = terminal.value
    val reader = lineReader.value

    // begin calculating with source context
    val middlePrefix = "║ "
    val errPrefix = "→ "
    val stopPrefix = "✗ "
    val lineContextTemplate = "$middlePrefix%lineNum┊ %lineText"
    val errContextPrefix = "$errPrefix%lineNum┊ %lineText"
    val stopContextPrefix = "$stopPrefix%lineNum┊ %lineText"

    val (startingLineNumber, errorContext) = errorContextLines(exc)
    val startLine = startingLineNumber + max((exc.sourceLocation?.startLine ?: 0) - 1, 0)
    val endLine = startingLineNumber + max((exc.sourceLocation?.endLine ?: 0) - 1, 0)
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
    val doPrintStack = stacktrace || errRange.count() < 2
    val stacktraceContent = if (doPrintStack) {
      val stackString = ErrPrinter.fmt(
        exc,
        settings = errPrinterSettings(),
      )
      when (val cause = exc.cause ?: if (exc.isHostException) exc.asHostException() else exc) {
        null -> {}
        else -> if (cause !== exc) {
          if (withCause) {
            stackString.append("\nCause stacktrace: \n\n")
            stackString.append(ErrPrinter.fmt(cause))
          } else if (exc.isHostException) {
            stackString.append("\nCause: ${cause.message}")
            stackString.append("   Failed to gather stacktrace for host exception of type ${cause::class.simpleName}.")
          }
        }
      }
      stackString.toString()
    } else {
      ""
    }
    val stacktraceLines = if (doPrintStack) {
      stacktraceContent.lines()
    } else {
      emptyList()
    }

    val pad = 2 * 2
    val maxErrLineSize = if (lineContextRendered.isNotEmpty()) lineContextRendered.maxOf { it.length } + pad else 0

    // calculate the maximum width needed to display the error box, but don't exceed the width of the terminal.
    val width = maxOf(
      80,
      minOf(
        term?.width ?: ErrPrinter.DEFAULT_MAX_WIDTH,
        maxOf(
          // message plus padding
          message.length + pad + 1,

          // error context lines
          maxErrLineSize,

          // advice
          (advice?.length ?: 0) + pad,

          // stacktrace max line size
          stacktraceLines.maxOfOrNull { it.length } ?: 0,
        ),
      ),
    )

    val textWidth = width - (pad / 2) + if (doPrintStack) {
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
        val fmtMsg = ErrPrinter.fmtMessage(message, settings = errPrinterSettings())
        append(middlePrefix).append(fmtMsg.padEnd(textWidth - 2, ' ')).append(" ║\n")
      }

      if (doPrintStack || advice?.isNotBlank() == true) {
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
          if (doPrintStack) appendLine(divider)
        }

        // append stacktrace next
        if (doPrintStack) {
          // ║ Stacktrace:          ║
          append(middlePrefix).append("Stacktrace:".padEnd(textWidth - 1, ' ') + "║\n")
          appendLine(blankLine)

          // ║ ...                  ║
          stacktraceLines.forEach {
            if (it.startsWith('\t')) {
              // if it's a spaced line, don't add additional end-spacing (for tab-prefixes)
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
  private fun processUserCodeError(language: GuestLanguage, exc: PolyglotException, msg: String? = null): Throwable? {
    when {
      exc.isSyntaxError -> displayFormattedError(
        exc,
        msg ?: exc.message ?: "Syntax error",
        "Check ${language.label} syntax",
      )

      exc.isIncompleteSource -> displayFormattedError(
        exc,
        msg ?: exc.message ?: "Syntax error",
        "${language.label} syntax is incomplete",
      )

      exc.isHostException || exc.message?.contains("HostException: ") == true -> {
        when (exc.asHostException()) {
          // guest error thrown from host-side logic
          is GuestError -> displayFormattedError(
            exc,
            msg ?: exc.message ?: "An error was thrown",
            stacktrace = !isInteractive(),
          )

          else -> displayFormattedError(
            exc.asHostException(),
            msg ?: exc.asHostException().message ?: "A runtime error was thrown",
            advice = "This is an error in Elide. Please report this to the Elide Team with `elide bug`",
            stacktrace = true,
            internal = true,
          )
        }
      }

      // if this is a guest-side exception, throw it
      exc.guestObject != null && exc.guestObject.isException -> {
        logging.debug("Detected guest-side exception; throwing")
        exc.guestObject.throwException()
      }

      exc.isGuestException -> displayFormattedError(
        exc,
        msg ?: exc.message ?: "An error was thrown",
        stacktrace = !isInteractive(),
      )

      // @TODO(sgammon): interrupts
      exc.isInterrupted -> {}

      // @TODO(sgammon): resource exhaustion
      exc.isResourceExhausted -> {}

      exc.isInternalError -> displayFormattedError(
        exc,
        msg ?: exc.message ?: "An error was thrown",
        internal = true,
      )

      else -> error("Unhandled polyglot error type: $exc")
    }
    // in interactive sessions, return `null` if the error is non-fatal; this tells the outer execution loop to ignore
    // the exception (since it has been printed to the user), and continue with the interactive session.
    return if (isInteractive()) {
      null
    } else {
      ShellError.USER_CODE_ERROR.raise(exc)
    }
  }

  private fun showValue(value: Value) {
    printHighlighted(value.toString())
  }

  // Wrap an interactive REPL session in exit protection.
  private fun beginInteractiveSession(
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    engine: PolyglotEngine,
    ctxAccessor: ContextAccessor,
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
              ctxAccessor,
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
            if (exitSeen.value) exitSeen.value = false

            // if we are running in modes which show the result, print it
            if (!quiet && primaryLanguage.id != RUBY.id) showValue(result)

          } catch (_: NoSuchElementException) {
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
          } catch (_: UserInterruptException) {
            if (exitSeen.value) {
              break  // we've already seen the exit, so we need to break
            } else try {
              exitSeen.compareAndSet(false, true)
              println("ctrl+c caught; do it again to exit")
              continue
            } catch (_: Exception) {
              break  // just in case
            }
          } catch (_: EndOfFileException) {
            break  // user sent ctrl+d
          } catch (_: InterruptedException) {
            println("Interrupted (sys)")
            logging.debug("Session interrupted; concluding")
            break  // exit on interrupt
          }
        }
      }
    }
  }

  private fun KotlinJarBundleInfo.asSource(): Source {
    TODO("Not yet implemented: ${this::class.simpleName}")
  }

  // Resolve a compiler for the provided language, compile the entrypoint, and return the resolved executable symbol.
  private fun compileEntrypoint(language: GuestLanguage, ctxAccessor: ContextAccessor, entry: File): ShellRunnable {
    return when (language) {
      // use the host java compiler, which supports up to the maximum JVM bytecode level anyway
      JAVA -> ShellRunnable.HostExecutable {
        ToolProvider.getSystemJavaCompiler().let {
          error("No way to compile pure Java yet")
        }
      }

      // use the embedded kotlin compiler to compile to java bytecode first
      KOTLIN -> ShellRunnable.HostExecutable {
        val sourceInfo = Precompiler.PrecompileSourceInfo(
          name = entry.name,
          path = entry.toPath(),
        )
        val config = KotlinCompilerConfig.DEFAULT

        runBlocking(Dispatchers.IO) {
          KotlinPrecompiler.precompile(
            Precompiler.PrecompileSourceRequest(
              sourceInfo,
              config,
            ),
            entry.readText(StandardCharsets.UTF_8),
          )
        }.let {
          when (it) {
            null -> error("Failed to precompile Kotlin runnable")
            is KotlinScriptCallable -> it.apply(
              ctxAccessor.invoke()
            )
            is KotlinJarBundleInfo -> {
              val ctx = ctxAccessor()
              val interpreter = GuestKotlinEvaluator(ctx)
              interpreter.evaluate(it.asSource(), ctx)
            }
          }
        }
      }

      else -> error("Compiled sources for language '${language.name}' are not yet supported")
    }
  }

  // Read an executable script file and return it as a `File` and a `Source`.
  @Suppress("ThrowsCount")
  private fun readExecutableScript(
    supportedLangs: EnumSet<GuestLanguage>,
    languages: EnumSet<GuestLanguage>,
    language: GuestLanguage,
    script: File,
  ): Source {
    logging.debug { "Reading executable user script at path '${script.path}' (language: ${language.id})" }
    if (!script.exists()) {
      logging.debug { "Script file does not exist" }
      throw ShellError.FILE_NOT_FOUND.asError()
    } else {
      logging.trace { "Script check: File exists" }
    }
    if (!script.canRead()) {
      logging.debug { "Script file cannot be read" }
      throw ShellError.FILE_NOT_READABLE.asError()
    } else {
      logging.trace { "Script check: File is readable" }
    }
    if (!script.isFile) {
      logging.debug { "Script file is not a regular file" }
      throw ShellError.NOT_A_FILE.asError()
    } else {
      logging.trace { "Script check: File is a regular file" }
    }

    // type check: first, check file extension
    val allowedMimeTypes = supportedLangs.flatMap { it.mimeTypes }.toSortedSet()
    val allowedExtensions = supportedLangs.flatMap { it.extensions }.toSortedSet()
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
      supportedLangs.find {
        it.extensions.contains(script.extension)
      }
    }
    requireNotNull(targetLang) {
      "Failed to resolve target language for source file"
    }
    val abs = script.absoluteFile
    return Source.newBuilder(targetLang.symbol, abs)
      .encoding(StandardCharsets.UTF_8)
      .internal(false)
      .cached(true)
      .uri(abs.toURI())
      .build()
  }

  private val commandSpecifiesServer: Boolean by lazy {
    Statics.args.let {
      it.contains("serve") || it.contains("start")
    }
  }

  private val commandSpecifiesTest: Boolean by lazy {
    Statics.args.let {
      it.contains("test") || it.contains("tests")
    }
  }

  // Detect whether we are running in `serve` mode (with alias `start`).
  private fun serveMode(): Boolean = (
          commandSpecifiesServer ||
                  executeServe ||
                  actionHint?.lowercase()?.trim().let {
                    it != null && it == "serve" || it == "start"
                  }
          )

  // Detect whether we are running in `test` mode (with alias `test` or `tests`).
  private fun testMode(): Boolean = (
          commandSpecifiesTest ||
                  actionHint?.lowercase()?.trim().let {
                    it != null && it == "test" || it == "tests"
                  }
          )

  // Read an executable script, and then execute the script and keep it started as a server.
  private fun readStartServer(
    label: String,
    langs: EnumSet<GuestLanguage>,
    language: GuestLanguage,
    ctxAccessor: ContextAccessor,
    source: Source,
    execProvider: GuestExecutorProvider,
  ) {
    try {
      // enter VM context
      logging.trace("Entered VM for server application (language: ${language.id}). Consuming script from: '$label'")

      // initialize the server intrinsic and run using the provided source
      serverAgent.run(source, execProvider) { resolvePolyglotContext(langs) }
      phaser.value.register()
      serverRunning.value = true
    } catch (exc: PolyglotException) {
      processUserCodeError(language, exc)?.let { throw it }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun execWrapped(
    label: String,
    ctxAccessor: ContextAccessor,
    source: Source,
  ) {
    // parse the source
    val ctx = ctxAccessor.invoke()
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
  }

  // Read an executable script, and then execute registered tests.
  private fun readRunTests(
    label: String,
    langs: EnumSet<GuestLanguage>,
    language: GuestLanguage,
    ctxAccessor: ContextAccessor,
    source: List<Source>,
    execProvider: GuestExecutorProvider,
  ) {
    // enter VM context
    var didPrepare = false
    val project = activeProject.value
    val ctx = ctxAccessor.invoke()
    ctx.enter()

    try {
      logging.trace("Entered VM for test run (language: ${language.id}). Consuming script from: '$label'")

      // execute all matched or provided source files (interpreted lang tests)
      source.forEach { entry ->
        execWrapped(label, ctxAccessor, entry)
      }

      // invoke all test contributors (handles registration for non-source tests)
      if (project != null) {
        runBlocking {
          coroutineScope {
            discoverTests(beanContext, project)
          }
        }
      }

      // continue to configure and plan test run
      didPrepare = true
      val testRegistry = beanContext.getBean(TestingRegistrar::class.java)
      val allTests = testRegistry.freeze().grouped().toList()
      if (allTests.isEmpty()) {
        logging.info("No tests to run.")
        return
      }

      // start up the test runner and run all eligible/matched tests
      logging.info { "Would run ${allTests.size} tests" }
      allTests.forEach {
        logging.info { "- scope=(${it.first.qualifiedName}) test=(${it.second.qualifiedName})" }
      }
      return

    } catch (exc: PolyglotException) {
      val phase = if (didPrepare) "running" else "preparing"

      val msg = "Failure while $phase tests: ${exc.message}"
      throw (processUserCodeError(
        language,
        exc,
        msg = msg,
      ) ?: exc)
    } finally {
      ctx.leave()
    }
  }

  // Invoke an executable entrypoint, having been loaded from compiled VM symbols.
  private fun executeCompiled(
    entrypointFile: File,
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    ctxAccessor: ContextAccessor,
    entrypoint: ShellRunnable,
  ) {
    when (entrypoint) {
      is ShellRunnable.HostExecutable -> entrypoint.value.call()

      is ShellRunnable.GuestExecutable -> {
        require(entrypoint.value.canExecute()) { "Guest compiled entrypoint is not executable" }
        when (primaryLanguage) {
          JAVA -> {
            error("No ability to execute compiled Java yet")
          }

          KOTLIN -> {
            error("No ability to execute compiled entrypoint symbols yet")
          }

          else -> error("No ability to execute compiled entrypoint symbols yet for language: $primaryLanguage")
        }
      }
    }
  }

  // Read an executable script, and then execute the script; if it's a server, delegate to `readStartServer`.
  @Suppress("TooGenericExceptionCaught")
  private fun executeSource(
    label: String,
    languages: EnumSet<GuestLanguage>,
    primaryLanguage: GuestLanguage,
    ctxAccessor: ContextAccessor,
    source: Source,
  ) {
    when {
      // in server mode, read the sources and then expect to start a server.
      serveMode() -> readStartServer(
        label,
        languages,
        primaryLanguage,
        ctxAccessor,
        source,
        guestExec,
      )

      // in test mode, read the sources with test APIs/registration/execution enabled.
      testMode() -> readRunTests(
        label,
        languages,
        primaryLanguage,
        ctxAccessor,
        listOf(source),
        guestExec,
      )

      // otherwise, it's a normal test run.
      else -> try {
        // enter VM context
        logging.trace("Entered VM for script execution ('${primaryLanguage.id}'). Consuming script from: '$label'")
        execWrapped(label, ctxAccessor, source)
      } catch (exc: PolyglotException) {
        logging.debug("Caught polyglot exception: $exc")
        if (logging.isEnabled(LogLevel.DEBUG)) {
          logging.debug(
            StringBuilder().apply {
              append("Error Info: ")
              append("message{${exc.message}} ")
              append("source{${exc.sourceLocation}} ")
              append("isGuestException{${exc.isGuestException}} ")
              append("isHostException{${exc.isHostException}} ")
              append("isCancelled{${exc.isCancelled}} ")
              append("isInterrupted{${exc.isInterrupted}} ")
              append("isResourceExhausted{${exc.isResourceExhausted}} ")
              append("isSyntaxError{${exc.isSyntaxError}} ")
              append("isIncompleteSource{${exc.isIncompleteSource}} ")
              append("isInternalError{${exc.isInternalError}} ")
              append("cause{${exc.cause}}")
              val guestObj = exc.guestObject
              if (guestObj != null) {
                append(" // Guest Object: ")
                append("isHostObject{${guestObj.isHostObject}} ")
                append("isNull{${guestObj.isNull}} ")
                append("isException{${guestObj.isException}} ")
                append("isString{${guestObj.isString}}")
              }
            }.toString(),
          )
        }
        when (val throwable = processUserCodeError(primaryLanguage, exc)) {
          null -> {}
          else -> throw throwable
        }
      } catch (exc: Throwable) {
        logging.debug("Caught (and re-throwing) exception: $exc")
      }
    }
  }

  // Make sure the file-system bundle specified by `bundleSpec` is usable.
  private fun checkFsBundle(bundleSpec: String?): URI? = when {
    bundleSpec.isNullOrBlank() -> null
    bundleSpec.startsWith("classpath:") -> URI.create(bundleSpec)
    else -> File(bundleSpec).toURI()
  }

  // Resolve the default language to use when interpreting a given `fileInput`, or use a sensible fallback from the set
  // of supported languages. If JS is disabled, there can only be one language; otherwise the default language is JS. If
  // a file is provided with a specific matching file extension for a given language, that language is used.
  private fun resolvePrimaryLanguage(
    languageSelector: LanguageSelector?,
    languages: EnumSet<GuestLanguage>,
    fileInput: File?,
  ): GuestLanguage = when {
    // if we have a file input, the extension for the file takes next precedence
    fileInput != null && fileInput.exists() -> {
      val ext = fileInput.extension
      languages.find { it.extensions.contains(ext) } ?: JS
    }

    // if there is only one language, that's the result
    languages.size == 1 -> languages.first()

    // an explicit flag from a user takes top precedence
    languageSelector != null -> languageSelector.primary(commandSpec, languages, languageHint)

    // we have to have at least one language
    languages.isEmpty() -> error("Cannot start VM with no enabled guest languages")

    // otherwise, if JS is included in the set of languages, that is the default.
    else -> if (languages.contains(JS)) JS else languages.first()
  }

  // Return the suite of JAR bases we should use for a guest classpath.
  private fun initialGuestClasspathJars(langHomeResources: Path): Sequence<Path> {
    return sequenceOf(
      langHomeResources.resolve("elide-kotlin.jar"),
      langHomeResources.resolve("elide-kotlin-runtime.jar"),
      langHomeResources.resolve("kotlinx-coroutines-core-jvm.jar"),
      langHomeResources.resolve("kotlin-stdlib.jar"),
      langHomeResources.resolve("kotlin-reflect.jar"),
      langHomeResources.resolve("kotlin-script-runtime.jar"),
    )
  }

  // Amend a base classpath with any additional resolved project dependencies.
  private fun withProjectClasspath(project: ElideProject, base: Sequence<Path>): List<Path> {
    return when (project.manifest.dependencies.maven.hasPackages()) {
      // the project doesn't specify any maven dependencies; return the base classpath
      false -> base.toList()

      // the project defines maven dependencies; assemble a classpath and return.
      else -> runBlocking {
        val runtimeClasspath = if (skipInstall) null else {
          val mavenResolver = BuildDriver.configure(beanContext, project).let {
            resolve(it, dependencies(it).await()).also {
              it.second.joinAll()
            }.first.filterIsInstance<MavenAetherResolver>().first()
          }
          mavenResolver.classpathProvider(object: ClasspathSpec {
            override val usage: MultiPathUsage get() = when (testMode()) {
              true -> MultiPathUsage.TestRuntime
              false -> MultiPathUsage.Runtime
            }
          })?.classpath()
        }

        Classpath.empty().toMutable().apply {
          base.forEach { add(it) }

          // the user's classpath goes first
          runtimeClasspath?.let { prepend(it) }
        }.map {
          it.path
        }.toList()
      }
    }
  }

  override fun PolyglotEngineConfiguration.configureEngine(langs: EnumSet<GuestLanguage>) {
    // grab project configurations, if available
    val project = activeProject.value
    if (project != null) logging.debug("Resolved project info: {}", project)

    // conditionally apply debugging settings
    if (debug) debugger.apply(this)
    inspector.apply(this)
    val requiresIo = langs.let { it.contains(PYTHON) || it.contains(RUBY) }

    // configure host access rules
    hostAccess = when {
      accessControl.allowAll -> HostAccess.ALLOW_ALL
      accessControl.allowIo -> HostAccess.ALLOW_IO
      accessControl.allowEnv -> if (requiresIo) HostAccess.ALLOW_ALL else HostAccess.ALLOW_ENV
      else -> if (requiresIo) HostAccess.ALLOW_IO else HostAccess.ALLOW_NONE
    }

    // configure environment variables
    appEnvironment.apply(
      project,
      this,
      host = accessControl.allowEnv,
      dotenv = appEnvironment.dotenv,
    )

    // load arguments into context if we have them
    when (val arguments = arguments) {
      null -> emptyArray()
      else -> if (arguments.isEmpty()) emptyArray() else {
        arguments.toTypedArray()
      }
    }.let {
      val runnable = runnable?.ifBlank { null }
      args(
        if (runnable == null) it else {
          listOf(runnable).plus(it).toTypedArray()
        },
      )
    }

    // configure support for guest languages
    val versionProp = VERSION_INSTRINSIC_NAME to Elide.version()
    val intrinsics = intrinsicsManager.get().resolver()

    // resolve entrypoint arguments
    val cmd = if (ImageInfo.inImageCode()) {
      ProcessHandle.current().info().command().orElse("elide")
    } else {
      "elide"
    }
    val args = Statics.args

    // configure resource path
    val gvmResources = when (ImageInfo.inImageCode()) {
      true -> Statics.binPath.parent.resolve("resources")

      // in JVM mode, pull from system properties
      else -> requireNotNull(System.getProperty("elide.gvmResources")) {
        "Failed to resolve GraalVM resources path: please set `elide.gvmResources`"
      }.let { Path(it) }
    }

    langs.forEach { lang ->
      when (lang) {
        // Primary Engines
        JS -> configure(elide.runtime.plugins.js.JavaScript) {
          logging.debug("Configuring JS VM")
          resourcesPath = gvmResources
          executable = cmd
          testing = testMode()
          executableList = listOf(cmd).plus(args)
          installIntrinsics(intrinsics, GraalVMGuest.JAVASCRIPT, versionProp)
          jsSettings.apply(this)
        }

        WASM -> configure(elide.runtime.plugins.wasm.Wasm) {
          logging.debug("Configuring WASM VM")
          resourcesPath = gvmResources
        }

        TYPESCRIPT -> configure(elide.runtime.plugins.typescript.TypeScript) {
          logging.debug("Configuring TypeScript support")
          resourcesPath = gvmResources
        }

//        RUBY -> ignoreNotInstalled {
//           install(elide.runtime.plugins.ruby.Ruby) {
//             logging.debug("Configuring Ruby VM")
//             resourcesPath = gvmResources
//             executable = cmd
//             executableList = listOf(cmd).plus(args)
//             installIntrinsics(intrinsics, GraalVMGuest.RUBY, versionProp)
//           }
//         }

        PYTHON -> configure(elide.runtime.plugins.python.Python) {
          logging.debug("Configuring Python VM")
          installIntrinsics(intrinsics, GraalVMGuest.PYTHON, versionProp)
          resourcesPath = gvmResources
          executable = cmd
          testing = testMode()
          executableList = listOf(cmd).plus(args)
          pySettings.apply(this)
        }

        // Secondary Engines: JVM
        JVM, KOTLIN, JAVA, SCALA, GROOVY -> {
          val javaHome: String? = System.getProperty("java.home")
            ?.ifBlank { null }
            ?.takeIf { it.isNotEmpty() }
            ?: System.getenv("JAVA_HOME")
              ?.ifBlank { null }
              ?.takeIf { it.isNotEmpty() }

          if (javaHome == null) {
            logging.warn {
              "JAVA_HOME is not set; JVM features may not work properly."
            }
          } else if (javaHome != System.getProperty("java.home")) {
            System.setProperty("java.home", javaHome)
          }

          val langResourcesRelative = gvmResources.resolve("kotlin")
            .resolve(KotlinLanguage.VERSION)
            .resolve("lib")

          val kotlinSpecificResources = System.getProperty("elide.kotlinResources")
            ?.let { Path(it) }
            ?.let { it.resolve("lib") }

          val langResources = kotlinSpecificResources ?: langResourcesRelative

          val langHomeResources = Path(System.getProperty("user.home"))
            .resolve("elide")
            .resolve("resources")
            .resolve("kotlin")
            .resolve(KotlinLanguage.VERSION)
            .resolve("lib")

          val pathsFromLangResources = if (Files.exists(langResources)) {
            initialGuestClasspathJars(langResources)
          } else {
            emptySequence()
          }
          val pathsFromHomeResources = if (Files.exists(langHomeResources)) {
            initialGuestClasspathJars(langHomeResources)
          } else {
            emptySequence()
          }

          val extraHome = System.getenv("KOTLIN_HOME") ?: System.getenv("ELIDE_KOTLIN_HOME")
          val fullClasspath: List<Path> = (
            when (extraHome) {
              null -> emptySequence()
              else -> initialGuestClasspathJars(Path(extraHome).resolve("lib"))
            }
          ).plus(
            pathsFromLangResources
          ).plus(
            pathsFromHomeResources
          ).distinct().asStream().parallel().filter {
            Files.exists(it)
          }.let {
            when (project) {
              // with no active project, just return the initial classpath
              null -> it.toList()

              // otherwise, assemble a classpath from the base and any project deps
              else -> withProjectClasspath(project, it.asSequence())
            }
          }

          logging.debug {
            "Guest classpath: ${fullClasspath.joinToString(":")}"
          }

          configure(elide.runtime.plugins.jvm.Jvm) {
            logging.debug("Configuring JVM")
            resourcesPath = gvmResources
            multithreading = !langs.contains(JS)
            testing = testMode()
            classpath(fullClasspath.map { it.absolutePathString() })
            javaHome?.let { guestJavaHome = it }
          }.also {
            configure(elide.runtime.plugins.java.Java) {
              logging.debug("Configuring Java")
            }
            when (lang) {
              KOTLIN -> configure(elide.runtime.plugins.kotlin.Kotlin) {
                guestClasspathRoots.addAll(fullClasspath)
                javaHome?.let { guestJavaHome = it }
                extraHome?.let { guestKotlinHome = it }
              }
              GROOVY -> logging.warn("Groovy runtime plugin is not yet implemented")
              SCALA -> logging.warn("Scala runtime plugin is not yet implemented")
              else -> {}
            }
          }
        }

        // Secondary engines: LLVM
        LLVM -> configure(elide.runtime.plugins.llvm.LLVM) {
          // Nothing at this time.
        }

        else -> {}
      }
    }

    // configure VFS with user-specified bundles
    vfs {
      deferred = true
      languages.addAll(langs)

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
            logging.trace("Checking bundle at URI '{}'", uri)
            File(uri)
          } catch (_: IOException) {
            throw ShellError.BUNDLE_NOT_FOUND.asError()
          }

          logging.trace("Checking existence of '{}'", uri)
          if (!file.exists()) throw ShellError.BUNDLE_NOT_FOUND.asError()

          logging.trace("Checking readability of '{}'", uri)
          if (!file.canRead()) throw ShellError.BUNDLE_NOT_ALLOWED.asError()

          logging.debug("Mounting guest filesystem at URI: '{}'", uri)
          include(uri)
        }
      }

      // register listeners
      vfsListeners.get().forEach(::listener)
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    logging.debug("Shell/run command invoked")
    Elide.requestNatives(server = true, tooling = !skipInstall)

    // resolve project configuration (async)
    val projectResolution = launch {
      activeProject.value = runCatching {
        projectManager.resolveProject(projectOptions().projectPath)
      }.getOrNull()
    }

    // begin resolving language support
    val supported = determineSupportedLanguages()
    val allSupported = EnumSet.copyOf(supported.map { it.first })
    if (languages) {
      logging.debug("User asked for a list of supported languages. Printing and exiting.")
      out.line("Supported languages: ${allSupported.joinToString(", ")}")
      return success()
    }

    logging.trace("All supported languages: ${allSupported.joinToString(", ") { it.id }}")
    val supportedEnginesAndLangs = supported.flatMap { listOf(it.first.engine, it.first.id) }.toSortedSet()
    val allSupportedLangs = EnumSet.copyOf(supported.map { it.first }.toSortedSet())
    val langs = resolveLangs(languageHint)

    // make sure each requested language is supported
    langs.forEach {
      if (!supportedEnginesAndLangs.contains(it.engine))
        throw ShellError.LANGUAGE_NOT_SUPPORTED.asError("Language at ID '${it.id}' is not supported")
    }

    logging.debug("Initializing language contexts (${langs.joinToString(", ") { it.id }})")

    if (!executeLiteral && !useStdin && runnable == null) {
      // if no script inputs are specified, we are entering an interactive session. in this case, we should notify the
      // JS engine, so that it can apply relevant options.
      System.setProperty("vm.interactive", "true")
    }
    val experimentalLangs = allSupportedLangs.filter {
      it.experimental && !it.suppressExperimentalWarning
    }
    val primaryLang: (File?) -> GuestLanguage = { target ->
      resolvePrimaryLanguage(
        language,
        allSupportedLangs,
        when {
          // can't guess the language of a script from `stdin`
          useStdin || executeLiteral -> null

          // in `serve` mode, or with a `runnable`, we return `runnable` (script is required)
          else -> target
        },
      )
    }
    val onByDefaultLangs = EnumSet.copyOf(
      allSupportedLangs.filter {
        it.onByDefault || (!executeLiteral && runnable != null && it.extensions.any { runnable!!.endsWith(it) })
      },
    )

    // if no entrypoint was specified, attempt to use the one in the project manifest, or try resolving the runnable as
    // a script name in either `elide.pkl` or a foreign manifest.
    when (val tgt = runnable) {
      null -> {
        projectResolution.join()

        // @TODO ability to select an entrypoint

        // make sure we don't write this in test mode, which gathers tests instead of running
        if (!testMode()) {
          runnable = activeProject.value?.manifest?.entrypoint?.first()?.let {
            // resolve against the current directory before assigning
            val path = Path.of(it)

            if (path.isAbsolute) {
              // absolute paths go right through
              it
            } else {
              // otherwise, resolve against the project, not cwd by default
              activeProject.value!!.root.resolve(path).absolutePathString()
            }
          }
        }
      }

      // if we have a runnable that is a simple string, maybe it's mapped to a script name?
      else -> if (tgt.isNotEmpty() && '.' !in tgt) {
        projectResolution.join()
        (when (val project = activeProject.value) {
          // `null` triggers foreign manifest fallback behavior
          null -> null

          // check for a script mapped in `elide.pkl` or fall back to foreign manifests
          else -> if (tgt !in project.manifest.scripts) null else {
            // in this case, we have a `runnable` script name, as in `elide check` with `["check"] = "..."` mapped in
            // the project configuration. this is a corner-case where we should short-circuit file resolution and simply
            // delegate to this method instead.
            requireNotNull(project.manifest.scripts[tgt]) to project.manifest
          }
        } ?: manifests.resolveToTask(
          tgt,
          allSupportedLangs,
        ))?.let { (found, manifest) ->
          // we have found a script to run, as a string; this either originates from the first branch above, which pulls
          // from `elide.pkl`, or from the foreign manifest fallback, which pulls from things like `package.json`.
          logging.debug { "Running '$tgt' from manifest '$manifest'" }
          when (manifest) {
            // if the task originates from an elide package manifest, we don't need a delegated runner.
            is ElidePackageManifest -> null

            // based on the manifest it came from, try to resolve a delegated runner.
            else -> delegatedRunner(manifest.ecosystem)
          }.let { delegate ->
            return when (delegate) {
              // with no delegated runner, we are now free to spawn the task and delegate to it as the command.
              null -> stringToTask(found).let { task ->
                emitCommand(task)
                delegateTask(task)
              }

              // allow the delegated runner to transform the arguments, as needed; then, spawn the task and delegate to
              // it as normal.
              else -> stringToTask(found).apply {
                args = delegate.transform(args)
                env = delegate.transform(env)
              }.let { task ->
                emitCommand(task)
                delegateTask(task)
              }
            }
          }
        }
      }
    }

    // maybe it's just a project binary, but not mapped as a script?
    val binReturn = runnable?.let { maybeBinary ->
      allProjectPaths.map { it.resolve(maybeBinary) }.firstOrNull {
        Files.exists(it) && Files.isExecutable(it)
      }?.let { resolvedBinary ->
        val runnableArgs = Statics.args.drop(1)
        val binpath = resolvedBinary.absolute()

        delegateTask(subprocess(binpath) {
          // delegate streams by default
          streams = ProcessRunner.StdStreams.Defaults

          // by default, the system env is in use
          env = Environment.HostEnv

          // activate shell support
          options = ProcessRunner.ProcessOptions(shell = ProcessRunner.ProcessShell.Active)

          // copy in the provided arguments
          runnableArgs.takeIf { it.isNotEmpty() }?.let { arguments ->
            args.addAllStrings(arguments)
          }
        })
      }
    }
    when (binReturn) {
      // if null, we proceed
      null -> {}
      else -> return binReturn
    }

    try {
      resolveEngine(onByDefaultLangs).unwrap().use {
        withDeferredContext(onByDefaultLangs) {
          // warn about experimental status, as applicable
          if (verbose && experimentalLangs.isNotEmpty()) {
            logging.warn(
              "Caution: Support for ${experimentalLangs.joinToString(", ") { i -> i.formalName }} " +
                      "considered experimental.",
            )
          }

          val testOrServeMode = testMode() || serveMode()
          when (val scriptTargetOrCode = runnable) {
            // run in interactive mode
            null, "-" -> if (useStdin || runnable == "-") {
              // activate interactive behavior
              if (!testOrServeMode) {
                enableInteractive()
              }

              // consume from stdin
              primaryLang(null).let { lang ->
                input.buffer.use { buffer ->
                  executeSource(
                    "from stdin",
                    langs,
                    lang,
                    it,
                    Source.newBuilder(lang.symbol, buffer, "stdin")
                      .cached(false)
                      .buildLiteral(),
                  )
                }
              }
            } else if (!testOrServeMode) {
              logging.debug("Beginning interactive guest session")
              enableInteractive()
              beginInteractiveSession(
                langs,
                primaryLang(null),
                engine(),
                it,
              )
            } else when {
              // trigger a test-run with no sources; this also triggers, by side effect, test discovery.
              testMode() -> readRunTests(
                "tests",
                langs,
                primaryLang(null),
                it,
                emptyList(),
                guestExec,
              )

              else -> logging.error("To run a server, pass a file, or code via stdin or `-c`")
            }

            // run a script as a file, or perhaps a string literal
            else -> if (executeLiteral) {
              logging.trace("Interpreting runnable parameter as code")
              executeSingleStatement(
                langs,
                primaryLang(null),
                it,
                scriptTargetOrCode,
              )
            } else {
              // no literal execution flag = we need to parse `runnable` as a file path
              logging.trace("Interpreting runnable parameter as file path (`--code` was not passed)")
              File(scriptTargetOrCode).let { scriptFile ->
                primaryLang.invoke(scriptFile).let { lang ->
                  when (lang.executionMode) {
                    // if this engine supports direct execution of source files, execute it that way
                    ExecutionMode.SOURCE_DIRECT -> executeSource(
                      scriptFile.name,
                      langs,
                      lang,
                      it,
                      readExecutableScript(
                        allSupportedLangs,
                        langs,
                        lang,
                        scriptFile,
                      ),
                    )

                    // otherwise, if we need to "compile" this source first (as is the case for LLVM targets and JVM
                    // targets like Java and Kotlin), then conduct that phase and load a symbol to begin execution.
                    ExecutionMode.SOURCE_COMPILED -> executeCompiled(
                      scriptFile,
                      langs,
                      lang,
                      it,
                      compileEntrypoint(
                        lang,
                        it,
                        scriptFile,
                      ),
                    )
                  }
                }
              }
            }
          }
        }

        // don't exit if we have a running server
        if (serverRunning.value) {
          // wait for all tasks to arrive
          logging.debug("Waiting for long-lived tasks to arrive")
          phaser.value.arriveAndAwaitAdvance()
          logging.debug("Exiting server context")
        }
        engine().unwrap().let { engine ->
          val terminationPeriod = (commons().timeoutSeconds).seconds
          logging.debug { "Preparing to shut down execution (termination period: $terminationPeriod)" }

          try {
            logging.trace { "Triggering graceful engine shutdown" }
            engine.close()
          } catch (exc: IllegalStateException) {
            logging.trace { "Shutdown produced ISE: probably still executing (message: '${exc.message}')" }

            try {
              logging.trace { "Awaiting termination for guest execution" }
              val startedAwaiting = System.currentTimeMillis()
              guestExec.executor().awaitTermination(terminationPeriod.toJavaDuration())
              logging.trace { "Guest executor terminated, re-closing engine" }

              runCatching {
                engine.close()
              }.onFailure {
                // only sleep for any remaining time in timeout
                val remaining = terminationPeriod - (System.currentTimeMillis() - startedAwaiting).milliseconds
                Thread.sleep(remaining.inWholeMilliseconds)
                logging.trace { "Shutdown trace period exceeded; shutting down forcefully" }
                engine.close(/* cancelIfExecuting = */ true)
                logging.trace { "Engine forcefully terminated" }
              }.onSuccess {
                logging.trace { "Engine shut down on second call" }
              }
            } catch (_: TimeoutException) {
              logging.trace { "Shutdown trace period exceeded; shutting down forcefully" }

              // we have exceeded our timeout; so, forcefully cancel execution, at the executor first, and then via the
              // guest execution engine.
              guestExec.executor().shutdownNow()
              logging.trace { "Guest executor forcefully terminated" }
              engine.close(/* cancelIfExecuting = */ true)
              logging.trace { "Guest engine forcefully terminated" }
            }
          }
        }
        logging.trace { "Exiting with successful code" }
        return success()
      }
    } catch (err: AbstractToolError.Known) {
      when (err.case) {
        ShellError.FILE_NOT_FOUND -> {
          logging.warn("File not found: '$runnable'")
          return err("File not found", exitCode = 1)
        }

        ShellError.FILE_NOT_READABLE -> {
          logging.warn("File not readable: '$runnable'")
          return err("File not readable", exitCode = 1)
        }

        ShellError.NOT_A_FILE -> {
          logging.warn("Not a file: '$runnable'")
          return err("Not a file", exitCode = 1)
        }

        ShellError.FILE_TYPE_MISMATCH -> {
          logging.warn("File type mismatch: '$runnable'")
          return err("File type mismatch", exitCode = 1)
        }

        else -> throw err
      }
    }
  }
}

private fun PackageManifestService.resolveToTask(
  taskName: String,
  langs: Set<GuestLanguage>,
): Pair<String, PackageManifest>? {
  val selfDir = Path(System.getProperty("user.dir"))

  // try npm
  val npmPath = resolve(selfDir, ProjectEcosystem.Node)
  if (Files.exists(npmPath) && Files.isRegularFile(npmPath) && Files.isReadable(npmPath)) {
    val parsed = Files.newInputStream(npmPath).use { parse(it, ProjectEcosystem.Node) } as NodePackageManifest
    parsed.scripts?.let { packageScripts ->
      if (taskName in packageScripts) {
        return requireNotNull(packageScripts[taskName]) to parsed
      }
    }
  }
  return null
}
