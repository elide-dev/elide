/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.runtime

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import elide.runtime.jvm.jvmLevel

/** Tests for acquiring [Logger] instances on the JVM. */
class LoggerTest {
  @Test fun testLoggerLevels() {
    val levels = LogLevel.entries
    assertNotNull(levels)
    levels.forEach {
      assertNotNull(it.name)
      assertNotNull(it.ordinal)
      assertNotNull(it.jvmLevel)
    }
  }

  @Test fun testLoggerAsSlf4j() {
    val logger = Logging.of(LoggerTest::class)
    assertIs<org.slf4j.Logger>(logger)
    val openLogger: org.slf4j.Logger = logger
    assertDoesNotThrow {
      openLogger.trace("Here is a string trace log")
    }
    assertDoesNotThrow {
      openLogger.debug("Here is a string debug log")
    }
    assertDoesNotThrow {
      openLogger.info("Here is a string info log")
    }
    assertDoesNotThrow {
      openLogger.warn("Here is a string warn log")
    }
    assertDoesNotThrow {
      openLogger.error("Here is a string error log")
    }
    assertNotNull(openLogger.isTraceEnabled)
    assertNotNull(openLogger.isDebugEnabled)
    assertNotNull(openLogger.isInfoEnabled)
    assertNotNull(openLogger.isWarnEnabled)
    assertNotNull(openLogger.isErrorEnabled)
  }

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
        "Here is a string trace log with context {}",
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
        "Here is a string warn log with context {}",
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

  @Test fun testFormatSpecialTypes() {
    val logger = Logging.of(LoggerTest::class)
    assertDoesNotThrow {
      logger.log(
        LogLevel.INFO,
        listOf(
          Throwable("Here is an exception")
        )
      )
    }
    assertDoesNotThrow {
      logger.log(
        LogLevel.INFO,
        listOf(
          Throwable("Here is an exception"),
          "Here is a message"
        )
      )
    }
  }
}
