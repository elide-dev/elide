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

package elide.runtime.jvm

import elide.runtime.LogLevel

/** JVM implementation of a cross-platform Elide [elide.runtime.Logger] which wraps an [org.slf4j.Logger]. */
public class Logger (private val logger: org.slf4j.Logger): elide.runtime.Logger {
  private fun StringBuilder.formatValue(value: Any): Any = when (value) {
    // formatting for errors (print stacktrace and message)
    is Throwable -> value.stackTraceToString().let { stacktrace ->
      append(value.javaClass.name)
      append(": ")
      append(value.message)
      append("\n")
      append(stacktrace)
    }

    // any other type
    else -> append(value.toString())
  }

  // Format a list of values for emission as part of a log message.
  private fun formatLogLine(message: List<Any>): String = StringBuilder().apply {
    val portions = message.size
    message.forEachIndexed { index, value ->
      formatValue(value)
      if (index < portions - 1) append(" ")
    }
  }.toString()

  override fun isEnabled(level: LogLevel): Boolean = if (System.getProperty("elide.test", "false") == "true") {
    true
  } else {
    level.isEnabled(logger)
  }

  override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean) {
    val enabled = levelChecked || isEnabled(level)
    if (enabled) {
      level.resolve(logger).invoke(
        formatLogLine(message)
      )
    }
  }
}
