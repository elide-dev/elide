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


/** @return SLF4J (JVM) equivalent of the current log level. */
public val LogLevel.jvmLevel: org.slf4j.event.Level get() = when (this) {
  LogLevel.TRACE -> org.slf4j.event.Level.TRACE
  LogLevel.DEBUG -> org.slf4j.event.Level.DEBUG
  LogLevel.WARN -> org.slf4j.event.Level.WARN
  LogLevel.INFO -> org.slf4j.event.Level.INFO
  LogLevel.ERROR -> org.slf4j.event.Level.ERROR
}

/** @return Whether the provided [logger] enables the current log level. */
public fun LogLevel.isEnabled(logger: org.slf4j.Logger): Boolean = when (this) {
  LogLevel.TRACE -> logger.isTraceEnabled
  LogLevel.DEBUG -> logger.isDebugEnabled
  LogLevel.WARN -> logger.isWarnEnabled
  LogLevel.INFO -> logger.isInfoEnabled
  LogLevel.ERROR -> logger.isErrorEnabled
}

/** @return Logger method which should be called for the current log level. */
public fun LogLevel.resolve(logger: org.slf4j.Logger): (String) -> Unit = when (this) {
  LogLevel.TRACE -> logger::trace
  LogLevel.DEBUG -> logger::debug
  LogLevel.WARN -> logger::warn
  LogLevel.INFO -> logger::info
  LogLevel.ERROR -> logger::error
}
