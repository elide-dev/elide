package elide.runtime

import kotlin.test.Test
import kotlin.test.assertNotNull

/** Tests for acquiring [Logger] instances on the JVM. */
class LoggingFactoryTest {
  @Test fun testAcquireRootLogger() {
    assertNotNull(
      Logging.root(),
      "should be able to acquire root logger on JVM"
    )
  }

  @Test fun testAcquireNamedLogger() {
    assertNotNull(
      Logging.named("some.logger"),
      "should be able to acquire a logger with an arbitrary name on JVM"
    )
  }

  @Test fun testAcquireLoggerForClass() {
    assertNotNull(
      Logging.of(LoggingFactoryTest::class),
      "should be able to acquire a logger for a Kotlin class"
    )
    assertNotNull(
      Logging.of(LoggingFactoryTest::class.java),
      "should be able to acquire a logger for a Java class"
    )
  }
}
