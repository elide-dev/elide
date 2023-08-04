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

package elide.server.http

import elide.server.assets.AssetManager

/** Effective namespace for request context values and objects. */
public object RequestContext {
  /**
   * Defines a known key within the context payload of a request.
   *
   * @param name Name associated with this key.
   */
  public data class Key (
    public val name: String,
  ) {
    /** Keys where request context may be accessed. */
    public companion object {
      /** Key for accessing the [AssetManager]. */
      public val ASSET_MANAGER: Key = Key("elide.assetManager")
    }
  }
}
