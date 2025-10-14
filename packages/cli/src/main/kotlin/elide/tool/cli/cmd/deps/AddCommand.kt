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

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import jakarta.inject.Inject
import jakarta.inject.Provider
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tooling.project.ProjectManager

@CommandLine.Command(
  name = "add",
  mixinStandardHelpOptions = true,
  description = [
    "For this or a specified project, add one or more dependencies, and then install them. " +
    "Dependency coordinates are accepted in many forms; see below.",
    "",
    "Running @|bold elide add|@ without arguments begins an interactive session to find and add a library." +
    "Running @|bold elide add <package...>|@ will add one or more packages to the project's dependency manifest(s).",
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
    "elide @|bold,fg(cyan) add|@",
    "   or: elide @|bold,fg(cyan) add|@ [OPTIONS] [PACKAGE...] [--] [ARGS]",
    "   or: elide @|bold,fg(cyan) add|@ [@|bold,fg(cyan) -p|@/@|bold,fg(cyan) --project|@=<path>] [OPTIONS] " +
            "[PACKAGE...]",
    "",
  ],
)
@Introspected
@ReflectiveAccess
internal class AddCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>

  /** Package names or coordinates to install. */
  @CommandLine.Parameters(
    index = "0",
    description = ["Packages to install."],
    scope = CommandLine.ScopeType.LOCAL,
    arity = "0..*",
    paramLabel = "PACKAGE",
  )
  internal var packageSpecs: List<String> = emptyList()

  @CommandLine.Option(
    names = ["--save"],
    negatable = true,
    description = [
      "Whether to save the package to the project manifest. " +
      "If not specified, the package will be saved by default.",
    ],
    scope = CommandLine.ScopeType.LOCAL,
    defaultValue = "true",
  )
  internal var save: Boolean = true

  @CommandLine.Option(
    names = ["--dev"],
    negatable = true,
    description = [
      "Add the packages as development dependencies only. Applies to NPM; ignored otherwise.",
    ],
    scope = CommandLine.ScopeType.LOCAL,
    defaultValue = "false",
  )
  internal var saveDev: Boolean = false

  @CommandLine.Option(
    names = ["--test"],
    negatable = true,
    description = [
      "Add the packages as test dependencies only. Applies to Maven; ignored otherwise.",
    ],
    scope = CommandLine.ScopeType.LOCAL,
    defaultValue = "false",
  )
  internal var saveTest: Boolean = false

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    TODO("Not yet implemented")
  }
}
