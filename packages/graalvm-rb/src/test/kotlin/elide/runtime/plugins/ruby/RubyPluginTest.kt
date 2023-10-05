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

package elide.runtime.plugins.ruby

import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine

@OptIn(DelicateElideApi::class)
internal class RubyPluginTest {
  @Test fun testExecution() {
    val engine = PolyglotEngine { install(Ruby) }
    val context = engine.acquire()
    
    val result = context.ruby("""
    def getValue()
      return 42
    end
    
    getValue()
    """.trimIndent())
    
    assertEquals(
      expected = 42,
      actual = result.asInt(),
      message = "should return correct value",
    )
  }
}
