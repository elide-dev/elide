package elide.runtime

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Tests for acquiring [Logger] instances on the JVM. */
class LoggerTest {
  @Test fun testLogLevelTrace() {
    val logger = Logging.of(LoggerTest::class)
    assertDoesNotThrow {
      logger.log(
        LogLevel.TRACE,
        listOf(
          "Here is a low-level trace log"
        )
      )
    }
    assertDoesNotThrow {
      logger.trace(
        "Here is a string trace log"
      )
    }
    assertDoesNotThrow {
      logger.trace(
        "Here is a string trace log with context",
        5
      )
    }
    assertDoesNotThrow {
      logger.trace {
        "Here is a trace log which uses a producer"
      }
    }
  }

  @Test fun testLogLevelDebug() {
    val logger = Logging.of(LoggerTest::class)
    assertDoesNotThrow {
      logger.log(
        LogLevel.DEBUG,
        listOf(
          "Here is a low-level debug log"
        )
      )
    }
    assertDoesNotThrow {
      logger.debug(
        "Here is a string debug log"
      )
    }
    assertDoesNotThrow {
      logger.debug(
        "Here is a string debug log with context",
        5
      )
    }
    assertDoesNotThrow {
      logger.debug {
        "Here is a debug log which uses a producer"
      }
    }
  }

  @Test fun testLogLevelInfo() {
    val logger = Logging.of(LoggerTest::class)
    assertDoesNotThrow {
      logger.log(
        LogLevel.INFO,
        listOf(
          "Here is an low-level info log"
        )
      )
    }
    assertDoesNotThrow {
      logger.info(
        "Here is a string info log"
      )
    }
    assertDoesNotThrow {
      logger.info(
        "Here is a string info log with context",
        5
      )
    }
    assertDoesNotThrow {
      logger.info {
        "Here is an info log which uses a producer"
      }
    }
  }

  @Test fun testLogLevelWarn() {
    val logger = Logging.of(LoggerTest::class)
    assertDoesNotThrow {
      logger.log(
        LogLevel.WARN,
        listOf(
          "Here is a low-level warn log"
        )
      )
    }
    assertDoesNotThrow {
      logger.warn(
        "Here is a string warn log"
      )
    }
    assertDoesNotThrow {
      logger.warn(
        "Here is a string warn log with context",
        5
      )
    }
    assertDoesNotThrow {
      logger.warn {
        "Here is a warn log which uses a producer"
      }
    }
    assertDoesNotThrow {
      logger.warning(
        "Here is a string warning (alias) log"
      )
    }
    assertDoesNotThrow {
      logger.warning(
        "Here is a string warning (alias) log with context",
        5
      )
    }
    assertDoesNotThrow {
      logger.warning {
        "Here is a warning (alias) log which uses a producer"
      }
    }
  }

  @Test fun testLogLevelError() {
    val logger = Logging.of(LoggerTest::class)
    assertDoesNotThrow {
      logger.log(
        LogLevel.ERROR,
        listOf(
          "Here is an low-level error log"
        )
      )
    }
    assertDoesNotThrow {
      logger.error(
        "Here is a string error log"
      )
    }
    assertDoesNotThrow {
      logger.error(
        "Here is a string error log with context",
        5
      )
    }
    assertDoesNotThrow {
      logger.error {
        "Here is an error log which uses a producer"
      }
    }
  }
}
