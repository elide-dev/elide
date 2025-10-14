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

package elide.tool.cli.cmd.runner

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import picocli.CommandLine.Command
import java.nio.file.Path
import jakarta.inject.Inject
import jakarta.inject.Provider
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tooling.project.ProjectManager

@Command(
  name = "test",
  mixinStandardHelpOptions = true,
  description = [
    "For this or a specified project, run the testsuite, or a script mapped at the name " +
        "'test' within @|bold elide.pkl|@, or project manifests like @|bold package.json|@.",
    "",
    "Running @|bold elide test|@ without arguments runs all tests in the project's graph." +
        "Running @|bold elide test <task...>|@ runs the specified suite(s).",
    "",
    "After the `--` token, any arguments passed via the command-line are considered arguments" +
        " to the build. Such arguments are made available to executing tasks. Argument files are" +
        " supported and may be passed as @|bold @<file>|@.",
    "",
    "Project structure and dependencies are managed via @|bold elide.pkl|@.",
    "",
    "For more information, run @|fg(magenta) elide help projects|@.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) test|@",
    "   or: elide @|bold,fg(cyan) test|@ [OPTIONS] [TASKS] [--] [ARGS]",
    "   or: elide @|bold,fg(cyan) test|@ [@|bold,fg(cyan) -p|@/@|bold,fg(cyan) --project|@=<path>] [OPTIONS] " +
            "[TASKS] [--] [ARGS]",
    "",
  ],
)
@Introspected
@ReflectiveAccess
class ToolTestCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManager: Provider<ProjectManager>

  class CoverageOptions {
    @CommandLine.Option(
      names = ["--coverage"],
      description = ["Enable coverage (default: on)"],
      defaultValue = "true",
      negatable = true,
    )
    internal var enableCoverage: Boolean = true

    @CommandLine.Option(
      names = ["--coverage-report"],
      description = [
        "Add a coverage report (format via extension).",
        "Formats supported: 'json', 'lcov', 'xml', 'html'."
      ],
      paramLabel = "<path>",
      arity = "0..*",
    )
    internal var coverageReports: List<Path> = emptyList()
  }

  @CommandLine.ArgGroup(
    heading = "%nCoverage Options:%n",
    headingKey = "coverage",
    exclusive = false,
  )
  internal var coverageOptions: CoverageOptions = CoverageOptions()

  @CommandLine.Option(
    names = ["--tests"],
    description = ["Run matching test files, classes, or names"],
    paramLabel = "<glob>",
    arity = "0..*",
  )
  internal var testFilters: List<String> = emptyList()

  /** Script file arguments. */
  @CommandLine.Parameters(
    index = "0",
    description = ["Test tasks, suites, or scripts to run"],
    scope = CommandLine.ScopeType.LOCAL,
    arity = "0..*",
    paramLabel = "TASK",
  )
  internal var tasks: List<String>? = null

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    output {
      append("Test runner is not implemented yet.")
    }
    return success()
  }
}
