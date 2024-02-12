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
public actual class MutableHttpRequest : MutableHttpMessage(), HttpRequestAPI {
  actual override var method: HttpMethod = TODO("Not yet implemented")
  actual override var url: URL get() = TODO("Not yet implemented")
  actual override var path: HttpString = "/"
  actual override var query: MutableHttpMapping<HttpString, HttpString>
    get() = TODO("Not yet implemented")
    set(value) {}
}
