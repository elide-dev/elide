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

package elide.tool.ssg

import io.micronaut.http.HttpRequest
import java.net.URL

/**
 * Describes an asset detected after parsing an HTML response.
 *
 * @param url Absolute URL of the asset.
 * @param request HTTP request which yielded the asset.
 * @param type Type of the asset.
 */
public data class DetectedArtifact(
  val url: URL,
  val request: HttpRequest<*>,
  val type: StaticContentReader.ArtifactType,
)
