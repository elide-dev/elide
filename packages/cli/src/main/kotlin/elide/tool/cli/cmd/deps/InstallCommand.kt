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

package elide.tool.cli.cmd.deps

import io.micronaut.context.BeanContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.coroutineScope
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.Elide
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.project.ProjectManager
import elide.tooling.project.CompositePackageManifestService
import elide.tooling.builder.BuildDriver
import elide.tooling.builder.BuildDriver.dependencies
import elide.tooling.builder.BuildDriver.resolve
import elide.tooling.project.ElideProject

@CommandLine.Command(
  name = "install",
  aliases = ["i"],
  mixinStandardHelpOptions = true,
  description = [
    "For this or a specified project, resolve and install all dependencies. " +
    "Dependencies are installed for all declared ecosystems, modulo provided flags.",
    "",
    "Running @|bold elide install|@ without arguments installs all dependencies visible to the project." +
    "Running @|bold elide install <ecosystem...>|@ installs the transitive closure for each specified ecosystem.",
    "",
    "Project structure and dependencies are managed via @|bold elide.pkl|@, or via foreign manifests such as " +
      "@|bold package.json|@.",
    "",
    "Supported ecosystems include:",
    "  - @|bold,fg(cyan) npm|@ (Node.js)",
    "  - @|bold,fg(cyan) pip|@ (Python)",
    "  - @|bold,fg(cyan) maven|@ (Maven)",
    "",
    "For more information, run @|fg(magenta) elide help projects|@.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) install|@",
    "   or: elide @|bold,fg(cyan) install|@ [OPTIONS] [ECOSYSTEM...] [--] [ARGS]",
    "   or: elide @|bold,fg(cyan) install|@ [@|bold,fg(cyan) -p|@/@|bold,fg(cyan) --project|@=<path>] [OPTIONS] " +
            "[ECOSYSTEM...]",
    "",
  ],
)
@Introspected
@ReflectiveAccess
internal class InstallCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var beanContext: BeanContext
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>
  @Inject private lateinit var compositeManifestResolver: Provider<CompositePackageManifestService>

  /** Ecosystems for which dependencies should be installed. */
  @CommandLine.Parameters(
    index = "0",
    description = ["Ecosystems to install dependencies for."],
    scope = CommandLine.ScopeType.LOCAL,
    arity = "0..*",
    defaultValue = "(all)",
    paramLabel = "ECOSYSTEM",
  )
  internal var ecosystems: List<String> = emptyList()

  // Install dependencies for an Elide project.
  private suspend fun CommandContext.installDepsForProject(project: ElideProject): CommandResult = coroutineScope {
    BuildDriver.configure(beanContext, project).let { config ->
      try {
        resolve(config, dependencies(config).await())
        success()
      } catch (e: Exception) {
        err("Failed to install dependencies: ${e.message}")
      }
    }
  }

  // Resolve and install dependencies from foreign manifests.
  private suspend fun CommandContext.installForeignManifestDepsOnly(): CommandResult {
    TODO("Foreign-manifest installation is not implemented yet")
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // tools typically require native access; force early init
    Elide.requestNatives(server = false, tooling = true)

    return when (val project = projectManagerProvider.get().resolveProject(projectOptions().projectPath)) {
      null -> installForeignManifestDepsOnly()
      else -> installDepsForProject(project)
    }
  }
}
