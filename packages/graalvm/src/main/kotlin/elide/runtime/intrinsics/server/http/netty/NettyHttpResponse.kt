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
package elide.runtime.intrinsics.server.http.netty

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.DateFormatter
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.server.http.HttpRequest
import elide.runtime.intrinsics.server.http.HttpResponse
import elide.vm.annotations.Polyglot

// Methods and properties exposed to guest code on responses.
private val NETTY_HTTP_RESPONSE_PROPS_AND_METHODS = arrayOf(
  "append",
  "end",
  "get",
  "header",
  "send",
  "set",
  "status",
)

/** [HttpRequest] implementation wrapping a Netty handler context. */
@DelicateElideApi internal class NettyHttpResponse(
  private val context: ChannelHandlerContext,
  private val includeDefaults: Boolean = true,
)
  : HttpResponse, ProxyObject {
  /** Whether the response has already been sent. */
  private val sent = AtomicBoolean(false)

  /** Whether the response has already been sent. */
  private val body = AtomicReference<PolyglotValue>(null)

  /** Explicit status set by guest code. */
  private val status = AtomicInteger(0)

  /** The HTTP version of the response. */
  @Volatile private var httpVersion = HttpVersion.HTTP_1_1

  /** Headers for this response, dispatched once the response is sent to the client. */
  private val headers = DefaultHttpHeaders()

  init {
    // prepare headers
    headers
      .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
      .set(HttpHeaderNames.SERVER, "elide")
  }

  @Polyglot override fun header(name: String, value: String) {
    headers.set(name, value)
  }

  @Polyglot override fun set(name: String, value: String?) {
    headers.set(name, value)
  }

  @Polyglot override fun get(name: String): String? {
    return headers.get(name)
  }

  @Polyglot override fun append(name: String, value: String?) {
    headers.add(name, value)
  }

  @Polyglot override fun status(status: Int) {
    this.status.set(status)
  }

  @Polyglot override fun send(status: Int, body: PolyglotValue?) {
    status(status)
    this.body.set(body)
    end()
  }

  @Polyglot override fun end() {
    val status = status.get()
    val effective = if (status == 0) HttpResponseStatus.OK.code() else status
    send(HttpResponseStatus.valueOf(effective), body.get())
  }

  // Fills response headers expected by-spec.
  private fun fillResponseHeaders() {
    if (includeDefaults) {
      headers.set(HttpHeaderNames.DATE, DateFormatter.format(Date.from(Instant.now())))
    }
  }

  /**
   * Sends a response with the provided [status] and [body], allocating a buffer for the content as necessary.
   *
   * @param status The HTTP status code of the response.
   * @param body A guest value to be unwrapped and used as response content.
   */
  private fun send(status: HttpResponseStatus, body: PolyglotValue?) {
    if (!sent.compareAndSet(false, true)) return

    // TODO: support JSON objects and other types of content
    // treat any type of value as a string (force conversion)
    val content = body?.let { Unpooled.wrappedBuffer(it.toString().toByteArray()) } ?: Unpooled.EMPTY_BUFFER

    // prepare the response object
    fillResponseHeaders()

    val response = if (content != null) {
      headers.set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex())

      DefaultFullHttpResponse(
        /* version = */ httpVersion,
        /* status = */ status,
        /* content = */ content,
        /* headers = */ headers,
        /* trailingHeaders = */ EmptyHttpHeaders.INSTANCE,
      )
    } else {
      DefaultHttpResponse(
        /* version = */ httpVersion,
        /* status = */ status,
        /* headers = */ headers,
      )
    }

    // send the response
    context.write(response)
  }

  override fun getMemberKeys(): Array<String> = NETTY_HTTP_RESPONSE_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in NETTY_HTTP_RESPONSE_PROPS_AND_METHODS
  override fun putMember(key: String?, value: Value?) {
    // no-op
  }

  override fun removeMember(key: String?): Boolean {
    return false  // not supported
  }

  override fun getMember(key: String?): Any? = when (key) {
    "get" -> ProxyExecutable {
      val name = it.getOrNull(0)
      when {
        name == null || !name.isString -> throw JsError.valueError("Header name must be a string")
        else -> get(name.asString())
      }
    }

    "set" -> ProxyExecutable {
      val name = it.getOrNull(0)
      val value = it.getOrNull(1)
      when {
        name == null || !name.isString -> throw JsError.valueError("Header name must be a string")
        value == null -> throw JsError.typeError("Header value is required")
        else -> set(name.asString(), value.asString())
      }
    }

    "append" -> ProxyExecutable {
      val name = it.getOrNull(0)
      val value = it.getOrNull(1)
      when {
        name == null || !name.isString -> throw JsError.valueError("Header name must be a string")
        value == null -> throw JsError.typeError("Header value is required")
        else -> append(name.asString(), value.asString())
      }
    }

    "status" -> ProxyExecutable {
      val status = it.getOrNull(0)
      when {
        status == null || !status.fitsInInt() -> throw JsError.typeError("Status code must be an integer")
        else -> status(status.asInt())
      }
    }

    "header" -> ProxyExecutable {
      val name = it.getOrNull(0)
      val value = it.getOrNull(1)
      when {
        name == null || !name.isString -> throw JsError.valueError("Header name must be a string")
        value == null -> throw JsError.typeError("Header value is required")
        else -> header(name.asString(), value.asString())
      }
    }

    "send" -> ProxyExecutable {
      val status = it.getOrNull(0)
      val body = it.getOrNull(1)
      when {
        status == null || !status.fitsInInt() -> throw JsError.typeError("Status code must be an integer")
        else -> send(status.asInt(), body)
      }
    }

    "end" -> ProxyExecutable { this.end() }

    else -> null
  }
}
