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

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package elide.tool.cli.cmd.tool.jar

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.collections.immutable.toPersistentList
import kotlin.io.path.relativeTo
import elide.tool.Argument
import elide.tool.Arguments
import elide.tool.Environment
import elide.tool.Tool
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.Statics
import elide.tool.cli.ToolState
import elide.tool.cli.cmd.tool.DelegatedToolCommand
import elide.tooling.AbstractTool
import elide.tooling.GenericTool.Companion.gatherArgs
import elide.tooling.jvm.JAR
import elide.tooling.jvm.JARTOOL_DESCRIPTION
import elide.tooling.jvm.JarTool
import elide.tooling.jvm.jartool

// Argument names which require a value following, or separated by `=`.
private val argNamesThatExpectValues = sortedSetOf(
  "-f", "--file",
  "-i", "--generate-index",
  "--release",
  "-e", "--main-class",
  "-m", "--manifest",
  "--module-version",
  "--hash-modules",
  "-p", "--module-path",
  "--date",
  "--dir",
)

/**
 * # JAR Tool
 *
 * Implements an adapter to `jar`. Arguments are passed to the tool verbatim from the command-line.
 */
@ReflectiveAccess @Introspected class JarToolAdapter private constructor (
  args: Arguments,
  env: Environment,
  inputs: JarTool.JarToolInputs,
  outputs: JarTool.JarToolOutputs,
  private val jartool: JarTool = JarTool(args, env, inputs, outputs),
): ProjectAwareSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val srcs = (jartool.inputs as JarTool.JarToolInputs.InputFiles).files
    val inputsCount = srcs.size
    val plural = if (inputsCount > 1) "inputs" else "input file"
    val outputPath: Path? = when (val outs = jartool.outputs) {
      is JarTool.JarToolOutputs.Jar -> outs.path
      is JarTool.JarToolOutputs.NoOutputs -> null
    }
    val outputRelativeToCwd = outputPath
      ?.toAbsolutePath()
      ?.relativeTo(Paths.get(System.getProperty("user.dir")))
    val start = System.currentTimeMillis()

    val result = when (jartool.invoke(object: AbstractTool.EmbeddedToolState {
      override val resourcesPath: Path get() = Statics.resourcesPath
    })) {
      is Tool.Result.Success -> {
//        val regularOutput = jartool.out.toString()
//        val regularError = jartool.err.toString()
//
//        if (regularOutput.isNotEmpty()) {
//          output {
//            append(regularOutput)
//          }
//        }
//        if (regularError.isNotEmpty()) {
//          output {
//            append(regularError)
//          }
//        }
        output {
          val ms = System.currentTimeMillis() - start
          val outputOrNothing = if (outputRelativeToCwd != null) " → $outputRelativeToCwd" else ""
          append("[jar] Assembled $inputsCount $plural in ${ms}ms$outputOrNothing")
        }
        success()
      }
      else -> err("Failed to run jar tool")
    }
    return result
  }

  @CommandLine.Command(
    name = JAR,
    description = [JARTOOL_DESCRIPTION],
    mixinStandardHelpOptions = false,
  )
  @ReflectiveAccess
  @Introspected
  class JarCliTool: DelegatedToolCommand<JarTool, JarToolAdapter>(jartool) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec
    @CommandLine.Parameters @Suppress("unused") lateinit var allParams: List<String>

    override fun configure(args: Arguments, environment: Environment): JarTool = gatherArgs(
      argNamesThatExpectValues,
      args,
    ).let { (effective, likelyInputs) ->
      // gather all inputs which should be paths; parse them.
      val parsedInputs = likelyInputs.mapNotNull {
        try {
          Paths.get(it)
        } catch (_: IllegalArgumentException) {
          // skip: not a path
          return@mapNotNull null
        }.takeIf {
          // only extant paths can be passed to jar tool, unless we are specifying an output, in which case the arg is
          // prefixed or assigned with `=` and so is not present here.
          Files.exists(it)
        }
      }.let {
        when (it.size) {
          0 -> JarTool.JarToolInputs.NoInputs
          else -> JarTool.JarToolInputs.InputFiles(it.toPersistentList())
        }
      }

      // resolve directory/jar output path args
      val fileArg = effective.find {
        it is Argument.KeyValueArg && (it.name == "-f" || it.name == "--file")
      }?.let {
        Paths.get((it as Argument.KeyValueArg).value)
      }
      val dirArg = effective.find {
        it is Argument.KeyValueArg && (it.name == "--directory")
      }?.let {
        Paths.get((it as Argument.KeyValueArg).value)
      }
      JarTool(
        args = args,
        env = environment,
        inputs = parsedInputs,
        outputs = when (val outfile = fileArg) {
          null -> JarTool.JarToolOutputs.NoOutputs
          else -> JarTool.JarToolOutputs.Jar(dirArg to outfile)
        },
      )
    }

    override fun create(args: Arguments, environment: Environment): JarToolAdapter = JarToolAdapter(
      args = args,
      env = environment,
      inputs = JarTool.JarToolInputs.NoInputs,
      outputs = JarTool.JarToolOutputs.NoOutputs,
      jartool = configure(
        args,
        environment,
      ),
    )
  }
}
