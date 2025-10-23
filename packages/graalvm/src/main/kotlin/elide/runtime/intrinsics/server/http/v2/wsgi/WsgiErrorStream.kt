/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.intrinsics.server.http.v2.wsgi

import java.util.concurrent.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.Logger

/**
 * WSGI `wsgi.errors` implementation that forwards writes to an [elide.runtime.Logger].
 *
 * Messages are buffered until a newline or flush is observed to reduce fragmented log lines. Servers should call
 * [dispose] once request handling finishes to guarantee that any pending content is emitted.
 */
internal class WsgiErrorStream(private val log: Logger) : ProxyObject {
  private val lock = ReentrantLock()
  private val buffer = StringBuilder()

  fun dispose() {
    flushBuffer()
  }

  override fun getMember(key: String?): Any? = when (key) {
    MEMBER_FLUSH -> ProxyExecutable {
      flushBuffer()
      null
    }

    MEMBER_WRITE -> ProxyExecutable { args ->
      writeValue(args.getOrNull(0))
      null
    }

    MEMBER_WRITE_LINES -> ProxyExecutable { args ->
      writeLines(args.getOrNull(0))
      null
    }

    MEMBER_CLOSE -> ProxyExecutable {
      dispose()
      null
    }

    else -> null
  }

  override fun getMemberKeys(): Array<String> = MEMBERS
  override fun hasMember(key: String?): Boolean = key != null && MEMBERS.binarySearch(key) >= 0
  override fun putMember(key: String?, value: Value?) = error("Modifying the error stream is not allowed")

  private fun writeValue(value: Value?) {
    if (value == null || value.isNull) return
    val text = when {
      value.isString -> value.asString()
      value.isNumber && value.fitsInInt() -> value.asInt().toString()
      value.isNumber && value.fitsInLong() -> value.asLong().toString()
      value.isNumber && value.fitsInDouble() -> value.asDouble().toString()
      value.isBoolean -> value.asBoolean().toString()
      else -> value.toString()
    }

    appendAndEmit(text)
  }

  private fun writeLines(lines: Value?) {
    if (lines == null || lines.isNull) return

    when {
      lines.hasArrayElements() -> for (index in 0 until lines.arraySize) writeValue(lines.getArrayElement(index))
      lines.isIterator -> emitIterator(lines)
      lines.hasIterator() -> emitIterator(lines.iterator)
      else -> writeValue(lines)
    }
  }

  private fun emitIterator(iterator: Value) {
    while (iterator.hasIteratorNextElement()) writeValue(iterator.iteratorNextElement)
  }

  private fun appendAndEmit(text: String) {
    if (text.isEmpty()) return

    lock.withLock {
      buffer.append(text)

      var newlineIndex = buffer.indexOf("\n")
      while (newlineIndex >= 0) {
        val endIndex = if (newlineIndex > 0 && buffer[newlineIndex - 1] == '\r') newlineIndex - 1 else newlineIndex
        val line = buffer.substring(0, endIndex)
        logLine(line)
        buffer.delete(0, newlineIndex + 1)
        newlineIndex = buffer.indexOf("\n")
      }
    }
  }

  private fun flushBuffer() {
    val pending = lock.withLock {
      if (buffer.isEmpty()) null
      else buffer.toString().removeSuffix("\r").also { buffer.setLength(0) }
    } ?: return

    logLine(pending)
  }

  private fun logLine(line: String) {
    log.error(line.ifEmpty { "" })
  }

  companion object {
    private const val MEMBER_FLUSH = "flush"
    private const val MEMBER_WRITE = "write"
    private const val MEMBER_WRITE_LINES = "writelines"
    private const val MEMBER_CLOSE = "close"

    private val MEMBERS = arrayOf(
      MEMBER_CLOSE,
      MEMBER_FLUSH,
      MEMBER_WRITE,
      MEMBER_WRITE_LINES,
    ).sortedArray()
  }
}
