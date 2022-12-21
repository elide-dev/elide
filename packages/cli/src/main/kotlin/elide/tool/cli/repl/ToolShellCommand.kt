package elide.tool.cli.repl

import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.GuestLanguage
import elide.tool.cli.ToolState
import elide.tool.cli.err.ShellError
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.management.ExecutionEvent
import org.graalvm.polyglot.management.ExecutionListener
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.EnumSet
import java.util.Scanner
import kotlin.io.path.Path

/** TBD. */
@Command(
  name = "run",
  aliases = ["shell", "repl"],
  description = ["%nRun a script or an interactive shell in a given language"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  synopsisHeading = "",
  customSynopsis = [
    "Usage:  elide @|bold,fg(cyan) run|@ [OPTIONS] FILE                      (1st form, script from file)",
    "   or:  elide @|bold,fg(cyan) run|@ [OPTIONS] @|bold,fg(cyan) --stdin|@                   (2nd form, script from stdin)",
    "   or:  elide @|bold,fg(cyan) run|@ [OPTIONS] [@|bold,fg(cyan) -c|@|@|bold,fg(cyan) --code|@ CODE]          (3rd form, execute literal)",
    "   or:  elide [@|bold,fg(cyan) shell|@|@|bold,fg(cyan) repl|@] [OPTIONS]                  (4th form, interactive)",
    "   or:  elide [@|bold,fg(cyan) shell|@|@|bold,fg(cyan) repl|@] --js [OPTIONS]             (5th form, interactive)",
    "   or:  elide [@|bold,fg(cyan) shell|@|@|bold,fg(cyan) repl|@] --languages",
    "   or:  elide [@|bold,fg(cyan) shell|@|@|bold,fg(cyan) repl|@] --language=[@|bold,fg(green) JS|@] [OPTIONS]",
  ]
)
@Singleton internal class ToolShellCommand : AbstractSubcommand<ToolState>() {
  internal companion object {
    private val logging: Logger = Logging.of(ToolShellCommand::class)
  }

  /** Allows selecting a language by name. */
  class LanguageSelector {
    /** Specifies the guest language to run. */
    @Option(
      names = ["--language", "-l"],
      description = ["Specify language by name. Options: \${COMPLETION-CANDIDATES}."],
      defaultValue = "JS",
    )
    internal var language: GuestLanguage = GuestLanguage.JS

    /** Alias flag for a JavaScript VM. */
    @Option(
      names = ["--js", "--javascript"],
      description = ["Equivalent to passing '--language=JS'."],
    )
    internal var javascript: Boolean = false

    // Resolve the specified language.
    internal fun resolve(): GuestLanguage = GuestLanguage.JS
  }

  /** Settings which apply to JavaScript only. */
  class JavaScriptSettings {
    /** Whether to activate JS strict mode. */
    @Option(
      names = ["--js:strict"],
      description = ["Activate JavaScript strict mode"],
      defaultValue = "true",
    )
    internal var strict: Boolean = true

    /** Whether to activate JS strict mode. */
    @Option(
      names = ["--js:ecma"],
      description = ["ECMA standard to use for JavaScript."],
      defaultValue = "ES2022",
    )
    internal var ecma: JsLanguageLevel = JsLanguageLevel.ES2022

    /** Whether to activate NPM support. */
    @Option(
      names = ["--js:npm"],
      description = ["Whether to enable NPM support. Experimental."],
      defaultValue = "false",
    )
    internal var nodeModules: Boolean = false

    /** Whether to activate NPM support. */
    @Option(
      names = ["--js:esm"],
      description = ["Whether to enable ESM support. Experimental."],
      defaultValue = "false",
    )
    internal var esm: Boolean = false
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

  /** Host access settings. */
  class HostAccessSettings {
    /** Whether to activate NPM support. */
    @Option(
      names = ["--allow-all"],
      description = ["Whether to allow host access."],
      defaultValue = "true",
    )
    internal var allowAll: Boolean = true

    /** Apply access control settings to the target [context]. */
    fun apply(context: VMContext.Builder) {
      if (allowAll) context.allowAllAccess(true)
    }

    companion object {
      /** Defaults for host access. */
      val DEFAULTS = HostAccessSettings()
    }
  }

  /** Host access settings. */
  @ArgGroup(
    validate = false,
    heading = "%nAccess Control:%n",
  ) internal var accessControl: HostAccessSettings = HostAccessSettings.DEFAULTS

  /** Language selector. */
  @ArgGroup(
    exclusive = true,
    heading = "%nLanguage Selection:%n",
  ) internal var language: LanguageSelector = LanguageSelector()

  /** Settings specific to JavaScript. */
  @ArgGroup(
    validate = false,
    heading = "%nLanguage: JavaScript%n",
  ) internal var jsSettings: JavaScriptSettings = JavaScriptSettings()

  /** File to run within the VM. */
  @Parameters(
    index = "0",
    arity = "0..1",
    paramLabel = "FILE|CODE",
    description = [
      "File or snippet to run. If `-c|--code` is passed, interpreted" +
      "as a snippet. If not specified, an interactive shell is started."
    ],
  )
  internal var runnable: String? = null

  // Executed when a guest statement is entered.
  private fun onStatementEnter(event: ExecutionEvent) {
    println(event.location.characters)
  }

  // Execute a single chunk of code, or literal statement.
  @Suppress("SameParameterValue") private fun executeOneChunk(
    language: GuestLanguage,
    ctx: VMContext,
    origin: String,
    code: String,
    interactive: Boolean = false,
    literal: Boolean = false,
  ): Value {
    // build a source code chunk from the line
    val chunk = Source.newBuilder(language.id, code, origin)
      .interactive(interactive)

    val source = if (literal) {
      chunk.buildLiteral()
    } else {
      chunk.build()
    }

    logging.trace("Code chunk built. Evaluating")
    return try {
      ctx.enter()
      val value = ctx.eval(source)
      logging.trace("Code chunk evaluation complete")
      value
    } finally {
      ctx.leave()
    }
  }

  // Execute a single chunk of code as a literal statement.
  private fun executeSingleStatement(language: GuestLanguage, ctx: VMContext, code: String) {
    ExecutionListener.newBuilder().onEnter(::onStatementEnter).statements(true).attach(ctx.engine).use {
      executeOneChunk(language, ctx, "stdin", code, interactive = false, literal = false)
    }
  }

  // Wrap an interactive REPL session in exit protection.
  private fun beginInteractiveSession(language: GuestLanguage, ctx: VMContext) {
    // execution step listener
    ExecutionListener.newBuilder().onEnter(::onStatementEnter).statements(true).attach(ctx.engine).use {
      // watch for input until cancelled
      val inbuf = Scanner(input.stdin)

      while (true) {
        try {
          print("elide (${language.id})> ")
          val line = inbuf.nextLine() ?: break  // exit on empty line
          logging.debug("Source line received; executing as statement")
          logging.trace { "Code provided: '$line'" }

          // build a source code chunk from the line
          executeOneChunk(
            language,
            ctx,
            "<shell:${language.id}>",
            line,
            interactive = true,
            literal = true,
          )

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
            exc.printStackTrace()  // otherwise print exception
            throw ShellError.USER_CODE_ERROR.asError()
          }
        } catch (interrupt: InterruptedException) {
          logging.debug("Session interrupted; concluding")
          break  // exit on interrupt
        }
      }
    }
  }

  // Read an executable script file and return it as a `File` and a `Source`.
  private fun readExecutableScript(language: GuestLanguage, script: File): Source {
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
    if (!language.extensions.contains(script.extension)) {
      logging.debug("Script fail: Extension is not present in language allowed extensions")

      val contentType = Files.probeContentType(Path(script.path))
      if (!language.mimeTypes.contains(contentType)) {
        logging.debug("Script fallback fail: Mime type is also not in allowed set for language. Rejecting.")
        throw ShellError.FILE_TYPE_MISMATCH.asError()
      } else {
        logging.trace("Script check: File mimetype matches")
      }
    } else {
      logging.trace("Script check: File extension matches")
    }

    return Source.newBuilder(language.id, script)
      .encoding(StandardCharsets.UTF_8)
      .build()
  }

  // Read an executable script, and then execute the script.
  private fun readExecuteCode(label: String, language: GuestLanguage, ctx: VMContext, source: Source) {
    try {
      // enter VM context
      ctx.enter()
      logging.trace("Entered VM for script execution (language: ${language.id}). Consuming script from: '$label'")

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
      logging.debug("User code exception caught", exc)
      throw ShellError.USER_CODE_ERROR.asError()
    } finally {
      ctx.leave()
    }
  }

  /** @inheritDoc */
  override fun invoke(context: ToolContext<ToolState>) {
    logging.debug("Shell/run command invoked")
    val supported = determineSupportedLanguages()
    val allSupported = EnumSet.copyOf(supported.map { it.first })
    if (languages) {
      logging.debug("User asked for a list of supported languages. Printing and exiting.")
      out.line("Supported languages: ${allSupported.joinToString(", ")}")
      return
    }

    val lang = language.resolve()
    logging.trace("All supported languages: ${allSupported.joinToString(", ") { it.id }}")
    val engineLang = supported.find { it.first == lang }?.second ?: throw ShellError.LANGUAGE_NOT_SUPPORTED.asError()
    logging.debug("Initializing language context ('${lang.id}')")

    withVM(context, lang, accessControl::apply) {
      // warn about experimental status, as applicable
      logging.warn("Caution: Elide support for ${engineLang.name} is considered experimental.")

      when (val scriptTargetOrCode = runnable) {
        // run in interactive mode
        null -> if (useStdin) {
          // consume from stdin
          input.buffer.use { buffer ->
            readExecuteCode(
              "from stdin",
              lang,
              it,
              Source.create(lang.id, buffer.readText())
            )
          }
        } else {
          logging.debug("Beginning interactive guest session")
          beginInteractiveSession(lang, it)
        }

        // run a script as a file, or perhaps a string literal
        else -> if (executeLiteral) {
          logging.trace("Interpreting runnable parameter as code")
          executeSingleStatement(
            lang,
            it,
            scriptTargetOrCode,
          )
        } else {
          // no literal execute flag = we need to parse `runnable` as a file path
          logging.trace("Interpreting runnable parameter as file path (`--code` was not passed)")
          File(scriptTargetOrCode).let { scriptFile ->
            logging.debug("Beginning script execution")
            readExecuteCode(
              scriptFile.name,
              lang,
              it,
              readExecutableScript(lang, scriptFile),
            )
          }
        }
      }
    }
  }
}
