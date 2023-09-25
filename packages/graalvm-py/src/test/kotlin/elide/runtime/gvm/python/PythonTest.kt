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

package elide.runtime.gvm.python

import elide.runtime.gvm.PythonTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.runtime.core.DelicateElideApi

/** Basic Python runtime utility tests. */
@OptIn(DelicateElideApi::class)
@TestCase class PythonTest : PythonTest() {
  @Test fun testPythonGuestCodeBasic() = executeGuest {
    // language=python
    """
      def say_hello():
        return "Hello, Python!"
      say_hello()
    """.trimIndent()
  }.doesNotFail()

  @Test fun testPythonGuestCodeReturnValue() = executeGuest {
    // language=python
    """
      def say_hello():
        return "Hello, Python!"
      say_hello()
    """.trimIndent()
  }.thenAssert {
    val returnValue = it.returnValue()
    assertNotNull(returnValue, "should not get `null` return value from python execution")
    assertEquals(
      "Hello, Python!",
      returnValue.asString(),
      "should get correct string from python execution",
    )
  }
}
