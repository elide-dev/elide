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

package elide.runtime.gvm.ruby

import elide.runtime.gvm.RubyTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Basic Ruby runtime utility tests. */
@TestCase class RubyTest : RubyTest() {
  @Test fun testRubyGuestCodeBasic() = executeGuest {
    // language=ruby
    """
      def say_hello ()
        "Hello, Ruby!"
      end
    """.trimIndent()
  }.doesNotFail()

  @Test fun testRubyGuestCodeReturnValue() = executeGuest {
    // language=ruby
    """
      def say_hello ()
        "Hello, Ruby!"
      end
      say_hello
    """.trimIndent()
  }.thenAssert {
    val returnValue = it.returnValue()
    assertNotNull(returnValue, "should not get `null` return value from ruby execution")
    assertEquals(
      "Hello, Ruby!",
      returnValue.asString(),
      "should get correct string from ruby execution",
    )
  }
}
