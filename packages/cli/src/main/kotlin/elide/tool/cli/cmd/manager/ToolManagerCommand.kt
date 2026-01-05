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
package elide.tool.cli.cmd.manager

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.github.kinquirer.components.promptListObject
import com.github.kinquirer.core.Choice
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import picocli.CommandLine.Command
import jakarta.inject.Provider
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlin.time.Clock
import elide.annotations.Inject
import elide.manager.DownloadCompletedEvent
import elide.manager.DownloadProgressEvent
import elide.manager.DownloadStartEvent
import elide.manager.FileVerifyCompletedEvent
import elide.manager.FileVerifyIndeterminateEvent
import elide.manager.FileVerifyProgressEvent
import elide.manager.FileVerifyStartEvent
import elide.manager.InstallCompletedEvent
import elide.manager.InstallFileEvent
import elide.manager.InstallManager
import elide.manager.InstallProgressEvent
import elide.manager.InstallStartEvent
import elide.manager.UninstallCompletedEvent
import elide.manager.UninstallProgressEvent
import elide.manager.UninstallStartEvent
import elide.manager.VerifyCompletedEvent
import elide.manager.VerifyProgressEvent
import elide.manager.VerifyStartEvent
import elide.manager.repository.RepositoryManager
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.cli.cfg.ElideCLITool
import elide.tool.cli.progress.Progress
import elide.tool.cli.progress.TrackedTask
import elide.tooling.cli.Statics

/** Subcommand for managing concurrent installations of Elide. */
@Command(
  name = "manager",
  description = ["Manage Elide installations on this system"],
  mixinStandardHelpOptions = true,
)
@Introspected
@ReflectiveAccess
internal class ToolManagerCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var managerProvider: Provider<InstallManager>
  @Inject private lateinit var repositoryManagerProvider: Provider<RepositoryManager>
  private val manager: InstallManager by lazy { managerProvider.get() }
  private val repositoryManager: RepositoryManager by lazy { repositoryManagerProvider.get() }

  /** Specifies a version of Elide that should be installed. */
  @CommandLine.Option(
    names = ["--install-version"],
    description = ["Installs specified version of Elide if possible and exits."],
    paramLabel = "version",
  )
  private var installVersion: String? = null

  /** Specifies a version of Elide that should be uninstalled. */
  @CommandLine.Option(
    names = ["--uninstall-version"],
    description = ["Uninstalls specified version of Elide if possible and exits."],
    paramLabel = "version",
  )
  private var uninstallVersion: String? = null

  /** Specifies a path to install Elide to. */
  @CommandLine.Option(
    names = ["--install-path"],
    description = ["Specifies a path to install Elide to."],
    paramLabel = "path",
  )
  private var installPath: String? = null

  /** Specifies if confirmations should be ignored. */
  @CommandLine.Option(
    names = ["--no-confirm"],
    description = ["If specified, ignores confirmations."],
    defaultValue = "false",
  )
  private var noConfirm: Boolean = false

  /** Specifies if this command was run elevated by another Elide process. */
  @CommandLine.Option(
    names = ["--elevated"],
    defaultValue = "false",
    hidden = true,
  )
  private var elevated: Boolean = false

  /** Specifies if the repository catalog generator should be used. */
  @CommandLine.Option(
    names = ["--catalog"],
    defaultValue = "false",
    hidden = true,
  )
  private var catalog: Boolean = false

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    if (catalog) return catalogTool()
    if (installVersion != null && uninstallVersion != null) {
      return CommandResult.err(1, "--install-version and --uninstall-version must not be set simultaneously")
    }
    try {
      installVersion?.let {
        install(it)
        return CommandResult.success()
      }
      uninstallVersion?.let {
        uninstall(it)
        return CommandResult.success()
      }
      return mainMenu()
    } catch (e: Exception) {
      return CommandResult.err(1, e.message, e)
    }
  }

  private fun catalogTool(): CommandResult {
    val repositoryType = KInquirer.promptList("Do you want to create a local or a remote repository catalog?", listOf("local", "remote"))
    val path = Path(KInquirer.promptInput("Please enter the absolute path to the directory containing Elide packages"))
    if (!SystemFileSystem.exists(path)) return CommandResult.err(1, "Directory does not exist")
    val json = when (repositoryType) {
      "local" -> {
        val relativePaths = KInquirer.promptConfirm("Do you want to use relative paths?")
        repositoryManager.createLocalCatalog(path, relativePaths)
      }
      "remote" -> {
        val root = KInquirer.promptInput("Please enter the HTTPS address prefix")
        repositoryManager.createRemoteCatalog(path, root)
      }
      else -> return CommandResult.err(1, "Invalid repository type: $repositoryType")
    }
    SystemFileSystem.sink(Path(path, "catalog.json")).buffered().use {
      it.writeString(json)
    }
    return CommandResult.success()
  }

  private suspend fun mainMenu(): CommandResult {
    var running = true
    while (running) {
      when (KInquirer.promptListObject("Main menu:", MainMenuOptions.entries.map { Choice(it.displayName, it) })) {
        MainMenuOptions.LIST_INSTALLS -> Statics.terminal.println(listInstalls())
        MainMenuOptions.INSTALL -> install()
        MainMenuOptions.UNINSTALL -> uninstall()
        MainMenuOptions.LIST_ALL -> Statics.terminal.println(listAvailable())
        MainMenuOptions.EXIT -> running = false
      }
    }
    return CommandResult.success()
  }

  private fun listInstalls(): String =
    manager.getInstallations(true).asSequence().map {
      "${it.version.version} (${it.path})"
    }.joinToString("\n")

  private suspend fun listAvailable(): String =
    manager.getAvailable(false).asSequence().map {
      "${it.version} (${it.platform.platformString().replace('_', ' ')})"
    }.joinToString("\n")

  private suspend fun install(version: String? = null) {
    val installedVersions = manager.getInstallations(true).map { it.version }.toSet()
    val available = manager.getAvailable().filter { it !in installedVersions }
    if (available.isEmpty()) {
      Statics.terminal.println("No versions available")
      return
    }
    val version = if (version == null) {
      KInquirer.promptListObject("Please select a version to install:", available.map { Choice(it.version, it) })
    } else {
      available.find { it.version == version }
    }
    if (version == null) {
      Statics.terminal.println("Could not find elide version $version")
      return
    }
    val directory = installPath ?: KInquirer.promptList("Please select a path to install to:", manager.getInstallPaths())
    if (noConfirm || KInquirer.promptConfirm("Do you want to install Elide \"${version.version}\" to \"$directory\"?")) {
      val progress = Progress.create("Installing Elide version \"${version.version}\" to \"$directory\"", Statics.terminal) {
        add(TrackedTask("Download", 1000))
        add(TrackedTask("Verify", 1000))
        add(TrackedTask("Install", 1000))
        add(TrackedTask("Verify files", 1000))
      }
      manager.install(elevated, version.version, directory) {
        when(it) {
          is DownloadStartEvent -> {
            progress.updateTask(0) {
              copy(position = 0)
            }
            progress.start()
          }
          is DownloadProgressEvent -> {
            progress.updateTask(0) {
              copy(position = (it.progress * 1000).toInt())
            }
          }
          DownloadCompletedEvent -> {
            progress.updateTask(0) {
              copy(position = target)
            }
          }
          is VerifyStartEvent -> {
            progress.updateTask(1) {
              copy(position = 0)
            }
          }
          is VerifyProgressEvent -> {
            progress.updateTask(1) {
              copy(position = (it.progress * 1000).toInt())
            }
          }
          VerifyCompletedEvent -> {
              progress.updateTask(1) {
                copy(position = target)
              }
            }
          is InstallStartEvent -> {
            progress.updateTask(2) {
              copy(position = 0)
            }
          }
          is InstallProgressEvent -> {
            progress.updateTask(2) {
              copy(position = (it.progress * 1000).toInt())
            }
          }
          is InstallFileEvent -> {
            val time = Clock.System.now().toEpochMilliseconds()
            progress.updateTask(2) { copy(output = output + (time to it.name)) }
          }
          InstallCompletedEvent -> {
            progress.updateTask(2) {
              copy(position = target)
            }
          }
          is FileVerifyStartEvent -> {
            progress.updateTask(3) {
              copy(position = 0)
            }
          }
          is FileVerifyProgressEvent -> {
            val time = Clock.System.now().toEpochMilliseconds()
            progress.updateTask(3) { copy(position = (it.progress * 1000).toInt(), output = output + (time to it.name)) }
          }
          FileVerifyCompletedEvent -> {
            progress.updateTask(3) {
              copy(position = target)
            }
          }
          FileVerifyIndeterminateEvent -> {
            progress.updateTask(3) {
              copy(position = target, status = "no stampfile, individual files not verified")
            }
          }
        }
      }
      if (progress.running) progress.stop()
    }
  }

  private suspend fun uninstall(version: String? = null) {
    val current = ElideCLITool.ELIDE_TOOL_VERSION
    val installations = manager.getInstallations(false).filter { it.version.version != current }
    if (installations.isEmpty()) {
      Statics.terminal.println("No installs to uninstall")
      return
    }
    val install = if (version == null) {
      KInquirer.promptListObject("Please select a version to uninstall:", installations.map { Choice(it.path, it) })
    } else {
      installations.find { it.version.version == version }
    }
    if (install == null) {
      Statics.terminal.println("Could not find elide version $version")
      return
    }
    require(!elevated || installPath == install.path) { "Process is elevated and specified install path $installPath does not match found version install path ${install.path}" }
    if (noConfirm || KInquirer.promptConfirm("Do you want to uninstall Elide \"${install.version.version}\" from \"${install.path}\"?")) {
      val progress = Progress.create("Uninstalling Elide version \"${install.version.version}\" from \"${install.path}\"", Statics.terminal) {
        add(TrackedTask("Uninstall", 1000))
      }
      manager.uninstall(elevated, install) {
        when (it) {
          UninstallStartEvent -> {
            progress.updateTask(0) {
              copy(position = 0)
            }
            progress.start()
          }
          is UninstallProgressEvent -> {
            val time = Clock.System.now().toEpochMilliseconds()
            progress.updateTask(0) { copy(position = (it.progress * 1000).toInt(), output = output + (time to it.name)) }
          }
          UninstallCompletedEvent -> {
            progress.updateTask(0) {
              copy(position = target)
            }
          }
        }
      }
      if (progress.running) progress.stop()
    }
  }

  private enum class MainMenuOptions(val displayName: String) {
    LIST_INSTALLS("List installations"),
    INSTALL("Install a version of Elide"),
    UNINSTALL("Uninstall a version of Elide"),
    LIST_ALL("List available versions for all systems"),
    EXIT("Exit install management"),
  }
}
