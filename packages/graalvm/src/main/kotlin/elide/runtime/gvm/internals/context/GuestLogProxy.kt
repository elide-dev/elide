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

package elide.runtime.gvm.internals.context

import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import elide.runtime.Logger
import elide.runtime.Logging

/**
 * TBD.
 */
internal class GuestLogProxy private constructor (private val logger: Logger): Handler() {
  companion object {
    /**
     * Return a log proxy for the provided [name].
     *
     * @param name Name for the logger which will be proxied by the resulting object.
     * @return Guest log proxy wrapping the new logger.
     */
    @JvmStatic fun named(name: String): GuestLogProxy = GuestLogProxy(name)

    /**
     * Return a log proxy wrapping the provided [logger].
     *
     * @param logger Logger to wrap.
     * @return Guest log proxy wrapping the logger.
     */
    @JvmStatic fun wrapping(logger: Logger): GuestLogProxy = GuestLogProxy(logger)
  }

  private constructor (name: String): this(Logging.named(name))

  override fun publish(record: LogRecord?) {
    if (record == null) return
    val fmt = record.message

    when (record.level) {
      // no-op if off
      Level.OFF -> {}

      // FINEST becomes trace
      Level.FINEST -> logger.info(fmt)

      // FINE and FINER become debug
      Level.FINE,
      Level.FINER -> logger.debug(fmt)

      // INFO stays info
      Level.INFO -> logger.info(fmt)

      // WARN becomes warning, ERROR becomes SEVERE
      Level.WARNING -> logger.warn(fmt)
      Level.SEVERE -> logger.error(fmt)
    }
  }

  override fun flush() {
    // nothing at this time
  }

  override fun close() {
    // nothing at this time
  }
}
