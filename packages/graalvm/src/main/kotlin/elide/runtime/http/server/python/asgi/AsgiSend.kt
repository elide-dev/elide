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

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.Logging
import elide.runtime.http.server.HttpCall
import elide.runtime.http.server.HttpResponseBody

/**
 * ASGI `send` callable that bridges ASGI response messages back to the Netty HTTP response pipeline.
 *
 * The ASGI application calls `await send(message)` with message dicts of the following types:
 *
 * - `http.response.start`: Sets the response status code and headers.
 * - `http.response.body`: Writes response body bytes. The `more_body` key indicates streaming.
 *
 * This callable captures the response state and writes it back to the Netty [HttpCall] when complete.
 */
public class AsgiSend(
  private val response: HttpResponse,
  private val responseBody: HttpResponseBody,
  private val onComplete: () -> Unit,
) : ProxyExecutable {
  private val headersSent = AtomicBoolean(false)
  private val bodyComplete = AtomicBoolean(false)
  private val bodyChunks = mutableListOf<ByteArray>()

  /**
   * Called by the ASGI application as `await send(message)`.
   *
   * Processes ASGI response messages and maps them to Netty response operations.
   */
  override fun execute(vararg arguments: Value?): Any? {
    val message = arguments.firstOrNull() ?: error("send() requires a message argument")
    val type = message.getMember("type")?.asString() ?: error("ASGI message must have a 'type' key")

    when (type) {
      "http.response.start" -> handleResponseStart(message)
      "http.response.body" -> handleResponseBody(message)
      "http.response.disconnect" -> handleDisconnect()
      else -> log.warn("Unknown ASGI send message type: {}", type)
    }
    return null
  }

  /**
   * Handle `http.response.start` — set status code and response headers.
   */
  private fun handleResponseStart(message: Value) {
    check(!headersSent.getAndSet(true)) { "Response headers already sent (duplicate http.response.start)" }

    val status = message.getMember("status")
    check(status != null && status.isNumber && status.fitsInInt()) {
      "http.response.start must include an integer 'status' field"
    }
    response.status = HttpResponseStatus.valueOf(status.asInt())

    val headers = message.getMember("headers")
    if (headers != null && headers.hasArrayElements()) {
      applyHeaders(headers)
    }
  }

  /**
   * Handle `http.response.body` — write body bytes to the response.
   *
   * If `more_body` is `false` (or absent), the response is finalized.
   */
  private fun handleResponseBody(message: Value) {
    check(headersSent.get()) { "Cannot send body before http.response.start" }

    val body = message.getMember("body")
    if (body != null && !body.isNull) {
      val bytes = extractBytes(body)
      if (bytes.isNotEmpty()) {
        bodyChunks.add(bytes)
      }
    }

    val moreBody = message.getMember("more_body")?.let {
      it.isBoolean && it.asBoolean()
    } ?: false

    if (!moreBody) {
      finalizeResponse()
    }
  }

  private fun handleDisconnect() {
    if (!bodyComplete.getAndSet(true)) {
      onComplete()
    }
  }

  /**
   * Apply ASGI headers to the Netty response.
   *
   * ASGI headers are `[[name_bytes, value_bytes], ...]` — each element is a two-element array of byte strings.
   */
  private fun applyHeaders(headers: Value) {
    for (i in 0 until headers.arraySize) {
      val pair = headers.getArrayElement(i)
      check(pair.hasArrayElements() && pair.arraySize == 2L) {
        "ASGI header must be a two-element array [name, value], got: $pair"
      }

      val nameValue = pair.getArrayElement(0)
      val valueValue = pair.getArrayElement(1)

      val name = extractString(nameValue)
      val value = extractString(valueValue)

      response.headers().add(name, value)
    }
  }

  /**
   * Extract a string from a GraalVM Value that may be a string, byte array, or buffer.
   */
  private fun extractString(value: Value): String = when {
    value.isString -> value.asString()
    value.hasBufferElements() -> {
      val bytes = ByteArray(value.bufferSize.toInt())
      value.readBuffer(0L, bytes, 0, bytes.size)
      String(bytes, Charsets.UTF_8)
    }
    value.hasArrayElements() -> {
      val bytes = ByteArray(value.arraySize.toInt())
      for (j in bytes.indices) {
        bytes[j] = value.getArrayElement(j.toLong()).asByte()
      }
      String(bytes, Charsets.UTF_8)
    }
    else -> value.toString()
  }

  /**
   * Extract bytes from a GraalVM Value that may be a buffer, byte array, or string.
   */
  private fun extractBytes(value: Value): ByteArray = when {
    value.hasBufferElements() -> {
      val bytes = ByteArray(value.bufferSize.toInt())
      value.readBuffer(0L, bytes, 0, bytes.size)
      bytes
    }
    value.hasArrayElements() -> {
      val bytes = ByteArray(value.arraySize.toInt())
      for (j in bytes.indices) {
        bytes[j] = value.getArrayElement(j.toLong()).asByte()
      }
      bytes
    }
    value.isString -> value.asString().toByteArray(Charsets.UTF_8)
    else -> ByteArray(0)
  }

  /**
   * Finalize the response by setting content-length and signaling completion.
   */
  private fun finalizeResponse() {
    if (bodyComplete.getAndSet(true)) return

    val totalBytes = bodyChunks.sumOf { it.size }

    if (!response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
      response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, totalBytes)
    }

    if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
    }

    responseBody.source { writer ->
      for (chunk in bodyChunks) {
        writer.write(chunk)
      }
      writer.end()
    }

    onComplete()
  }

  /** Returns all collected body chunks as a single byte array. Used for testing and debugging. */
  internal fun collectedBody(): ByteArray {
    val total = bodyChunks.sumOf { it.size }
    val result = ByteArray(total)
    var offset = 0
    for (chunk in bodyChunks) {
      System.arraycopy(chunk, 0, result, offset, chunk.size)
      offset += chunk.size
    }
    return result
  }

  /** Whether the response body has been fully sent. */
  public val isComplete: Boolean get() = bodyComplete.get()

  public companion object {
    private val log = Logging.of(AsgiSend::class.java)
  }
}
