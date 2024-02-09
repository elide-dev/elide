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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.http.api.HttpHeaders.HeaderName

class HttpHeadersTest {
  @Test fun testEmpty() {
    val headers = HttpHeaders.empty()
    assertTrue(headers.isEmpty())
    assertFalse(headers.isNotEmpty())
  }

  @Test fun testCreate() {
    val headers = HttpHeaders.of("Content-Type" to "application/json")
    assertTrue(headers.isNotEmpty())
    assertFalse(headers.isEmpty())
    assertEquals("application/json", headers["Content-Type"])
    assertEquals("application/json", headers[HeaderName.of("Content-Type")])
  }
}
