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
package elide.runtime.http.server.python.asgi

import io.netty.buffer.ByteBuf
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.http.server.HttpRequestBody
import elide.runtime.http.server.HttpRequestConsumer

/**
 * ASGI `receive` callable that bridges Netty request body chunks into ASGI message dicts.
 *
 * When the ASGI application calls `await receive()`, this returns a dict describing the next chunk of the request body:
 *
 * ```python
 * {"type": "http.request", "body": b"...", "more_body": True/False}
 * ```
 *
 * The receive callable blocks until data is available from the Netty pipeline, making it safe to use from Python's
 * asyncio event loop via `asyncio.to_thread()` or GraalPy's threading bridge.
 */
public class AsgiReceive : ProxyExecutable, HttpRequestConsumer {
  private val chunks = LinkedBlockingQueue<BodyChunk>()
  private val closed = AtomicBoolean(false)
  private var reader: HttpRequestBody.Reader? = null

  private sealed interface BodyChunk {
    data class Data(val bytes: ByteArray) : BodyChunk
    data object End : BodyChunk
    data class Error(val cause: Throwable) : BodyChunk
  }

  override fun onAttached(reader: HttpRequestBody.Reader) {
    this.reader = reader
    reader.pull()
  }

  override fun onRead(content: ByteBuf) {
    val bytes = ByteArray(content.readableBytes())
    content.readBytes(bytes)
    chunks.offer(BodyChunk.Data(bytes))
    reader?.pull()
  }

  override fun onClose(failure: Throwable?) {
    if (failure != null) {
      chunks.offer(BodyChunk.Error(failure))
    } else {
      chunks.offer(BodyChunk.End)
    }
    closed.set(true)
  }

  /**
   * Called by the ASGI application as `await receive()`. Returns a dict with:
   * - `type`: `"http.request"`
   * - `body`: byte content of this chunk
   * - `more_body`: whether more data is expected
   *
   * If no data is available yet, this call blocks until the Netty pipeline delivers more content or closes the stream.
   */
  override fun execute(vararg arguments: org.graalvm.polyglot.Value?): Any {
    val chunk = chunks.poll(30, TimeUnit.SECONDS)
      ?: return disconnectMessage()

    return when (chunk) {
      is BodyChunk.Data -> ProxyHashMap.from(mutableMapOf<Any, Any>(
        "type" to "http.request",
        "body" to chunk.bytes,
        "more_body" to !closed.get(),
      ))

      is BodyChunk.End -> ProxyHashMap.from(mutableMapOf<Any, Any>(
        "type" to "http.request",
        "body" to ByteArray(0),
        "more_body" to false,
      ))

      is BodyChunk.Error -> ProxyHashMap.from(mutableMapOf<Any, Any>(
        "type" to "http.disconnect",
      ))
    }
  }

  private fun disconnectMessage(): ProxyHashMap = ProxyHashMap.from(mutableMapOf<Any, Any>(
    "type" to "http.disconnect",
  ))

  /**
   * Release any resources held by this receive callable. Servers should call this after request handling completes
   * to ensure pending Netty buffers are drained.
   */
  public fun dispose() {
    reader?.release()
    reader = null
    chunks.clear()
    closed.set(true)
  }
}
