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
package elide.tool.cli.cmd.versions

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
import kotlin.io.path.absolutePathString
import kotlin.time.Clock
import elide.annotations.Inject
import elide.versions.DownloadCompletedEvent
import elide.versions.DownloadProgressEvent
import elide.versions.DownloadStartEvent
import elide.versions.FileVerifyCompletedEvent
import elide.versions.FileVerifyIndeterminateEvent
import elide.versions.FileVerifyProgressEvent
import elide.versions.FileVerifyStartEvent
import elide.versions.InstallCompletedEvent
import elide.versions.InstallFileEvent
import elide.versions.VersionManager
import elide.versions.InstallProgressEvent
import elide.versions.InstallStartEvent
import elide.versions.UninstallCompletedEvent
import elide.versions.UninstallProgressEvent
import elide.versions.UninstallStartEvent
import elide.versions.VerifyCompletedEvent
import elide.versions.VerifyProgressEvent
import elide.versions.VerifyStartEvent
import elide.versions.repository.VersionCatalogFactory
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.cli.cfg.ElideCLITool
import elide.tool.cli.progress.Progress
import elide.tool.cli.progress.TrackedTask
import elide.tooling.cli.Statics
import elide.versions.VersionsValues

/** Subcommand for managing concurrent installations of Elide. */
@Command(
  name = VersionsValues.VERSIONS_COMMAND,
  description = ["Manage Elide installations on this system"],
  mixinStandardHelpOptions = true,
)
@Introspected
@ReflectiveAccess
internal class ToolVersionsCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var managerProvider: Provider<VersionManager>
  @Inject private lateinit var factoryProvider: Provider<VersionCatalogFactory>
  private val manager: VersionManager by lazy { managerProvider.get() }
  private val factory: VersionCatalogFactory by lazy { factoryProvider.get() }

  /** Specifies a version of Elide that should be installed. */
  @CommandLine.Option(
    names = [VersionsValues.INSTALL_VERSION_FLAG],
    description = ["Install specified version of Elide if possible and exit"],
    paramLabel = "version",
  )
  private var installVersion: String? = null

  /** Specifies a version of Elide that should be uninstalled. */
  @CommandLine.Option(
    names = [VersionsValues.UNINSTALL_VERSION_FLAG],
    description = ["Uninstall specified version of Elide if possible and exit"],
    paramLabel = "version",
  )
  private var uninstallVersion: String? = null

  /** Specifies if this version of Elide should be verified with a stampfile. */
  @CommandLine.Option(
    names = ["--verify-stampfile"],
    description = ["Verify all files of this Elide installation and exit"],
    defaultValue = "false",
  )
  private var verify: Boolean = false

  /** Specifies if a stampfile should be generated. */
  @CommandLine.Option(
    names = ["--generate-stampfile"],
    defaultValue = "false",
    hidden = true,
  )
  private var generate: Boolean = false

  /** Specifies if the repository catalog generator should be used. */
  @CommandLine.Option(
    names = ["--create-catalog"],
    defaultValue = "false",
    hidden = true,
  )
  private var catalog: Boolean = false

  /** Specifies a path to install Elide to. */
  @CommandLine.Option(
    names = [VersionsValues.INSTALL_PATH_FLAG],
    description = ["Path to install Elide to"],
    paramLabel = "path",
  )
  private var installPath: String? = null

  /** Specifies if confirmations should be ignored. */
  @CommandLine.Option(
    names = [VersionsValues.NO_CONFIRM_FLAG],
    description = ["Ignore all confirmations"],
    defaultValue = "false",
  )
  private var noConfirm: Boolean = false

  /** Specifies if this command was run elevated by another Elide process. */
  @CommandLine.Option(
    names = [VersionsValues.ELEVATED_FLAG],
    defaultValue = "false",
    hidden = true,
  )
  private var elevated: Boolean = false

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val operatingModes = mapOf(
      VersionsValues.INSTALL_VERSION_FLAG to (installVersion != null),
      VersionsValues.UNINSTALL_VERSION_FLAG to (uninstallVersion != null),
      "--verify-stampfile" to verify,
      "--generate-stampfile" to generate,
      "--create-catalog" to catalog
    ).filterValues { it }
    if (operatingModes.size > 1) {
      return CommandResult.err(1, "Only one of these flags can be used at a time: ${operatingModes.keys.joinToString()}")
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
      if (verify) {
        verify()?.let { return CommandResult.err(1, it) }
        return CommandResult.success()
      }
      if (generate) {
        generate()
        return CommandResult.success()
      }
      if (catalog) return catalogTool()
      return mainMenu()
    } catch (e: Exception) {
      return CommandResult.err(1, e.message, e)
    }
  }

  private suspend fun verify(): String? {
    val home = Statics.elideHome.absolutePathString()
    val progress = Progress.create("Verify installation files", Statics.terminal) {
      add(TrackedTask("Verify files", 1000))
    }
    manager.verifyInstall(home) {
      when (it) {
        is FileVerifyStartEvent -> {
          progress.updateTask(0) {
            copy(position = 0)
          }
          progress.start()
        }
        is FileVerifyProgressEvent -> {
          val time = Clock.System.now().toEpochMilliseconds()
          progress.updateTask(0) { copy(position = (it.progress * 1000).toInt(), output = output + (time to it.name)) }
        }
        FileVerifyCompletedEvent -> {
          progress.updateTask(0) {
            copy(position = target)
          }
        }
        FileVerifyIndeterminateEvent -> {
          progress.updateTask(0) {
            copy(position = target, status = "no stampfile, individual files not verified")
          }
        }
      }
    }.apply {
      if (isNotEmpty()) {
        progress.updateTask(0) {
          copy(position = target, status = "invalid files", failed = true)
        }
        return "The following files did not match the stampfile:\n${joinToString("\n")}"
      }
    }
    return null
  }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun generate() {
    val home = Statics.elideHome.absolutePathString()
    Statics.terminal.println("generating stampfile...")
    val data = manager.generateStampFile(home)
    val stampFile = Path(home, VersionsValues.STAMP_FILE)
    SystemFileSystem.sink(stampFile).buffered().use {
      it.writeString(data)
    }
    Statics.terminal.println("stampfile written to $stampFile")
  }

  private fun catalogTool(): CommandResult {
    val repositoryType = KInquirer.promptList("Do you want to create a local or a remote repository catalog?", listOf("local", "remote"))
    val path = Path(KInquirer.promptInput("Please enter the absolute path to the directory containing Elide packages:"))
    if (!SystemFileSystem.exists(path)) return CommandResult.err(1, "Directory does not exist")
    val json = when (repositoryType) {
      "local" -> {
        val relativePaths = KInquirer.promptConfirm("Do you want to use relative paths?")
        factory.createLocalCatalog(path, relativePaths)
      }
      "remote" -> {
        val root = KInquirer.promptInput("Please enter the HTTPS address prefix:")
        factory.createRemoteCatalog(path, root)
      }
      else -> return CommandResult.err(1, "Invalid repository type: $repositoryType")
    }
    SystemFileSystem.sink(Path(path, VersionsValues.CATALOG_FILE)).buffered().use {
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
