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

package elide.http

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*

class HttpParamsTest {
  internal fun Params.assertParamCount(count: UInt): Params = apply {
    assertEquals(count, sizeDistinct)
  }

  internal fun Params.assertEmptyParams(): Params = apply {
    assertEquals(0u, sizeDistinct)
  }

  internal fun Params.assertNonEmptyParams(): Params = apply {
    assertNotEquals(0u, sizeDistinct)
  }

  internal fun Params.assertParam(name: String): Params = apply {
    assertTrue(name in this)
    assertTrue(ParamName.of(name) in this)
  }

  internal fun Params.assertParam(name: String, value: String): Params = apply {
    assertParam(name)
    assertEquals(value, this[name]?.asString())
  }

  internal fun Params.assertParam(name: String, value: List<String>): Params = apply {
    assertParam(name)
    value.forEach {
      assertTrue(it in (this[name]?.values?.toList() ?: emptyList()))
    }
  }

  internal fun Params.assertSingle(name: String): Params = apply {
    assertIs<ParamValue.StringParam>(this[name])
  }

  internal fun Params.assertMulti(name: String): Params = apply {
    assertIs<ParamValue.MultiParam>(this[name])
  }

  @Test fun testParseParams() {
    assertDoesNotThrow { Params.parse("") }.assertEmptyParams()
    assertDoesNotThrow { Params.parse("?") }.assertEmptyParams()
    assertDoesNotThrow { Params.parse("?a") }.assertNonEmptyParams()

    assertDoesNotThrow { Params.parse("a=b") }
      .also { assertNotNull(it) }
      .assertNonEmptyParams()
      .assertParamCount(1u)
      .assertParam("a", "b")
      .assertSingle("a")

    assertDoesNotThrow { Params.parse("?a=b") }
      .also { assertNotNull(it) }
      .assertNonEmptyParams()
      .assertParamCount(1u)
      .assertParam("a", "b")
      .assertSingle("a")
  }

  @Test fun testParseMulti() {
    assertDoesNotThrow { Params.parse("?a=1&b=2&c=3&c=4") }
      .also { assertNotNull(it) }
      .assertNonEmptyParams()
      .assertParamCount(3u)
      .assertParam("a", "1")
      .assertParam("b", "2")
      .assertParam("c", listOf("3", "4"))
      .assertSingle("a")
      .assertSingle("b")
      .assertMulti("c")
  }

  @Test fun testStringParam() {
    val param = ParamValue.of("test")
    assertNotNull(param)
    assertIs<ParamValue.StringParam>(param)
  }

  @Test fun testParamOfNullable() {
    assertNull(ParamValue.ofNullable(null))
    ParamValue.ofNullable("test").let {
      assertNotNull(it)
      assertIs<ParamValue.StringParam>(it)
      assertEquals("test", it.asString())
      assertNotNull(it.values)
      assertTrue("test" in it.values.toList())
    }
    assertNull(ParamValue.ofNullable(emptyList<String>().asSequence()))
    ParamValue.ofNullable(listOf("test").asSequence()).let {
      assertNotNull(it)
      assertIs<ParamValue.StringParam>(it)
      assertNotNull(it.values)
      assertTrue("test" in it.values.toList())
    }
    ParamValue.ofNullable(listOf("test", "again").asSequence()).let {
      assertNotNull(it)
      assertIs<ParamValue.MultiParam>(it)
      assertEquals("test, again", it.asString())
      assertNotNull(it.values)
      assertTrue("test" in it.values.toList())
    }
  }
}
