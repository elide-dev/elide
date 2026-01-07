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

import kotlinx.serialization.Serializable

/**
 * Configuration for install management.
 *
 * @property installDirs Paths to directories that Elide may be installed to.
 * @property searchDirs Paths to directories that Elide may be installed to, but should not be installed to with install
 *   management.
 * @property repositories Repositories to search for versions of Elide.
 * @property defaultInstallDir Default directory to install Elide versions to.
 * @author Lauri Heino <datafox>
 */
@Serializable
public data class ElideInstallConfig(
  public val installDirs: List<String> = emptyList(),
  public val searchDirs: List<String> = emptyList(),
  public val repositories: List<String> = emptyList(),
  public val defaultInstallDir: String? = null,
)
