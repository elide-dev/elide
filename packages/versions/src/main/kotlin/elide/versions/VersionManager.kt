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
package elide.versions

import kotlinx.coroutines.flow.FlowCollector

/**
 * Manages concurrent installations of Elide.
 *
 * @author Lauri Heino <datafox>
 */
public interface VersionManager {
  /** Returns the path to the version of Elide that should be run, or `null` if current should be used. */
  public fun onStartup(currentVersion: String, requestedVersion: String? = null): String?

  /** Returns all versions of Elide installed in the current system. Searches directories specified in
   * [ElideInstallConfig.installDirs] and optionally [ElideInstallConfig.searchDirs].
   *
   * @param includeSearchDirs If `true`, [ElideInstallConfig.searchDirs] are searched.
   */
  public fun getInstallations(includeSearchDirs: Boolean): List<ElideInstallation>

  /** Returns [ElideInstallConfig.installDirs]. */
  public fun getInstallPaths(): List<String>

  /**
   * Returns all versions of Elide available in [ElideInstallConfig.repositories].
   *
   * @param onlyCurrentSystem If `true`, only returns versions of Elide available for the current operating system and
   * architecture.
   */
  public suspend fun getAvailable(onlyCurrentSystem: Boolean = true): List<ElideVersionDto>

  /**
   * Installs a [version] of Elide to [path] or [ElideInstallConfig.defaultInstallDir] and returns the path to the
   * installation.
   *
   * @param elevated If `true`, this process is considered elevated and will not attempt elevation again, throwing an
   * exception if a file cannot be installed.
   * @param version Version of Elide to install.
   * @param path Path to install Elide to, or `null` if [ElideInstallConfig.defaultInstallDir] should be used.
   * @param progress Flow collector for progress events.
   */
  public suspend fun install(elevated: Boolean, version: String, path: String? = null, progress: FlowCollector<ElideInstallEvent>? = null): String?

  /**
   * Verifies an installation of Elide at [path] with a stampfile. Returns the relative paths to any files that were not
   * verified successfully. An empty list means verification was successful.
   *
   * @param path Path to the Elide install.
   * @param progress Flow collector for progress events.
   */
  public suspend fun verifyInstall(path: String, progress: FlowCollector<ElideFileVerifyEvent>? = null): List<String>

  /**
   * Uninstalls an [installation][install] of Elide.
   *
   * @param elevated If `true`, this process is considered elevated and will not attempt elevation again, throwing an
   * exception if a file cannot be installed.
   * @param install Installation of Elide.
   * @param progress Flow collector for progress events.
   */
  public suspend fun uninstall(elevated: Boolean, installation: ElideInstallation, progress: FlowCollector<ElideUninstallEvent>? = null)

  /**
   * Generates a stampfile for all files in [path] and returns it.
   *
   * @param path Path to a directory.
   */
  public suspend fun generateStampFile(path: String): String
}
