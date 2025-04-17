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

package elide.tool.cli.cmd.tool.javac

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import jakarta.inject.Singleton
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.io.path.relativeTo
import elide.runtime.Logging
import elide.runtime.diag.DiagnosticsContainer
import elide.runtime.diag.DiagnosticsReceiver
import elide.runtime.diag.DiagnosticsSuite
import elide.runtime.gvm.jvm.fromJavacDiagnostic
import elide.tool.Arguments
import elide.tool.Environment
import elide.tool.Inputs
import elide.tool.MutableArguments
import elide.tool.Outputs
import elide.tool.Tool
import elide.tool.cli.CommandContext
import elide.tool.cli.cmd.tool.AbstractTool
import elide.tool.cli.cmd.tool.DelegatedToolCommand
import elide.runtime.diag.Diagnostic as ElideDiagnostic

// Name of the `javac` tool.
private const val JAVAC = "javac"

// Build-time Java support flag.
private val javaIsSupported = System.getProperty("elide.jvm") == "true"

// Description to show for `javac`.
private const val JAVAC_DESCRIPTION = "The Java compiler, supporting up to JDK 24."

// Tool description.
private val javac = Tool.describe(
  name = JAVAC,
  label = "Java Compiler",
  version = System.getProperty("java.version"),
  docs = URI.create("https://kotl.in/cli"),
  description = JAVAC_DESCRIPTION,
  helpText = """
  Usage: elide javac <options> <source files>
  where possible options include:

  @<filename>                  Read options and filenames from file
  -Akey[=value]                Options to pass to annotation processors
  --add-modules <module>(,<module>)*
        Root modules to resolve in addition to the initial modules,
        or all modules on the module path if <module> is ALL-MODULE-PATH.
  --boot-class-path <path>, -bootclasspath <path>
        Override location of bootstrap class files
  --class-path <path>, -classpath <path>, -cp <path>
        Specify where to find user class files and annotation processors
  -d <directory>               Specify where to place generated class files
  -deprecation
        Output source locations where deprecated APIs are used
  --enable-preview
        Enable preview language features.
        To be used in conjunction with either -source or --release.
  -encoding <encoding>         Specify character encoding used by source files
  -endorseddirs <dirs>         Override location of endorsed standards path
  -extdirs <dirs>              Override location of installed extensions
  -g                           Generate all debugging info
  -g:{lines,vars,source}       Generate only some debugging info
  -g:none                      Generate no debugging info
  -h <directory>
        Specify where to place generated native header files
  --help, -help, -?            Print this help message
  --help-extra, -X             Print help on extra options
  -implicit:{none,class}
        Specify whether to generate class files for implicitly referenced files
  -J<flag>                     Pass <flag> directly to the runtime system
  --limit-modules <module>(,<module>)*
        Limit the universe of observable modules
  --module <module>(,<module>)*, -m <module>(,<module>)*
        Compile only the specified module(s), check timestamps
  --module-path <path>, -p <path>
        Specify where to find application modules
  --module-source-path <module-source-path>
        Specify where to find input source files for multiple modules
  --module-version <version>
        Specify version of modules that are being compiled
  -nowarn                      Generate no warnings
  -parameters
        Generate metadata for reflection on method parameters
  -proc:{none,only,full}
        Control whether annotation processing and/or compilation is done.
  -processor <class1>[,<class2>,<class3>...]
        Names of the annotation processors to run;
        bypasses default discovery process
  --processor-module-path <path>
        Specify a module path where to find annotation processors
  --processor-path <path>, -processorpath <path>
        Specify where to find annotation processors
  -profile <profile>
        Check that API used is available in the specified profile.
        This option is deprecated and may be removed in a future release.
  --release <release>
        Compile for the specified Java SE release.
        Supported releases:
            8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
  -s <directory>               Specify where to place generated source files
  --source <release>, -source <release>
        Provide source compatibility with the specified Java SE release.
        Supported releases:
            8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
  --source-path <path>, -sourcepath <path>
        Specify where to find input source files
  --system <jdk>|none          Override location of system modules
  --target <release>, -target <release>
        Generate class files suitable for the specified Java SE release.
        Supported releases:
            8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
  --upgrade-module-path <path>
        Override location of upgradeable modules
  -verbose                     Output messages about what the compiler is doing
  --version, -version          Version information
  -Werror                      Terminate compilation if warnings occur
  """
)

/**
 * # Java Compiler
 *
 * Implements an [AbstractTool] adapter to `javac`, the Java compiler. Arguments are passed to the compiler verbatim
 * from the command-line.
 */
@ReflectiveAccess @Introspected class JavaCompiler private constructor (
  args: Arguments,
  env: Environment,
  private val inputs: JavaCompilerInputs,
  private val outputs: JavaCompilerOutputs,
): AbstractTool(info = javac.extend(
  args,
  env,
).using(
  inputs = inputs,
  outputs = outputs.flatten(),
)) {
  // Receives Java Compiler diagnostics.
  private inner class JavaDiagnosticsReceiver(
    private val locale: Locale,
    private val container: DiagnosticsContainer = DiagnosticsContainer.create()
  ) : DiagnosticListener<JavaFileObject>,
    DiagnosticsSuite by container,
    DiagnosticsReceiver by container {
    // Whether errors have been seen for a compile run.
    private val errorsSeen = atomic(false)

    /** Proxy close calls to the underlying container. */
    override fun close(): Unit = container.close()

    override fun report(diagnostic: Diagnostic<out JavaFileObject>?) {
      if (diagnostic == null) {
        javacLogger.warn { "Received null diagnostic from compiler" }
        return
      }
      val msg = "[javac] ${diagnostic.kind.name}: ${diagnostic.getMessage(locale)}"
      when (diagnostic.kind) {
        Diagnostic.Kind.ERROR -> javacLogger.error(msg)
        Diagnostic.Kind.MANDATORY_WARNING,
        Diagnostic.Kind.WARNING -> javacLogger.warn(msg)
        else -> javacLogger.info(msg)
      }
      if (diagnostic.kind == Diagnostic.Kind.ERROR) {
        errorsSeen.value = true
      }
      container.report(
        ElideDiagnostic.fromJavacDiagnostic(locale, diagnostic)
      )
    }
  }

  // Java compiler.
  private val systemCompiler by lazy { ToolProvider.getSystemJavaCompiler() }

  // Logging.
  private val javacLogger by lazy { Logging.of(JavaCompiler::class) }

  override fun supported(): Boolean = javaIsSupported

  /**
   * Java Compiler inputs.
   *
   * Implements understanding of Java source inputs for `javac`.
   */
  sealed interface JavaCompilerInputs: Inputs.Files {
    /**
     * Describes a sequence of source files to specify as Java sources.
     */
    @JvmInline value class SourceFiles(internal val files: PersistentList<Path>): JavaCompilerInputs
  }

  /**
   * Java Compiler outputs.
   *
   * Implements understanding of Java compiler outputs -- class files only.
   */
  sealed interface JavaCompilerOutputs {
    /**
     * Flatten into an [Outputs] type.
     *
     * @return Outputs value.
     */
    fun flatten(): Outputs

    /** Defines a [directory] path to build class output. */
    @JvmInline value class Classes(internal val directory: Path): JavaCompilerOutputs, Outputs.Disk.Directory {
      override fun flatten(): Outputs = this
    }
  }

  // Amend Java Compiler args with additional options that were passed in, or which enable Elide features.
  @Suppress("UNUSED_PARAMETER") private fun amendArgs(args: MutableArguments) {
    // Nothing at this time.
  }

  override suspend fun CommandContext.invoke(state: EmbeddedToolState): Tool.Result {
    val javaToolchainHome = resolveJavaToolchain(javac)
    val locale = Locale.getDefault()
    val charset = StandardCharsets.UTF_8
    val args = MutableArguments.from(info.args).also { amendArgs(it) }

    output {
      """
        Invoking `javac` with:
  
          Locale: $locale
          Charset: $charset
          Java Home: $javaToolchainHome
  
          -- Arguments:
          $args
  
          -- Inputs:
          $inputs
  
          -- Outputs:
          $outputs
  
          -- Environment:
          ${info.environment}
      """.trimIndent().also {
        javacLogger.debug(it)
        if (state.cmd.state.output.verbose) {
          append(it)
        }
      }
    }

    javacLogger.debug { "Initializing Java compiler support" }
    val compiler: javax.tools.JavaCompiler = systemCompiler
    javacLogger.debug { "Preparing compiler diagnostics (locale: $locale)" }
    val diagnosticsReceiver = JavaDiagnosticsReceiver(locale)
    javacLogger.debug { "Initializing compiler FS support" }

    @Suppress("TooGenericExceptionCaught", "PrintStackTrace")
    val fileManager = try {
      compiler.getStandardFileManager(diagnosticsReceiver, locale, charset)
    } catch (err: Throwable) {
      logging.error { "Failed to initialize standard file manager: ${err::class.java.simpleName}(${err.message})" }
      err.printStackTrace()
      throw err
    }
    javacLogger.debug { "Resolving sources" }
    val units = fileManager.getJavaFileObjectsFromPaths(
      (inputs as JavaCompilerInputs.SourceFiles).files
    )
    val compileStart = System.currentTimeMillis()
    val compileEnd: Long
    val srcsCount = inputs.files.size

    @Suppress("TooGenericExceptionCaught")
    try {
      val plural = if (srcsCount > 1) "source" else "source file"
      javacLogger.debug { "Preparing Java compile task for $srcsCount $plural" }

      compiler.getTask(
        null,
        fileManager,
        diagnosticsReceiver,
        args.asArgumentList().filter { !it.endsWith(".java") },
        null,
        units,
      ).let {
        it.setLocale(locale)
        javacLogger.debug { "Invoking compile task '$it' with $srcsCount $plural" }

        when (it.call()) {
          true -> {} // nothing to do
          false -> error("Failed to compile Java source code")
        }
      }
    } catch (rxe: RuntimeException) {
      javacLogger.debug { "Exception while compiling: $rxe" }

      embeddedToolError(
        javac,
        "Java compiler invocation failed: ${rxe.message}",
        cause = rxe,
      )
    } finally {
      javacLogger.debug { "Java compile job finished" }
      compileEnd = System.currentTimeMillis()
      fileManager.close()
    }

    val totalMs = compileEnd - compileStart
    javacLogger.debug { "Java compile job completed in ${totalMs}ms" }
    val outputPath = (outputs as JavaCompilerOutputs.Classes).directory
    val outputRelativeToCwd = outputPath
      .toAbsolutePath()
      .relativeTo(Paths.get(System.getProperty("user.dir")))

    output {
      val sourceFiles = if (srcsCount > 1) "sources" else "source file"
      append("[javac] Compiled $srcsCount $sourceFiles in ${totalMs}ms → $outputRelativeToCwd")
    }
    return Tool.Result.Success
  }

  /** Factories for configuring and obtaining instances of [JavaCompiler]. */
  companion object {
    // Resolve the Java toolchain for the Kotlin compiler.
    @JvmStatic internal fun resolveJavaToolchain(tool: Tool.CommandLineTool): Path {
      return Paths.get(
        (System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))?.ifBlank { null } ?:
        embeddedToolError(tool, "Java toolchain missing; please set `JAVA_HOME`")
      )
    }

    @JvmStatic internal fun jvmStyleArgs(tool: Tool.CommandLineTool, args: Arguments): Pair<Sequence<Path>, String> {
      val argsList = args.asArgumentList()
      val outSpecPositionMinusOne = argsList.indexOf("-d")
      if (outSpecPositionMinusOne < 0) {
        embeddedToolError(tool, "Please provide `-d` to specify a directory to place output classes in.")
      }
      val outSpec = argsList[argsList.indexOf("-d") + 1]

      val sources = argsList.filter {
        !it.startsWith("-") && (it != outSpec)
      }.map {
        Paths.get(it)
      }
      return sources.asSequence() to outSpec
    }

    /**
     * Create inputs for the Java compiler which include the provided source paths.
     *
     * @param sequence Sequence of source paths to include.
     * @return Compiler inputs.
     */
    @JvmStatic fun sources(sequence: Sequence<Path>): JavaCompilerInputs = JavaCompilerInputs.SourceFiles(
      sequence.toList().toPersistentList().also {
        if (it.isEmpty()) embeddedToolError(javac, "No source files provided")
      }
    )

    /**
     * Create outputs for the Java compiler which specify a directory of classes.
     *
     * @param at Expected path to the built class output.
     * @return Compiler outputs.
     */
    @JvmStatic fun classesDir(at: Path): JavaCompilerOutputs = JavaCompilerOutputs.Classes(at)

    /**
     * Create a Java Compiler instance from the provided inputs.
     *
     * @param args Arguments to the compiler.
     * @param inputs Inputs to the compiler.
     * @param outputs Outputs from the compiler.
     * @param env Environment for the compiler; defaults to the host environment.
     */
    @JvmStatic fun create(
      args: Arguments,
      env: Environment,
      inputs: JavaCompilerInputs,
      outputs: JavaCompilerOutputs,
    ): JavaCompiler = JavaCompiler(
      args = args,
      env = env,
      inputs = inputs,
      outputs = outputs,
    )
  }

  @CommandLine.Command(
    name = JAVAC,
    description = [JAVAC_DESCRIPTION],
    mixinStandardHelpOptions = false,
  )
  @Singleton
  @ReflectiveAccess
  @Introspected
  class JavacCliTool: DelegatedToolCommand<JavaCompiler>(javac) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec

    companion object {
      // Gather options, inputs, and outputs for an invocation of the Java compiler.
      @JvmStatic private fun gatherArgs(args: Arguments): Pair<JavaCompilerInputs, JavaCompilerOutputs> {
        val (sources, outSpec) = jvmStyleArgs(javac, args)
        val outputs = classesDir(Paths.get(outSpec))
        return Pair(sources(sources.asSequence()), outputs)
      }
    }

    override fun configure(args: Arguments, environment: Environment): JavaCompiler = gatherArgs(args).let { state ->
      JavaCompiler(
        args = args,
        env = environment,
        inputs = state.first,
        outputs = state.second,
      )
    }
  }
}
