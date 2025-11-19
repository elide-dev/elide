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

package elide.tool.cli.cmd.adopt

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Command
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState

/**
 * Adopt build files from other tools to Elide format.
 *
 * This command provides subcommands for adopting from various build tools:
 * - maven: Adopt Maven pom.xml to elide.pkl
 * - gradle: Adopt Gradle build files to elide.pkl
 */
@Command(
  name = "adopt",
  mixinStandardHelpOptions = true,
  subcommands = [
    MavenAdoptCommand::class,
    GradleAdoptCommand::class,
    BazelAdoptCommand::class,
    NodeAdoptCommand::class,
  ],
  description = [
    "Adopt build files from other tools to Elide format.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) adopt|@ <COMMAND>",
    "",
    "Available Commands:",
    "  @|bold maven|@     Adopt Maven pom.xml to elide.pkl",
    "  @|bold gradle|@    Adopt Gradle build files to elide.pkl",
    "  @|bold bazel|@     Adopt Bazel BUILD and WORKSPACE files to elide.pkl",
    "  @|bold node|@      Adopt Node.js package.json to elide.pkl",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal class AdoptCommand : AbstractSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // This is a parent command, so it just shows help if called directly
    output {
      append("Use 'elide adopt <command>' to adopt build files.")
      appendLine()
      append("Available commands:")
      append("  maven     Adopt Maven pom.xml to elide.pkl")
      append("  gradle    Adopt Gradle build files to elide.pkl")
      append("  bazel     Adopt Bazel BUILD and WORKSPACE files to elide.pkl")
      append("  node      Adopt Node.js package.json to elide.pkl")
    }
    return success()
  }
}
