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

package elide.tool.cli.cmd.tool.kotlinc

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.EXCEPTION
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.destinationAsFile
import picocli.CommandLine
import picocli.CommandLine.ScopeType
import java.io.Closeable
import java.net.URI
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
import elide.runtime.gvm.kotlin.KotlinLanguage
import elide.runtime.gvm.kotlin.KotlinPrecompiler.KOTLIN_VERSION
import elide.runtime.gvm.kotlin.fromKotlincDiagnostic
import elide.tool.Arguments
import elide.tool.Environment
import elide.tool.Inputs
import elide.tool.Outputs
import elide.tool.Tool
import elide.tool.cli.CommandContext
import elide.tool.cli.Statics
import elide.tool.cli.cmd.tool.AbstractTool
import elide.tool.cli.cmd.tool.DelegatedToolCommand
import elide.tool.cli.cmd.tool.javac.JavaCompiler

// Name of the Kotlin Compiler.
private const val KOTLINC = "kotlinc"

// Build-time Kotlin support flag.
private val kotlinIsSupported = System.getProperty("elide.kotlin") == "true"

// Description to show.
private const val KOTLIN_COMPILER_DESCRIPTION =
  "The Kotlin compiler ($KOTLINC) is a command-line tool that compiles Kotlin programs."

// Tool description.
private val kotlinc = Tool.describe(
  name = KOTLINC,
  label = "Kotlin Compiler",
  version = KotlinLanguage.VERSION,
  docs = URI.create("https://kotl.in/cli"),
  description = KOTLIN_COMPILER_DESCRIPTION,
  helpText = """
  Usage: elide $KOTLINC <elide options> -- <kotlinc options> <kotlin source files>
  Kotlin version: ${KotlinLanguage.VERSION}

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
@ReflectiveAccess @Introspected class KotlinCompiler private constructor (
  args: Arguments,
  env: Environment,
  private val inputs: KotlinCompilerInputs,
  private val outputs: KotlinCompilerOutputs,
): AbstractTool(info = kotlinc.extend(
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
      val msg = "[kotlinc] ${severity.name.lowercase()}: $message"
      when {
        severity.isError -> ktLogger.error(msg)
        severity.isWarning -> ktLogger.warn(msg)
        else -> ktLogger.info(msg)
      }
      if (severity == ERROR || severity == EXCEPTION) {
        errorsSeen.value = true
      }
      container.report(
        Diagnostic.fromKotlincDiagnostic(severity, message, location)
      )
    }
  }

  // Kotlin compiler.
  private val ktCompiler by lazy { K2JVMCompiler() }

  // Logging.
  private val ktLogger by lazy { Logging.of(KotlinCompiler::class) }

  override fun supported(): Boolean = kotlinIsSupported

  /**
   * Kotlin Compiler inputs.
   *
   * Implements understanding of Kotlin source inputs for `kotlinc`.
   */
  sealed interface KotlinCompilerInputs: Inputs.Files {
    /**
     * Describes a sequence of source files to specify as Kotlin sources.
     */
    @JvmInline value class SourceFiles internal constructor (
      internal val files: PersistentList<Path>,
    ): KotlinCompilerInputs
  }

  /**
   * Kotlin Compiler outputs.
   *
   * Implements understanding of Kotlin compiler outputs -- JARs and class files.
   */
  sealed interface KotlinCompilerOutputs {
    /**
     * Flatten into an [Outputs] type.
     *
     * @return Outputs value.
     */
    fun flatten(): Outputs

    /** Defines a simple single-file [path] to an expected JAR output. */
    @JvmInline value class Jar(internal val path: Path): KotlinCompilerOutputs, Outputs.Disk.File {
      override fun flatten(): Outputs = this
    }

    /** Defines a [directory] path to build class output. */
    @JvmInline value class Classes(internal val directory: Path): KotlinCompilerOutputs, Outputs.Disk.Directory {
      override fun flatten(): Outputs = this
    }
  }

  /** Factories for configuring and obtaining instances of [KotlinCompiler]. */
  companion object {
    /**
     * Create inputs for the Kotlin compiler which include the provided source paths.
     *
     * @param sequence Sequence of source paths to include.
     * @return Compiler inputs.
     */
    @JvmStatic fun sources(sequence: Sequence<Path>): KotlinCompilerInputs = KotlinCompilerInputs.SourceFiles(
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
    @JvmStatic fun outputJar(at: Path): KotlinCompilerOutputs = KotlinCompilerOutputs.Jar(at)

    /**
     * Create outputs for the Kotlin compiler which specify a directory of classes.
     *
     * @param at Expected path to the built class output.
     * @return Compiler outputs.
     */
    @JvmStatic fun classesDir(at: Path): KotlinCompilerOutputs = KotlinCompilerOutputs.Classes(at)

    /**
     * Create a Kotlin Compiler instance from the provided inputs.
     *
     * @param args Arguments to the compiler.
     * @param inputs Inputs to the compiler.
     * @param outputs Outputs from the compiler.
     * @param env Environment for the compiler; defaults to the host environment.
     */
    @JvmStatic fun create(
      args: Arguments,
      env: Environment,
      inputs: KotlinCompilerInputs,
      outputs: KotlinCompilerOutputs,
    ): KotlinCompiler = KotlinCompiler(
      args = args,
      env = env,
      inputs = inputs,
      outputs = outputs,
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
  }

  // Resolve the Kotlin home directory for the Kotlin compiler.
  private fun resolveKotlinHome(): Path {
    val homeEnvOverride = System.getenv("KOTLIN_HOME")
      ?.ifBlank { null }
      ?.let { Paths.get(it) }

    val binRelativeHome = Statics.resourcesPath
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

  override suspend fun CommandContext.invoke(state: EmbeddedToolState): Tool.Result {
    if (state.cmd.state.output.verbose) output {
      append("""
      Invoking `kotlinc` with:

        -- Arguments:
        ${info.args}

        -- Inputs:
        $inputs

        -- Outputs:
        $outputs

        -- Environment:
        ${info.environment}
      """.trimIndent())
    }

    val javaToolchainHome = JavaCompiler.resolveJavaToolchain(kotlinc)
    val kotlinVersionRoot = resolveKotlinHome()
    val closeables = LinkedList<Closeable>()
    val diagnostics = K2DiagnosticsListener()
    val kotlinMajorMinor = KOTLIN_VERSION.substringBeforeLast('.')
    val svcs = Services.EMPTY
    val args = ktCompiler.createArguments().apply {
      ktCompiler.parseArguments(
        info.args.asArgumentList().toTypedArray(),
        this,
      )
      amendArgs(
        this,
        javaToolchainHome,
        kotlinVersionRoot,
        kotlinMajorMinor,
        inputs as KotlinCompilerInputs.SourceFiles,
      )
    }

    val compileStart = System.currentTimeMillis()
    val compileEnd: Long

    @Suppress("TooGenericExceptionCaught")
    try {
      ktCompiler.exec(diagnostics, svcs, args)
    } catch (rxe: RuntimeException) {
      embeddedToolError(
        kotlinc,
        "Kotlin compiler invocation failed: ${rxe.message}",
        cause = rxe,
      )
    } finally {
      compileEnd = System.currentTimeMillis()
      closeables.forEach { it.close() }
    }

    val totalMs = compileEnd - compileStart
    val srcsCount = (inputs as KotlinCompilerInputs.SourceFiles).files.size
    val outputPath = when (outputs) {
      is KotlinCompilerOutputs.Jar -> outputs.path
      is KotlinCompilerOutputs.Classes -> outputs.directory
    }
    val outputRelativeToCwd = outputPath
      .toAbsolutePath()
      .relativeTo(Paths.get(System.getProperty("user.dir")))

    output {
      val sourceFiles = if (srcsCount > 1) "sources" else "source file"
      append("[kotlinc] Compiled $srcsCount $sourceFiles in ${totalMs}ms → $outputRelativeToCwd")
    }
    return Tool.Result.Success
  }

  @CommandLine.Command(
    name = KOTLINC,
    version = [KotlinLanguage.VERSION],
    description = [KOTLIN_COMPILER_DESCRIPTION],
    mixinStandardHelpOptions = false,
    scope = ScopeType.LOCAL,
    synopsisHeading = "",
    customSynopsis = [],
  )
  @ReflectiveAccess
  @Introspected
  class KotlinCliTool: DelegatedToolCommand<KotlinCompiler>(kotlinc) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec

    companion object {
      // Gather options, inputs, and outputs for an invocation of the Kotlin compiler.
      @JvmStatic private fun gatherArgs(args: Arguments): Pair<KotlinCompilerInputs, KotlinCompilerOutputs> {
        return JavaCompiler.jvmStyleArgs(kotlinc, args).let { (sources, outSpec) ->
          sources(sources) to when (outSpec.endsWith(".jar")) {
            true -> outputJar(Paths.get(outSpec))
            false -> classesDir(Paths.get(outSpec))
          }
        }
      }
    }

    override fun configure(args: Arguments, environment: Environment): KotlinCompiler = gatherArgs(args).let { state ->
      KotlinCompiler(
        args = args,
        env = environment,
        inputs = state.first,
        outputs = state.second,
      )
    }
  }
}
