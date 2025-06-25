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
import picocli.CommandLine
import picocli.CommandLine.ScopeType
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.relativeTo
import elide.runtime.gvm.kotlin.KotlinLanguage
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
import elide.tooling.jvm.JavaCompiler.Companion.jvmStyleArgs
import elide.tooling.kotlin.KOTLINC
import elide.tooling.kotlin.KOTLIN_COMPILER_DESCRIPTION
import elide.tooling.kotlin.KotlinCompiler
import elide.tooling.kotlin.KotlinCompiler.Companion.classesDir
import elide.tooling.kotlin.KotlinCompiler.Companion.outputJar
import elide.tooling.kotlin.KotlinCompiler.Companion.sources
import elide.tooling.kotlin.KotlinCompiler.KotlinCompilerInputs
import elide.tooling.kotlin.KotlinCompiler.KotlinCompilerOutputs
import elide.tooling.kotlin.kotlinc

/**
 * # Kotlin Compiler
 *
 * Implements an [AbstractTool] adapter to `kotlinc`, the Kotlin compiler. Arguments are passed to the compiler verbatim
 * from the command-line.
 */
@ReflectiveAccess @Introspected class KotlinCompilerAdapter private constructor (
  private val compiler: KotlinCompiler,
): ProjectAwareSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    if (verbose) output {
      append(
        """
      Invoking `kotlinc` with:

        -- Arguments:
        ${compiler.info.args}

        -- Inputs:
        ${compiler.inputs}

        -- Outputs:
        ${compiler.outputs}

        -- Environment:
        ${compiler.info.environment}
      """.trimIndent(),
      )
    }

    val compileStart = System.currentTimeMillis()
    val compileEnd: Long
    var toolErr: Throwable? = null

    @Suppress("TooGenericExceptionCaught")
    val result: Tool.Result = try {
      compiler.invoke(object: AbstractTool.EmbeddedToolState {
        override val resourcesPath: Path get() = Statics.resourcesPath
      })
      Tool.Result.Success
    } catch (err: RuntimeException) {
      toolErr = err
      Tool.Result.UnspecifiedFailure
    } finally {
      compileEnd = System.currentTimeMillis()
    }
    return when (result) {
      Tool.Result.Success -> {
        val totalMs = compileEnd - compileStart
        val srcsCount = (compiler.inputs as KotlinCompilerInputs.SourceFiles).files.size
        val outputPath = when (val outs = compiler.outputs) {
          is KotlinCompilerOutputs.Jar -> outs.path
          is KotlinCompilerOutputs.Classes -> outs.directory
        }
        val outputRelativeToCwd = outputPath
          .toAbsolutePath()
          .relativeTo(Paths.get(System.getProperty("user.dir")))

        output {
          val sourceFiles = if (srcsCount > 1) "sources" else "source file"
          append("[kotlinc] Compiled $srcsCount $sourceFiles in ${totalMs}ms → $outputRelativeToCwd")
        }
        success()
      }
      else -> {
        logging.error { "Tool failed: ${toolErr?.message}" }
        err()
      }
    }
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
  class KotlinCliTool: DelegatedToolCommand<KotlinCompiler, KotlinCompilerAdapter>(kotlinc) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec
    @CommandLine.Parameters @Suppress("unused") lateinit var allParams: List<String>

    @CommandLine.Option(
      names = ["--no-default-plugins"],
      description = ["Disable default suite of Kotlin Compiler plugins"],
    )
    var noDefaultPlugins: Boolean = false

    @CommandLine.Option(
      names = ["--tests"],
      description = ["Compile in test mode, which enables things like PowerAssert"],
    )
    var tests: Boolean = false

    companion object {
      // Gather options, inputs, and outputs for an invocation of the Kotlin compiler.
      @JvmStatic private fun gatherArgs(args: Arguments): Pair<KotlinCompilerInputs, KotlinCompilerOutputs> {
        return jvmStyleArgs(kotlinc, args).let { (sources, outSpec) ->
          sources(sources) to when (outSpec.endsWith(".jar")) {
            true -> outputJar(Paths.get(outSpec))
            false -> classesDir(Paths.get(outSpec))
          }
        }
      }
    }

    override fun configure(args: Arguments, environment: Environment): KotlinCompiler {
      return gatherArgs(args).let { state ->
        KotlinCompiler.create(
          args = args,
          env = environment,
          inputs = state.first,
          outputs = state.second,
          test = tests,
        ) {
          // configure default plugins
          if (!noDefaultPlugins) {
            KotlinCompiler.configureDefaultPlugins(this, tests)
          }
        }
      }
    }

    override fun create(args: Arguments, environment: Environment): KotlinCompilerAdapter {
      return KotlinCompilerAdapter(
        compiler = configure(
          args,
          environment,
        )
      )
    }
  }
}
