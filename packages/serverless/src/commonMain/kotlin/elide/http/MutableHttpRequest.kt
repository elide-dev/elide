/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.http

import elide.http.api.HttpMethod
import elide.http.api.HttpString
import elide.http.api.MutableHttpMapping
import elide.net.api.URL
import elide.http.api.MutableHttpRequest as HttpRequestAPI

/**
 *
 */
public expect class MutableHttpRequest : MutableHttpMessage, HttpRequestAPI {
  override var method: HttpMethod
  override var path: HttpString
  override var url: URL
  override var query: MutableHttpMapping<HttpString, HttpString>
}
