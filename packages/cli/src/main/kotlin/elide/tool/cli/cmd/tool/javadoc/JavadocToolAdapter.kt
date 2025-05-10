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

package elide.tool.cli.cmd.tool.javadoc

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import java.nio.file.Path
import elide.tool.Arguments
import elide.tool.Environment
import elide.tool.Tool
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.Statics
import elide.tool.cli.ToolState
import elide.tool.cli.cmd.tool.DelegatedToolCommand
import elide.tooling.GenericTool.Companion.gatherArgs
import elide.tooling.AbstractTool
import elide.tooling.jvm.JAVADOC
import elide.tooling.jvm.JAVADOCTOOL_DESCRIPTION
import elide.tooling.jvm.JavadocTool
import elide.tooling.jvm.javadoc

// Argument names which require a value following, or separated by `=`.
private val argNamesThatExpectValues = sortedSetOf(
  // Javadoc Parameters
  "--add-modules",
  "-bootclasspath",
  "--class-path", "-classpath", "-cp",
  "-doclet",
  "-docletpath",
  "-encoding",
  "-exclude",
  "--expand-requires",
  "-extdirs",
  "--legal-notices",
  "--limit-modules",
  "--module",
  "--module-path", "-p",
  "--module-source-path",
  "--release",
  "--show-members",
  "--show-module-contents",
  "--show-packages",
  "--show-types",
  "--source", "-source",
  "--source-path", "-sourcepath",
  "-subpackages",
  "--system",
  "--upgrade-module-path",

  // Doclet Parameters (Standard)
  "--add-script",
  "--add-stylesheet",
  "-bottom",
  "-charset",
  "-d",
  "-docencoding",
  "-doctitle",
  "-excludedocfilessubdir",
  "-footer",
  "-group",
  "-header",
  "-helpfile",
  "-link",
  "--link-modularity-mismatch",
  "-linkoffline",
  "--link-platform-properties",
  "--main-stylesheet", "-stylesheetfile",
  "-noqualifier",
  "--override-methods",
  "-overview",
  "--since",
  "--since-label",
  "--snippet-path",
  "-sourcetab",
  "--spec-base-url",
  "-taglet",
  "-top",
  "-windowtitle",
)

/**
 * # Javadoc Tool
 *
 * Implements an [AbstractTool] adapter to `javadoc`. Arguments are passed to the tool verbatim from the command-line.
 */
@ReflectiveAccess @Introspected class JavadocToolAdapter private constructor (
  private val jdoc: JavadocTool,
): ProjectAwareSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    return when (jdoc.invoke(object: elide.tooling.AbstractTool.EmbeddedToolState {
      override val resourcesPath: Path get() = Statics.resourcesPath
    })) {
      is Tool.Result.Success -> success()
      else -> err("Failed to run Javadoc")
    }
  }

  @CommandLine.Command(
    name = JAVADOC,
    description = [JAVADOCTOOL_DESCRIPTION],
    mixinStandardHelpOptions = false,
  )
  @ReflectiveAccess
  @Introspected
  class JavadocCliTool: DelegatedToolCommand<JavadocTool, JavadocToolAdapter>(javadoc) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec

    override fun configure(args: Arguments, environment: Environment): JavadocTool = gatherArgs(
      argNamesThatExpectValues,
      args,
    ).let { _ ->
      JavadocTool(
        args = args,
        env = environment,
        inputs = JavadocTool.JavadocInputs.NoInputs,
        outputs = JavadocTool.JavadocOutputs.NoOutputs,
      )
    }

    override fun create(args: Arguments, environment: Environment): JavadocToolAdapter = JavadocToolAdapter(
      jdoc = configure(args, environment),
    )
  }
}
