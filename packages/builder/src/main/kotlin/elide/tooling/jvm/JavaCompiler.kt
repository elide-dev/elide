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
package elide.tooling.jvm

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import elide.runtime.Logging
import elide.runtime.diag.DiagnosticsContainer
import elide.runtime.diag.DiagnosticsReceiver
import elide.runtime.diag.DiagnosticsSuite
import elide.runtime.gvm.jvm.fromJavacDiagnostic
import elide.tooling.ArgumentContext
import elide.tooling.Arguments
import elide.tooling.Environment
import elide.tooling.Inputs
import elide.tooling.MutableArguments
import elide.tooling.Outputs
import elide.tooling.Tool
import elide.tooling.AbstractTool
import elide.tooling.cli.Statics
import elide.tooling.jvm.JavaCompiler.JavaCompilerInputs.SourceFiles
import elide.runtime.diag.Diagnostic as ElideDiagnostic

// Name of the `javac` tool.
public const val JAVAC: String = "javac"

// Build-time Java support flag.
public val javaIsSupported: Boolean = System.getProperty("elide.jvm") == "true"

// Description to show for `javac`.
public const val JAVAC_DESCRIPTION: String = "The Java compiler, supporting up to JDK 24."

// Tool description.
public val javac: Tool.CommandLineTool = Tool.describe(
  name = JAVAC,
  label = "Java Compiler",
  version = System.getProperty("java.version"),
  docs = URI.create("https://docs.oracle.com/javase/8/docs/technotes/guides/javac/index.html"),
  description = JAVAC_DESCRIPTION,
  registered = true,
  helpText = """
  Usage: elide javac <elide options> -- <options> <source files>
  Java: ${System.getProperty("java.version")}

  where possible elide options include:
    (None at this time.)

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
@ReflectiveAccess @Introspected public class JavaCompiler (
  public val args: Arguments,
  public val env: Environment,
  public val inputs: JavaCompilerInputs,
  public val outputs: JavaCompilerOutputs,
  public val processors: List<AnnotationProcessor> = emptyList(),
) : AbstractTool(info = javac.extend(
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
  public sealed interface JavaCompilerInputs : Inputs.Files {
    /**
     * Describes a sequence of source files to specify as Java sources.
     */
    @JvmInline public value class SourceFiles(public val files: PersistentList<Path>) : JavaCompilerInputs
  }

  /**
   * Java Compiler outputs.
   *
   * Implements understanding of Java compiler outputs -- class files only.
   */
  public sealed interface JavaCompilerOutputs {
    /**
     * Flatten into an [Outputs] type.
     *
     * @return Outputs value.
     */
    public fun flatten(): Outputs

    /** Defines a [directory] path to build class output. */
    @JvmInline public value class Classes(public val directory: Path) : JavaCompilerOutputs, Outputs.Disk.Directory {
      override fun flatten(): Outputs = this
    }
  }

  // Amend Java Compiler args with additional options that were passed in, or which enable Elide features.
  @Suppress("UNUSED_PARAMETER") private fun amendArgs(args: MutableArguments) {
    // Nothing at this time.
  }

  override suspend fun invoke(state: EmbeddedToolState): Tool.Result {
    val javaToolchainHome = resolveJavaToolchain(javac)
    val locale = Locale.getDefault()
    val charset = StandardCharsets.UTF_8
    val args = MutableArguments.from(info.args).also { amendArgs(it) }

    javacLogger.debug { "Initializing Java compiler support (home: $javaToolchainHome)" }
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
      (inputs as JavaCompilerInputs.SourceFiles).files.filter {
        it.exists() && it.isRegularFile()
      }
    )
    val compileStart = System.currentTimeMillis()
    val compileEnd: Long
    val srcsCount = inputs.files.size

    @Suppress("TooGenericExceptionCaught")
    try {
      val plural = if (srcsCount > 1) "source" else "source file"
      javacLogger.debug { "Preparing Java compile task for $srcsCount $plural" }

      val ctx = ArgumentContext.of(
        argSeparator = ' ',
        kvToken = ' ',
      )
      compiler.getTask(
        null,
        fileManager,
        diagnosticsReceiver,
        args.asArgumentList(ctx).filter { !it.endsWith(".java") }.flatMap {
          // @TODO fix this in the Arguments object instead
          if (it.startsWith("-classpath ")) {
            it.split(" ")
          } else listOf(
            it
          )
        },
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
      javacLogger.debug("Exception while compiling: $rxe", rxe)

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
    return Tool.Result.Success
  }

  /** Factories for configuring and obtaining instances of [JavaCompiler]. */
  public companion object {
    private const val USE_FALLBACK_JVM_TOOLCHAIN = true
    private val logging by lazy { Logging.of(JavaCompiler::class) }

    @Suppress("KotlinConstantConditions")
    @JvmStatic private fun fallbackToolchain(tool: Tool.CommandLineTool): String {
      if (!USE_FALLBACK_JVM_TOOLCHAIN) embeddedToolError(tool, "Java toolchain missing; please set `JAVA_HOME`")
      return Statics.resourcesPath.resolve("gvm").absolutePathString()
    }

    // Resolve the Java toolchain for the Kotlin compiler.
    @JvmStatic public fun resolveJavaToolchain(tool: Tool.CommandLineTool): Path {
      return Paths.get(
        (System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))?.ifBlank { null }
          ?: fallbackToolchain(tool)
      )
    }

    @JvmStatic public fun resolveFileArgInput(arg: String): List<String> {
      return try {
        Paths.get(arg.drop(1)).let { path ->
          if (path.isAbsolute) path else Paths.get(System.getProperty("user.dir")).resolve(path)
        }.absolute().also { path ->
          when {
            !path.exists() -> {
              logging.warn("Path for @-file argument does not exist: {}", path.absolutePathString())
            }
            !path.isReadable() -> {
              logging.warn("Path for @-file argument is not readable: {}", path.absolutePathString())
            }
            !path.isRegularFile() -> {
              logging.warn("Path for @-file argument is not a regular file: {}", path.absolutePathString())
            }
          }
        }.inputStream().bufferedReader(StandardCharsets.UTF_8).use { stream ->
          stream.lines().toList().filter { it.isNotEmpty() && it.isNotBlank() }
        }
      } catch (err: InvalidPathException) {
        logging.debug("Invalid path for @-file argument: {} (\"{}\")", err, arg)
        return listOf(arg) // fallback as-is
      }
    }

    @JvmStatic public fun jvmStyleArgs(
      tool: Tool.CommandLineTool,
      args: Arguments,
    ): Triple<Sequence<Path>, String, List<String>> {
      val argsList = args.asArgumentList().flatMap {
        if (it.startsWith("@")) {
          resolveFileArgInput(it)
        } else {
          listOf(it)
        }
      }.flatMap {
        if (it.startsWith("-d") && " " in it) {
          // If the argument is `-d` with a space, split it into two parts.
          it.split(" ", limit = 2).let { parts ->
            if (parts.size == 2) parts else listOf(it)
          }
        } else {
          listOf(it)
        }
      }
      val outSpecPositionMinusOne = argsList.indexOf("-d")
      if (outSpecPositionMinusOne < 0) {
        embeddedToolError(tool, "Please provide `-d` to specify a directory to place output classes in.")
      }
      val outSpec = argsList[argsList.indexOf("-d") + 1]

      val sources = argsList.filter {
        !it.startsWith("-") && (it != outSpec)
      }.map {
        Paths.get(it)
      }.filter {
        it.exists()
      }
      return Triple(sources.asSequence(), outSpec, argsList)
    }

    /**
     * Create inputs for the Java compiler which include the provided source paths.
     *
     * @param sequence Sequence of source paths to include.
     * @return Compiler inputs.
     */
    @JvmStatic public fun sources(sequence: Sequence<Path>): SourceFiles = SourceFiles(
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
    @JvmStatic public fun classesDir(at: Path): JavaCompilerOutputs = JavaCompilerOutputs.Classes(at)

    /**
     * Create a Java Compiler instance from the provided inputs.
     *
     * @param args Arguments to the compiler.
     * @param inputs Inputs to the compiler.
     * @param outputs Outputs from the compiler.
     * @param env Environment for the compiler; defaults to the host environment.
     */
    @JvmStatic public fun create(
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
}
