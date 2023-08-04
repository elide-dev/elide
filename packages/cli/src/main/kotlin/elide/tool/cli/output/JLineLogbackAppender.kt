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

package elide.tool.cli.output

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.Context
import ch.qos.logback.core.Layout
import org.jline.reader.LineReader

/** Implements a Logback appender which proxies output to JLine. */
internal class JLineLogbackAppender (
  ctx: Context,
  private val reader: LineReader,
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

  /** @inheritDoc */
  override fun append(eventObject: ILoggingEvent) {
    reader.printAbove(pattern.doLayout(eventObject))
  }
}
