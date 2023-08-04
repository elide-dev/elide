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

/**
 * Model for a dependency relationship between assets.
 *
 * @param depender Module which is establishing the dependency.
 * @param dependee Module which is being depended on.
 * @param optional Whether the dependency is optional. Defaults to `false`.
 */
internal data class AssetDependency(
  val depender: String,
  val dependee: String,
  val optional: Boolean = false,
) {
  init {
    require(depender != dependee) {
      "Asset cannot depend on itself"
    }
  }
}
