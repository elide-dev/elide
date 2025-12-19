/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
@file:Suppress("ReturnCount")

package elide.tool.cli.cmd.manifest

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import elide.tool.cli.*
import elide.tooling.project.ProjectManager

@CommandLine.Command(
  name = "manifest",
  mixinStandardHelpOptions = true,
  description = [
    "For this or a specified project, resolve and render manifest information as JSON.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) manifest|@",
    "   or: elide @|bold,fg(cyan) manifest|@ [OPTIONS] [USAGE...]",
    "",
  ],
)
@Introspected
@ReflectiveAccess
internal class ManifestCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManager: ProjectManager

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    Elide.requestNatives(server = false, tooling = true)

    // resolve current or specified project
    val project = projectManager.resolveProject(
      projectOptions().projectPath(),
      object : ProjectManager.ProjectEvaluatorInputs {
        override val debug: Boolean get() = false
        override val release: Boolean get() = false
        override val params: List<String> get() = emptyList()
      },
    ) ?: return CommandResult.err(
      message = "No valid Elide project found",
    )

    // serialize resolved manifest
    val manifestJson = ExportJson.encodeToString(project.manifest)

    // print JSON manifest to output
    output { append(manifestJson) }

    return success()
  }

  private companion object {
    private val ExportJson by lazy {
      Json {
        prettyPrint = false
        encodeDefaults = false
        explicitNulls = false
      }
    }
  }
}
