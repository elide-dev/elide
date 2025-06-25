package elide.tool.err

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.arthenica.smartexception.java9.Exceptions
import com.github.ajalt.mordant.rendering.TextAlign.NONE
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.Whitespace.NORMAL
import com.github.ajalt.mordant.terminal.Terminal
import org.graalvm.polyglot.PolyglotException
import java.util.function.Supplier
import elide.tooling.cli.Statics

/**
 * Internal utility for formatting exceptions nicely.
 */
object ErrPrinter {
  private const val ENABLE_COLOR_SUPPORT = false
  internal const val DEFAULT_MAX_WIDTH = 120
  private val classpathCiteRef = Regex("at ([a-zA-Z.:()0-9]*.)$")
  private val classFnSpec = Regex(
    "([a-zA-Z].*)\\.([a-zA-Z]{1,90})\\(([a-zA-Z0-9]{1,50}\\.[a-zA-Z0-9]{1,20}):([0-9]{1,7})\\)"
  )

  // Reset format state.
  private val resetFmt get() = reset

  // Muted style for reference text.
  private val muted get() = (dim)

  // Underline for filename references.
  private val filename get() = (underline)

  // Dim cyan for line numbers.
  private val lineRef get() = (dim + cyan)

  // Wrap a string in a style, if enabled.
  private fun ErrPrinterSettings.style(style: TextStyles, text: String): String =
    if (!useColor) text else term.render(style(text), NORMAL, NONE)

  // Wrap a string in a style, if enabled.
  private fun ErrPrinterSettings.style(style: TextStyle, text: String): String =
    if (!useColor) text else term.render(style(text), NORMAL, NONE)

  /**
   * Settings to apply to the error printer.
   */
  data class ErrPrinterSettings(
    val enableColor: Boolean = true,
    val shortenPackages: Boolean = true,
    val maxLength: Int = DEFAULT_MAX_WIDTH,
    val terminal: Supplier<Terminal> = Supplier { Statics.terminal },
  ) {
    // Whether color output should be used.
    internal val useColor: Boolean get() = enableColor && ENABLE_COLOR_SUPPORT

    // Cached terminal accessor.
    internal val term: Terminal by lazy { terminal.get() }
  }

  // Default settings if none are provided.
  private val DEFAULT_SETTINGS = ErrPrinterSettings()

  init {
    // -- Ignore Packages
    // Describes packages which are omitted from stack traces by default.
    listOf(
      "picocli",
      "jdk.internal.reflect",
      "kotlin.coroutines",
      "kotlin.coroutines.intrinsics",
      "kotlin.coroutines.jvm.internal",
      "kotlinx.coroutines",
      "elide.tool.cli",
      "java.util.concurrent",
      "java.base/java.util.concurrent",
      "org.graalvm.polyglot",
    ).forEach {
      Exceptions.registerIgnorePackage(it, false)
    }

    // -- Group Packages
    // Describes packages which are grouped together in stack traces.
    listOf(
      "com.oracle.truffle.js.parser",
      "elide.runtime.lang.javascript",
    ).map {
      Exceptions.registerGroupPackage(it)
    }
  }

  // Shorten a class reference.
  private fun shortenClassReferenceLine(settings: ErrPrinterSettings, line: String): String {
    // like:
    // `package.here.SomeClass.someMethod(SomeFile.java:123)`
    val refSpec = classFnSpec.matchEntire(line.removePrefix("\tat ")) ?: return line
    val (clsQualified, fn, file, lineNum) = refSpec.destructured
    val clspkg = clsQualified.substringBeforeLast(".")
    val clsname = clsQualified.substringAfterLast(".")
    // --- begin rewriting -- //
    val clsref = if (!settings.shortenPackages) clspkg else {
      clspkg.split('.').map { it.first() }.joinToString(".")
    }
    return StringBuilder().apply {
      settings.apply {
        append(" ".repeat(4))
        append(style(muted, "at"))
        append(" ")
        append(style(muted, clsref))
        append(style(muted, "."))
        append(clsname)
        append(".")
        append(fn)
        append("(")
        append(style(filename, file))
        append(":")
        append(style(lineRef, lineNum))
        append(")")
      }
    }.toString()
  }

  // Apply standard string fixes.
  private fun String.stdTransforms(settings: ErrPrinterSettings): String = let {
    // forcibly trim the message if it exceeds the max length
    if (it.length >= settings.maxLength) {
      it.substring(0, settings.maxLength - 10) + settings.style(resetFmt, " ...")
    } else it
  }.replace("\t", "    ")

  // Format a line.
  private fun fmtLine(settings: ErrPrinterSettings, line: String): String {
    return when (line.drop(1).matches(classpathCiteRef)) {
      // no match: it's not a class/fn reference line
      false -> fmtMessage(line, settings)

      // match: it's a class/fn reference line, shorten the class package path
      else -> shortenClassReferenceLine(settings, line).stdTransforms(settings)
    }
  }

  // Format using default logic.
  private fun fmtDefault(settings: ErrPrinterSettings, exc: Throwable): StringBuilder = StringBuilder().apply {
    Exceptions.getStackTraceString(exc).split("\n").map { line ->
      fmtLine(settings, line)
    }.map {
      appendLine(it)
    }
  }

  // Format a polyglot exception.
  private fun fmtPolyglot(settings: ErrPrinterSettings, exc: PolyglotException): StringBuilder =
    fmtDefault(settings, exc)

  /**
   * Pretty-format an exception; this includes:
   *
   * - Grouping or reducing package paths
   * - Removing built-in Java or Kotlin frames
   */
  fun fmt(throwable: Throwable, settings: ErrPrinterSettings = DEFAULT_SETTINGS): StringBuilder = when (throwable) {
    // special handling for cross-lang exceptions
    is PolyglotException -> fmtPolyglot(settings, throwable)

    // otherwise, format into a string builder and return
    else -> fmtDefault(settings, throwable)
  }

  /**
   * Pretty-format an exception; this includes:
   *
   * - Grouping or reducing package paths
   * - Removing built-in Java or Kotlin frames
   */
  fun print(throwable: Throwable, settings: ErrPrinterSettings = DEFAULT_SETTINGS): Unit = settings
    .term
    .println(
      fmt(throwable, settings).toString()
    )

  /**
   * Format a raw string message by replacing well-known tokens.
   *
   * @param message The message to format.
   * @return The formatted message.
   */
  fun fmtMessage(message: String, settings: ErrPrinterSettings = DEFAULT_SETTINGS): String = message
    .replace("org.graalvm.polyglot.PolyglotException: ", "")
    .replace(" because ", " ... ")
    .replace("java.lang.", "")
    .stdTransforms(settings)
}
