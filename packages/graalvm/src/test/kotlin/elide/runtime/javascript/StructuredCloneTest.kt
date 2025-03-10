/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.javascript

import org.graalvm.polyglot.Value
import org.junit.jupiter.api.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.plugins.js.javascript
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

@TestCase internal class StructuredCloneTest : AbstractJsIntrinsicTest<StructuredCloneBuiltin>() {
  @Inject lateinit var structuredClone: StructuredCloneBuiltin
  override fun provide(): StructuredCloneBuiltin = structuredClone

  override fun testInjectable() {
    assertNotNull(structuredClone)
  }

  @Test fun testCloneSimple() {
    val (value, cloned) = withContext {
      val value = javascript(
        // language=JavaScript
        """
          const x = { a: 1, b: 2 };
          x;
        """.trimIndent()
      )
      value to structuredClone.clone(Value.asValue(value), unwrap())
    }
    assertNotNull(cloned)
    assertTrue(cloned.hasMembers())
    assertTrue(cloned.hasMember("a"))
    assertTrue(cloned.hasMember("b"))
    assertFalse(cloned.hasMember("c"))
    assertEquals(1, cloned.getMember("a").asInt())
    assertEquals(2, cloned.getMember("b").asInt())
    assertEquals(2, cloned.memberKeys.size)
    assertNotSame(value, cloned)
  }
}
