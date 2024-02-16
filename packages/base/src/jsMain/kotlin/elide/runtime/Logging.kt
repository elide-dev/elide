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

import kotlin.reflect.KClass
import elide.runtime.js.resolve

/** Describes an expected class which is able to produce [Logger] instances as a factory. */
@Suppress("MemberVisibilityCanBePrivate", "unused") public actual class Logging {
  public companion object {
    // Singleton logging manager instance.
    private val singleton = Logging()

    /** @return Generic root logger. */
    public fun acquire(): elide.runtime.js.Logger = root()

    /** @return Logger created, or resolved, for the [target] Kotlin class. */
    public fun of(target: KClass<*>): elide.runtime.js.Logger = named(
      target.simpleName ?: ""
    )

    /** @return Logger resolved at the root name. */
    public fun root(): elide.runtime.js.Logger = named(
      ""
    )

    /** @return Logger created for the specified [name]. */
    public fun named(name: String): elide.runtime.js.Logger {
      return if (name.isEmpty() || name.isBlank()) {
        singleton.logger() as elide.runtime.js.Logger
      } else {
        singleton.logger(name) as elide.runtime.js.Logger
      }
    }

    // @TODO(sgammon): logging control JS-side
    @Suppress("UNUSED_PARAMETER")
    internal fun isEnabled(level: LogLevel) = true

    // Static log sender.
    internal fun sendLog(level: LogLevel, messages: List<Any>, levelChecked: Boolean) {
      val enabled = levelChecked || isEnabled(level)
      if (enabled) {
        level.resolve().invoke(messages.toTypedArray())
      }
    }
  }

  /**
   * Acquire a [Logger] for the given logger [name], which can be any identifying string; in JVM circumstances, the full
   * class name of the subject which is sending the logs is usually used.
   *
   * @param name Name of the logger to create and return.
   * @return Desired logger.
   */
  public actual fun logger(name: String): Logger {
    return elide.runtime.js.Logger(name)
  }

  /**
   * Acquire a root [Logger] which is unnamed, or uses an empty string value (`""`) for the logger name.
   *
   * @return Root logger.
   */
  public actual fun logger(): Logger {
    return elide.runtime.js.Logger(null)
  }
}
