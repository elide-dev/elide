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

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.Engine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import jakarta.inject.Provider
import elide.annotations.Inject
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
@Introspected
@ReflectiveAccess
internal class ToolInfoCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  companion object {
    private fun Boolean.label(): String = if (this) "Yes" else "No"
  }

  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>
  @Inject private lateinit var workdirProvider: Provider<RuntimeWorkdirManager>
  private val projectManager: ProjectManager by lazy { projectManagerProvider.get() }
  private val workdir: RuntimeWorkdirManager by lazy { workdirProvider.get() }

  // Check if the library group at the name `group` was loaded.
  private fun libraryGroupLoaded(group: String): Boolean = NativeEngine.didLoad(group)

  @Option(
    names = ["--all", "-a"],
    hidden = true,
    description = ["Show all info, including internal debug information"],
  )
  internal var showAll: Boolean = false

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    Elide.requestNatives(
      server = true,
      tooling = true,
    )
    val version = Elide.version()
    val engine = Engine.create()
    val projectRoot = workdir.projectRoot()
    val workingRoot = workdir.workingRoot()
    val tempRoot = workdir.tmpDirectory()
    val caches = workdir.cacheDirectory()
    val natives = workdir.nativesDirectory()
    val project = projectManager.resolveProject(projectOptions().projectPath())
    val operatingMode = if (ImageInfo.inImageCode()) "Native" else "JVM"
    val lang = System.getProperty("user.language") ?: "(unknown)"
    val country = System.getProperty("user.country") ?: "(unknown)"

    val projectBlock = if (project == null) StringBuilder("") else StringBuilder().apply {
      appendLine("Project:")
      appendLine("- Name: ${project.manifest.name ?: "(None)"}")
      appendLine("- Version: ${project.manifest.version ?: "(None)"}")
      appendLine("- Root Path: ${project.root}")

      when (val env = project.env) {
        null -> {}
        else -> {
          appendLine()
          appendLine("Environment:")
          when (env.vars.isEmpty()) {
            true -> appendLine("  (None).")
            else -> env.vars.forEach {
              appendLine("- ${it.key} = ${it.value}")
            }
          }
        }
      }
    }

    val allInfo = if (!showAll) null else StringBuilder().apply {
      // nothing yet
    }

    output {
      val (transportEngine, _) = NativeEngine.transportEngine()
      appendLine("Elide v${version} (${ElideCLITool.ELIDE_RELEASE_TYPE})")
      appendLine("Engine: ${engine.implementationName} v${engine.version}")
      appendLine("Platform: $operatingMode")
      appendLine("Languages: " + engine.languages.keys.joinToString(", "))
      appendLine()
      appendLine("Locale:")
      appendLine("- Language: $lang")
      appendLine("- Country: $country")
      appendLine()
      appendLine("Native:")
      appendLine("- Console: ${libraryGroupLoaded("console").label()}")
      appendLine("- Crypto: ${libraryGroupLoaded("crypto").label()}")
      appendLine("- SQLite: ${libraryGroupLoaded("sqlite").label()}")
      appendLine("- Tools: ${libraryGroupLoaded("tools").label()}")
      appendLine("- Transport: ${libraryGroupLoaded("transport").label()} (${transportEngine})")
      appendLine()
      appendLine("Paths:")
      appendLine("- Project Root: ${projectRoot?.absolutePath}")
      appendLine("- Working Root: ${workingRoot.absolutePath}")
      appendLine("- Temporary Root: ${tempRoot.absolutePath}")
      appendLine("- Native Libs: ${natives.absolutePath}")
      appendLine("- Caches: ${caches.absolutePath}")
      if (projectBlock.isNotBlank()) {
        appendLine()
        append(projectBlock)
      }
      if (allInfo != null) {
        appendLine()
        appendLine(allInfo)
      }
    }
    return success()
  }
}
