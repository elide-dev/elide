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
package elide.secrets

import elide.secrets.dto.persisted.*
import elide.secrets.remote.Remote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.io.files.Path
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tooling.project.manifest.ElidePackageManifest

/** @author Lauri Heino <datafox> */
internal object SecretsState {
  private val interactiveFlow: MutableStateFlow<Boolean?> = MutableStateFlow(null)
  private val pathFlow: MutableStateFlow<Path?> = MutableStateFlow(null)
  private val manifestFlow: MutableStateFlow<ElidePackageManifest?> = MutableStateFlow(null)
  internal val metadataFlow: MutableStateFlow<LocalMetadata?> = MutableStateFlow(null)
  internal val userKeyFlow: MutableStateFlow<UserKey?> = MutableStateFlow(null)
  internal val localFlow: MutableStateFlow<LocalProfile?> = MutableStateFlow(null)
  internal val profileFlow: MutableStateFlow<Pair<SecretProfile, SecretKey>?> = MutableStateFlow(null)
  internal val remoteFlow: MutableStateFlow<Remote?> = MutableStateFlow(null)

  internal val initialized: Boolean
    get() = interactiveFlow.value != null

  internal inline val interactive: Boolean
    get() = interactiveFlow.value!!

  internal inline val path: Path
    get() = pathFlow.value!!

  internal inline val manifest: ElidePackageManifest?
    get() = manifestFlow.value

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

  private val logger: Logger = Logging.of(SecretsState::class)

  internal fun init(interactive: Boolean, path: Path, manifest: ElidePackageManifest?) {
    if (initialized) {
      logger.warn(Values.SECRETS_STATE_INITIALIZED_WARNING)
      return
    }
    interactiveFlow.value = interactive
    pathFlow.value = path
    manifestFlow.value = manifest
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
