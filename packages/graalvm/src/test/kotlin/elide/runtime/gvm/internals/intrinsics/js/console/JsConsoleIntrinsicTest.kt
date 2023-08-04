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

@file:Suppress("JSUnresolvedVariable")

package elide.runtime.gvm.internals.intrinsics.js.console

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import elide.annotations.Inject
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

 /** Tests for intrinsic JS console implementation, which pipes to logging. */
@TestCase internal class JsConsoleIntrinsicTest : AbstractJsIntrinsicTest<ConsoleIntrinsic>() {
  // Logger facade to use for testing.
  private val loggerFacade: AtomicReference<Logger> = AtomicReference(null)

  // Buffer of emitted logs.
  private val loggerBuffer: AtomicReference<ArrayList<Pair<LogLevel, List<Any>>>> = AtomicReference(ArrayList())

  // Console intrinsic under test.
  @Inject internal lateinit var console: ConsoleIntrinsic

  override fun provide(): ConsoleIntrinsic = console

  @BeforeEach
  fun mockLogger() {
    loggerBuffer.get().clear()
    loggerFacade.set(object : Logger {
      override fun isEnabled(level: LogLevel): Boolean = true

      override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean) {
        loggerBuffer.get().add(level to message)
      }
    })
    console.setInterceptor(loggerFacade.get())
  }

  private fun checkGuestTestLogs(level: LogLevel) {
    checkTestLogs(level, dual = true, guest = true)
  }

  private fun checkTestLogs(level: LogLevel, dual: Boolean = false, guest: Boolean = false) {
    val buf = loggerBuffer.get()
    assertNotNull(buf, "should have a buffer of emitted logs")

    val offset = if (dual) {
      // advance the buffer by 4
      assertEquals(8, buf.size, "should have emitted 8 logs")
      4
    } else {
      assertEquals(4, buf.size, "should have emitted 4 logs")
      0
    }
    val label = if (guest) "guest" else "host"

    assertEquals(
      level,
      buf[0 + offset].first,
      "$label: should have emitted a `${level.name}` log each time (0)",
    )
    assertEquals(
      level,
      buf[1 + offset].first,
      "$label: should have emitted a `${level.name}` log each time (1)",
    )
    assertEquals(
      level,
      buf[2 + offset].first,
      "$label: should have emitted a `${level.name}` log each time (2)",
    )
    assertEquals(
      level,
      buf[3 + offset].first,
      "$label: should have emitted a `${level.name}` log each time (3)",
    )
    assertEquals(
      listOf("hello log"),
      buf[0].second,
      "$label: should have emitted the correct log message (0)",
    )
    assertEquals(
      listOf("hello log", "with", "args"),
      buf[1].second,
      "$label: should have emitted the correct log message (1)",
    )
    assertEquals(
      listOf("hello log", "with", "args", 1, 2, 3),
      buf[2].second,
      "$label: should have emitted the correct log message (2)",
    )
    assertEquals(
      listOf("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false),
      buf[3].second,
      "$label: should have emitted the correct log message (3)",
    )
  }

  @Test override fun testInjectable() {
    assertNotNull(console, "should be able to resolve console intrinsic via injection")
  }

  @Test fun testConsoleLog() {
    console.log("hello log")
    console.log("hello log", "with", "args")
    console.log("hello log", "with", "args", 1, 2, 3)
    console.log("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false)
    checkTestLogs(LogLevel.DEBUG)

  }

  @Test fun testConsoleInfo() {
    console.info("hello log")
    console.info("hello log", "with", "args")
    console.info("hello log", "with", "args", 1, 2, 3)
    console.info("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false)
    checkTestLogs(LogLevel.INFO)
  }

  @Test fun testConsoleWarn() {
    console.warn("hello log")
    console.warn("hello log", "with", "args")
    console.warn("hello log", "with", "args", 1, 2, 3)
    console.warn("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false)
    checkTestLogs(LogLevel.WARN)
  }

  @Test fun testConsoleError() {
    console.error("hello log")
    console.error("hello log", "with", "args")
    console.error("hello log", "with", "args", 1, 2, 3)
    console.error("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false)
    checkTestLogs(LogLevel.ERROR)
  }

  @CsvSource(value = [
    // Guest Logging: Direct
    "DEBUG,direct",
    "INFO,direct",
    "WARN,direct",
    "ERROR,direct",

    // Guest Logging: Facade
    "DEBUG,facade",
    "INFO,facade",
    "WARN,facade",
    "ERROR,facade",
  ])
  @Suppress("UNUSED_PARAMETER")
  @ParameterizedTest fun testConsole(level: LogLevel, mode: String) = dual {
    // host-side test
    val hostMethod = when (level) {
      LogLevel.INFO -> console::info
      LogLevel.WARN -> console::warn
      LogLevel.ERROR -> console::error
      else -> console::log
    }
    hostMethod.invoke(arrayOf("hello log"))
    hostMethod.invoke(arrayOf("hello log", "with", "args"))
    hostMethod.invoke(arrayOf("hello log", "with", "args", 1, 2, 3))
    hostMethod.invoke(arrayOf("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false))
    checkTestLogs(level)

  }.thenRun {
    // guest-side test
    val modeBase = if (mode == "direct") {
      "Console"
    } else {
      "console"
    }
    val methodName = when (level) {
      LogLevel.INFO -> "info"
      LogLevel.WARN -> "warn"
      LogLevel.ERROR -> "error"
      else -> "log"
    }

    // `Console.<x>` or `console.<x>`
    val method = "$modeBase.$methodName"

    // language=javascript
    """
      if ("$mode" === "direct") {
        ${method}(["hello log"]);
        ${method}(["hello log", "with", "args"]);
        ${method}(["hello log", "with", "args", 1, 2, 3]);
        ${method}(["hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false]);      
      } else {
        ${method}("hello log");
        ${method}("hello log", "with", "args");
        ${method}("hello log", "with", "args", 1, 2, 3);
        ${method}("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false);
      }
    """
  }.thenAssert {
    checkGuestTestLogs(level)
  }

  @Test fun testConsoleFormatValue() {
    val target = this.console
    val format: (Any?) -> Any? = target::formatLogComponent
    val assertFormatted: (String, Any?, String) -> Unit = { expected, value, msg ->
      assertEquals(expected, format(value), msg)
    }
    val assertGuestValue: (String, Any?, String) -> Unit = { expected, value, msg ->
      withContext {
        val guest = asValue(value)
        assertEquals(expected, format(guest), msg)
      }
    }

    val tests = listOf(
      "null" to null,
      "true" to true,
      "false" to false,
      "Hello" to "Hello",
      "1" to 1,
      "2" to 2L,
    )

    tests.forEach {
      assertFormatted(it.first, it.second, "host-formatted value mismatch")
      assertGuestValue(it.first, it.second, "guest-formatted value mistmatch")
    }

    assertFormatted("1.0", 1.0, "host-formatted double should be expected value")
    assertGuestValue("1", 1.0, "guest-formatted double can trim the `.0`")
  }

  @Test fun testConsoleFormatTemporal() {
    val target = this.console
    val format: (Any?) -> Any? = target::formatLogComponent
    val assertFormatted: (String, Any?, String) -> Unit = { expected, value, msg ->
      assertEquals(expected, format(value), msg)
    }
    val assertGuestValue: (String, Any?, String) -> Unit = { expected, value, msg ->
      withContext {
        val guest = asValue(value)
        assertEquals(expected, format(guest), msg)
      }
    }

    val tests = listOf(
      "2023-01-15T22:34:45Z" to Instant.ofEpochSecond(1673822085),
      "2023-01-15T22:34:45Z" to kotlinx.datetime.Instant.fromEpochSeconds(1673822085),
    )

    tests.forEach {
      assertFormatted(it.first, it.second, "host-formatted value mismatch")
      assertGuestValue(it.first, it.second, "guest-formatted value mistmatch")
    }
  }

  @Test fun testConsoleFormatGuestTypes() = executeGuest {
    // language=javascript
    """
      console.log("null test", null);
      console.log("bool test: `true`", true);
      console.log("bool test: `false`", false);
      console.log("string test", "Hello");
      console.log("number test: integer", 1);
      console.log("number test: long", 2);
      console.log("date test", new Date());
    """
  }.doesNotFail()
}
