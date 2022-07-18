package elide.server.runtime.jvm

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test

/** Test for the default [UncaughtExceptionHandler]. */
class UncaughtExceptionHandlerTest {
  val handler = UncaughtExceptionHandler()

  @Test fun testLogUncaughtException() {
    assertDoesNotThrow {
      handler.uncaughtException(
        Thread.currentThread(),
        IllegalStateException("sample exception (not real)")
      )
    }
  }
}
