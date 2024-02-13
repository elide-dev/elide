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

import elide.http.api.HttpHeaders as HttpHeadersAPI
import kotlin.jvm.JvmStatic

/**
 * # HTTP Headers
 *
 * Keeps track of HTTP headers in a given HTTP message payload.
 */
public class HttpHeaders private constructor () : HttpHeadersAPI {
  public companion object {
    @JvmStatic public fun create(): HttpHeaders {
      TODO("not yet implemented")
    }
  }
}
