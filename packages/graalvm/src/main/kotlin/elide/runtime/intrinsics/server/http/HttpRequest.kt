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

package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi

/** Represents an incoming HTTP request received by the server, accessible by guest code. */
@DelicateElideApi public interface HttpRequest {
  /** The URI (path) for this request. */
  @get:Export public val uri: String

  /** The HTTP method for this request */
  @get:Export public val method: HttpMethod

  /** Parameters extracted from the request path and query variables. */
  @get:Export public val params: Map<String, Any>
}
