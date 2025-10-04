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
package elide.tool.cli.cmd.secrets

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.*
import com.github.kinquirer.core.Choice
import elide.annotations.Inject
import elide.secrets.RemoteManagement
import elide.secrets.SecretManagement
import elide.secrets.SecretType
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.project.ProjectManager
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import jakarta.inject.Provider
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteString
import picocli.CommandLine
import picocli.CommandLine.Command

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
  @Inject private lateinit var secretsProvider: Provider<SecretManagement>
  private val projectManager: ProjectManager by lazy { projectManagerProvider.get() }
  private val secrets: SecretManagement by lazy { secretsProvider.get() }

  /** Specifies that a single statement should be run from the command line. */
  @CommandLine.Option(
    names = ["--non-interactive", "-n"],
    description = ["Initialize from a remote non-interactively"],
    defaultValue = "false",
  )
  internal var nonInteractive: Boolean = false

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val projectOptions = projectOptions()
    val projectPath = projectOptions.projectPath()
    val project = projectManager.resolveProject(projectPath)
    if (nonInteractive) {
      if (project?.manifest == null)
        return CommandResult.err(1, "No elide.pkl manifest found, which is required in non-interactive mode.")
      secrets.initNonInteractive(projectPath, project.manifest)
      println("Secrets initialized non-interactively.")
      return CommandResult.success()
    }
    secrets.init(projectPath, project?.manifest)
    return mainMenu()
  }

  private suspend fun mainMenu(): CommandResult {
    var running = true
    while (running) {
      when (KInquirer.promptListObject("Main menu:", MainMenuOptions.entries.map { Choice(it.displayName, it) })) {
        MainMenuOptions.CREATE -> createProfile()
        MainMenuOptions.LIST -> println(secrets.listProfiles().joinToString())
        MainMenuOptions.SELECT -> selectProfile()
        MainMenuOptions.REMOVE -> removeProfile()
        MainMenuOptions.CHANGE -> secrets.changeEncryption()
        MainMenuOptions.PULL -> secrets.pullFromRemote()
        MainMenuOptions.PUSH -> secrets.pushToRemote()
        MainMenuOptions.MANAGE -> manageRemoteMenu(secrets.manageRemote())
        MainMenuOptions.EXIT -> running = false
      }
    }
    return CommandResult.success()
  }

  private fun createProfile() {
    val name = KInquirer.promptInput("Please enter a name for the new profile:")
    if (name.isNotEmpty()) secrets.createProfile(name)
  }

  private fun selectProfile() {
    val profiles = secrets.listProfiles()
    if (profiles.isEmpty()) {
      println("No profiles found")
      return
    }
    val profile = KInquirer.promptList("Please select a profile to edit:", profiles.toList() + "Local secrets")
    if (profile == "Local secrets") {
      println("Editing local secrets can be dangerous and break things if you don't know what you're doing!")
      if (!KInquirer.promptConfirm("Do you want to proceed?")) {
        return
      }
      secrets.loadLocalProfile()
    } else secrets.loadProfile(profile)
    editProfileMenu(profile)
  }

  private fun removeProfile() {
    val profiles = secrets.listProfiles()
    if (profiles.isEmpty()) {
      println("No profiles found")
    } else {
      secrets.deleteProfile(KInquirer.promptList("Please select a profile to remove:", profiles.toList()))
    }
  }

  private fun editProfileMenu(profile: String) {
    var running = true
    while (running) {
      when (
        KInquirer.promptListObject(
          "Edit menu for profile \"$profile\"",
          EditProfileOptions.entries.map { Choice(it.displayName, it) },
        )
      ) {
        EditProfileOptions.CREATE -> createSecret()
        EditProfileOptions.LIST ->
          println(secrets.listSecrets().map { (name, type) -> "$name (${type.displayName})" }.joinToString())
        EditProfileOptions.UPDATE -> updateSecret()
        EditProfileOptions.REMOVE -> removeSecret()
        EditProfileOptions.REVEAL -> revealSecret()
        EditProfileOptions.WRITE -> {
          secrets.writeChanges()
          secrets.unloadProfile()
          running = false
        }
        EditProfileOptions.DESELECT -> {
          secrets.unloadProfile()
          running = false
        }
      }
    }
  }

  private fun createSecret() {
    val name = KInquirer.promptInput("Please enter a name for the new secret:")
    if (name.isEmpty()) return
    if (
      name !in secrets.listSecrets() ||
        KInquirer.promptConfirm("A secret with this name already exists, do you want to replace it?")
    ) {
      when (
        KInquirer.promptListObject(
          "Select the type of secret you want to create",
          SecretType.entries.map { Choice(it.displayName, it) },
        )
      ) {
        SecretType.TEXT -> {
          val secret = KInquirer.promptInputPassword("Please type or paste in the secret:")
          val repeat = KInquirer.promptInputPassword("Please type or paste in the secret again:")
          if (secret != repeat) {
            println("Secrets were not identical")
          } else {
            val envVar =
              if (KInquirer.promptConfirm("Do you want to use this secret as an environment variable for your app?"))
                KInquirer.promptInput("Please enter the name of the environment variable:")
              else null
            secrets.setTextSecret(name, secret, envVar)
          }
        }
        SecretType.BINARY -> {
          val path = KInquirer.promptInput("Please type or paste in the absolute path to the secret binary file:")
          val data = SystemFileSystem.source(Path(path)).buffered().use { it.readByteString() }
          secrets.setBinarySecret(name, data)
        }
      }
    }
  }

  private fun updateSecret() {
    val secretMap = secrets.listSecrets()
    if (secretMap.isEmpty()) {
      println("No secrets found")
      return
    }
    val selected = KInquirer.promptList("Please select a secret to update:", secretMap.keys.toList())
    when (secretMap[selected]!!) {
      SecretType.TEXT -> {
        val secret = KInquirer.promptInputPassword("Please type or paste in the new value:")
        val repeat = KInquirer.promptInputPassword("Please type or paste in the new value again:")
        if (secret != repeat) {
          println("Values were not identical")
        } else {
          secrets.updateTextSecret(selected, secret)
        }
      }
      SecretType.BINARY -> {
        val path = KInquirer.promptInput("Please type or paste in the absolute path to the new binary file:")
        val data = SystemFileSystem.source(Path(path)).buffered().use { it.readByteString() }
        secrets.setBinarySecret(selected, data)
      }
    }
  }

  private fun removeSecret() {
    val secretNames = secrets.listSecrets().keys
    if (secretNames.isEmpty()) {
      println("No secrets found")
    } else {
      secrets.removeSecret(KInquirer.promptList("Please select a secret to remove:", secretNames.toList()))
    }
  }

  private fun revealSecret() {
    val secretNames = secrets.listSecrets().filterValues { it == SecretType.TEXT }.keys
    if (secretNames.isEmpty()) {
      println("No text secrets found")
    } else {
      val name = KInquirer.promptList("Please select a secret to reveal:", secretNames.toList())
      if (
        KInquirer.promptConfirm(
          "Are you sure you want to reveal the secret. It will be printed to your console in plain text!"
        )
      )
        println(secrets.getStringSecret(name))
    }
  }

  private suspend fun manageRemoteMenu(remote: RemoteManagement) {
    var running = true
    while (running) {
      when (
        KInquirer.promptListObject(
          "Remote management menu:",
          ManageRemoteOptions.entries.map { Choice(it.displayName, it) },
        )
      ) {
        ManageRemoteOptions.CREATE -> createAccess(remote)
        ManageRemoteOptions.LIST -> println(remote.listAccesses())
        ManageRemoteOptions.SELECT -> selectAccess(remote)
        ManageRemoteOptions.REMOVE -> removeAccess(remote)
        ManageRemoteOptions.REKEY -> rekeyProfiles(remote)
        ManageRemoteOptions.DELETE -> deleteProfile(remote)
        ManageRemoteOptions.RESTORE -> restoreProfile(remote)
        ManageRemoteOptions.PUSH -> {
          remote.push()
          running = false
        }
        ManageRemoteOptions.EXIT -> {
          running = false
        }
      }
    }
  }

  private fun createAccess(remote: RemoteManagement) {
    val name = KInquirer.promptInput("Please enter a name for the access file:")
    if (name.isNotEmpty()) remote.createAccess(name)
  }

  private fun selectAccess(remote: RemoteManagement) {
    val accesses = remote.listAccesses()
    if (accesses.isEmpty()) {
      println("No access files found")
    } else {
      val access = KInquirer.promptList("Please select an access file to edit:", accesses.toList())
      remote.selectAccess(access)
      editAccessMenu(remote, access)
    }
  }

  private fun removeAccess(remote: RemoteManagement) {
    val accesses = remote.listAccesses()
    if (accesses.isEmpty()) {
      println("No access files found")
    } else {
      remote.deleteAccess(KInquirer.promptList("Please select an access file to remove:", accesses.toList()))
    }
  }

  private fun rekeyProfiles(remote: RemoteManagement) {
    val profiles = secrets.listProfiles()
    if (profiles.isEmpty()) {
      println("No profiles found")
    } else {
      KInquirer.promptCheckbox("Please select profiles to rekey:", profiles.toList()).forEach { remote.rekeyProfile(it) }
    }
  }

  private fun deleteProfile(remote: RemoteManagement) {
    remote.deleteProfile(
      KInquirer.promptList(
        "Please select a profile to delete:",
        (secrets.listProfiles() subtract remote.deletedProfiles()).toList(),
      )
    )
  }

  private fun restoreProfile(remote: RemoteManagement) {
    remote.restoreProfile(
      KInquirer.promptList(
        "Please select a deleted profile to restore:",
        remote.deletedProfiles().toList(),
      )
    )
  }

  private fun editAccessMenu(remote: RemoteManagement, access: String) {
    var running = true
    while (running) {
      when (
        KInquirer.promptListObject(
          "Edit menu for access file \"$access\":",
          EditAccessOptions.entries.map { Choice(it.displayName, it) },
        )
      ) {
        EditAccessOptions.ADD -> addProfileToAccess(remote)
        EditAccessOptions.LIST -> println(remote.listProfiles().joinToString())
        EditAccessOptions.REMOVE -> removeProfileFromAccess(remote)
        EditAccessOptions.CHANGE -> remote.changeEncryption()
        EditAccessOptions.DESELECT -> {
          remote.deselectAccess()
          running = false
        }
      }
    }
  }

  private fun addProfileToAccess(remote: RemoteManagement) {
    val profiles = (secrets.listProfiles() - remote.listProfiles())
    if (profiles.isEmpty()) {
      println("No eligible profiles found")
    } else {
      remote.addProfile(KInquirer.promptList("Please select a profile to add to the access file:", profiles.toList()))
    }
  }

  private fun removeProfileFromAccess(remote: RemoteManagement) {
    val profiles = remote.listProfiles()
    if (profiles.isEmpty()) {
      println("No profiles found")
    } else {
      remote.removeProfile(
        KInquirer.promptList("Please select a profile to remove from the access file:", profiles.toList())
      )
    }
  }

  private enum class MainMenuOptions(val displayName: String) {
    CREATE("Create a new profile"),
    LIST("List profiles"),
    SELECT("Select a profile to edit"),
    REMOVE("Remove a profile"),
    CHANGE("Change encryption details"),
    PULL("Pull changes from remote"),
    PUSH("Push changes to remote"),
    MANAGE("Manage remote as a superuser"),
    EXIT("Exit secret management"),
  }

  private enum class EditProfileOptions(val displayName: String) {
    CREATE("Create a secret"),
    LIST("List secrets"),
    UPDATE("Update a secret"),
    REMOVE("Remove a secret"),
    REVEAL("Reveal a secret"),
    WRITE("Write changes and go to main menu"),
    DESELECT("Go to the main menu without writing changes"),
  }

  private enum class ManageRemoteOptions(val displayName: String) {
    CREATE("Create an access file"),
    LIST("List access files"),
    SELECT("Select an access file"),
    REMOVE("Remove an access file"),
    REKEY("Regenerate profile keys"),
    DELETE("Delete a profile"),
    RESTORE("Restore a deleted profile"),
    PUSH("Push changes to remote and go to the main menu"),
    EXIT("Go to the main menu without pushing changes"),
  }

  private enum class EditAccessOptions(val displayName: String) {
    ADD("Add a profile to the access file"),
    LIST("List profiles in the access file"),
    REMOVE("Remove a profile from the access file"),
    CHANGE("Change encryption details"),
    DESELECT("Return to the remote management menu"),
  }
}
