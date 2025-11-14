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

/**
 * Superuser access to remote secrets.
 *
 * @author Lauri Heino <datafox>
 */
public interface RemoteManagement {
  /** Initializes remote management, syncing state with the remote. */
  public suspend fun init()

  /** Lists names of all registered access files. */
  public fun listAccesses(): Set<String>

  /** Creates a new access file. */
  public fun createAccess(name: String)

  /** Deletes an access file. */
  public fun deleteAccess(name: String)

  /** Selects an access file for editing. */
  public fun selectAccess(name: String)

  /** Adds a profile to the selected access file. */
  public fun addProfile(profile: String)

  /** Removes a profile from the selected access file. */
  public fun removeProfile(profile: String)

  /** Lists profiles in the selected access file. */
  public fun listProfiles(): Set<String>

  /** Prompts user on a new encryption mode for the selected access file. */
  public fun changeEncryption()

  /** Deselects the selected access file. */
  public fun deselectAccess()

  /** Prompts user on a new encryption mode for superuser access. */
  public fun changeSuperEncryption()

  /** Generates new keys for specified profiles. */
  public fun rekeyProfile(profile: String)

  /** Deletes a profile completely. */
  public fun deleteProfile(profile: String)

  /** Restores a profile deleted in this [RemoteManagement]'s lifetime. */
  public fun restoreProfile(profile: String)

  /** Lists profiles deleted in this [RemoteManagement]'s lifetime. */
  public fun deletedProfiles(): Set<String>

  /** Pushes changes to the remote. Do not use this [RemoteManagement] instance after calling. */
  public suspend fun push()
}
