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
package elide.runtime.node.util

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import kotlinx.atomicfu.atomic
import elide.runtime.Logging
import elide.runtime.intrinsics.js.node.util.DebugLogger

// Constants used in deprecation callable handling.
private const val DEFAULT_DEPRECATION_MESSAGE: String = "This function has been marked deprecated"

// Wraps a deprecated callable function that emits a warning log the first time it is called.
internal class DeprecatedCallable private constructor (
  private val fn: Value,
  private val info: DeprecationInfo,
) : ProxyExecutable {
  private val logger by lazy { Logging.named(DebugLogger.JS_DEBUG_LOGGER_NAME) }
  private val hasWarned = atomic(false)

  @JvmRecord private data class DeprecationInfo(
    val message: String? = null,
    val code: String? = null,
  )

  // Render a message about this callable's deprecation.
  private fun deprecationMessage(): String = buildString {
    val name = fn.metaSimpleName?.ifBlank { null }
    when (val msg = info.message) {
      null -> append(DEFAULT_DEPRECATION_MESSAGE).also {
        if (name != null) {
          append(": ")
          append(name)
        }
      }
      else -> append(msg)
    }
  }

  // Emit the warning just once; this will also trigger loading of the logger.
  private inline fun doWarnIfNeeded(op: () -> Value): Value {
    if (!hasWarned.value) synchronized(this) {
      hasWarned.value = true
      logger.warn(deprecationMessage())
    }
    return op()
  }

  @Suppress("SpreadOperator")
  internal operator fun invoke(arguments: Array<out Value>): Value = doWarnIfNeeded {
    fn.execute(*arguments)
  }

  override fun execute(vararg arguments: Value): Any? = this(arguments)

  internal companion object {
    /**
     * Create a new [DeprecatedCallable] instance which wraps the provided guest [fn], and which specifies the provided
     * [message] and/or [code] (as applicable) for the deprecation warning.
     *
     * @param fn Function to wrap as a deprecated callable.
     * @param message Optional message to include in the deprecation warning.
     * @param code Optional code to include in the deprecation warning.
     * @return A new [DeprecatedCallable] instance which wraps the provided function.
     */
    @JvmStatic fun create(fn: Value, message: String? = null, code: String? = null): DeprecatedCallable {
      return DeprecatedCallable(fn, DeprecationInfo(message = message, code = code))
    }
  }
}
