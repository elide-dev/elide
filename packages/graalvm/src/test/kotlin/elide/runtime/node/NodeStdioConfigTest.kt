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

package elide.runtime.node

import org.graalvm.polyglot.Value.asValue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.err.ValueError
import elide.runtime.intrinsics.js.node.childProcess.StdioConfig
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols
import elide.testing.annotations.TestCase

@TestCase internal class NodeStdioConfigTest : AbstractJsTest() {
  @Test fun testGetDefaultStdio() {
    assertNotNull(StdioConfig.DEFAULTS)
  }

  @Test fun testStdioSymbols() {
    assertNotNull(StdioSymbols.toString())
    assertEquals("pipe", StdioSymbols.PIPE)
    assertEquals("inherit", StdioSymbols.INHERIT)
    assertEquals("ignore", StdioSymbols.IGNORE)
  }

  @Test fun testStdioConfigToString() {
    assertNotNull(StdioConfig.DEFAULTS.toString())
    assertNotEquals("", StdioConfig.DEFAULTS.toString())
    assertNotEquals(StdioConfig.DEFAULTS.toString(), StdioConfig.DEFAULTS.copy(stdin = "ignore").toString())
    assertNotEquals(StdioConfig.DEFAULTS.toString(), StdioConfig.DEFAULTS.copy(stdout = "ignore").toString())
    assertNotEquals(StdioConfig.DEFAULTS.toString(), StdioConfig.DEFAULTS.copy(stderr = "ignore").toString())
  }

  @Test fun testStdioConfigEquals() {
    assertEquals(StdioConfig.DEFAULTS, StdioConfig.DEFAULTS)
    assertEquals(StdioConfig.DEFAULTS, StdioConfig.DEFAULTS.copy())
    assertNotEquals(StdioConfig.DEFAULTS, StdioConfig.DEFAULTS.copy(stdin = "ignore"))
  }

  @Test fun testStdioConfigDefaults() {
    val defaults = assertNotNull(StdioConfig.DEFAULTS)
    assertNotNull(defaults.stdin)
    assertNotNull(defaults.stdout)
    assertNotNull(defaults.stderr)
    assertEquals("pipe", defaults.stdin)
    assertEquals("pipe", defaults.stdout)
    assertEquals("pipe", defaults.stderr)
  }

  @Test fun testStdioConfigPipeMode() {
    val defaults = assertNotNull(StdioConfig.pipe())
    assertNotNull(defaults.stdin)
    assertNotNull(defaults.stdout)
    assertNotNull(defaults.stderr)
    assertEquals("pipe", defaults.stdin)
    assertEquals("pipe", defaults.stdout)
    assertEquals("pipe", defaults.stderr)
  }

  @Test fun testStdioConfigInheritMode() {
    val defaults = assertNotNull(StdioConfig.inherit())
    assertNotNull(defaults.stdin)
    assertNotNull(defaults.stdout)
    assertNotNull(defaults.stderr)
    assertEquals("inherit", defaults.stdin)
    assertEquals("inherit", defaults.stdout)
    assertEquals("inherit", defaults.stderr)
  }

  @Test fun testStdioConfigIgnoreMode() {
    val defaults = assertNotNull(StdioConfig.ignore())
    assertNotNull(defaults.stdin)
    assertNotNull(defaults.stdout)
    assertNotNull(defaults.stderr)
    assertEquals("ignore", defaults.stdin)
    assertEquals("ignore", defaults.stdout)
    assertEquals("ignore", defaults.stderr)
  }

  @Test fun testStdioConfigPipeModeApply() {
    val defaults = assertNotNull(StdioConfig.pipe())
    assertNotNull(defaults.stdin)
    assertNotNull(defaults.stdout)
    assertNotNull(defaults.stderr)
    assertEquals("pipe", defaults.stdin)
    assertEquals("pipe", defaults.stdout)
    assertEquals("pipe", defaults.stderr)
    val pb = ProcessBuilder()
    defaults.applyTo(pb)
    assertEquals(ProcessBuilder.Redirect.PIPE, pb.redirectInput())
    assertEquals(ProcessBuilder.Redirect.PIPE, pb.redirectOutput())
    assertEquals(ProcessBuilder.Redirect.PIPE, pb.redirectError())
  }

  @Test fun testStdioConfigInheritModeApply() {
    val defaults = assertNotNull(StdioConfig.inherit())
    assertNotNull(defaults.stdin)
    assertNotNull(defaults.stdout)
    assertNotNull(defaults.stderr)
    assertEquals("inherit", defaults.stdin)
    assertEquals("inherit", defaults.stdout)
    assertEquals("inherit", defaults.stderr)
    val pb = ProcessBuilder()
    defaults.applyTo(pb)
    assertEquals(ProcessBuilder.Redirect.INHERIT, pb.redirectInput())
    assertEquals(ProcessBuilder.Redirect.INHERIT, pb.redirectOutput())
    assertEquals(ProcessBuilder.Redirect.INHERIT, pb.redirectError())
  }

  @Test fun testStdioConfigIgnoreModeApply() {
    val defaults = assertNotNull(StdioConfig.ignore())
    assertNotNull(defaults.stdin)
    assertNotNull(defaults.stdout)
    assertNotNull(defaults.stderr)
    assertEquals("ignore", defaults.stdin)
    assertEquals("ignore", defaults.stdout)
    assertEquals("ignore", defaults.stderr)
    val pb = ProcessBuilder()
    defaults.applyTo(pb)
    assertEquals(ProcessBuilder.Redirect.DISCARD, pb.redirectOutput())
    assertEquals(ProcessBuilder.Redirect.DISCARD, pb.redirectError())
  }

  @Test fun testApplyToInvalidMode() {
    assertThrows<IllegalStateException> {
      val defaults = assertNotNull(StdioConfig("invalid", "invalid", "invalid"))
      val pb = ProcessBuilder()
      defaults.applyTo(pb)
    }
  }

  @Test fun testCreateFromHostNull() {
    assertEquals(
      StdioConfig.DEFAULTS,
      assertNotNull(assertDoesNotThrow { StdioConfig.from(null) }),
    )
  }

  @Test fun testCreateFromGuestNull() {
    assertEquals(
      StdioConfig.DEFAULTS,
      assertNotNull(assertDoesNotThrow { StdioConfig.from(asValue(null)) }),
    )
  }

  @Test fun testCreateFromGuestString() {
    val defaults = StdioConfig.DEFAULTS
    val pipe = StdioConfig.pipe()
    val ignore = StdioConfig.ignore()
    val inherit = StdioConfig.inherit()

    assertEquals(
      pipe,
      assertNotNull(assertDoesNotThrow { StdioConfig.from(asValue("pipe")) }),
    )
    assertEquals(
      ignore,
      assertNotNull(assertDoesNotThrow { StdioConfig.from(asValue("ignore")) }),
    )
    assertNotEquals(
      defaults,
      assertNotNull(assertDoesNotThrow { StdioConfig.from(asValue("ignore")) }),
    )
    assertEquals(
      inherit,
      assertNotNull(assertDoesNotThrow { StdioConfig.from(asValue("inherit")) }),
    )
    assertNotEquals(
      defaults,
      assertNotNull(assertDoesNotThrow { StdioConfig.from(asValue("inherit")) }),
    )
    assertThrows<ValueError> { StdioConfig.from(asValue("invalid")) }
  }

  @Test fun testCreateFromGuestArrayInherit() {
    val defaults = StdioConfig.DEFAULTS
    val inherit = StdioConfig.inherit()

    executeGuest {
      // language=JavaScript
      """
        export const sample = ['inherit', 'inherit', 'inherit']
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      val parsed = assertNotNull(assertDoesNotThrow { StdioConfig.from(spec) })
      assertNotEquals(defaults, parsed)
      assertEquals(inherit, parsed)
    }
  }

  @Test fun testCreateFromGuestArrayPipe() {
    val pipe = StdioConfig.pipe()

    executeGuest {
      // language=JavaScript
      """
        export const sample = ['pipe', 'pipe', 'pipe']
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      assertEquals(pipe, assertNotNull(assertDoesNotThrow { StdioConfig.from(spec) }))
    }
  }

  @Test fun testCreateFromGuestArrayIgnore() {
    val defaults = StdioConfig.DEFAULTS
    val ignore = StdioConfig.ignore()

    executeGuest {
      // language=JavaScript
      """
        export const sample = ['ignore', 'ignore', 'ignore']
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      val parsed = assertNotNull(assertDoesNotThrow { StdioConfig.from(spec) })
      assertNotEquals(defaults, parsed)
      assertEquals(ignore, parsed)
    }
  }

  @Test fun testCreateFromGuestArrayEmpty() {
    val defaults = StdioConfig.DEFAULTS

    executeGuest {
      // language=JavaScript
      """
        export const sample = []
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      val parsed = assertNotNull(assertDoesNotThrow { StdioConfig.from(spec) })
      assertEquals(defaults, parsed)
    }
  }

  @Test fun testCreateFromGuestArrayOneEntry() {
    val defaults = StdioConfig.DEFAULTS

    executeGuest {
      // language=JavaScript
      """
        export const sample = ['inherit']
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      val parsed = assertNotNull(assertDoesNotThrow { StdioConfig.from(spec) })
      assertNotEquals(defaults, parsed)
      assertEquals("inherit", parsed.stdin)
      assertEquals("pipe", parsed.stdout)
      assertEquals("pipe", parsed.stderr)
    }
  }

  @Test fun testCreateFromGuestArrayTwoEntries() {
    val defaults = StdioConfig.DEFAULTS

    executeGuest {
      // language=JavaScript
      """
        export const sample = ['inherit', 'inherit']
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      val parsed = assertNotNull(assertDoesNotThrow { StdioConfig.from(spec) })
      assertNotEquals(defaults, parsed)
      assertEquals("inherit", parsed.stdin)
      assertEquals("inherit", parsed.stdout)
      assertEquals("pipe", parsed.stderr)
    }
  }

  @Test fun testCreateFromGuestArrayTooManyEntries() {
    executeGuest {
      // language=JavaScript
      """
        export const sample = ['inherit', 'inherit', 'inherit', 'inherit']
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      assertThrows<ValueError> { StdioConfig.from(spec) }
    }
  }

  @Test fun testCreateFromGuestInvalidType() {
    executeGuest {
      // language=JavaScript
      """
        export const sample = false
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      assertThrows<TypeError> { StdioConfig.from(spec) }
    }
  }

  @Test fun testCreateFromGuestArrayIntegers() {
    executeGuest {
      // language=JavaScript
      """
        export const sample = [0, 1, 2]
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      val stdio = assertNotNull(assertDoesNotThrow { StdioConfig.from(spec) })
      assertEquals(0, stdio.stdin)
      assertEquals(1, stdio.stdout)
      assertEquals(2, stdio.stderr)
    }
  }

  @Test fun testCreateFromGuestArrayInvalid() {
    executeGuest {
      // language=JavaScript
      """
        export const sample = [false, true, NaN]
      """
    }.thenAssert {
      val spec = assertNotNull(it.returnValue()?.getMember("sample"))
      assertThrows<ValueError> { StdioConfig.from(spec) }
    }
  }
}
