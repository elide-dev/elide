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

package elide.tool.cli.cmd.tool.detekt

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import picocli.CommandLine.ScopeType
import elide.tool.Arguments
import elide.tool.Environment
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.cli.cmd.tool.DelegatedToolCommand
import elide.tooling.kotlin.DETEKT
import elide.tooling.kotlin.DETEKT_DESCRIPTION
import elide.tooling.kotlin.Detekt
import elide.tooling.kotlin.detekt

@ReflectiveAccess @Introspected class DetektAdapter (
  private val detektTool: Detekt,
): ProjectAwareSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    TODO("Not yet implemented")
  }

  @CommandLine.Command(
    name = DETEKT,
    version = ["1.28.3"],
    description = [DETEKT_DESCRIPTION],
    mixinStandardHelpOptions = false,
    scope = ScopeType.LOCAL,
    synopsisHeading = "",
    customSynopsis = [],
  )
  @ReflectiveAccess
  @Introspected
  class DetektCliTool: DelegatedToolCommand<Detekt, DetektAdapter>(detekt) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec

    override fun configure(args: Arguments, environment: Environment): Detekt = Detekt(
      args = args,
      env = environment,
      inputs = Detekt.DetektInputs.Empty,
      outputs = Detekt.DetektOutputs.Empty,
    )

    override fun create(args: Arguments, environment: Environment): DetektAdapter = DetektAdapter(
      detektTool = configure(args, environment),
    )
  }
}
