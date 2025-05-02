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

package elide.tool.cli.output

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.Context
import ch.qos.logback.core.Layout
import com.github.ajalt.mordant.terminal.Terminal
import org.slf4j.LoggerFactory

/** Implements a Logback appender which proxies output to JLine. */
internal class MordantLogbackAppender (
  ctx: Context,
  private val terminal: Terminal,
) : AppenderBase<ILoggingEvent>() {
  // Pattern layout.
  private val pattern: Layout<ILoggingEvent> = PatternLayout().apply {
    pattern = "%cyan(%d{HH:mm:ss.SSS}) %highlight(%-5level) %magenta(elide) - %msg%n"
  }

  init {
    pattern.context = ctx
    setContext(ctx)
  }

  override fun start() {
    super.start()
    pattern.start()
  }

  override fun stop() {
    pattern.stop()
    super.stop()
  }

  override fun append(eventObject: ILoggingEvent) {
    terminal.println(pattern.doLayout(eventObject))
  }
}

// Redirect logging calls to Mordant's terminal for output.
internal fun Terminal.redirectLoggingToMordant() {
  val rootLogger = LoggerFactory.getLogger(TOOL_LOGGER_NAME) as? ch.qos.logback.classic.Logger
    ?: return
  val current = rootLogger.getAppender(TOOL_LOGGER_APPENDER) as? ConsoleAppender<ILoggingEvent>
    ?: return
  val ctx = current.context
  val appender = MordantLogbackAppender(ctx, this)
  rootLogger.detachAndStopAllAppenders()
  rootLogger.addAppender(appender)
  appender.start()
}
