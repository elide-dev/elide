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

package elide.tool.cli.cmd.info

import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.Engine
import picocli.CommandLine.Command
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.tool.cli.*
import elide.tool.cli.cfg.ElideCLITool
import elide.tool.engine.NativeEngine
import elide.tool.io.RuntimeWorkdirManager
import elide.tool.project.ProjectManager

/** TBD. */
@Command(
  name = "info",
  description = ["Show info about the current app and environment"],
  mixinStandardHelpOptions = true,
)
@Singleton internal class ToolInfoCommand @Inject constructor(
  private val projectManager: ProjectManager,
  private val workdir: RuntimeWorkdirManager,
) : AbstractSubcommand<ToolState, CommandContext>() {
  companion object {
    private fun Boolean.label(): String = if (this) "Yes" else "No"
  }

  // Check if the library group at the name `group` was loaded.
  private fun libraryGroupLoaded(group: String): Boolean = NativeEngine.didLoad(group)

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val version = ElideTool.version()
    val engine = Engine.create()
    val workingRoot = workdir.workingRoot()
    val tempRoot = workdir.tmpDirectory()
    val caches = workdir.cacheDirectory()
    val natives = workdir.nativesDirectory()
    val project = projectManager.resolveProject()
    val operatingMode = if (ImageInfo.inImageCode()) "Native" else "JVM"

    val projectBlock = if (project == null) StringBuilder("") else StringBuilder().apply {
      appendLine("Project:")
      appendLine("- Name: ${project.name ?: "(None)"}")
      appendLine("- Version: ${project.version ?: "(None)"}")
      appendLine("- Config: ${project.config?.path ?: "(None)"}")
      appendLine("- Root Path: ${project.root}")
      when (val env = project.env) {
        null -> {}
        else -> {
          appendLine("")
          appendLine("Environment:")
          env.vars.forEach {
            appendLine("- ${it.key} = ${it.value}")
          }
        }
      }
    }

    output {
      appendLine("Elide v${version} (${ElideCLITool.ELIDE_RELEASE_TYPE})")
      appendLine("Engine: ${engine.implementationName} v${engine.version}")
      appendLine("Platform: $operatingMode")
      appendLine("Languages: " + engine.languages.keys.joinToString(", "))
      appendLine()
      appendLine("Native:")
      appendLine("- Crypto: ${libraryGroupLoaded("crypto").label()}")
      appendLine("- Transport: ${libraryGroupLoaded("transport").label()}")
      appendLine()
      appendLine("Paths: ")
      appendLine("- Working Root: ${workingRoot.absolutePath}")
      appendLine("- Temporary Root: ${tempRoot.absolutePath}")
      appendLine("- Native Libs: ${natives.absolutePath}")
      appendLine("- Caches: ${caches.absolutePath}")
      if (projectBlock.isNotBlank()) {
        appendLine()
        append(projectBlock)
      }
    }
    return success()
  }
}
