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
package dev.elide.secrets.impl

import dev.elide.secrets.Secrets
import dev.elide.secrets.SecretsState
import dev.elide.secrets.Utils
import dev.elide.secrets.Values
import dev.elide.secrets.dto.persisted.*
import dev.elide.secrets.remote.Remote
import dev.elide.secrets.remote.RemoteInitializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.SerializationException

/**
 * Implementation of [Secrets].
 *
 * @author Lauri Heino <datafox>
 */
public class SecretsImpl(
    private val interactive: Boolean,
    private val path: Path = Path(System.getProperty("user.dir"), Values.DEFAULT_PATH),
    // Using impl for visibility reasons until DI is set up
    private val encryption: EncryptionImpl = EncryptionImpl(),
    private val dataHandler: DataHandlerImpl = DataHandlerImpl(encryption),
    private val console: ConsoleImpl = ConsoleImpl(),
) : Secrets {
    private val passphrase: MutableStateFlow<ByteString> = MutableStateFlow(ByteString())
    private val local: MutableStateFlow<SecretCollection> = MutableStateFlow(SecretCollection())
    private val remote: MutableStateFlow<Remote?> = MutableStateFlow(null)
    private val selected: MutableStateFlow<Pair<String, SecretCollection>?> = MutableStateFlow(null)
    private val selectedProfile: String?
        get() = selected.value?.first

    private val selectedCollection: SecretCollection?
        get() = selected.value?.second

    override suspend fun init() {
        SecretsState.Companion.set(SecretsState(interactive, path))
        if (dataHandler.metadataExists() && dataHandler.localExists()) loadSystem()
        else if (dataHandler.metadataExists())
            throw IllegalStateException(
                "Secrets system is invalid, \"${Values.LOCAL_COLLECTION_NAME}\" is missing in \"$path\""
            )
        else if (dataHandler.localExists())
            throw IllegalStateException(
                "Secrets system is invalid, \"${Values.METADATA_NAME}\" is missing in \"$path\""
            )
        else if (interactive) createSystem()
        else
            throw IllegalStateException(
                "Secrets have not been initialized and interactive mode is off"
            )
    }

    private suspend fun loadSystem() {
        passphrase.value =
            encryption.hash(
                readPassphraseFromEnvironment()
                    ?: if (interactive) readPassphraseFromConsole(false)
                    else
                        throw IllegalStateException(
                            "No passphrase was found in the environment and interactive mode is off"
                        )
            )
        Utils.checkPassphrase(
                console,
                SecretsState.Companion.get().interactive,
                passphrase.value,
                "passphrase",
                { encryption.hash(readRemotePassphraseFromConsole(false)) },
            ) {
                try {
                    dataHandler.readLocal(passphrase.value)
                } catch (_: SerializationException) {
                    null
                }
            }
            .let {
                passphrase.value = it.first
                local.value = it.second
            }
        validateCollections()
    }

    private suspend fun createSystem() {
        console.println("Welcome to Elide Secrets!")
        console.println("First, you need a personal passphrase")
        console.println(
            "This passphrase protects all secrets stored locally on your system, so keep it safe!"
        )
        passphrase.value =
            encryption.hash(
                readPassphraseFromEnvironment()?.apply {
                    console.println("Your passphrase was read from the environment")
                } ?: readPassphraseFromConsole(true)
            )
        SystemFileSystem.createDirectories(path)
        dataHandler.writeLocal(passphrase.value, local.value)
        if (Utils.confirm(console, "Do you want to import existing secrets from a remote?")) {
            importSystem()
        } else {
            val name = Utils.readWithConfirm(console, "Please enter the name of your project: ")
            val organization =
                Utils.readWithConfirm(console, "Please enter the name of your organization: ")
            val metadata = SecretMetadata(name, organization, emptyMap())
            dataHandler.writeMetadata(metadata)
            console.println("Secrets system has been created")
        }
    }

    private suspend fun importSystem() {
        if (remote.value == null) initRemote()
        val remote = remote.value!!
        val metadata =
            remote.getMetadata()?.let { dataHandler.deserializeMetadata(it) }
                ?: throw IllegalStateException("Remote has not been initialized")
        val profiles = metadata.collections.keys
        val profilesToGet: Set<String>
        while (true) {
            console.println("The remote has following profiles: ${profiles.joinToString(", ")}")
            console.println(
                "Please enter the profiles you want separated by whitespace, or leave empty to get all"
            )
            console.print("Profiles: ")
            val input = console.readln().trim()
            if (input.isEmpty()) {
                profilesToGet = profiles
                break
            }
            val inputs = input.replace(Regex(" +"), " ").split(" ")
            if (inputs.all { it in profiles }) {
                profilesToGet = inputs.toSet()
                break
            }
            console.println(
                "Invalid profiles: ${inputs.filter { it !in profiles }. joinToString(", ")}"
            )
        }
        val collections = profilesToGet.map { it to remote.getCollection(it) }
        val remotePassphrase = validateRemotePassphrase(metadata)
        collections.forEach { (profile, encrypted) ->
            val key = dataHandler.decryptKey(remotePassphrase, encrypted.first)
            val collection = dataHandler.decryptCollection(key, encrypted.second)
            dataHandler.writeKey(profile, passphrase.value, key)
            dataHandler.writeCollection(profile, passphrase.value, collection)
        }
        local.update { it.add(BinarySecret(Remote.PASSPHRASE_NAME, remotePassphrase)) }
        dataHandler.writeMetadata(
            metadata.copy(collections = metadata.collections.filter { it.key in profilesToGet })
        )
        dataHandler.writeLocal(passphrase.value, local.value)
    }

    private suspend fun initRemote() {
        lateinit var initializer: RemoteInitializer
        var remoteName = local.value.get<String>(Remote.REMOTE_NAME)
        remoteName?.let {
            Remote.remotes[it]?.let { block -> initializer = block(dataHandler, console) }
        }
        if (interactive && remote.value == null) {
            Utils.options(
                console,
                Remote.remotes.map { (name, block) ->
                    name to
                        {
                            remoteName = name
                            initializer = block(dataHandler, console)
                        }
                },
            )
        }
        remote.value = initializer.initialize(interactive, local.value)
        if (remote.value == null) throw IllegalStateException("Remote could not be initialized")
        local.update {
            initializer.updateLocal(it).add(StringSecret(Remote.REMOTE_NAME, remoteName!!))
        }
        dataHandler.writeLocal(passphrase.value, local.value)
    }

    private suspend fun validateCollections() {
        var isCorrupted = false
        dataHandler.readMetadata().collections.forEach {
            try {
                dataHandler.readCollection(it.key, passphrase.value)
            } catch (_: SerializationException) {
                console.println("Collection for profile \"${it.key}\" is corrupted")
                isCorrupted = true
            }
        }
        if (isCorrupted) throw IllegalStateException("One or more corrupted collections were found")
    }

    private fun readPassphraseFromEnvironment(): String? =
        System.getenv(Values.PASSPHRASE_ENVIRONMENT_VARIABLE)

    private fun readPassphraseFromConsole(confirm: Boolean): String =
        if (confirm)
            Utils.passphrase(
                console,
                "Please enter your passphrase: ",
                "Please enter your passphrase again: ",
                "Passphrases were not identical",
            )
        else {
            console.print("Please enter your passphrase: ")
            console.readPassword()
        }

    private fun readRemotePassphraseFromConsole(confirm: Boolean): String =
        if (confirm)
            Utils.passphrase(
                console,
                "Please enter the remote passphrase: ",
                "Please enter the remote passphrase again: ",
                "Passphrases were not identical",
            )
        else {
            console.print("Please enter the remote passphrase: ")
            console.readPassword()
        }

    override suspend fun createProfile(profile: String) {
        val metadata = dataHandler.readMetadata()
        if (profile in metadata.collections)
            throw IllegalArgumentException("Profile \"$profile\" already exists")
        val key = Utils.generateBytes(Values.KEY_SIZE)
        val collection = SecretCollection(emptyMap())
        dataHandler.writeKey(profile, passphrase.value, key)
        val sha = dataHandler.writeCollection(profile, passphrase.value, collection)
        val newMetadata = metadata.add(CollectionMetadata(profile, sha))
        dataHandler.writeMetadata(newMetadata)
    }

    override suspend fun removeProfile(profile: String) {
        val metadata = dataHandler.readMetadata()
        if (profile !in metadata.collections)
            throw IllegalArgumentException("Profile \"$profile\" does not exist")
        dataHandler.deleteCollection(profile)
        val newMetadata = metadata.copy(collections = metadata.collections - profile)
        dataHandler.writeMetadata(newMetadata)
    }

    override suspend fun getProfiles(): List<String> =
        dataHandler.readMetadata().collections.keys.toList()

    override suspend fun getRemoteProfiles(): List<String> {
        if (remote.value == null) initRemote()
        return remote.value!!
            .getMetadata()
            ?.let { dataHandler.deserializeMetadata(it) }
            ?.collections
            ?.keys
            ?.toList() ?: emptyList()
    }

    override suspend fun updateLocal(vararg profiles: String) {
        if (remote.value == null) initRemote()
        val remote = remote.value!!
        val remoteMetadata =
            remote.getMetadata()?.let { dataHandler.deserializeMetadata(it) }
                ?: throw IllegalStateException("Remote is not initialized")
        val metadata = dataHandler.readMetadata()
        val updated =
            if (profiles.isNotEmpty()) {
                val nonExistent = remoteMetadata.collections.keys.filter { it !in profiles }
                if (nonExistent.isNotEmpty()) {
                    if (interactive) {
                        console.println(
                            "Profiles \"${nonExistent.joinToString(", ")}\" do not exist on the remote"
                        )
                        if (!Utils.confirm(console, "Do you want to update the existing profiles?"))
                            return
                    } else
                        throw IllegalStateException(
                            "Profiles \"${nonExistent.joinToString(", ")}\" do not exist on the remote"
                        )
                }
                remoteMetadata.collections.keys.filter { it in profiles }
            } else remoteMetadata.collections.keys.toList()
        val remotePassphrase = validateRemotePassphrase(metadata)
        val updatedWithSha =
            updated.map {
                val (encryptedKey, encryptedCollection) = remote.getCollection(it)
                val key = dataHandler.decryptKey(remotePassphrase, encryptedKey)
                val collection = dataHandler.decryptCollection(key, encryptedCollection)
                dataHandler.writeKey(it, passphrase.value, key)
                it to dataHandler.writeCollection(it, passphrase.value, collection)
            }
        val newMetadata =
            metadata.copy(
                collections =
                    metadata.collections.filterNot { it.key in updated } +
                        updatedWithSha.map { (profile, sha) ->
                            profile to CollectionMetadata(profile, sha)
                        }
            )
        dataHandler.writeMetadata(newMetadata)
    }

    private suspend fun validateRemotePassphrase(metadata: SecretMetadata): ByteString {
        if (remote.value == null) initRemote()
        val remote = remote.value!!
        val validator = remote.getValidator()!!
        var remotePassphrase: ByteString =
            local.value[Remote.PASSPHRASE_NAME]
                ?: encryption.hash(readRemotePassphraseFromConsole(false))
        remotePassphrase =
            Utils.checkValidatorPassphrase(
                dataHandler,
                console,
                SecretsState.Companion.get().interactive,
                metadata,
                remotePassphrase,
                validator,
                "remote passphrase",
            ) {
                encryption.hash(readRemotePassphraseFromConsole(false))
            }
        local.update { it.add(BinarySecret(Remote.PASSPHRASE_NAME, remotePassphrase)) }
        dataHandler.writeLocal(passphrase.value, local.value)
        return remotePassphrase
    }

    override suspend fun updateRemote(vararg profiles: String) {
        if (remote.value == null) initRemote()
        val remote = remote.value!!
        val metadata = dataHandler.readMetadata()
        val updated =
            if (profiles.isNotEmpty()) {
                val nonExistent = metadata.collections.keys.filter { it !in profiles }
                if (nonExistent.isNotEmpty()) {
                    if (interactive) {
                        console.println(
                            "Profiles \"${nonExistent.joinToString(", ")}\" do not exist"
                        )
                        if (
                            !Utils.confirm(
                                console,
                                "Do you want to update the remote with the existing profiles?",
                            )
                        )
                            return
                    } else
                        throw IllegalStateException(
                            "Profiles \"${nonExistent.joinToString(", ")}\" do not exist"
                        )
                }
                metadata.collections.keys.filter { it in profiles }
            } else metadata.collections.keys.toList()
        val remoteMetadata = remote.getMetadata()?.let { dataHandler.deserializeMetadata(it) }
        val remotePassphrase: ByteString
        if (remoteMetadata == null) {
            if (!interactive)
                throw IllegalStateException("Remote is not initialized and interactive mode is off")
            console.println("Remotes use a different passphrase than your personal one.")
            remotePassphrase =
                local.value[Remote.PASSPHRASE_NAME]
                    ?: encryption.hash(readRemotePassphraseFromConsole(true))
            remote.init(metadata, dataHandler.createValidator(metadata, remotePassphrase))
            local.update { it.add(BinarySecret(Remote.PASSPHRASE_NAME, remotePassphrase)) }
            dataHandler.writeLocal(passphrase.value, local.value)
        } else {
            remotePassphrase = validateRemotePassphrase(remoteMetadata)
        }
        remote.update(
            updated.associateWith {
                Pair(
                    dataHandler.encryptKey(
                        remotePassphrase,
                        dataHandler.readKey(it, passphrase.value),
                    ),
                    dataHandler.readEncryptedCollection(it),
                )
            }
        )
    }

    override suspend fun selectProfile(profile: String) {
        val metadata = dataHandler.readMetadata()
        if (profile !in metadata.collections)
            throw IllegalArgumentException("Profile \"${profile}\" does not exist")
        val collection = dataHandler.readCollection(profile, passphrase.value)
        selected.update { it?.copy(profile, collection) ?: Pair(profile, collection) }
    }

    override suspend fun getSecret(name: String): Any? =
        selectedCollection?.secrets[name] ?: throw IllegalStateException("No profile is selected")

    override suspend fun getSecret(profile: String, name: String): Any? {
        val metadata = dataHandler.readMetadata()
        if (profile !in metadata.collections)
            throw IllegalArgumentException("Profile \"${profile}\" does not exist")
        return dataHandler.readCollection(profile, passphrase.value).secrets[name]
    }

    override suspend fun setSecret(secret: Secret<*>): Unit =
        selected.update {
            it?.copy(second = it.second.add(secret))
                ?: throw IllegalStateException("No profile is selected")
        }

    override suspend fun removeSecret(name: String) {
        selectedCollection?.apply {
            if (name !in secrets)
                throw IllegalArgumentException(
                    "Secret \"$name\" does not exist in profile \"$selectedProfile\""
                )
            selected.update {
                it?.copy(
                    second =
                        it.second.copy(
                            secrets = it.second.secrets.filterKeys { key -> key != name }
                        )
                ) ?: throw IllegalStateException("No profile is selected")
            }
        } ?: throw IllegalStateException("No profile is selected")
    }

    override suspend fun writeChanges() {
        selectedCollection?.let {
            val profile = selectedProfile!!
            val sha = dataHandler.writeCollection(profile, passphrase.value, it)
            val metadata = dataHandler.readMetadata()
            val newMetadata =
                metadata.copy(
                    collections =
                        metadata.collections.mapValues { (p, c) ->
                            if (p == profile) c.copy(sha = sha) else c
                        }
                )
            dataHandler.writeMetadata(newMetadata)
        }
    }

    override suspend fun deselectProfile() {
        selected.value = null
    }

    override suspend fun removeProfile() {
        selectedProfile?.let { removeProfile(it) }
    }

    override suspend fun removeRemoteProfile(profile: String) {
        if (remote.value == null) initRemote()
        remote.value!!.removeCollection(profile)
    }
}
