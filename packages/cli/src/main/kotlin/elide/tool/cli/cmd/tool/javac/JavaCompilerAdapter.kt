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
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import kotlin.io.path.relativeTo
import elide.tooling.Arguments
import elide.tooling.Environment
import elide.tooling.Tool
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tooling.cli.Statics
import elide.tool.cli.ToolState
import elide.tool.cli.cmd.tool.DelegatedToolCommand
import elide.tooling.AbstractTool
import elide.tooling.jvm.JAVAC
import elide.tooling.jvm.JAVAC_DESCRIPTION
import elide.tooling.jvm.JavaCompiler
import elide.tooling.jvm.JavaCompiler.Companion.classesDir
import elide.tooling.jvm.JavaCompiler.Companion.jvmStyleArgs
import elide.tooling.jvm.JavaCompiler.Companion.resolveJavaToolchain
import elide.tooling.jvm.JavaCompiler.Companion.sources
import elide.tooling.jvm.JavaCompiler.JavaCompilerInputs
import elide.tooling.jvm.JavaCompiler.JavaCompilerOutputs
import elide.tooling.jvm.javac

/**
 * # Java Compiler
 *
 * Implements an [AbstractTool] adapter to `javac`, the Java compiler. Arguments are passed to the compiler verbatim
 * from the command-line.
 */
@ReflectiveAccess @Introspected class JavaCompilerAdapter private constructor (
  private val compiler: JavaCompiler,
): ProjectAwareSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val javaToolchainHome = resolveJavaToolchain(javac)
    val locale = Locale.getDefault()
    val charset = StandardCharsets.UTF_8

    output {
      """
        Invoking `javac` with:
  
          Locale: $locale
          Charset: $charset
          Java Home: $javaToolchainHome
  
          -- Arguments:
          ${compiler.args}
  
          -- Inputs:
          ${compiler.inputs}
  
          -- Outputs:
          ${compiler.outputs}
  
          -- Environment:
          ${compiler.info.environment}
      """.trimIndent().also {
        if (verbose) {
          append(it)
        }
      }
    }

    val compileStart = System.currentTimeMillis()
    val compileEnd: Long
    val inputs = (compiler.inputs as JavaCompilerInputs.SourceFiles)
    val srcsCount = inputs.files.size
    var toolErr: Throwable? = null

    @Suppress("TooGenericExceptionCaught")
    val result: Tool.Result = try {
      compiler.invoke(object: AbstractTool.EmbeddedToolState {
        override val resourcesPath: Path get() = Statics.resourcesPath
      })
      Tool.Result.Success
    } catch (rxe: RuntimeException) {
      toolErr = rxe
      Tool.Result.UnspecifiedFailure
    } finally {
      compileEnd = System.currentTimeMillis()
    }
    return when (result) {
      is Tool.Result.Success -> {
        val totalMs = compileEnd - compileStart
        val outputPath = (compiler.outputs as JavaCompilerOutputs.Classes).directory
        val outputRelativeToCwd = outputPath
          .toAbsolutePath()
          .relativeTo(Paths.get(System.getProperty("user.dir")))

        output {
          val sourceFiles = if (srcsCount > 1) "sources" else "source file"
          append("[javac] Compiled $srcsCount $sourceFiles in ${totalMs}ms → $outputRelativeToCwd")
        }
        success()
      }
      else -> err(toolErr?.message ?: "Failed to compile")
    }
  }

  @CommandLine.Command(
    name = JAVAC,
    description = [JAVAC_DESCRIPTION],
    mixinStandardHelpOptions = false,
  )
  @ReflectiveAccess
  @Introspected
  class JavacCliTool: DelegatedToolCommand<JavaCompiler, JavaCompilerAdapter>(javac) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec
    @CommandLine.Parameters @Suppress("unused") lateinit var allParams: List<String>

    companion object {
      // Gather options, inputs, and outputs for an invocation of the Java compiler.
      @JvmStatic private fun gatherArgs(
        args: Arguments,
      ): Triple<JavaCompilerInputs, JavaCompilerOutputs, List<String>> {
        val (sources, outSpec, allArgs) = jvmStyleArgs(javac, args)
        val outputs = classesDir(Paths.get(outSpec))
        return Triple(sources(sources.asSequence()), outputs, allArgs)
      }
    }

    override fun configure(args: Arguments, environment: Environment): JavaCompiler = gatherArgs(args).let { state ->
      JavaCompiler(
        args = Arguments.from(state.third),
        env = environment,
        inputs = state.first,
        outputs = state.second,
      )
    }

    override fun create(args: Arguments, environment: Environment): JavaCompilerAdapter = JavaCompilerAdapter(
      compiler = configure(
        args,
        environment,
      )
    )
  }
}
