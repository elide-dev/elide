package elide.tool.ssg

import com.google.common.annotations.VisibleForTesting
import elide.tool.ssg.SiteCompilerParams as CompilerParams
import elide.tool.ssg.SiteCompilerParams.Options
import elide.tool.ssg.SiteCompileResult as CompileResult
import com.google.errorprone.annotations.CanIgnoreReturnValue
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tool.ssg.SiteCompilerParams
import elide.tool.ssg.cfg.ElideSSGCompiler.ELIDE_TOOL_VERSION
import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import tools.elide.data.CompressionMode
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

/** Entrypoint for the site compiler command-line tool. */
@Command(
  name = "ssg",
  description = ["Compile a static site from an Elide application."],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
)
@Suppress("MemberVisibilityCanBePrivate")
public class SiteCompiler internal constructor () : Runnable {
  public companion object {
    /** CLI entrypoint and [args]. */
    @JvmStatic public fun main(args: Array<String>): Unit = exitProcess(exec(args))

    /** @return Tool version. */
    @JvmStatic public fun version(): String = ELIDE_TOOL_VERSION

    /**
     * Programmatic entrypoint for the SSG compiler; equivalent to invoking [main].
     *
     * @param manifest Filesystem path to the manifest which should be read and interpreted.
     * @param target File path to the application JAR which should be launched, or HTTP URL if running in HTP mode.
     * @param output Output settings for this compiler run.
     * @param options Options to apply to this compile execution.
     * @param logging Logger to use for compiler messages.
     * @param dispatcher Co-routine dispatcher to use, as applicable.
     * @return Compiler result.
     */
    @JvmStatic public fun compile(
      manifest: String,
      target: String,
      output: CompilerParams.Output,
      options: Options,
      logging: Logger? = null,
      dispatcher: CoroutineDispatcher? = null,
    ): CompileResult {
      return SiteCompiler().apply {
        val ioDispatcher = dispatcher ?: Dispatchers.IO
        // setup default actors
        manifestReader = FilesystemManifestReader(ioDispatcher)
        requestFactory = DefaultRequestFactory()
        contentReader = DefaultAppStaticReader()
        contentWriter = DefaultAppStaticWriter(ioDispatcher)
        appLoader = DefaultAppLoader(contentReader, ioDispatcher)
        compiler = DefaultAppStaticCompiler(ioDispatcher)

        // configure the compiler
        configure(logging = logging, params = CompilerParams(
          target = target,
          output = output,
          manifest = manifest,
          options = options.copy(
            httpMode = options.httpMode,
          ),
        ))
      }.compile()
    }

    // Wrap the tool execution with exception protection.
    private suspend fun <R> executeTool(logging: Logger, op: suspend () -> R): R {
      return try {
        op.invoke()
      } catch (err: SSGCompilerError) {
        logging.error("SSG compiler failed: error of type '${err::class.simpleName}'. ${err.message}", err)
        throw err
      } catch (thr: Throwable) {
        // translate to generic SSG compiler error
        logging.error("SSG compiler failed with unhandled exception: ${thr.message}", thr)
        throw SSGCompilerError.Generic(thr)
      }
    }

    // Private execution entrypoint for customizing core Picocli settings.
    @JvmStatic @VisibleForTesting internal fun exec(args: Array<String>): Int {
      return ApplicationContext.builder().start().use {
        CommandLine(SiteCompiler::class.java, MicronautFactory(it))
          .setUsageHelpAutoWidth(true)
          .execute(*args)
      }
    }
  }

  // Logger.
  private lateinit var logging: Logger

  // Compiler params.
  internal lateinit var params: CompilerParams

  // Whether parameters have been parsed.
  internal val paramsAvailable: AtomicBoolean = AtomicBoolean(false)

  // Compiler params.
  internal var appTarget: AtomicReference<URL> = AtomicReference(null)

  // Whether the app target is available.
  internal val appTargetAvailable: AtomicBoolean = AtomicBoolean(false)

  // Compile result.
  private val result: AtomicReference<CompileResult> = AtomicReference(null)

  // Whether compile results are available.
  private val resultAvailable: AtomicBoolean = AtomicBoolean(false)

  // Manifest reader implementation.
  @Inject internal lateinit var manifestReader: ManifestReader

  // App runner implementation.
  @Inject internal lateinit var appLoader: AppLoader

  // Static content reader implementation.
  @Inject internal lateinit var contentReader: StaticContentReader

  // Static content writer implementation.
  @Inject internal lateinit var contentWriter: AppStaticWriter

  // Request factory implementation.
  @Inject internal lateinit var requestFactory: RequestFactory

  // Site compiler logic implementation.
  @Inject internal lateinit var compiler: AppStaticCompiler

  /** Verbose logging mode. */
  @Option(
    names = ["-v", "--verbose"],
    description = ["Activate verbose logging."],
  )
  internal var verbose: Boolean = false

  /** Debug mode. */
  @Option(
    names = ["--debug"],
    description = ["Activate debug mode/wait for a debugger."],
  )
  internal var debug: Boolean = false

  /** Classpath to apply. */
  @Option(
    names = ["--classpath"],
    description = ["Add JARs to the application execution classpath."],
  )
  internal var classpath: String? = null

  /** HTTP fetch mode. */
  @Suppress("HttpUrlsUsage")
  @Option(
    names = ["--http"],
    negatable = true,
    description = ["Activate HTTP mode. Automatically active if `target` starts with `http://` or `https://`."],
    defaultValue = "false",
  )
  internal var httpMode: Boolean = false

  /** Ignore certificate errors. */
  @Option(
    names = ["--ignore-cert-errors"],
    negatable = true,
    description = ["Ignore certificate validation errors. CAREFUL: This can be dangerous."],
    defaultValue = "false",
  )
  internal var ignoreCertErrors: Boolean = false

  /** Crawl returned HTML. */
  @Option(
    names = ["--crawl"],
    negatable = true,
    description = ["Whether to crawl returned HTML for additional URLs. Defaults to `false`."],
    defaultValue = "false",
  )
  internal var crawl: Boolean = false

  /** Whether to activate pretty logging; on by default. */
  @Option(
    names = ["--pretty"],
    negatable = true,
    description = ["Timeout to apply to application requests. Expressed in seconds, defaults to 30s."],
    defaultValue = "true",
  )
  internal var pretty: Boolean = false

  /** Crawl returned HTML. */
  @Option(
    names = ["--precompress"],
    description = [
      "Whether to pre-compress eligible outputs with GZIP or Brotli. Pass `GZIP` or `BROTLI`, zero or more times."
    ],
  )
  internal var precompress: Set<CompressionMode> = emptySet()

  /** Extra allowable origins. */
  @Option(
    names = ["--allow-origin"],
    description = ["Extra origins to allow downloads from."],
  )
  internal var extraOrigins: Set<String> = emptySet()

  /** Request timeout value to apply. */
  @Option(
    names = ["--timeout"],
    description = ["Timeout to apply to application requests. Expressed in seconds, defaults to 30s."],
    defaultValue = "30",
  )
  internal var timeout: Int = Options.DEFAULT_REQUEST_TIMEOUT

  /** Location of the manifest to read. */
  @Parameters(
    index = "0",
    description = ["Location of the app manifest to read. Prefix with `classpath:` or provide a file system path."],
  )
  internal lateinit var manifest: String

  /** Location of the app JAR to execute requests against, or URL to execute against if [httpMode] is active. */
  @Parameters(
    index = "1",
    description = ["Target URL or JAR for the application."],
  )
  internal lateinit var target: String

  /** Output target for this compilation run; can be a directory path, or a zip file path, or tar file path. */
  @Parameters(
    index = "2",
    description = ["Output directory, zip, or tarball."],
  )
  internal lateinit var output: String

  /** Interface for manually configuring the compiler; meant for testing and internal use only. */
  @VisibleForTesting internal fun configure(params: SiteCompilerParams? = null, logging: Logger? = null): SiteCompiler {
    if (params != null) {
      manifest = params.manifest.ifBlank {
        throw SSGCompilerError.InvalidArgument("Cannot load blank manifest path")
      }
      target = params.target.ifBlank {
        throw SSGCompilerError.InvalidArgument("Cannot load blank JAR path/HTTP target")
      }
      this.params = params
      this.logging = logging ?: Logging.of(SiteCompiler::class.java)
      paramsAvailable.compareAndSet(false, true)
      params.options.applyTo(this)
    }
    return this
  }

  /** Utility function to indicate a successful compile. */
  @CanIgnoreReturnValue internal fun success(
    info: LoadedAppInfo,
    out: String,
    buf: StaticSiteBuffer,
  ): CompileResult.Success {
    val success = CompileResult.Success(params, info, out, buf)
    result.set(success)
    resultAvailable.compareAndSet(false, true)
    return success
  }

  /** Utility function to indicate a compile failure. */
  @CanIgnoreReturnValue internal fun failure(err: Throwable, exitCode: Int = -1): CompileResult.Failure {
    val failure = CompileResult.Failure(params, err, exitCode)
    result.set(failure)
    resultAvailable.compareAndSet(false, true)
    return failure
  }

  // Trigger a compiler run programmatically.
  private fun compile(): CompileResult {
    require(!resultAvailable.get()) {
      "Cannot compile more than once"
    }
    run()
    val result = result.get()
    require(resultAvailable.get() && result != null) {
      "Compile produced no result"
    }
    return result
  }

  // Main execution function.
  private suspend fun execute(): CompileResult {
    logging.info("Compiling Elide app to static site...")
    logging.debug("SSG Compiler parameters: $params")
    val manifest = manifestReader.use {
      manifestReader.readManifest(params)
    }
    val appInfo = appLoader.prep(params, manifest)

    // generate expected requests via `appLoader`
    appLoader.use {
      if (!appInfo.eligible) {
        // in this case, there are no pre-compiled endpoints within the target app manifest; so the compiler has
        // nothing to do. this is not considered an error.
        logging.warn("App loader indicates no eligible SSG endpoints. Skipping compile.")
        return success(appInfo, target, StaticSiteBuffer())
      } else {
        logging.info("App loader indicates eligible SSG endpoints. Compiling...")
      }

      // mount app target
      appTarget.set(appInfo.target)
      appTargetAvailable.compareAndSet(false, true)

      val (count, seq) = appLoader.generateRequests(requestFactory)
      logging.info("Executing $count baseline requests against app target...")

      // then begin executing requests, and extracting results, into the `StaticSiteBuffer`
      val buffer = StaticSiteBuffer()
      compiler.prepare(
        params,
        appInfo,
        appLoader,
      )

      return when (val result = compiler.compileStaticSite(count, appInfo, seq, buffer)) {
        is CompileResult.Failure -> failure(result.err, result.exitCode)
        is CompileResult.Success -> {
          // at this point, we're ready to write outputs.
          logging.info("Site compile complete. Writing outputs...")
          contentWriter.use {
            contentWriter.write(params, buffer)
          }
          logging.info("Static site compile succeeded.")
          success(appInfo, result.output, buffer)
        }
      }
    }
  }

  /** Run the SSG compiler. */
  override fun run(): Unit = runBlocking {
    // if called via CLI, we will need to initialize parameters manually.
    if (!paramsAvailable.get()) {
      logging = Logging.of(SiteCompiler::class)
      params = CompilerParams(
        manifest = manifest,
        target = target,
        output = CompilerParams.Output.fromParams(output),
        options = Options(
          verbose = verbose,
          debug = debug,
          classpath = classpath,
          httpMode = httpMode || target.startsWith("http"),
          ignoreCertErrors = ignoreCertErrors,
          crawl = crawl,
          timeout = timeout,
          extraOrigins = extraOrigins.toSortedSet(),
          precompress = precompress,
          pretty = pretty,
        )
      )
      paramsAvailable.compareAndSet(false, true)
    }

    // wrap for error protection and begin execution
    executeTool(logging) {
      compiler.use {
        execute()
      }
    }
  }
}
