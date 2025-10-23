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

import io.netty.handler.codec.http.HttpContent
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.io.bytestring.ByteStringBuilder
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.ContextLocal
import elide.runtime.exec.compute
import elide.runtime.intrinsics.server.http.v2.HttpContentSource

/**
 * File-like WSGI input stream backed by an [HttpContentSource].
 *
 * The stream requests content chunks on demand from the provided executor thread and exposes standard Python file
 * APIs (`read`, `readline`, `readlines`, iteration).
 *
 * Reading from this stream will block until enough data is available. Up to [limit] bytes will be read in total from
 * the source before automatically releasing this consumer.
 */
public class WsgiInputStream(
  private val executor: ContextAwareExecutor,
  private val limit: Long,
) : ProxyObject, HttpContentSource.Consumer {

  private val lock = ReentrantLock()
  private val bytesRead = AtomicLong()
  private val exhausted = AtomicBoolean()

  private var currentBuffer: HttpContent? = null
  private var sourceHandle: HttpContentSource.Handle? = null
  private var readLatch: CountDownLatch? = null

  override fun attached(handle: HttpContentSource.Handle): Unit = lock.withLock {
    sourceHandle = handle
    releaseLatch()
  }

  override fun released(): Unit = lock.withLock {
    sourceHandle = null
    releaseLatch()
  }

  /**
   * Release any buffered content and detach from the underlying source.
   *
   * Servers should call this after WSGI request handling completes to ensure all pending Netty buffers are released,
   * even if the application did not drain the stream fully.
   */
  public fun dispose() {
    val handle = lock.withLock {
      val activeHandle = sourceHandle
      sourceHandle = null
      releaseCurrentBufferLocked()
      releaseLatch()
      exhausted.set(true)
      activeHandle
    }

    handle?.release()
  }

  override fun consume(content: HttpContent, handle: HttpContentSource.Handle) {
    lock.withLock {
      releaseCurrentBufferLocked()
      sourceHandle = handle
      currentBuffer = content

      releaseLatch()
    }
  }

  private fun releaseLatch() {
    readLatch?.countDown()
    readLatch = null
  }

  private fun releaseCurrentBufferLocked() {
    currentBuffer?.release()
    currentBuffer = null
  }

  private fun prepareReadCount(size: Long?): Int {
    if (size == 0L) return 0

    val remaining = (limit - bytesRead.get()).coerceAtLeast(0)
    if (remaining <= 0) return markExhausted()

    val requested = size?.takeIf { it > 0 }
    val cap = requested?.coerceAtMost(remaining) ?: remaining
    if (cap <= 0) return markExhausted()

    return cap.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().takeIf { it > 0 } ?: markExhausted()
  }

  private fun markExhausted(): Int {
    lock.withLock { releaseCurrentBufferLocked() }
    exhausted.set(true)
    return 0
  }

  // blocks until content is available; returns null if EOF
  private fun awaitBuffer(): HttpContent? {
    if (bytesRead.get() >= limit) {
      lock.withLock { releaseCurrentBufferLocked() }
      exhausted.set(true)
      return null
    }

    var available: HttpContent? = null
    var latch: CountDownLatch? = null
    val eof = lock.withLock {
      val buffer = currentBuffer
      if (buffer?.content()?.isReadable == true) {
        available = buffer
        return@withLock false
      }

      releaseCurrentBufferLocked()

      val handle = sourceHandle ?: return@withLock true

      if (readLatch == null) {
        readLatch = CountDownLatch(1)
        handle.pull()
      }

      latch = readLatch
      false
    }

    available?.let { return it }
    if (eof) {
      exhausted.set(true)
      return null
    }

    latch?.await()

    return lock.withLock {
      val buffer = currentBuffer
      if (buffer == null) exhausted.set(true)
      buffer
    }
  }

  private fun convertBytes(bytes: ByteArray): Value {
    val factory = LocalBytesFactory.compute(executor) { Context.getCurrent().eval(BYTES_FACTORY) }
    return factory.execute(bytes)
  }

  private fun readLine(size: Long? = null): ByteArray {
    if (exhausted.get()) return ByteArray(0)

    val count = prepareReadCount(size)
    if (count == 0) return ByteArray(0)

    var read = 0
    var buffer = awaitBuffer()?.content() ?: return ByteArray(0)
    val bytes = ByteStringBuilder()

    while (read < count) {
      if (!buffer.isReadable) {
        buffer = awaitBuffer()?.content() ?: break
        continue
      }
      val byte = buffer.readByte()

      bytes.append(byte)
      read++

      if (byte == '\n'.code.toByte()) break
    }

    if (read == 0) return ByteArray(0)

    bytesRead.addAndGet(read.toLong())
    if (bytesRead.get() >= limit) exhausted.set(true)

    return bytes.toByteString().toByteArray()
  }

  private fun readBytes(size: Long? = null): ByteArray {
    if (exhausted.get()) return ByteArray(0)

    val count = prepareReadCount(size)
    if (count == 0) return ByteArray(0)

    val bytes = ByteArray(count)
    var read = 0
    while (read < count) {
      val buffer = awaitBuffer()?.content() ?: break
      if (!buffer.isReadable) continue
      val length = buffer.readableBytes().coerceAtMost(count - read)

      buffer.readBytes(bytes, read, length)
      read += length
    }

    if (read == 0) return ByteArray(0)

    bytesRead.addAndGet(read.toLong())
    if (bytesRead.get() >= limit) exhausted.set(true)

    return if (read == count) bytes else bytes.copyOf(read)
  }

  private fun readLines(): List<Value> = buildList {
    while (true) readLine().takeIf { it.isNotEmpty() }?.let { add(convertBytes(it)) } ?: break
  }

  override fun getMemberKeys(): Array<String> = MEMBERS
  override fun hasMember(key: String): Boolean = MEMBERS.binarySearch(key) >= 0
  override fun putMember(key: String?, value: Value?): Nothing = error("Modifying the input stream is not allowed")

  override fun getMember(key: String?): Any? = when (key) {
    MEMBER_ITER -> ProxyExecutable { this }
    MEMBER_NEXT -> ProxyExecutable { convertBytes(readLine()) }
    MEMBER_READ -> ProxyExecutable { args -> convertBytes(readBytes(args.getOrNull(0)?.asLong())) }
    MEMBER_READ_LINE -> ProxyExecutable { args -> convertBytes(readLine(args.getOrNull(0)?.asLong())) }
    MEMBER_READ_LINES -> ProxyExecutable { readLines() }
    MEMBER_CLOSE -> ProxyExecutable {
      dispose()
      null
    }
    else -> null
  }

  public companion object {
    private const val MEMBER_ITER = "__iter__"
    private const val MEMBER_NEXT = "__next__"
    private const val MEMBER_READ = "read"
    private const val MEMBER_READ_LINE = "readline"
    private const val MEMBER_READ_LINES = "readlines"
    private const val MEMBER_CLOSE = "close"

    private val MEMBERS = arrayOf(
      MEMBER_ITER,
      MEMBER_NEXT,
      MEMBER_READ,
      MEMBER_READ_LINE,
      MEMBER_READ_LINES,
      MEMBER_CLOSE,
    ).sortedArray()

    // language=python
    private val BYTES_FACTORY = Source.create("python", "lambda x: bytes(x)")
    private val LocalBytesFactory = ContextLocal<Value>()
  }
}
