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

import tools.elide.meta.AppManifest
import java.net.URL

/**
 * Info describing a loaded Elide application.
 *
 * @param target Path to the loaded JAR or HTTP server.
 * @param manifest Loaded application manifest.
 * @param params Original compiler parameters.
 * @param eligible Whether there are any pre-compilable endpoints.
 */
public data class LoadedAppInfo(
  val target: URL,
  val manifest: AppManifest,
  val params: SiteCompilerParams,
  val eligible: Boolean,
)
