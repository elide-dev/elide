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

import tools.elide.assets.AssetBundle
import java.util.*
import elide.server.AssetModuleId
import elide.server.AssetTag

/**
 * Loaded/interpreted asset manifest data structure.
 *
 * @param bundle De-serialized asset bundle, embedded in the application.
 * @param moduleIndex Sorted map (index) of asset module IDs to asset pointers.
 * @param tagIndex Sorted map (index) of asset tags to content payload indexes.
 */
internal class ServerAssetManifest(
  internal val bundle: AssetBundle,
  internal val moduleIndex: SortedMap<AssetModuleId, AssetPointer>,
  internal val tagIndex: SortedMap<AssetTag, Int>,
)
