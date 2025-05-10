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
package elide.exec

import org.slf4j.event.Level

/**
 * # Native Tracing
 */
public object TraceNative {
  /**
   * ## Log Record
   */
  public data class LogRecord(
    var severity: String? = null,
    var file: String? = null,
    var line: Int? = null,
    var message: String? = null,
  )

  /**
   * ## Trace Record
   */
  public data class TraceRecord(
    var severity: String? = null,
    var message: String? = null,
  )

  @JvmStatic
  @JvmName("lookupLoggerEnabled") public fun lookupLoggerEnabled(name: String): Boolean {
    return try {
      val logger = org.slf4j.LoggerFactory.getLogger(name)
      logger.isEnabledForLevel(Level.ERROR)
    } catch (e: Exception) {
      false
    }
  }

  // Object delivery.
  @JvmStatic
  @JvmName("deliverNativeLog") public fun deliverNativeLog(record: LogRecord): Boolean {
    return true //
  }

  // Primitive delivery.
  // (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
  @JvmStatic
  @JvmName("deliverNativeLog") public fun deliverNativeLog(
    level: String?,
    target: String?,
    message: String?,
    thread: String?,
  ): Boolean {
    return true //
  }

  // Object delivery.
  @JvmStatic
  @JvmName("deliverNativeTrace") public fun deliverNativeTrace(record: TraceRecord): Boolean {
    return true //
  }

  // Primitive delivery.
  // (JLjava/lang/String;Ljava/lang/String;)V
  @JvmStatic
  @JvmName("deliverNativeTrace") public fun deliverNativeTrace(
    timestamp: Long?,
    level: String?,
    message: String?,
  ): Boolean {
    return true //
  }

  @JvmStatic
  @JvmName("flushNativeLoggers") public fun flushNativeLoggers(): Boolean {
    return true //
  }
}
