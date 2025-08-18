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

package elide.tool.cli.cmd.secrets

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptCheckbox
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptInputPassword
import com.github.kinquirer.components.promptList
import com.github.kinquirer.components.promptListObject
import com.github.kinquirer.core.Choice
import dev.elide.secrets.Secrets
import dev.elide.secrets.dto.persisted.BinarySecret
import dev.elide.secrets.dto.persisted.StringSecret
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import org.fusesource.jansi.internal.JansiLoader
import picocli.CommandLine.Command
import jakarta.inject.Provider
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteString
import kotlinx.io.readString
import elide.annotations.Inject
import elide.tool.cli.*
import elide.tool.io.RuntimeWorkdirManager
import elide.tool.project.ProjectManager
import elide.tooling.cli.Statics

/** TBD. */
@Command(
  name = "secrets",
  description = ["Manage secrets for the current app"],
  mixinStandardHelpOptions = true,
)
@Introspected
@ReflectiveAccess
internal class ToolSecretsCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>
  @Inject private lateinit var workdirProvider: Provider<RuntimeWorkdirManager>
  @Inject private lateinit var secretsProvider: Provider<Secrets>
  private val projectManager: ProjectManager by lazy { projectManagerProvider.get() }
  private val workdir: RuntimeWorkdirManager by lazy { workdirProvider.get() }
  private val secrets: Secrets by lazy { secretsProvider.get() }

  private enum class MainMenuOptions(val text: String) {
    CreateProfile("Create profile"),
    SelectProfile("Select profile"),
    UpdateLocal("Update local from remote"),
    UpdateRemote("Update remote from local"),
    Exit("Exit"),
  }

  private suspend fun CommandContext.mainMenu(): CommandResult? {
    val option = KInquirer.promptListObject("Main menu:", MainMenuOptions.entries.map { Choice(it.text, it) })
    when (option) {
      MainMenuOptions.CreateProfile -> {
        val profile = KInquirer.promptInput("Please enter a name for the profile:")
        secrets.createProfile(profile)
        if (KInquirer.promptConfirm("Do you want to select the created profile? ")) secrets.selectProfile(profile)
      }
      MainMenuOptions.SelectProfile -> {
        val profile = KInquirer.promptList("Please select a profile:", secrets.getProfiles())
        secrets.selectProfile(profile)
      }
      MainMenuOptions.UpdateLocal -> {
        if (KInquirer.promptConfirm("Do you want to update all profiles?")) secrets.updateLocal()
        else secrets.updateLocal(*selectProfiles(secrets.getRemoteProfiles()))
      }
      MainMenuOptions.UpdateRemote -> {
        if (KInquirer.promptConfirm("Do you want to update all profiles?")) secrets.updateLocal()
        else secrets.updateRemote(*selectProfiles(secrets.getProfiles()))
      }
      MainMenuOptions.Exit -> return success()
    }
    return null
  }

  private enum class EditMenuOptions(val text: String) {
    GetSecret("Reveal secret"),
    SetSecret("Set secret"),
    RemoveSecret("Remove secret"),
    WriteChanges("Update local from remote"),
    Exit("Exit"),
  }

  private suspend fun CommandContext.editMenu(): CommandResult? {
    val profile = secrets.getSelectedProfile()!!
    val option = KInquirer.promptListObject("Profile \"$profile\" menu:",
                                            EditMenuOptions.entries.map { Choice(it.text, it) })
    when (option) {
      EditMenuOptions.GetSecret -> {
        val name = KInquirer.promptInput("Please enter a name for the secret you want to reveal:")
        val secret = secrets.getSecret(name)
        if (secret == null) {
          output {
            appendLine("Secret \"$name\" not found in profile \"$profile\".")
          }
        }
        if (secret !is String) {
          if (!KInquirer.promptConfirm("The secret \"$name\" is not a string. Do you want to display it anyway?")) return null
        }
        output {
          appendLine("The secret is: \"$secret\"")
        }
      }
      EditMenuOptions.SetSecret -> {
        val name = KInquirer.promptInput("Please enter a name for the secret you want to set:")
        if (KInquirer.promptConfirm("Is this secret a string?")) {
          val value = KInquirer.promptInputPassword("Please enter the secret:")
          secrets.setSecret(StringSecret(name, value))
        } else {
          val path = KInquirer.promptInput("Please enter a path to the secret file:")
          secrets.setSecret(BinarySecret(name, SystemFileSystem.source(Path(path)).buffered().readByteString()))
        }
      }
      EditMenuOptions.RemoveSecret -> {
        val name = KInquirer.promptInput("Please enter a name for the secret you want to remove:")
        secrets.removeSecret(name)
      }
      EditMenuOptions.WriteChanges -> {
        secrets.writeChanges()
        output {
          appendLine("Changes written to profile \"$profile\"")
        }
        if (KInquirer.promptConfirm("Do you want to stop editing this profile?")) secrets.deselectProfile()
      }
      EditMenuOptions.Exit -> {
        secrets.deselectProfile()
      }
    }
    return null
  }

  private suspend fun CommandContext.selectProfiles(profiles: List<String>): Array<String> {
    val selected: MutableList<String> = mutableListOf()
    val notSelected: MutableList<String?> = profiles.toMutableList()
    notSelected.addFirst(null)
    while (true) {
      output {
        if (selected.isEmpty()) appendLine("Please select the profiles you want to update")
        else appendLine("You have selected the following profiles: ${selected.joinToString(", ")}")
      }
      val choice = KInquirer.promptListObject("Please select a profile", notSelected.map { Choice(it ?: "All done", it) })
      if (choice == null) return selected.toTypedArray()
      selected.add(choice)
      notSelected.remove(choice)
    }
  }

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val selected = KInquirer.promptList("Testing", listOf("Option 1", "Option 2", "Option 5"))
    println(selected)
    println(selected)
    println(selected)
    println(selected)
    val root = workdir.projectRoot()?.toFile() ?: workdir.workingRoot()
    val project = projectManager.resolveProject(projectOptions().projectPath())
    secrets.init(true, Path(root.absolutePath, ".elide-secrets"), project?.manifest?.name)

    //Main loop
    while (true) {
      val returned: CommandResult? = if (secrets.getSelectedProfile() == null) mainMenu() else editMenu()
      if (returned != null) return returned
    }
  }
}
