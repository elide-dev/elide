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
@file:Suppress("deprecation")

package elide.tooling.kotlin

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.nativeimage.ImageInfo
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.EXCEPTION
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.util.ServiceLoaderLite
import org.jline.builtins.ConfigurationPath
import org.jline.builtins.SyntaxHighlighter
import org.jline.utils.AttributedString
import java.io.Closeable
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.LinkedList
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.relativeTo
import elide.runtime.Logging
import elide.runtime.diag.Diagnostic
import elide.runtime.diag.DiagnosticsContainer
import elide.runtime.diag.DiagnosticsReceiver
import elide.runtime.diag.DiagnosticsSuite
import elide.runtime.gvm.kotlin.KotlinCompilerConfig
import elide.runtime.gvm.kotlin.KotlinCompilerConfig.KotlinBuiltinPlugin
import elide.runtime.gvm.kotlin.KotlinLanguage
import elide.runtime.gvm.kotlin.fromKotlincDiagnostic
import elide.tooling.ArgumentContext
import elide.tooling.Arguments
import elide.tooling.Environment
import elide.tooling.Inputs
import elide.tooling.Outputs
import elide.tooling.Tool
import elide.tooling.asArgumentString
import elide.tooling.cli.Statics
import elide.tooling.AbstractTool
import elide.tooling.cli.Statics.logging
import elide.tooling.jvm.JavaCompiler
import elide.tooling.term.TerminalUtil.toTrueColorAnsiString

// Internal debug logs.
private const val KOTLINC_DEBUG_LOGGING = false

// Name of the Kotlin Compiler.
public const val KOTLINC: String = "kotlinc"

// Build-time Kotlin support flag.
public val kotlinIsSupported: Boolean = System.getProperty("elide.kotlin") == "true"

// Description to show.
public const val KOTLIN_COMPILER_DESCRIPTION: String =
  "The Kotlin compiler ($KOTLINC) is a command-line tool that compiles Kotlin programs."

// Tool description.
public val kotlinc: Tool.CommandLineTool = Tool.describe(
  name = KOTLINC,
  label = "Kotlin Compiler",
  version = KotlinLanguage.VERSION,
  docs = URI.create("https://kotl.in/cli"),
  description = KOTLIN_COMPILER_DESCRIPTION,
  registered = true,
  helpText = """
  Usage: elide $KOTLINC <elide options> -- <kotlinc options> <kotlin source files>
  Kotlin: v${KotlinLanguage.VERSION}

  where possible elide options include:
    (None at this time.)

  where possible kotlinc options include:
    -classpath (-cp) <path>    List of directories and JAR/ZIP archives to search for user class files.
    -d <directory|jar>         Destination for generated class files.
    -expression (-e)           Evaluate the given string as a Kotlin script.
    -include-runtime           Include the Kotlin runtime in the resulting JAR.
    -java-parameters           Generate metadata for Java 1.8 reflection on method parameters.
    -jdk-home <path>           Include a custom JDK from the specified location in the classpath instead of the default 'JAVA_HOME'.
    -jvm-target <version>      The target version of the generated JVM bytecode (1.8 and 9–23), with 1.8 as the default.
    -module-name <name>        Name of the generated '.kotlin_module' file.
    -no-jdk                    Don't automatically include the Java runtime in the classpath.
    -no-reflect                Don't automatically include the Kotlin reflection dependency in the classpath.
    -no-stdlib                 Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection dependencies in the classpath.
    -script-templates <fully qualified class name[,]>
                               Script definition template classes.
    -Werror                    Report an error if there are any warnings.
    -api-version <version>     Allow using declarations from only the specified version of bundled libraries.
    -X                         Print a synopsis of advanced options.
    -Wextra                    Enable extra checkers for K2.
    -help (-h)                 Print a synopsis of standard options.
    -kotlin-home <path>        Path to the Kotlin compiler home directory used for the discovery of runtime libraries.
    -language-version <version> Provide source compatibility with the specified version of Kotlin.
    -opt-in <fq.name>          Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.
    -P plugin:<pluginId>:<optionName>=<value>
                               Pass an option to a plugin.
    -progressive               Enable progressive compiler mode.
                               In this mode, deprecations and bug fixes for unstable code take effect immediately
                               instead of going through a graceful migration cycle.
                               Code written in progressive mode is backward compatible; however, code written without
                               progressive mode enabled may cause compilation errors in progressive mode.
    -script                    Evaluate the given Kotlin script (*.kts) file.
    -nowarn                    Don't generate any warnings.
    -verbose                   Enable verbose logging output.
    -version                   Display the compiler version.
    -J<option>                 Pass an option directly to JVM.
    @<argfile>                 Read compiler arguments and file paths from the given file.

  For details, see https://kotl.in/cli
  For usage within Elide, see https://docs.elide.dev
"""
)

/**
 * # Kotlin Compiler
 *
 * Implements an [AbstractTool] adapter to `kotlinc`, the Kotlin compiler. Arguments are passed to the compiler verbatim
 * from the command-line.
 */
@ReflectiveAccess @Introspected public class KotlinCompiler (
  args: Arguments,
  env: Environment,
  public val inputs: KotlinCompilerInputs,
  public val outputs: KotlinCompilerOutputs,
  private val argsAmender: K2JVMCompilerArguments.() -> Unit = {},
  private val projectRoot: Path,
  private val testMode: Boolean = false,
  private val codeProcessor: (String) -> AttributedString = { AttributedString(it) },
) : AbstractTool(info = kotlinc.extend(
  args,
  env,
).using(
  inputs = inputs,
  outputs = outputs.flatten(),
)) {
  /** Report diagnostics from Kotlin's compiler. */
  private inner class K2DiagnosticsListener(
    private var container: DiagnosticsContainer = DiagnosticsContainer.create(),
  ) : MessageCollector,
    DiagnosticsSuite by container,
    DiagnosticsReceiver by container {
    // Whether this listener has seen errors.
    private val errorsSeen = atomic(false)

    // Close the underlying container.
    override fun close(): Unit = container.close()

    override fun clear() {
      container = DiagnosticsContainer.create()
    }

    override fun hasErrors(): Boolean = errorsSeen.value

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
      val severityColor = when (severity) {
        EXCEPTION, ERROR -> TextColors.red
        CompilerMessageSeverity.STRONG_WARNING -> TextColors.brightYellow
        CompilerMessageSeverity.WARNING -> TextColors.yellow
        CompilerMessageSeverity.FIXED_WARNING -> TextColors.gray
        CompilerMessageSeverity.INFO -> TextColors.cyan
        CompilerMessageSeverity.LOGGING,
        CompilerMessageSeverity.OUTPUT -> TextColors.gray
      }
      val (messageHighlighted, severityHighlighted) = when (severity) {
        EXCEPTION, ERROR -> TextStyles.bold + severityColor
        CompilerMessageSeverity.STRONG_WARNING -> TextStyles.bold + severityColor
        CompilerMessageSeverity.WARNING -> TextStyles.bold + severityColor
        CompilerMessageSeverity.FIXED_WARNING -> TextStyles.bold + severityColor
        CompilerMessageSeverity.INFO -> TextStyles.bold + severityColor
        CompilerMessageSeverity.LOGGING,
        CompilerMessageSeverity.OUTPUT -> TextStyles.dim + severityColor
      }.let {
        it(message) to it(severity.name.lowercase())
      }
      if (severity == ERROR || severity == EXCEPTION) {
        errorsSeen.value = true
      }
      container.report(
        Diagnostic.fromKotlincDiagnostic(severity, message, location)
      )
      if (message == "Argument -Xcompiler-plugin is experimental") {
        // skip this message, since we use this flag from internals.
        return
      }

      val msgBuilder = StringBuilder()
      val kotlincTag = TextStyles.dim("[kotlinc:${severityHighlighted}]")
      when {
        severity.isError || severity.isWarning -> if (location == null) {
          logging.debug { "No location given for kotlinc error." }
          StringBuilder("$kotlincTag $messageHighlighted")
        } else msgBuilder.apply {
          val severityMessage = when (severity) {
            ERROR, EXCEPTION -> "Compiler error"
            CompilerMessageSeverity.STRONG_WARNING -> "Strong warning"
            CompilerMessageSeverity.WARNING -> "Warning"
            CompilerMessageSeverity.INFO -> "Info"
            CompilerMessageSeverity.LOGGING -> "Logging"
            else -> "Compiler output"
          }.let {
            severityColor(it)
          }
          val msg = "$kotlincTag $severityMessage"
          msgBuilder.appendLine()
          msgBuilder.appendLine(msg)
          msgBuilder.appendLine()

          // if we have a location for this issue, we can load the source code and render a smarter error.
          val file = location.path.ifBlank { null }?.let { Paths.get(it) }
          val maxLineNumberSize = location.lineEnd.toString().length
          val relativeFile = file?.relativeTo(Path.of(System.getProperty("user.dir")))
          val ctx = location.line to location.column
          val lineAttributed = location.lineContent?.let { codeProcessor(it) }
          val lineSrc = lineAttributed?.toTrueColorAnsiString()
          val relativeFileAsLink = relativeFile?.let {
            val relativized = runCatching { it.relativeTo(projectRoot) }.onFailure {
              logging.debug { "Failed to relativize file: $it" }
            }.getOrDefault(it)

            (TextStyles.hyperlink(it.toString()) + TextStyles.underline)(relativized.toString())
          }
          appendLine("   ${TextStyles.dim("at")}: ${relativeFileAsLink ?: "<no file>"}:${ctx.first}:${ctx.second}")
          if (lineSrc != null) {
            val spaceIndentSize = lineSrc.takeWhile { it == ' ' }.length
            val spaceIndent = " ".repeat(spaceIndentSize)
            val leftpad = " ".repeat(location.column + 3 + spaceIndentSize)
            appendLine("  ${(ctx.first - 1).toString().padStart(maxLineNumberSize, ' ')} ⋮ ${spaceIndent}...")
            appendLine("  ${(ctx.first).toString().padStart(maxLineNumberSize, ' ')} ⋮ $lineSrc")
            appendLine("${leftpad}${severityColor("↑")}")
            appendLine("${leftpad}${messageHighlighted}")
          }
        }

        // don't print unconditionally at lower severities
        else -> null
      }?.also { builder ->
        ktLogger.debug(builder.toString())
        Statics.terminal.println(builder.toString())
      }
    }
  }

  // Kotlin compiler.
  private val ktCompiler by lazy { K2JVMCompiler() }

  override fun supported(): Boolean = kotlinIsSupported

  /**
   * Kotlin Compiler inputs.
   *
   * Implements understanding of Kotlin source inputs for `kotlinc`.
   */
  public sealed interface KotlinCompilerInputs : Inputs.Files {
    /**
     * Describes a sequence of source files to specify as Kotlin sources.
     */
    @JvmInline public value class SourceFiles(
      public val files: PersistentList<Path>,
    ) : KotlinCompilerInputs
  }

  /**
   * Kotlin Compiler outputs.
   *
   * Implements understanding of Kotlin compiler outputs -- JARs and class files.
   */
  public sealed interface KotlinCompilerOutputs {
    /**
     * Flatten into an [Outputs] type.
     *
     * @return Outputs value.
     */
    public fun flatten(): Outputs

    /** Defines a simple single-file [path] to an expected JAR output. */
    @JvmInline public value class Jar(public val path: Path) : KotlinCompilerOutputs, Outputs.Disk.File {
      override fun flatten(): Outputs = this
    }

    /** Defines a [directory] path to build class output. */
    @JvmInline public value class Classes(public val directory: Path) : KotlinCompilerOutputs, Outputs.Disk.Directory {
      override fun flatten(): Outputs = this
    }
  }

  /** Factories for configuring and obtaining instances of [KotlinCompiler]. */
  public companion object {
    // Logging.
    @JvmStatic private val ktLogger by lazy { Logging.of(KotlinCompiler::class) }

    @JvmStatic private fun debugLog(message: String) {
      if (KOTLINC_DEBUG_LOGGING) {
        System.err.println("[kotlinc:debug] $message")
      }
    }

    /**
     * Perform highlighting against the provided Kotlin code snippet.
     *
     * @param snippet Snippet of Kotlin code; spacing will be preserved.
     * @return Highlighted snippet of Kotlin code (for terminal use).
     */
    @JvmStatic public fun terminalHighlight(snippet: String): AttributedString {
      val appconfigs = Paths.get(System.getProperty("user.home"), "elide", "config", "nanorc")
      val jnanorc = Paths.get(System.getProperty("user.home"), ".elide", "nanorc")
      val configPath = ConfigurationPath(appconfigs, jnanorc)
      val langHighlighter = SyntaxHighlighter.build(configPath.getConfig("jnanorc"), "Kotlin")
      return langHighlighter.highlight(snippet)
    }

    init {
      initializeKotlinCompilerPlugins()
    }

    private fun createClassLoader(classpath: Iterable<Path>, parentDisposable: Disposable): URLClassLoader {
      val classLoader = URLClassLoader(
        classpath.map { it.toFile().toURI().toURL() }.toTypedArray(), this::class.java.classLoader
      )
      Disposer.register(parentDisposable, UrlClassLoaderDisposable(classLoader))
      return classLoader
    }

    private lateinit var compilerServices: Services

    @JvmStatic internal fun resolveCompilerServices(): Services = compilerServices

    @Suppress("TooGenericExceptionCaught")
    @OptIn(ExperimentalCompilerApi::class)
    @JvmStatic private fun initializeKotlinCompilerPlugins() {
      try {
        val rootDisposable = Disposer.newDisposable("kotlinc")

        val kotlinClassPath = Files.list(when {
          ImageInfo.inImageBuildtimeCode() -> Paths.get(System.getProperty("elide.kotlinResources")).resolve("lib")
          else -> Paths.get(System.getProperty("user.home"))
            .resolve("elide")
            .resolve("resources")
            .resolve("kotlin")
            .resolve(KotlinLanguage.VERSION)
            .resolve("lib")
        }).toList()

        val classLoader = createClassLoader(
          kotlinClassPath,
          rootDisposable,
        )

        // load services eagerly
        debugLog("Building Kotlin compiler services")

        buildSet {
          addAll(ServiceLoaderLite.loadImplementations(ComponentRegistrar::class.java, classLoader))
          addAll(ServiceLoaderLite.loadImplementations(CompilerPluginRegistrar::class.java, classLoader))
          add(org.jetbrains.kotlin.powerassert.PowerAssertCommandLineProcessor())
          add(dev.zacsweers.redacted.compiler.RedactedCommandLineProcessor())
          add(dev.zacsweers.redacted.compiler.RedactedComponentRegistrar())
        }.distinctBy { it::class }.let {
          if (it.isEmpty()) {
            error(
              "No Kotlin compiler plugins found; this is an error."
            )
          } else {
            val size = it.size
            Logging.root().debug { "Kotlin compiler plugins loaded: $it" }
            Services.Builder().apply {
              it.forEach { svc ->
                @Suppress("UNCHECKED_CAST")
                register(svc::class.java as Class<Any>, svc)
              }
            }.build().let { services ->
              debugLog("Registered $size Kotlin compiler plugins")
              compilerServices = services
            }
          }
        }
      } catch (err: Throwable) {
        // if we fail to initialize the compiler, we should log this and exit.
        val msg = "Failed to initialize Kotlin compiler: ${err.message}"
        Logging.of(KotlinCompiler::class).error(msg, err)
        error(msg)
      }
    }

    /**
     * Configure the default suite of plugins supported by the Kotlin compiler.
     *
     * @param config Arguments to configure the Kotlin compiler.
     * @param test Whether this is a test run; defaults to `false`.
     * @param enableSerialization Whether to enable the Kotlin serialization plugin; defaults to `true`.
     * @return Set of plugins which were configured.
     */
    @JvmStatic public fun configureDefaultPlugins(
      config: K2JVMCompilerArguments,
      test: Boolean = false,
      enableSerialization: Boolean = true,
      enablePowerAssert: Boolean = test,
    ): Set<KotlinBuiltinPlugin> {
      val defaultPlugins = KotlinCompilerConfig.getDefaultPlugins(test = test)
      defaultPlugins.forEach { plugin ->
        if (plugin == KotlinBuiltinPlugin.SERIALIZATION && !enableSerialization) {
          // skip serialization plugin if it is not enabled.
          return@forEach
        }
        if (plugin == KotlinBuiltinPlugin.POWER_ASSERT && !enablePowerAssert) {
          // skip power assert plugin if it is not enabled.
          return@forEach
        }
        plugin.apply(config, Statics.resourcesPath)
      }
      return defaultPlugins
    }

    /**
     * Create inputs for the Kotlin compiler which include the provided source paths.
     *
     * @param sequence Sequence of source paths to include.
     * @return Compiler inputs.
     */
    @JvmStatic public fun sources(sequence: Sequence<Path>): KotlinCompilerInputs = KotlinCompilerInputs.SourceFiles(
      sequence.toList().toPersistentList().also {
        if (it.isEmpty()) embeddedToolError(kotlinc, "No source files provided")
      }
    )

    /**
     * Create outputs for the Kotlin compiler which specify a single JAR.
     *
     * @param at Expected path to the JAR output.
     * @return Compiler outputs.
     */
    @JvmStatic public fun outputJar(at: Path): KotlinCompilerOutputs = KotlinCompilerOutputs.Jar(at)

    /**
     * Create outputs for the Kotlin compiler which specify a directory of classes.
     *
     * @param at Expected path to the built class output.
     * @return Compiler outputs.
     */
    @JvmStatic public fun classesDir(at: Path): KotlinCompilerOutputs = KotlinCompilerOutputs.Classes(at)

    /**
     * Create a Kotlin Compiler instance from the provided inputs.
     *
     * @param args Arguments to the compiler.
     * @param inputs Inputs to the compiler.
     * @param outputs Outputs from the compiler.
     * @param env Environment for the compiler; defaults to the host environment.
     */
    @JvmStatic public fun create(
      args: Arguments,
      env: Environment,
      inputs: KotlinCompilerInputs,
      outputs: KotlinCompilerOutputs,
      projectRoot: Path = Paths.get(System.getProperty("user.dir")),
      test: Boolean = false,
      codeProcessor: (String) -> AttributedString = ::terminalHighlight,
      argsAmender: K2JVMCompilerArguments.() -> Unit = {},
    ): KotlinCompiler = KotlinCompiler(
      args = args,
      env = env,
      inputs = inputs,
      outputs = outputs,
      projectRoot = projectRoot,
      argsAmender = argsAmender,
      testMode = test,
      codeProcessor = codeProcessor,
    )
  }

  // Amend arguments to the Kotlin compiler after parsing.
  private fun amendArgs(
    args: K2JVMCompilerArguments,
    javaToolchainHome: Path,
    kotlinHomePath: Path,
    kotlinMajorMinor: String,
    sources: KotlinCompilerInputs.SourceFiles,
  ) = args.apply {
    apiVersion = kotlinMajorMinor
    languageVersion = kotlinMajorMinor
    disableStandardScript = true
    kotlinHome = kotlinHomePath.absolutePathString()
    jdkHome = javaToolchainHome.absolutePathString()
    freeArgs = sources.files.map { it.absolutePathString() }.toMutableList()
    destinationAsFile = when (outputs) {
      is KotlinCompilerOutputs.Jar -> outputs.path.toFile()
      is KotlinCompilerOutputs.Classes -> outputs.directory.toFile()
    }

    // allow caller to amend args.
    argsAmender(this)
  }

  // Resolve the Kotlin home directory for the Kotlin compiler.
  private fun resolveKotlinHome(state: EmbeddedToolState): Path {
    val homeEnvOverride = System.getenv("KOTLIN_HOME")
      ?.ifBlank { null }
      ?.let { Paths.get(it) }

    val binRelativeHome = state.resourcesPath
      .resolve("kotlin")
      .resolve(KotlinLanguage.VERSION)

    return when {
      // prefer a user-specified kotlin home.
      homeEnvOverride != null -> homeEnvOverride.also {
        logging.debug { "Using KOTLIN_HOME, which is set to: $it" }
      }

      // otherwise, use the bin-relative home.
      else -> binRelativeHome.also {
        logging.debug { "Using bin-relative Kotlin home: $it" }
      }
    }.also {
      if (!it.exists()) {
        embeddedToolError(
          kotlinc,
          "Kotlin home missing; please set `KOTLIN_HOME` or re-install Elide " +
            "(checked at '$it')."
        )
      }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  override suspend fun invoke(state: EmbeddedToolState): Tool.Result {
    debugLog("Preparing Kotlin Compiler invocation")
    val javaToolchainHome = JavaCompiler.resolveJavaToolchain(kotlinc)
    debugLog("Java toolchain home: $javaToolchainHome")
    val kotlinVersionRoot = resolveKotlinHome(state)
    debugLog("Kotlin home: $kotlinVersionRoot")
    val closeables = LinkedList<Closeable>()
    val diagnostics = K2DiagnosticsListener()
    val kotlinMajorMinor = KotlinLanguage.VERSION.substringBeforeLast('.')
    debugLog("Kotlin version: $kotlinMajorMinor")

    val svcs = resolveCompilerServices()
    val renderCtx = ArgumentContext.of(
      argSeparator = ' ',
      kvToken = ' ',
    )
    debugLog("Arg render context ready for args: ${info.args}")
    val argList = info.args.asArgumentSequence().toList()
    debugLog("Arg list ready of size=${argList.size}")

    val finalizedArgs: List<String> = argList.flatMapIndexed { index, it ->
      debugLog("Flattening arg $index")
      it.asArgumentSequence()
    }.flatMapIndexed { index, it ->
      debugLog("Rendering flattened arg $index")
      try {
        it.asArgumentString(renderCtx).split(' ').also {
          debugLog("Rendered arg $index: $it")
        }
      } catch (err: Throwable) {
        val msg = "Failed to render argument: ${err.message}"
        debugLog(msg)
        embeddedToolError(kotlinc, msg, cause = err)
      }
    }.also {
      debugLog("Finished rendering args")
    }.toList().also {
      debugLog("Finalized args size=${it.size}")
    }

    debugLog("Creating Kotlin compiler arguments")
    val ktArgs = try {
      ktCompiler.createArguments().also {
        debugLog("Creating arguments with finalized suite of size=${finalizedArgs.size}")
      }
    } catch (err: Throwable) {
      val msg = "Failed to create kotlinc arguments: ${err.message}"
      debugLog(msg)
      embeddedToolError(kotlinc, msg, cause = err)
    }
    val args = ktArgs.apply {
      if (finalizedArgs.isNotEmpty()) {
        try {
          debugLog("Parsing arguments")
          ktCompiler.parseArguments(
            finalizedArgs.toTypedArray(),
            this,
          )
        } catch (ise: IllegalArgumentException) {
          debugLog("Failed to parse arguments: ${ise.message}")
          embeddedToolError(kotlinc, "Arguments failed to parse: ${ise.message}", cause = ise)
        }
      }

      debugLog("Amending arguments")
      amendArgs(
        this,
        javaToolchainHome,
        kotlinVersionRoot,
        kotlinMajorMinor,
        inputs as KotlinCompilerInputs.SourceFiles,
      )
    }

    debugLog("Finalized Kotlin Compiler args: $args")

    @Suppress("TooGenericExceptionCaught")
    try {
      debugLog("Firing Kotlin Compiler")
      ktCompiler.exec(diagnostics, svcs, args)
    } catch (rxe: RuntimeException) {
      debugLog("Failed to execute Kotlin compiler: ${rxe.message}")
      embeddedToolError(
        kotlinc,
        "Kotlin compiler invocation failed: ${rxe.message}",
        cause = rxe,
      )
    } finally {
      debugLog("Closing diagnostics listener")
      closeables.forEach { it.close() }
    }

    // if we've witnessed errors, or in other words if we have diagnostics above severity ERROR, we should fail.
    if (diagnostics.hasErrors()) {
      debugLog("Kotlin compiler invocation failed with errors")
      return Tool.Result.UnspecifiedFailure
    }
    debugLog("Kotlin compiler invocation complete")
    return Tool.Result.Success
  }

  internal class UrlClassLoaderDisposable(classLoader: URLClassLoader) : Disposable {
    private val classLoaderRef: WeakReference<URLClassLoader> = WeakReference(classLoader)

    override fun dispose() {
      val classLoader = classLoaderRef.get()
      if (classLoader != null) {
        classLoader.close()
        classLoaderRef.clear()
      }
    }
  }
}
