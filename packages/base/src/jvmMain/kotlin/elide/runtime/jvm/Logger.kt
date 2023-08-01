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

package elide.runtime.jvm

import elide.runtime.LogLevel

/** JVM implementation of a cross-platform Elide [elide.runtime.Logger] which wraps an [org.slf4j.Logger]. */
public class Logger (private val logger: org.slf4j.Logger): elide.runtime.Logger {
  // Format a list of values for emission as part of a log message.
  private fun formatLogLine(message: List<Any>): String {
    val builder = StringBuilder()
    val portions = message.size
    message.forEachIndexed { index, value ->
      builder.append(value)
      if (index < portions - 1)
        builder.append(" ")
    }
    return builder.toString()
  }

  /** @inheritDoc */
  override fun isEnabled(level: LogLevel): Boolean = if (System.getProperty("elide.test", "false") == "true") {
    true
  } else {
    level.isEnabled(logger)
  }

  /** @inheritDoc */
  override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean) {
    val enabled = levelChecked || isEnabled(level)
    if (enabled) {
      level.resolve(logger).invoke(
        formatLogLine(message)
      )
    }
  }
}
