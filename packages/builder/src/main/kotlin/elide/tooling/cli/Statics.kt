package elide.tooling.cli

import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import elide.runtime.Logger
import elide.runtime.Logging
import kotlinx.atomicfu.atomic
import org.graalvm.nativeimage.ImageInfo
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

/** Internal static tools and utilities used across the Elide CLI. */
public object Statics {
  public val disableStreams: Boolean = System.getProperty("elide.disableStreams") == "true"
  private val delegatedInStream = atomic<InputStream?>(null)
  private val delegatedOutStream = atomic<PrintStream?>(null)
  private val delegatedErrStream = atomic<PrintStream?>(null)

  /** Main tool logger. */
  public val logging: Logger by lazy {
    Logging.named("tool")
  }

  /** Server tool logger. */
  public val serverLogger: Logger by lazy {
    Logging.named("tool:server")
  }

  /** Whether to disable color output and syntax highlighting. */
  public val noColor: Boolean by lazy {
    System.getenv("NO_COLOR") != null || args.let { args ->
      args.contains("--no-pretty") || args.contains("--no-color")
    }
  }

  @Volatile private var execBinPath: String = ""

  @Volatile private var initialArgs = emptyArray<String>()

  /** Terminal theme access. */
  private val terminalTheme: Theme by lazy {
      Theme(from = Theme.Default) {
          styles["markdown.code.span"] = TextStyles.bold.style
          styles["markdown.link"] = TextStyles.underline.style
      }
  }

  /** Main terminal access. */
  public val terminal: Terminal by lazy {
      Terminal(theme = terminalTheme)
  }

  /** Invocation args. */
  public val args: Array<String> get() = initialArgs

  public val bin: String get() = execBinPath
  public val binPath: Path by lazy { Paths.get(bin).absolute() }
  public val elideHome: Path by lazy {
    if (ImageInfo.inImageCode()) {
      binPath.parent
    } else {
      // the bin path on JVM points to the JVM itself
      (System.getProperty("elide.home") ?: requireNotNull(System.getenv("HOME")) {
        "Failed to resolve ${'$'}HOME variable"
      }).let { Paths.get(it) }.resolve("elide").absolute()
    }
  }

  public val resourcesPath: Path by lazy {
    elideHome.resolve("resources").absolute()
  }

  // Stream which drops all data.
  private val noOpStream by lazy {
      PrintStream(object : OutputStream() {
          override fun write(b: Int) = Unit
      })
  }

  public val `in`: InputStream
      get() =
    delegatedInStream.value ?: System.`in`

  @JvmField public var out: PrintStream =
    when (disableStreams) {
      true -> noOpStream
      else -> delegatedOutStream.value ?: System.out
    }

  @JvmField public var err: PrintStream =
    when (disableStreams) {
      true -> noOpStream
      else -> delegatedErrStream.value ?: System.err
    }

  public fun mountArgs(bin: String, args: Array<String>) {
    check(initialArgs.isEmpty()) { "Args are not initialized yet!" }
    execBinPath = bin
    initialArgs = args
  }

  public fun assignStreams(out: PrintStream, err: PrintStream, `in`: InputStream) {
    if (disableStreams) return
    delegatedOutStream.value = out
    delegatedErrStream.value = err
    delegatedInStream.value = `in`
  }
}
