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
  public suspend fun init()

  public fun listAccesses(): Set<String>

  public fun createAccess(name: String)

  public fun removeAccess(name: String)

  public fun selectAccess(name: String)

  public fun addProfile(profile: String)

  public fun removeProfile(profile: String)

  public fun listProfiles(): Set<String>

  public fun deselectAccess()

  public fun deleteProfile(profile: String)

  public fun restoreProfile(profile: String)

  public fun deletedProfiles(): Set<String>

  public suspend fun push()
}
