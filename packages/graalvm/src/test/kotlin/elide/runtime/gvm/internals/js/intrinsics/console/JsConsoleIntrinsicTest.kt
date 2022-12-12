package elide.runtime.gvm.internals.js.intrinsics.console

import elide.annotations.Inject
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.gvm.internals.intrinsics.js.console.ConsoleIntrinsic
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

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

  private fun checkTestLogs(level: LogLevel) {
    val buf = loggerBuffer.get()
    assertNotNull(buf, "should have a buffer of emitted logs")
    assertEquals(4, buf.size, "should have emitted 4 logs")
    assertEquals(level, buf[0].first, "should have emitted a `${level.name}` log each time (0)")
    assertEquals(level, buf[1].first, "should have emitted a `${level.name}` log each time (1)")
    assertEquals(level, buf[2].first, "should have emitted a `${level.name}` log each time (2)")
    assertEquals(level, buf[3].first, "should have emitted a `${level.name}` log each time (3)")
    assertEquals(
      listOf("hello log"),
      buf[0].second,
      "should have emitted the correct log message (0)",
    )
    assertEquals(
      listOf("hello log", "with", "args"),
      buf[1].second,
      "should have emitted the correct log message (1)",
    )
    assertEquals(
      listOf("hello log", "with", "args", 1, 2, 3),
      buf[2].second,
      "should have emitted the correct log message (2)",
    )
    assertEquals(
      listOf("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false),
      buf[3].second,
      "should have emitted the correct log message (3)",
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

  @Test @Disabled fun testGuestConsoleLog() = executeGuest {
    // language=javascript
    """
      console.log("hello log")
      console.log("hello log", "with", "args")
      console.log("hello log", "with", "args", 1, 2, 3)
      console.log("hello log", "with", "args", 1, 2, 3, "and", "more", "args", true, false);
    """
  }.thenAssert {
    checkTestLogs(LogLevel.DEBUG)
  }
}
