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

package elide.ssr

import kotlin.test.Test
import kotlin.test.assertEquals

class CssChunkTest {
  @Test fun testBasic() {
    val chunk = CssChunk(
      ids = arrayOf("test1", "test2"),
      key = "abc",
      css = "body { color: red; }",
    )
    val chunk2 = CssChunk(
      ids = arrayOf("test1", "test2"),
      key = "abc",
      css = "body { color: red; }",
    )
    val chunk3 = chunk2.copy()
    assertEquals(chunk, chunk2)
    assertEquals(chunk, chunk3)
  }
}
