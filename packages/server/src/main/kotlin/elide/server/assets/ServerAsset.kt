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

/**
 * Describes a server-side asset which is embedded in an application bundle through Elide's asset tools and protocol
 * buffer for asset bundle metadata.
 *
 * @param module ID assigned by the developer to this asset module.
 * @param assetType Type of asset being referenced by this object.
 * @param index Index of this asset within the content bundle, if applicable.
 */
public sealed class ServerAsset private constructor(
  internal val module: AssetModuleId,
  internal val assetType: AssetType,
  internal val index: SortedSet<Int>?,
) {
  /**
   * Describes a JavaScript asset which is embedded in a given Elide application, and described by Elide's protocol
   * buffer structures; when read from the application bundle and interpreted, this class is used to hold script info.
   *
   * @param descriptor Script-type settings bundle describing this asset.
   * @param index Index of the content payload, within the live asset bundle, corresponding to this script.
   */
  public class Script(
    internal val descriptor: AssetBundle.ScriptBundle,
    index: SortedSet<Int>?,
  ) : ServerAsset(
    module = descriptor.module,
    assetType = AssetType.SCRIPT,
    index = index,
  )

  /**
   * Describes a stylesheet asset which is embedded in a given Elide application, and described by Elide's protocol
   * buffer structures; when read from the application bundle and interpreted, this class is used to hold document info.
   *
   * @param descriptor Stylesheet-type settings bundle describing this asset.
   * @param index Index of the content payload, within the live asset bundle, corresponding to this stylesheet.
   */
  public class Stylesheet(
    internal val descriptor: AssetBundle.StyleBundle,
    index: SortedSet<Int>?,
  ) : ServerAsset(
    module = descriptor.module,
    assetType = AssetType.STYLESHEET,
    index = index,
  )

  /**
   * Describes a generic text asset of some kind, for example, `humans.txt` or `robots.txt`; when read from the app
   * bundle and interpreted, this class is used to hold file info.
   *
   * @param descriptor Text-type settings bundle describing this asset.
   * @param index Index of the content payload, within the live asset bundle, corresponding to this text asset.
   */
  public class Text(
    internal val descriptor: AssetBundle.GenericBundle,
    index: SortedSet<Int>?,
  ) : ServerAsset(
    module = descriptor.module,
    assetType = AssetType.TEXT,
    index = index,
  )
}
