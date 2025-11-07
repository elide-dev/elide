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

package elide.runtime.http.server.js.node

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.PinnedContext
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchHeadersIntrinsic
import elide.runtime.http.server.*
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.node.buffer.NodeBufferInstance
import elide.runtime.node.buffer.NodeGuestBuffer
import elide.runtime.node.buffer.NodeHostBuffer
import elide.runtime.node.events.EventAware
import elide.runtime.node.events.EventAwareProxy

public class NodeHttpServerResponse(
  private val call: HttpCall<CallContext.Empty>,
  private val request: NodeHttpServerRequest,
  private val executor: ContextAwareExecutor,
  private val events: EventAwareProxy = EventAwareProxy.create(),
  private val origin: PinnedContext = PinnedContext.current(),
) : EventAware by events, ProxyObject, ContentStreamSource {
  private val targetWriter = AtomicReference<WritableContentStream.Writer?>(null)
  private val writeState = AtomicInteger(0)
  private val headersSent = AtomicBoolean(false)

  init {
    call.responseBody.source(this)
  }

  override fun eventNames(): List<String> = EventNames
  override fun getMemberKeys(): Array<String> = MemberKeys + events.memberKeys
  override fun hasMember(key: String): Boolean = key in MemberKeys || events.hasMember(key)

  override fun putMember(key: String?, value: Value?) {
    when (key) {
      MEMBER_STATUS_CODE -> call.response.let { response ->
        val statusCode = value?.takeIf { code -> code.isNumber && code.fitsInInt() }?.asInt()
          ?: throw TypeError.create("Status code must be an integer value")
        response.setStatus(HttpResponseStatus(statusCode, response.status().reasonPhrase()))
      }

      MEMBER_STATUS_MESSAGE -> call.response.let { response ->
        val message = value?.takeIf { m -> m.isString }?.asString()
          ?: throw TypeError.create("Status message must be a string")
        response.setStatus(HttpResponseStatus(response.status().code(), message))
      }

      else -> error("Cannot set the value of property $key for response object")
    }
  }

  override fun getMember(key: String?): Any? = when (key) {
    MEMBER_REQ -> request
    MEMBER_STATUS_CODE -> call.response.status().code()
    MEMBER_STATUS_MESSAGE -> call.response.status().reasonPhrase()

    MEMBER_HEADERS_SENT -> headersSent.get()
    MEMBER_WRITABLE_ENDED -> writeState.get() == STATE_ENDED
    MEMBER_WRITABLE_FINISHED -> writeState.get() == STATE_FINISHED

    MEMBER_GET_HEADER -> ProxyExecutable { args ->
      val name = args.getOrNull(0)?.takeIf { it.isString }?.asString()
        ?: throw TypeError.create("Header name must be a string: ${args.getOrNull(0)}")

      call.response.headers().getAll(name).let { if (it.size == 1) it.single() else it }
    }

    MEMBER_SET_HEADER -> ProxyExecutable { args ->
      val name = args.getOrNull(0).requireHeaderName()
      val value = args.getOrNull(1) ?: throw TypeError.create("Header value must not be null")

      when {
        value.isString -> call.response.headers().set(name, value.asString())
        value.isNumber -> call.response.headers().set(name, value.asLong())
        value.hasArrayElements() -> {
          val list = List(value.arraySize.toInt()) {
            val element = value.getArrayElement(it.toLong())
            if (!element.isString) throw TypeError.create("Header array element must be a string: $element")
            element.asString()
          }

          call.response.headers().set(name, list)
        }

        else -> throw TypeError.create("Header value must be a number, string, or string[]: $value")
      }
    }

    MEMBER_HAS_HEADER -> ProxyExecutable { args ->
      val name = args.getOrNull(0).requireHeaderName()
      call.response.headers().contains(name)
    }

    MEMBER_REMOVE_HEADER -> ProxyExecutable { args ->
      val name = args.getOrNull(0).requireHeaderName()
      call.response.headers().remove(name)
    }

    MEMBER_GET_HEADERS -> ProxyExecutable {
      val headers = call.response.headers()
      ProxyObject.fromMap(headers.names().associateWith { headers.getAll(it) })
    }

    MEMBER_SET_HEADERS -> ProxyExecutable { args ->
      applyHeaders(args.getOrNull(0) ?: throw TypeError.create("Headers must not be null"))
    }

    MEMBER_GET_HEADER_NAMES -> ProxyExecutable { call.response.headers().map { it.key.lowercase() } }

    MEMBER_WRITE_HEAD -> ProxyExecutable { args ->
      val statusCode = args.getOrNull(0)?.takeIf { it.isNumber && it.fitsInInt() }?.asInt()
        ?: throw TypeError.create("Status code must be an integer value")

      val statusMessage: String?
      val headers: Value?

      when (args.size) {
        1, 2 -> {
          if (args.getOrNull(1)?.isString == true) {
            statusMessage = args.getOrNull(1)?.asString()
            headers = null
          } else {
            statusMessage = null
            headers = args.getOrNull(1)
          }
        }

        3 -> {
          statusMessage = args.getOrNull(1)?.takeIf { it.isString }?.asString()
          headers = args.getOrNull(2)
        }

        else -> throw TypeError.create("Unsupported argument arrangement: $args")
      }

      call.response.status = HttpResponseStatus(statusCode, statusMessage.orEmpty())

      when {
        headers == null -> Unit // noop
        headers.hasArrayElements() -> {
          var nextName: String? = null
          repeat(headers.arraySize.toInt()) { i ->
            if (i % 2 == 0) nextName = headers.getArrayElement(i.toLong()).requireHeaderName()
            else {
              val headerValue = headers.getArrayElement(i.toLong()).takeIf { it.isString }?.asString()
                ?: throw TypeError.create("Header value must be a string")

              call.response.setHeader(nextName!!, headerValue)
            }
          }
        }

        else -> applyHeaders(headers)
      }

      headersSent.set(true)
      call.sendHeaders()
    }

    MEMBER_WRITE -> ProxyExecutable { args ->
      writeContent(args, end = false)
      false
    }

    MEMBER_END -> ProxyExecutable { args ->
      headersSent.set(true)
      if (writeState.compareAndSet(STATE_IDLE, STATE_ENDED))
        writeContent(args, end = true)

      this
    }

    MEMBER_FLUSH_HEADERS -> ProxyExecutable {
      headersSent.set(true)
      call.sendHeaders()
    }

    MEMBER_ADD_TRAILERS -> ProxyExecutable { /* noop */ } // unsupported for now
    MEMBER_CORK -> ProxyExecutable { /* noop */ }
    MEMBER_UNCORK -> ProxyExecutable { /* noop */ }

    MEMBER_STRICT_CONTENT_LENGTH -> ProxyExecutable { /* noop */ } // not configurable
    MEMBER_SEND_DATE -> ProxyExecutable { /* noop */ } // not configurable

    MEMBER_WRITE_CONTINUE -> ProxyExecutable { /* noop */ } // unsupported for now
    MEMBER_WRITE_EARLY_HINTS -> ProxyExecutable { /* noop */ } // unsupported for now
    MEMBER_WRITE_PROCESSING -> ProxyExecutable { /* noop */ } // unsupported for now

    MEMBER_SET_TIMEOUT -> ProxyExecutable { /* noop */ }
    MEMBER_SOCKET -> throw UnsupportedOperationException("Low level socket access it not yet implemented")
    else -> null
  }

  override fun onAttached(writer: WritableContentStream.Writer) {
    targetWriter.set(writer)
  }

  override fun onPull() {
    executor.execute(origin) { emit(EVENT_DRAIN) }
  }

  override fun onClose(failure: Throwable?) {
    writeState.set(STATE_FINISHED)
    targetWriter.set(null)
  }

  private fun applyHeaders(headers: Value) {
    val responseHeaders = call.response.headers()

    headers.takeIf { it.isHostObject }?.let {
      runCatching { it.asHostObject<FetchHeadersIntrinsic>() }.getOrNull()
    }?.let {
      for (name in it.keys) responseHeaders.set(name, it.getAll(name))
      return
    }

    if (!headers.hasMembers())
      throw TypeError.create("Headers value must be a Headers object or a Map: $headers")

    headers.memberKeys.forEach { name ->
      val value = headers.getMember(name)
      when {
        value.isString -> responseHeaders.set(name, value.asString())

        value.hasArrayElements() -> {
          val list = List(value.arraySize.toInt()) {
            val element = value.getArrayElement(it.toLong())
            if (!element.isString) throw TypeError.create("Header array element must be a string: $element")
            element.asString()
          }

          responseHeaders.set(name, list)
        }

        else -> responseHeaders.set(name, value.toString())
      }
    }
  }

  private fun writeContent(args: Array<out Value>, end: Boolean = false) {
    val writer = targetWriter.get() ?: error("Response is detached from call")
    val chunk = args.getOrNull(0)

    if (chunk == null) {
      if (end) writer.end()
      call.send()
      return
    }

    // map the chunk to a buffer and send it
    val encoding = args.getOrNull(1)?.takeIf { it.isString }?.asString() ?: "utf8"
    val callback = args.getOrNull(2)?.takeIf { it.canExecute() }

    val bytes = when {
      chunk.isString -> Unpooled.copiedBuffer(chunk.asString(), Charset.forName(encoding, Charsets.UTF_8))
      chunk.isProxyObject -> {
        val buffer = runCatching { chunk.asProxyObject<NodeBufferInstance>() }
          .getOrElse { throw TypeError.create("Unsupported chunk type: $chunk") }

        when (buffer) {
          is NodeGuestBuffer -> {
            val bytes = ByteArray(buffer.length)
            buffer.buffer.readBuffer(buffer.byteOffset.toLong(), bytes, 0, bytes.size)

            Unpooled.wrappedBuffer(bytes)
          }

          is NodeHostBuffer -> Unpooled.wrappedBuffer(buffer.byteBuffer)
        }
      }

      else -> throw TypeError.create("Unsupported chunk type: $chunk")
    }

    writer.write(bytes)
    if (end) writer.end()

    callback?.execute()
    call.send()
  }

  private companion object {
    private const val STATE_IDLE = 0
    private const val STATE_ENDED = 1
    private const val STATE_FINISHED = 2

    private const val MEMBER_WRITE: String = "write"
    private const val MEMBER_END: String = "end"
    private const val MEMBER_ADD_TRAILERS: String = "addTrailers"
    private const val MEMBER_WRITE_HEAD: String = "writeHead"
    private const val MEMBER_CORK: String = "cork"
    private const val MEMBER_UNCORK: String = "uncork"

    private const val MEMBER_REQ: String = "req"

    private const val MEMBER_FLUSH_HEADERS: String = "flush"

    private const val MEMBER_STATUS_CODE: String = "statusCode"
    private const val MEMBER_STATUS_MESSAGE: String = "statusMessage"

    private const val MEMBER_STRICT_CONTENT_LENGTH: String = "strictContentLength"
    private const val MEMBER_SEND_DATE: String = "sendDate"

    private const val MEMBER_HEADERS_SENT: String = "headersSent"
    private const val MEMBER_WRITABLE_ENDED: String = "writableEnded"
    private const val MEMBER_WRITABLE_FINISHED: String = "writableFinished"

    private const val MEMBER_WRITE_CONTINUE: String = "writeContinue"
    private const val MEMBER_WRITE_EARLY_HINTS: String = "writeEarlyHints"
    private const val MEMBER_WRITE_PROCESSING: String = "writeProcessing"

    private const val MEMBER_GET_HEADER: String = "getHeader"
    private const val MEMBER_SET_HEADER: String = "setHeader"
    private const val MEMBER_HAS_HEADER: String = "hasHeader"
    private const val MEMBER_REMOVE_HEADER: String = "removeHeader"

    private const val MEMBER_GET_HEADERS: String = "getHeaders"
    private const val MEMBER_SET_HEADERS: String = "setHeaders"

    private const val MEMBER_GET_HEADER_NAMES: String = "getHeaderNames"

    private const val MEMBER_SET_TIMEOUT: String = "setTimeout"
    private const val MEMBER_SOCKET: String = "socket"

    private const val EVENT_CLOSE: String = "close"
    private const val EVENT_FINISH: String = "finish"
    private const val EVENT_DRAIN: String = "drain"

    private val MemberKeys = arrayOf(
      MEMBER_WRITE,
      MEMBER_END,
      MEMBER_ADD_TRAILERS,
      MEMBER_WRITE_HEAD,
      MEMBER_CORK,
      MEMBER_UNCORK,
      MEMBER_REQ,
      MEMBER_FLUSH_HEADERS,
      MEMBER_STATUS_CODE,
      MEMBER_STATUS_MESSAGE,
      MEMBER_STRICT_CONTENT_LENGTH,
      MEMBER_SEND_DATE,
      MEMBER_HEADERS_SENT,
      MEMBER_WRITABLE_ENDED,
      MEMBER_WRITABLE_FINISHED,
      MEMBER_WRITE_CONTINUE,
      MEMBER_WRITE_EARLY_HINTS,
      MEMBER_WRITE_PROCESSING,
      MEMBER_GET_HEADER,
      MEMBER_SET_HEADER,
      MEMBER_HAS_HEADER,
      MEMBER_REMOVE_HEADER,
      MEMBER_GET_HEADERS,
      MEMBER_SET_HEADERS,
      MEMBER_GET_HEADER_NAMES,
      MEMBER_SET_TIMEOUT,
      MEMBER_SOCKET,
    )

    private val EventNames = listOf(EVENT_CLOSE, EVENT_FINISH, EVENT_DRAIN)

    private fun Value?.requireHeaderName(): String {
      return this?.takeIf { it.isString }?.asString() ?: throw TypeError.create("Header name must be a string: $this")
    }
  }
}
