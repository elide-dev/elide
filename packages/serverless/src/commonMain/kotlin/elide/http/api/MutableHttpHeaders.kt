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

package elide.http.api

import elide.http.api.HttpHeaders.HeaderName

/**
 *
 */
public interface MutableHttpHeaders : MutableHttpMapping<HeaderName, HttpString> {
  /**
   *
   */
  public override operator fun get(key: HeaderName): HttpString?

  /**
   *
   */
  public operator fun get(key: String): HttpString?

  /**
   *
   */
  public operator fun set(key: String, value: HttpString)

  /**
   *
   */
  public operator fun set(key: HeaderName, value: HttpString)
}
