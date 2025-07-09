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

import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.Value
import org.jetbrains.annotations.VisibleForTesting
import java.util.SortedSet
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import elide.runtime.Logging
import elide.runtime.intrinsics.js.node.util.DebugLogger
import elide.vm.annotations.Polyglot

// Implements a simple debug logger facade.
internal class DebugLoggerImpl private constructor (private val name: String) : DebugLogger {
  private val logger by lazy { Logging.named(DebugLogger.JS_DEBUG_LOGGER_NAME) }

  /**
   * Render a debug log line for the given logger, with the provided arguments.
   *
   * @param name Name of the logger to render a log line for.
   * @param args Arguments to render in the log line.
   */
  @VisibleForTesting
  internal fun renderLogLine(name: String, args: List<Any?>): StringBuilder = StringBuilder().apply {
    append(name)
    append(": ")
    args.forEachIndexed { idx, arg ->
      when (arg) {
        is Value -> when {
          arg.isNull -> {}
          arg.isString -> append(arg.asString())
          else -> append(arg.toString())
        }

        else -> append(arg.toString())
      }
      if (idx < args.size - 1) {
        append(" ")
      }
    }
  }

  @get:Polyglot override val loggerName: String get() = name
  @get:Polyglot override val enabled: Boolean get() = true

  override fun toString(): String = "DebugLogger(name=$name)"

  override operator fun invoke(vararg arguments: Any?): Any? = null.also {
    emit(renderLogLine(loggerName, arguments.toList()))
  }

  override fun emit(builder: StringBuilder) {
    logger.debug { builder.toString() }
  }

  // Factory for creating debug logger instances.
  @Suppress("unused") internal companion object Factory : DebugLogger.Factory {
    private val activatedLogs: AtomicRef<SortedSet<String>?> = atomic(null)

    @VisibleForTesting
    internal fun buildActivatedLogs(dbg: String = System.getenv("NODE_DEBUG") ?: ""): SortedSet<String> {
      return dbg.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSortedSet()
    }

    @VisibleForTesting
    internal fun resolveActivatedLogs(): SortedSet<String> = when (val logs = activatedLogs.value) {
      null -> buildActivatedLogs().also {
        activatedLogs.value = it
      }

      else -> logs
    }

    @VisibleForTesting
    internal fun resetActivatedLogs() {
      activatedLogs.value = null
    }

    @VisibleForTesting
    internal fun mountActivatedLogs(logs: SortedSet<String>) {
      activatedLogs.value = logs
    }

    // Create a new debug logger instance with the given name.
    @JvmStatic private fun create(name: String): DebugLoggerImpl = DebugLoggerImpl(name)

    // Indicate whether debug logging is enabled for the given name.
    @JvmStatic internal fun enabled(name: String): Boolean = (name in resolveActivatedLogs()).also {
      require(!ImageInfo.inImageBuildtimeCode())
    }

    override fun createLogger(name: String): DebugLogger = when (enabled(name)) {
      false -> InertDebugLogger
      true -> create(name)
    }
  }
}
