/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.server.assets

import java.util.*
import elide.server.AssetModuleId

/**
 * Reference to an application-embedded asset.
 *
 * @param moduleId Developer-assigned ID for this asset module.
 * @param type Type of asset represented by this reference.
 * @param token Full-length Asset Tag (referred to as the "asset token").
 * @param tag Generated asset tag (fingerprint) for this asset, in full (untrimmed) form.
 * @param etag Computed ETag for this asset.
 * @param modified Last-modified time for this asset; set to `-1` to indicate "unknown".
 * @param index Index of the asset within the asset content payload list of the active asset bundle.
 */
public data class AssetPointer(
  val moduleId: AssetModuleId,
  val type: AssetType,
  val token: String,
  val tag: String,
  val etag: String,
  val modified: Long,
  val index: SortedSet<Int>?,
) : java.io.Serializable
