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

package elide.tool.cli

import picocli.CommandLine
import elide.tool.cli.options.ProjectOptions

/**
 * ## Project-aware Subcommand
 *
 * Adds base implementations of project-related options and other logic related to projects.
 */
abstract class ProjectAwareSubcommand<State: ToolState, Context: CommandContext>: AbstractSubcommand<State, Context>() {
  // Common options shared by all commands.
  @CommandLine.ArgGroup(
    heading = "%nProject Options:%n",
    exclusive = false,
  )
  private var projectOptions: ProjectOptions = ProjectOptions()

  /** @return Effective or merged project options. */
  protected fun projectOptions(): ProjectOptions = projectOptions
}
