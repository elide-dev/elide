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

import io.micronaut.http.MediaType

/**
 * Enumerates known kinds of registered application assets.
 *
 * @param mediaType Micronaut media type associated with this asset type.
 */
public enum class AssetType constructor(internal val mediaType: MediaType) {
  /** Generic assets which employ custom configuration. */
  GENERIC(mediaType = MediaType("application/octet-stream")),

  /** Plain text assets. */
  TEXT(mediaType = MediaType("text/plain", "txt")),

  /** JavaScript assets. */
  SCRIPT(mediaType = MediaType("application/javascript", "js")),

  /** Stylesheet assets. */
  STYLESHEET(mediaType = MediaType("text/css", "css")),
}
