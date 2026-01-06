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
import elide.runtime.core.HostPlatform
import elide.runtime.version.ElideVersion
import elide.runtime.version.ElideVersionInfo

/**
 * DTO object for a platform-specific version of Elide.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
public data class ElideVersionDto(
  public val version: String,
  public val platform: @Serializable(with = HostPlatformSerializer::class) HostPlatform,
) {
  public val info: ElideVersion by lazy { ElideVersionInfo(version) }
}
