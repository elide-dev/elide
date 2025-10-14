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

package elide.tool.cli.cmd.tool.jib

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import java.nio.file.Path
import jakarta.inject.Inject
import jakarta.inject.Provider
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
import elide.tooling.containers.JIB
import elide.tooling.containers.JIB_DESCRIPTION
import elide.tooling.containers.JIB_VERSION
import elide.tooling.containers.JibDriver
import elide.tooling.containers.JibDriver.*
import elide.tooling.containers.jib
import elide.tooling.project.ElideProject

/**
 * # Jib Adapter
 *
 * Implements an [AbstractTool] adapter to `jib`, a command-line container builder from Google.
 */
@ReflectiveAccess @Introspected class JibAdapter private constructor (
  private val driver: JibDriver,
): ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>
  private val projectManager: ProjectManager get() = projectManagerProvider.get()

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    if (verbose) output {
      append(
        """
      Invoking `jib` with:

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
        success().also {
            logging.info { "Jib finished in ${totalMs}ms" }
        }
      }
      else -> {
        logging.error { "Tool failed: ${toolErr?.message}" }
        err()
      }
    }
  }

  @CommandLine.Command(
    name = JIB,
    version = [JIB_VERSION],
    description = [JIB_DESCRIPTION],
    mixinStandardHelpOptions = false,
    scope = CommandLine.ScopeType.LOCAL,
    synopsisHeading = "",
    customSynopsis = [],
  )
  @ReflectiveAccess
  @Introspected
  class JibCliTool: DelegatedToolCommand<JibDriver, JibAdapter>(jib) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec
    @CommandLine.Parameters @Suppress("unused") lateinit var allParams: List<String>

    private companion object {
      // Build inputs for Jib from arguments.
      @JvmStatic private fun buildJibInputs(@Suppress("unused") args: Arguments): JibInputs {
        return JibInputs.NoInputs  // @TODO
      }

      // Build outputs for Jib from arguments.
      @JvmStatic private fun buildJibOutputs(@Suppress("unused") args: Arguments): JibOutputs {
        return JibOutputs.NoOutputs  // @TODO
      }

      // Build arguments, inputs, and outputs, for Jib.
      @JvmStatic private fun buildArgs(args: Arguments): Triple<Arguments, JibInputs, JibOutputs> {
        return Triple(
          args,
          buildJibInputs(args),
          buildJibOutputs(args),
        )
      }
    }

    override fun configure(args: Arguments, environment: Environment): JibDriver = buildArgs(args).let {
      JibDriver(
        args = it.first,
        env = environment,
        inputs = it.second,
        outputs = it.third,
      )
    }

    override fun create(args: Arguments, environment: Environment): JibAdapter = JibAdapter(
      driver = configure(
        args,
        environment,
      )
    )
  }
}
