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

import kotlinx.serialization.Serializable
import elide.server.AssetModuleId

/**
 * Represents a resolved reference to an asset at serving-time, before it is rendered into a link or other tag.
 *
 * @param module ID of the asset module being referenced.
 * @param assetType Type of asset being referenced.
 * @param href Relative link to serve the asset.
 * @param type Type override for the tag, if applicable.
 * @param inline Whether this asset is eligible to be inlined into the page.
 * @param preload Whether this asset is eligible to be preloaded.
 */
@Serializable public data class AssetReference(
  val module: AssetModuleId,
  val assetType: AssetType,
  val href: String,
  val type: String? = null,
  val inline: Boolean = false,
  val preload: Boolean = false,
) : java.io.Serializable {
  internal companion object {
    /** @return [AssetReference] from the specified [pointer]. */
    @JvmStatic internal fun fromPointer(pointer: AssetPointer, uri: String): AssetReference = AssetReference(
      module = pointer.moduleId,
      assetType = pointer.type,
      href = uri,
    )
  }
}
