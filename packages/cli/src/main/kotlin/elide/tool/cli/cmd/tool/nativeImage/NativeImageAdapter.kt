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

package elide.tool.cli.cmd.tool.nativeImage

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import java.nio.file.Path
import java.nio.file.Paths
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlin.io.path.absolutePathString
import elide.tooling.Arguments
import elide.tooling.Environment
import elide.tooling.Tool
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tooling.cli.Statics
import elide.tool.cli.ToolState
import elide.tool.cli.cmd.tool.DelegatedToolCommand
import elide.tooling.project.ProjectManager
import elide.tooling.AbstractTool
import elide.tooling.gvm.nativeImage.GRAALVM_VERSION
import elide.tooling.gvm.nativeImage.NATIVE_IMAGE
import elide.tooling.gvm.nativeImage.NI_COMPILER_DESCRIPTION
import elide.tooling.gvm.nativeImage.NativeImageDriver
import elide.tooling.gvm.nativeImage.NativeImageDriver.*
import elide.tooling.gvm.nativeImage.NativeImageDriver.Companion.nativeImageInputs
import elide.tooling.gvm.nativeImage.NativeImageDriver.Companion.outputBinary
import elide.tooling.gvm.nativeImage.NativeImageDriver.Companion.sharedLibrary
import elide.tooling.gvm.nativeImage.nativeImage
import elide.tooling.jvm.JavaCompiler
import elide.tooling.project.ElideProject

/**
 * # Native Image Driver Adapter
 *
 * Implements an [AbstractTool] adapter to `native-image`, the GraalVM Native Image compiler. Arguments are passed to
 * the compiler verbatim from the command-line.
 */
@ReflectiveAccess @Introspected class NativeImageAdapter private constructor (
  private val driver: NativeImageDriver,
): ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>
  private val projectManager: ProjectManager get() = projectManagerProvider.get()

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    if (verbose) output {
      append(
        """
      Invoking `native-image` with:

        -- Arguments:
        ${driver.info.args}

        -- Inputs:
        ${driver.inputs}

        -- Outputs:
        ${driver.outputs}

        -- Environment:
        ${driver.info.environment}
      """.trimIndent(),
      )
    }

    val compileStart = System.currentTimeMillis()
    val compileEnd: Long
    var toolErr: Throwable? = null
    val project = projectManager.resolveProject(projectOptions().projectPath())

    @Suppress("TooGenericExceptionCaught")
    val result: Tool.Result = try {
      driver.invoke(object: AbstractTool.EmbeddedToolState {
        override val resourcesPath: Path get() = Statics.resourcesPath
        override val project: ElideProject? get() = project
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
        success()
      }
      else -> {
        logging.error { "Tool failed: ${toolErr?.message}" }
        err()
      }
    }
  }

  @CommandLine.Command(
    name = NATIVE_IMAGE,
    version = [GRAALVM_VERSION],
    description = [NI_COMPILER_DESCRIPTION],
    mixinStandardHelpOptions = false,
    scope = CommandLine.ScopeType.LOCAL,
    synopsisHeading = "",
    customSynopsis = [],
  )
  @ReflectiveAccess
  @Introspected
  class NativeImageCliTool: DelegatedToolCommand<NativeImageDriver, NativeImageAdapter>(nativeImage) {
    private enum class NativeBuildMode {
      BINARY,
      SHARED_LIBRARY,
    }

    companion object {
      // Detects whether we are building a shared library or binary.
      @JvmStatic private fun detectMode(args: Arguments): NativeBuildMode {
        // @TODO not very efficient
        return when ("--shared" in args.asArgumentList()) {
          true -> NativeBuildMode.SHARED_LIBRARY
          false -> NativeBuildMode.BINARY
        }
      }

      @JvmStatic private fun nativeImageArgs(args: Arguments): Triple<Sequence<Path>, String, List<String>> {
        val argsList = args.asArgumentList().flatMap {
          if (it.startsWith("@")) {
            JavaCompiler.resolveFileArgInput(it)
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
        val outSpecPositionMinusOne = (
          when (val idx = argsList.indexOf("-H:Path")) {
            -1 -> argsList.indexOf("-o")
            else -> idx
          }
        )
        val outSpec = if (outSpecPositionMinusOne < 0) {
          // come up with an output path; use `.dev/artifacts/native-image` by default
          // @TODO resolve from config
          Paths.get(System.getProperty("user.dir"))
            .resolve(".dev")
            .resolve("artifacts")
            .resolve("native-image")
            .absolutePathString()
        } else {
          argsList[argsList.indexOf("-d") + 1]
        }

        val sources = argsList.filter {
          !it.startsWith("-") && (it != outSpec)
        }.flatMap {
          Paths.get(it)
        }
        return Triple(sources.asSequence(), outSpec, argsList)
      }

      // Gather options, inputs, and outputs for an invocation of the Native Image tool.
      @JvmStatic private fun gatherArgs(args: Arguments): Pair<NativeImageInputs, NativeImageOutputs> {
        return nativeImageArgs(args).let { (sources, outSpec) ->
          nativeImageInputs(sources) to when (detectMode(args)) {
            NativeBuildMode.BINARY -> outputBinary(Paths.get(outSpec))
            NativeBuildMode.SHARED_LIBRARY -> sharedLibrary(Paths.get(outSpec))
          }
        }
      }
    }

    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec
    @CommandLine.Parameters @Suppress("unused") lateinit var allParams: List<String>

    @CommandLine.Option(
      names = ["--no-inject"],
      description = ["Disable argument injection from tool and project environment"],
    )
    var noInject: Boolean = false

    @CommandLine.Option(
      names = ["--optimized"],
      description = ["Build an opt-mode binary."],
    )
    var enableOpt: Boolean = false

    // Choose an optimization level to use.
    private fun chooseOptSetting(explicitOptSetting: String?): String = when (explicitOptSetting) {
      null -> when {
        commons().debug -> "-O0"
        enableOpt -> "-O3"
        else -> "-Ob"  // optimize for build time
      }
      else -> explicitOptSetting
    }

    override fun configure(args: Arguments, environment: Environment): NativeImageDriver {
      return gatherArgs(args).let { state ->
        NativeImageDriver.create(
          args = args,
          env = environment,
          inputs = state.first,
          outputs = state.second,
        ) {
          // @TODO don't serialize args everywhere
          val argsList = args.asArgumentList()
          val explicitOptSetting = argsList.find { it.startsWith("-O") && it.length == 3 }
          val effectiveOptSetting = chooseOptSetting(explicitOptSetting)

          // configure defaults from project/tool env
          if (!noInject) {
            if (explicitOptSetting == null) {
              this.args.add(effectiveOptSetting)
            }
            this.args.add("-H:-BuildOutputRecommendations")
          }
        }
      }
    }

    override fun create(args: Arguments, environment: Environment): NativeImageAdapter = NativeImageAdapter(
      driver = configure(
        args,
        environment,
      )
    )
  }
}
