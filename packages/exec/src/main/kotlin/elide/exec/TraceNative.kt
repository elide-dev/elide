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
import java.util.concurrent.ConcurrentLinkedQueue
import elide.runtime.Logging

/**
 * # Native Tracing
 *
 * Defines types and methods which are used as up-calls from JNI; as such, these must remain static, and any changes to
 * visible symbols must propagate to JNI code.
 */
public object TraceNative {
  private const val RECENT_CAPACITY = 20
  private val recentTraces = ConcurrentLinkedQueue<TraceRecord>()
  private val recentLogs = ConcurrentLinkedQueue<LogRecord>()

  /**
   * ## Log Record
   *
   * Describes a log record object; this is an intermediate type created by the native log layer, and then consumed by
   * the JVM layer upon log delivery.
   */
  @JvmRecord public data class LogRecord(
    val target: String? = null,
    val severity: String? = null,
    val file: String? = null,
    val line: Int? = null,
    val message: String? = null,
    val thread: String? = null,
  )

  /**
   * ## Trace Record
   *
   * Describes a trace record object; this is an intermediate type created by the native tracing layer, and then
   * consumed by the JVM layer upon trace delivery.
   */
  @JvmRecord public data class TraceRecord(
    public val timestamp: Long,
    public val id: Long,
    public val typeId: Int,
    public val level: Int,
    public val message: String?,
  ) {
    // Log-level severity for this trace record.
    public val severity: Level get() = when (level) {
      1 -> Level.ERROR
      2 -> Level.WARN
      3 -> Level.INFO
      4 -> Level.DEBUG
      5 -> Level.TRACE
      else -> Level.ERROR  // default
    }
  }

  // Internal access to recently-delivered logs.
  internal fun allRecentLogs(): Sequence<LogRecord> {
    val copy = recentLogs.toList()
    return copy.asSequence()
  }

  // Internal access to recently-delivered traces.
  internal fun allRecentTraces(): Sequence<TraceRecord> {
    val copy = recentTraces.toList()
    return copy.asSequence()
  }

  // JNI up-call to determine if a logger is enabled by name.
  @JvmStatic
  @JvmName("lookupLoggerEnabled") public fun lookupLoggerEnabled(name: String): Boolean {
    return org.slf4j.LoggerFactory.getLogger(name).isEnabledForLevel(Level.ERROR)
  }

  // Object JNI up-call delivery for logs.
  @JvmStatic
  @JvmName("deliverNativeLog") public fun deliverNativeLog(record: LogRecord): Boolean {
    if (recentLogs.size >= RECENT_CAPACITY) {
      recentLogs.remove()
    }
    recentLogs.add(record)

    val logger = record.target?.let { Logging.named(it) } ?: Logging.root()
    val severity = when (record.severity) {
      "TRACE" -> Level.TRACE
      "DEBUG" -> Level.DEBUG
      "INFO" -> Level.INFO
      "WARN" -> Level.WARN
      "ERROR" -> Level.ERROR
      else -> Level.ERROR  // default
    }
    if (logger.isEnabledForLevel(severity)) {
      logger.makeLoggingEventBuilder(severity).apply {
        record.message?.let { setMessage(it) }
        record.thread?.let { addKeyValue("thread", it) }
      }.log()
    }
    return true
  }

  // Primitive JNI up-call delivery for logs.
  // (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
  @JvmStatic
  @JvmName("deliverNativeLog") public fun deliverNativeLog(
    level: String?,
    target: String?,
    message: String?,
    thread: String?,
  ): Boolean = deliverNativeLog(LogRecord(
    severity = level,
    file = null,
    line = null,
    message = message,
    target = target,
    thread = thread,
  ))

  // Object JNI up-call delivery for tracing.
  @JvmStatic
  @JvmName("deliverNativeTrace") public fun deliverNativeTrace(record: TraceRecord): Boolean {
    if (recentTraces.size >= RECENT_CAPACITY) {
      recentTraces.remove()
    }
    recentTraces.add(record)
    Logging.root().warn("Received native trace ------- $record")
    return true //
  }

  // Primitive JNI up-call delivery for tracing.
  // (JJIILjava/lang/String;)Z
  @JvmStatic
  @JvmName("deliverNativeTrace") public fun deliverNativeTrace(
    timestamp: Long,
    id: Long,
    typeId: Int,
    level: Int,
    message: String?,
  ): Boolean {
    Logging.root().warn("Received native trace ------ " +
                               "[timestamp=$timestamp] [id=$id] [type=$typeId] [level=$level] [message=$message]")
    return deliverNativeTrace(TraceRecord(
      timestamp = timestamp,
      id = id,
      typeId = typeId,
      level = level,
      message = message,
    )) //
  }

  @JvmStatic
  @JvmName("flushNativeLoggers") public fun flushNativeLoggers(): Boolean {
    return true //
  }
}
