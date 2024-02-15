/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

import kotlin.test.*
import elide.http.api.CaseInsensitiveHttpString
import elide.http.api.HttpMessageType
import elide.http.api.HttpVersion

/** Various type tests. */
class HttpTypesTest {
  @Test fun testCaseInsensitiveString() {
    val sample = CaseInsensitiveHttpString.of("Hello")
    assertTrue(sample.equals("Hello"))
    assertTrue(sample.equals("hello"))
    assertTrue(sample.equals("HELLO"))
    assertTrue(sample.equals("hElLo"))
    assertFalse(sample.equals("Goodbye"))
    assertFalse(sample.equals(5))
    assertFalse(sample.equals(null))
  }

  @Test fun testMessageType() {
    HttpMessageType.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.name)
      assertTrue(it.name.isNotEmpty())
      assertTrue(it.name.isNotBlank())
    }

    assertEquals(HttpMessageType.REQUEST, HttpMessageType.valueOf("REQUEST"))
    assertEquals(HttpMessageType.RESPONSE, HttpMessageType.valueOf("RESPONSE"))
    assertNotEquals(HttpMessageType.RESPONSE, HttpMessageType.REQUEST)
    assertNotEquals(HttpMessageType.REQUEST, HttpMessageType.RESPONSE)
  }

  @Test fun testHttpVersion() {
    HttpVersion.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.name)
      assertTrue(it.name.isNotEmpty())
      assertTrue(it.name.isNotBlank())
    }
  }
}
