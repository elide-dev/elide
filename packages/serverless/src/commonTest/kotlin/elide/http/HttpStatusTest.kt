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

import kotlin.test.*
import elide.http.api.HttpStatus

class HttpStatusTest {
  @Test fun testStatusType() {
    HttpStatus.Type.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.range)
      assertNotNull(it.err)
      assertNotNull(it.description)
      assertNotNull(it.serverFault)
    }
  }

  @Test fun testStatusTypeFromCode() {
    val type = HttpStatus.Type.fromCode(200u)
    assertNotNull(type)
    assertEquals(HttpStatus.Type.SUCCESSFUL, type)
    assertFalse(type.err)
  }

  @Test fun testStatusTypeFromCodeExotic() {
    val type = HttpStatus.Type.fromCode(999u)
    assertNull(type)
  }
}
