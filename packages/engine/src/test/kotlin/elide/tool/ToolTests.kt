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

package elide.tool

import kotlin.test.*

class ToolTests {
  //
  // --- Arguments
  //

  @Test fun testArgs() {
    val arg = Argument.of("--example")
    assertNotNull(arg)
    assertEquals("--example", arg.asArgumentString())
    val args = Arguments.of("--example")
    val all = args.asArgumentList()
    assertNotNull(all)
    assertEquals(1, all.size)
    assertEquals("--example", all[0])
  }

  @Test fun testEmptyArgs() {
    assertNotNull(Arguments.empty())
    assertIs<Arguments.Suite>(Arguments.empty())
    val suite: Arguments.Suite = Arguments.empty()
    assertEquals(0, suite.size)
  }

  @Test fun testKvArgument() {
    val arg = Argument.of("--setting" to "true")
    assertNotNull(arg)
    assertEquals("--setting=true", arg.asArgumentString())
    val args = Arguments.of("--setting" to "true")
    val all = args.asArgumentList()
    assertNotNull(all)
    assertEquals(1, all.size)
    assertEquals("--setting=true", all[0])
    assertEquals("--setting=true", args.asArgumentString())
  }

  @Test fun testMultipleArgs() {
    Arguments.from(sequenceOf(
      "--example",
      "--another",
    )).let {
      assertNotNull(it)
      assertEquals(2, it.size)
      assertEquals("--example", it[0].asArgumentString())
      assertEquals("--another", it[1].asArgumentString())
      assertEquals("--example", it.first().asArgumentString())
      assertEquals("--example --another", it.asArgumentString())
    }
    Arguments.from(sequenceOf(
      "--example",
      "--another",
    )).let {
      assertNotNull(it)
      assertEquals(2, it.size)
      assertEquals("--example", it[0].asArgumentString())
      assertEquals("--another", it[1].asArgumentString())
      assertEquals("--example", it.first().asArgumentString())
      assertEquals("--example --another", it.asArgumentString())
    }
    Arguments.of(sequenceOf(
      Argument.of("--example"),
      Argument.of("--another"),
    )).let {
      assertNotNull(it)
      assertEquals(2, it.size)
      assertEquals("--example", it[0].asArgumentString())
      assertEquals("--another", it[1].asArgumentString())
      assertEquals("--example", it.first().asArgumentString())
      assertEquals("--example --another", it.asArgumentString())
    }
    Arguments.of(sequenceOf(
      Argument.of("--example"),
      Argument.of("--another"),
      Argument.of("--setting" to "true"),
    )).let {
      assertNotNull(it)
      assertEquals(3, it.size)
      assertEquals("--example", it[0].asArgumentString())
      assertEquals("--another", it[1].asArgumentString())
      assertEquals("--example", it.first().asArgumentString())
      assertEquals("--setting=true", it[2].asArgumentString())
      assertEquals("--example --another --setting=true", it.asArgumentString())
    }
  }

  //
  // --- Environment
  //

  @Test fun testEnvironment() {
    assertNotNull(Environment.empty())
    assertNotNull(Environment.host())
    assertNotNull(Environment.host()["PATH"])
  }

  @Test fun testEmptyEnvironment() {
    assertNotNull(Environment.empty())
    assertIs<Environment>(Environment.empty())
    assertIs<Environment.EmptyEnvironment>(Environment.empty())
    assertEquals(0, Environment.empty().size)
    assertTrue(Environment.empty().isEmpty())
    assertNull(Environment.empty()["EXAMPLE"])
  }

  @Test fun testMappedEnvironment() {
    Environment.of("EXAMPLE" to "value").let {
      assertNotNull(it)
      assertIs<Environment.MappedEnv>(it)
      assertEquals("EXAMPLE", it.keys.first())
      assertEquals("value", it.values.first())
      assertEquals(1, it.size)
      assertEquals("value", it["EXAMPLE"])
    }
  }
}
