package dev.elide.secrets

import dev.elide.secrets.dto.persisted.*
import dev.elide.secrets.remote.Remote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.io.files.Path

/** @author Lauri Heino <datafox> */
internal object SecretsState {
  private val interactiveFlow: MutableStateFlow<Boolean?> = MutableStateFlow(null)
  private val pathFlow: MutableStateFlow<Path?> = MutableStateFlow(null)
  internal val metadataFlow: MutableStateFlow<LocalMetadata?> = MutableStateFlow(null)
  internal val userKeyFlow: MutableStateFlow<UserKey?> = MutableStateFlow(null)
  internal val localFlow: MutableStateFlow<LocalProfile?> = MutableStateFlow(null)
  internal val profileFlow: MutableStateFlow<Pair<SecretProfile, SecretKey>?> = MutableStateFlow(null)
  internal val remoteFlow: MutableStateFlow<Remote?> = MutableStateFlow(null)

  internal inline val interactive: Boolean
    get() = interactiveFlow.value!!

  internal inline val path: Path
    get() = pathFlow.value!!

  internal inline var metadata: LocalMetadata
    get() = metadataFlow.value!!
    set(value) {
      metadataFlow.value = value
    }

  internal inline var userKey: UserKey
    get() = userKeyFlow.value!!
    set(value) {
      userKeyFlow.value = value
    }

  internal inline var local: LocalProfile
    get() = localFlow.value!!
    set(value) {
      localFlow.value = value
    }

  internal inline var profilePair: Pair<SecretProfile, SecretKey>
    get() = profileFlow.value!!
    set(value) {
      profileFlow.value = value
    }

  internal inline var profile: SecretProfile
    get() = profilePair.first
    set(value) {
      profileFlow.update { it!!.copy(first = value) }
    }

  internal inline var key: SecretKey
    get() = profilePair.second
    set(value) {
      profileFlow.update { it!!.copy(second = value) }
    }

  internal inline var remote: Remote
    get() = remoteFlow.value!!
    set(value) {
      remoteFlow.value = value
    }

  internal fun init(interactive: Boolean, path: Path) {
    if (interactiveFlow.value != null) throw IllegalStateException("Already initialized")
    interactiveFlow.value = interactive
    pathFlow.value = path
  }

  internal fun updateMetadata(block: LocalMetadata.() -> LocalMetadata) {
    metadataFlow.update { it!!.block() }
  }

  internal fun updateLocal(block: LocalProfile.() -> LocalProfile) {
    localFlow.update { it!!.block() }
  }

  internal fun updateProfile(block: SecretProfile.() -> SecretProfile) {
    profileFlow.update { it!!.copy(first = it.first.block()) }
  }
}
