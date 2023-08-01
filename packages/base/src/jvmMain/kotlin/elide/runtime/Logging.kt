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

package elide.runtime

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/** Describes an expected class which is able to produce [Logger] instances as a factory. */
public actual class Logging private constructor () {
  public companion object {
    // Singleton logging manager instance.
    private val singleton = Logging()

    /** @return Logger created, or resolved, for the [target] Kotlin class. */
    @JvmStatic public fun of(target: KClass<*>): elide.runtime.jvm.Logger = named(
      target.qualifiedName ?: target.simpleName ?: ""
    )

    /** @return Logger created, or resolved, for the [target] Java class. */
    @JvmStatic public fun of(target: Class<*>): elide.runtime.jvm.Logger = named(
      target.canonicalName ?: target.name ?: target.simpleName ?: ""
    )

    /** @return Logger resolved at the root name. */
    @JvmStatic public fun root(): elide.runtime.jvm.Logger = named(
      ""
    )

    /** @return Logger created for the specified [name]. */
    @JvmStatic public fun named(name: String): elide.runtime.jvm.Logger {
      return if (name.isEmpty() || name.isBlank()) {
        singleton.logger() as elide.runtime.jvm.Logger
      } else {
        singleton.logger(name) as elide.runtime.jvm.Logger
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
    return elide.runtime.jvm.Logger(
      LoggerFactory.getLogger(name)
    )
  }

  /**
   * Acquire a root [Logger] which is unnamed, or uses an empty string value (`""`) for the logger name.
   *
   * @return Root logger.
   */
  public actual fun logger(): Logger {
    return elide.runtime.jvm.Logger(
      LoggerFactory.getLogger("")
    )
  }
}
