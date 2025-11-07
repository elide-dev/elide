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

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpHeaderNames
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.PinnedContext
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.ContentStreamConsumer
import elide.runtime.http.server.HttpCall
import elide.runtime.http.server.ReadableContentStream
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.node.buffer.NodeHostBuffer
import elide.runtime.node.events.EventAware
import elide.runtime.node.events.EventAwareProxy
import elide.runtime.node.events.StandardEventName.NEW_LISTENER

public class NodeHttpServerRequest(
  private val call: HttpCall<CallContext.Empty>,
  private val executor: ContextAwareExecutor,
  private val events: EventAwareProxy = EventAwareProxy.create(),
  private val origin: PinnedContext = PinnedContext.current(),
) : EventAware by events, ReadOnlyProxyObject, ContentStreamConsumer {
  init {
    addListener(NEW_LISTENER) {
      // if a data listener attaches before we have a reader, set the pull flag
      // and start consuming the request; duplicate calls are guarded by the CAS)
      if (it.first() as String != EVENT_DATA) return@addListener
      if (flowing.compareAndSet(false, true)) call.requestBody.consume(this)
    }
  }

  private val headersMerged by lazy {
    // compute once; merge duplicate headers according to the spec
    val values = buildMap<String, Any> {
      for (header in call.request.headers().iteratorCharSequence()) when {
        UniqueHeaders.binarySearch(header.key) >= 0 -> putIfAbsent(header.key.toString(), header.value)
        header.key == HttpHeaderNames.SET_COOKIE -> compute(header.key.toString()) { _, value ->
          @Suppress("UNCHECKED_CAST")
          (value as? MutableList<CharSequence>)?.also { it.add(header.value) } ?: mutableListOf(header.value)
        }

        header.key == HttpHeaderNames.COOKIE -> compute(header.key.toString()) { _, value ->
          value?.let { "$it; ${header.value}" } ?: header.value
        }

        else -> compute(header.key.toString()) { _, value -> value?.let { "$it, ${header.value}" } ?: header.value }
      }
    }

    ProxyObject.fromMap(values)
  }

  private val headersDistinct by lazy {
    val values = buildMap {
      for (header in call.request.headers().iteratorAsString()) {
        compute(header.key.toString()) { _, value ->
          @Suppress("UNCHECKED_CAST")
          (value as? MutableList<CharSequence>)?.also { it.add(header.value) } ?: mutableListOf(header.value)
        }
      }
    }

    ProxyObject.fromMap(values)
  }

  private val headersRaw by lazy {
    buildList {
      for (header in call.request.headers().iteratorAsString()) {
        add(header.key)
        add(header.value)
      }
    }
  }

  private val trailersMerged by lazy {
    ProxyObject.fromMap(mutableMapOf())
  }

  private val complete = AtomicBoolean(false)
  private val sourceReader = AtomicReference<ReadableContentStream.Reader?>(null)
  private val flowing = AtomicBoolean(false)

  override fun getMemberKeys(): Array<String> = MemberKeys + events.memberKeys
  override fun eventNames(): List<String> = EventNames

  override fun getMember(key: String): Any? = when (key) {
    MEMBER_URL -> call.request.uri()
    MEMBER_METHOD -> call.request.method().name()
    MEMBER_HTTP_VERSION -> call.request.protocolVersion().text()

    MEMBER_HEADERS -> headersMerged
    MEMBER_HEADERS_DISTINCT -> headersDistinct
    MEMBER_RAW_HEADERS -> headersRaw

    MEMBER_TRAILERS -> trailersMerged
    MEMBER_TRAILERS_DISTINCT, MEMBER_RAW_TRAILERS -> emptyList<Any>()

    MEMBER_SET_TIMEOUT -> ProxyExecutable { /* noop */ }
    MEMBER_SOCKET -> throw UnsupportedOperationException("Low level socket access it not yet implemented")

    MEMBER_COMPLETE -> complete.get()
    MEMBER_DESTROY -> ProxyExecutable { call.fail(it.getOrNull(0)?.let { e -> IllegalStateException(e.toString()) }) }

    else -> events.getMember(key)
  }

  override fun onAttached(reader: ReadableContentStream.Reader) {
    sourceReader.set(reader)
    reader.pull()
  }

  override fun onRead(content: ByteBuf) {
    // read here, the buffer will be released after this method returns,
    // so we need to copy the data before that happens to use it later
    val bytes = ByteArray(content.readableBytes())
    content.readBytes(bytes)

    executor.execute(origin) {
      emit(EVENT_DATA, NodeHostBuffer.wrap(bytes))
      sourceReader.get()?.pull()
    }
  }

  override fun onClose(failure: Throwable?) {
    sourceReader.set(null)
    if (failure == null) complete.set(true)

    executor.execute(origin) {
      if (failure == null) emit(EVENT_END)
      else emit(EVENT_ERROR, failure)
      emit(EVENT_CLOSE)
    }
  }

  public companion object {
    private const val MEMBER_COMPLETE: String = "complete"
    private const val MEMBER_DESTROY: String = "destroy"
    private const val MEMBER_URL: String = "url"
    private const val MEMBER_HTTP_VERSION: String = "httpVersion"
    private const val MEMBER_METHOD: String = "method"
    private const val MEMBER_HEADERS: String = "headers"
    private const val MEMBER_HEADERS_DISTINCT: String = "headersDistinct"
    private const val MEMBER_RAW_HEADERS: String = "rawHeaders"
    private const val MEMBER_TRAILERS: String = "trailers"
    private const val MEMBER_TRAILERS_DISTINCT: String = "trailersDistinct"
    private const val MEMBER_RAW_TRAILERS: String = "rawTrailers"
    private const val MEMBER_SET_TIMEOUT: String = "setTimeout"
    private const val MEMBER_SOCKET: String = "socket"

    private val MemberKeys = arrayOf(
      MEMBER_COMPLETE,
      MEMBER_DESTROY,
      MEMBER_URL,
      MEMBER_HTTP_VERSION,
      MEMBER_METHOD,
      MEMBER_HEADERS,
      MEMBER_HEADERS_DISTINCT,
      MEMBER_RAW_HEADERS,
      MEMBER_TRAILERS,
      MEMBER_TRAILERS_DISTINCT,
      MEMBER_RAW_TRAILERS,
      MEMBER_SET_TIMEOUT,
      MEMBER_SOCKET,
    )

    public const val EVENT_DATA: String = "data"
    public const val EVENT_CLOSE: String = "close"
    public const val EVENT_ERROR: String = "error"
    public const val EVENT_END: String = "end"
    public const val EVENT_PAUSE: String = "pause"
    public const val EVENT_RESUME: String = "resume"
    public const val EVENT_READABLE: String = "readable"

    private val EventNames = listOf(
      EVENT_CLOSE,
      EVENT_DATA,
      EVENT_END,
      EVENT_ERROR,
      EVENT_PAUSE,
      EVENT_RESUME,
      EVENT_READABLE,
    )

    private val UniqueHeaders = arrayOf(
      HttpHeaderNames.AGE,
      HttpHeaderNames.AUTHORIZATION,
      HttpHeaderNames.CONTENT_LENGTH,
      HttpHeaderNames.CONTENT_TYPE,
      HttpHeaderNames.ETAG,
      HttpHeaderNames.EXPIRES,
      HttpHeaderNames.FROM,
      HttpHeaderNames.HOST,
      HttpHeaderNames.IF_MODIFIED_SINCE,
      HttpHeaderNames.IF_UNMODIFIED_SINCE,
      HttpHeaderNames.LAST_MODIFIED,
      HttpHeaderNames.LOCATION,
      HttpHeaderNames.MAX_FORWARDS,
      HttpHeaderNames.PROXY_AUTHORIZATION,
      HttpHeaderNames.REFERER,
      HttpHeaderNames.RETRY_AFTER,
      HttpHeaderNames.SERVER,
      HttpHeaderNames.USER_AGENT,
    ).sortedArray()
  }
}
