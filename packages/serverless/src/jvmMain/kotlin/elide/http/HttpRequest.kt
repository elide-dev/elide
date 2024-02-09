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

import elide.http.api.HttpMapping
import elide.http.api.HttpMethod
import elide.http.api.HttpString
import elide.net.api.URL
import elide.http.api.HttpRequest as HttpRequestAPI

/**
 *
 */
public actual class HttpRequest : HttpMessage(), HttpRequestAPI {
  actual override val method: HttpMethod get() = TODO("Not yet implemented")
  actual override val path: HttpString get() = TODO("Not yet implemented")
  actual override val url: URL get() = TODO("Not yet implemented")
  actual override val query: HttpMapping<HttpString, HttpString> get() = TODO("Not yet implemented")
}
