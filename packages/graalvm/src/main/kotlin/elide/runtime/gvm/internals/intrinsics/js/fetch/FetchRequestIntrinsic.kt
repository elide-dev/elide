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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.intrinsics.js.fetch

import io.micronaut.http.HttpRequest
import io.netty.buffer.ByteBufInputStream
import org.graalvm.polyglot.Value
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.http.Body
import elide.http.Request
import elide.http.body.NettyBody
import elide.http.body.PrimitiveBody
import elide.http.request.JavaNetHttpUri
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.js.*
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.runtime.gvm.internals.intrinsics.js.JsPromiseImpl
import elide.runtime.node.buffer.NodeBlob
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.vm.annotations.Polyglot

/** Implements an intrinsic for the Fetch API `Request` object. */
internal class FetchRequestIntrinsic internal constructor(
  targetUrl: URLIntrinsic.URLValue,
  targetMethod: String = FetchRequest.Defaults.DEFAULT_METHOD,
  requestHeaders: FetchHeaders = FetchHeadersIntrinsic.empty(),
  private val bodyData: ReadableStream? = null,
) : FetchMutableRequest, ReadOnlyProxyObject {
  /**
   * Implements options for the fetch Request constructor.
   */
  @JvmRecord data class FetchRequestOptions(
    val method: String = FetchRequest.Defaults.DEFAULT_METHOD,
    val headers: FetchHeaders? = null,
    val body: Value? = null,
  ) {
    companion object {
      @JvmStatic fun from(value: Value): FetchRequestOptions = when {
        value.hasMembers() -> FetchRequestOptions(
          method = value.getMember("method").asString(),
          body = if (value.hasMember("body")) value.getMember("body") else null,
          headers = when {
            value.hasMember("headers") -> {
              val headersValue = value.getMember("headers")
              if (headersValue.isNull) null else FetchHeaders.from(headersValue)
            }

            else -> null
          },
        )

        else -> throw JsError.typeError("Invalid options for Request")
      }
    }
  }

  internal companion object Factory : FetchMutableRequest.RequestFactory<FetchMutableRequest> {
    private const val MEMBER_BODY = "body"
    private const val MEMBER_BODY_USED = "bodyUsed"
    private const val MEMBER_CACHE = "cache"
    private const val MEMBER_CREDENTIALS = "credentials"
    private const val MEMBER_DESTINATION = "destination"
    private const val MEMBER_HEADERS = "headers"
    private const val MEMBER_INTEGRITY = "integrity"
    private const val MEMBER_METHOD = "method"
    private const val MEMBER_MODE = "mode"
    private const val MEMBER_PRIORITY = "priority"
    private const val MEMBER_REDIRECT = "redirect"
    private const val MEMBER_REFERRER = "referrer"
    private const val MEMBER_REFERRER_POLICY = "referrerPolicy"
    private const val MEMBER_MEMBER_URL = "url"

    private val MemberKeys = arrayOf(
      MEMBER_BODY,
      MEMBER_BODY_USED,
      MEMBER_CACHE,
      MEMBER_CREDENTIALS,
      MEMBER_DESTINATION,
      MEMBER_HEADERS,
      MEMBER_INTEGRITY,
      MEMBER_METHOD,
      MEMBER_MODE,
      MEMBER_PRIORITY,
      MEMBER_REDIRECT,
      MEMBER_REFERRER,
      MEMBER_REFERRER_POLICY,
      MEMBER_MEMBER_URL,
      "json",
      "text",
      "blob",
      "arrayBuffer",
      "formData",
    )

    @JvmStatic override fun forRequest(request: HttpRequest<*>): FetchMutableRequest {
      return FetchRequestIntrinsic(
        targetUrl = URLIntrinsic.URLValue.fromURL(request.uri),
        targetMethod = request.method.name,
        requestHeaders = FetchHeaders.fromPairs(
          request.headers.asMap().entries.flatMap {
            it.value.map { value ->
              it.key to value
            }
          },
        ),
        bodyData = request.getBody(InputStream::class.java).map {
          System.err.println("FetchRequestIntrinsic: buffering body from HttpRequest")
          try {
              val bytes = it.readAllBytes()
              System.err.println("FetchRequestIntrinsic: buffered ${bytes.size} bytes from HttpRequest")
              ReadableStream.wrap(bytes)
          } catch (e: Exception) {
              System.err.println("FetchRequestIntrinsic: failed to buffer body: $e")
              e.printStackTrace()
              null
          }
        }.orElse(null),
      )
    }

    @JvmStatic override fun forRequest(request: Request): FetchRequestIntrinsic {
      return FetchRequestIntrinsic(
        targetUrl = when (val url = request.url) {
          is JavaNetHttpUri -> URLIntrinsic.URLValue.fromString(url.absoluteString())
          else -> error("Unsupported URL value: ${request.url}")
        },
        targetMethod = request.method.symbol,

        requestHeaders = FetchHeaders.fromPairs(
          request.headers.asOrdered().flatMap { header ->
            header.value.values.map { value ->
              header.name.name to value
            }
          }.toList(),
        ),

        bodyData = when (val body = request.body) {
          is Body.Empty -> {
            println("FETC_REQ: Body is Empty")
            null
          }
          is NettyBody -> {
             println("FETC_REQ: Body is NettyBody")
             val buf = body.unwrap()
             val arr = ByteArray(buf.readableBytes())
             println("FETC_REQ: Read ${arr.size} bytes from NettyBody")
             buf.getBytes(buf.readerIndex(), arr)
             java.io.ByteArrayInputStream(arr)
          }
          else -> {
            println("FETC_REQ: Unrecognized body type: ${request.body}")
            error("Unrecognized body type: ${request.body}")
          }
        }?.let { ReadableStream.wrap(it) },
      )
    }
  }

  /** Construct a new `Request` from a plain string URL. */
  @Polyglot constructor (url: String) : this(targetUrl = URLIntrinsic.URLValue.fromString(url))

  /** Construct a new `Request` from a Fetch API spec `URL` object. */
  @Polyglot constructor (url: URLIntrinsic.URLValue) : this(targetUrl = url)

  /** Construct a new `Request` from a Java Fetch API spec `URL` object. */
  constructor (url: URL) : this(targetUrl = URLIntrinsic.URLValue.fromString(url.toString()))

  /** Construct a new `Request` from another request (i.e. make a mutable or non-mutable copy). */
  @Polyglot constructor (request: FetchRequest) : this(
    targetUrl = URLIntrinsic.URLValue.fromString(request.url),
    targetMethod = request.method,
    requestHeaders = request.headers,
  )

  // Atomic indicator of whether the request body has been consumed.
  private val bodyConsumed: AtomicBoolean = AtomicBoolean(false)

  // Target URL to be fetched.
  private val targetUrl: AtomicReference<URLIntrinsic.URLValue> = AtomicReference(targetUrl)

  // Target HTTP method to invoke.
  private val targetMethod: AtomicReference<String> = AtomicReference(targetMethod)

  // Request headers to enclose.
  private val requestHeaders: AtomicReference<FetchHeaders> = AtomicReference(requestHeaders)

  // -- Interface: Mutable HTTP Request -- //

  @get:Polyglot @set:Polyglot override var headers: FetchHeaders
    get() = requestHeaders.get()
    set(subject) = requestHeaders.set(subject)

  @get:Polyglot @set:Polyglot override var url: String
    get() = targetUrl.get().toString()
    set(newTarget) = targetUrl.set(URLIntrinsic.URLValue.fromString(newTarget))

  @get:Polyglot @set:Polyglot override var method: String
    get() = targetMethod.get()
    set(value) = targetMethod.set(value)

  // -- Interface: Immutable HTTP Request -- //

  @get:Polyglot override val bodyUsed: Boolean get() = bodyConsumed.get()
  @get:Polyglot override val destination: String get() = targetUrl.get().toString()

  @get:Polyglot override val body: ReadableStream?
    get() {
      if (bodyData == null) return null
      // Do not mark as consumed just by accessing the stream property,
      // wait until it is read
      return bodyData
    }

  @Polyglot override fun arrayBuffer(): JsPromise<Any> {
    val promise = JsPromiseImpl<Any>()
    val stream = body
    if (stream == null) {
      promise.resolve(ByteBuffer.allocate(0))
      return promise
    }
    if (bodyConsumed.getAndSet(true)) {
      promise.reject(JsError.typeError("Body already consumed"))
      return promise
    }

    // consume stream to buffer
    consumeStream(stream).then(
      onFulfilled = { bytes ->
        // Convert byte array to ByteBuffer/ArrayBuffer
        promise.resolve(ByteBuffer.wrap(bytes))
      },
      onCatch = { error ->
        promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
      }
    )
    return promise
  }

  @Polyglot override fun blob(): JsPromise<Blob> {
    val promise = JsPromiseImpl<Blob>()
    val stream = body
    if (stream == null) {
      promise.resolve(NodeBlob(ByteArray(0), null))
      return promise
    }
    if (bodyConsumed.getAndSet(true)) {
      promise.reject(JsError.typeError("Body already consumed"))
      return promise
    }

    consumeStream(stream).then(
      onFulfilled = { bytes ->
        promise.resolve(NodeBlob(bytes, null))
      },
      onCatch = { error ->
        promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
      }
    )
    return promise
  }

  @Polyglot override fun formData(): JsPromise<Any> {
    val promise = JsPromiseImpl<Any>()
    val stream = body
    if (stream == null) {
      promise.reject(JsError.typeError("Cannot read body: no body present"))
      return promise
    }
    
    // Check Content-Type header
    val contentType = headers.get("content-type") ?: ""
    val boundary = FormData.extractBoundary(contentType)

    // Using arrayBuffer() here would double-check consumed, so we do it manually or chain
    // Chaining is safer to reuse logic
    arrayBuffer().then(
      onFulfilled = { buffer ->
        try {
          // buffer is likely a ByteBuffer or byte[]
          val bytes = when (buffer) {
            is ByteBuffer -> {
              val arr = ByteArray(buffer.remaining())
              buffer.get(arr)
              arr
            }
            is ByteArray -> buffer
            else -> throw JsError.typeError("Unknown buffer type: ${buffer::class.java}")
          }

          val parsed = if (boundary != null) {
             FormData.parseMultipart(bytes, boundary)
          } else {
             // Fallback to URL encoded
             FormData.parseUrlEncoded(String(bytes, StandardCharsets.UTF_8))
          }
          promise.resolve(parsed)

        } catch (e: Exception) {
          promise.reject(JsError.typeError("Failed to parse FormData: ${e.message}"))
        }
      },
      onCatch = { error ->
        promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
      }
    )
    return promise
  }

  @Polyglot override fun json(): JsPromise<Any> {
    val promise = JsPromiseImpl<Any>()
    val stream = body
    if (stream == null) {
      promise.reject(JsError.typeError("Cannot read body: no body present"))
      return promise
    }

    // First read as text, then parse as JSON
    text().then(
      onFulfilled = { text ->
        try {
          // Use GraalJS JSON.parse to parse the text
          val context = Context.getCurrent()
          val jsonParse = context.eval("js", "JSON.parse")
          val parsed = jsonParse.execute(text)
          promise.resolve(parsed)
        } catch (e: Exception) {
          promise.reject(JsError.typeError("Invalid JSON: ${e.message}"))
        }
      },
      onCatch = { error ->
        promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
      }
    )
    return promise
  }

  @Polyglot override fun text(): JsPromise<String> {
    val promise = JsPromiseImpl<String>()
    val stream = body
    if (stream == null) {
      promise.resolve("")
      return promise
    }
    if (bodyConsumed.getAndSet(true)) {
      promise.reject(JsError.typeError("Body already consumed"))
      return promise
    }

    consumeStream(stream).then(
      onFulfilled = { bytes ->
        promise.resolve(String(bytes, StandardCharsets.UTF_8))
      },
      onCatch = { error ->
        promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
      }
    )
    return promise
  }

  private fun consumeStream(stream: ReadableStream): JsPromise<ByteArray> {
    val promise = JsPromiseImpl<ByteArray>()
    val chunks = ByteArrayOutputStream()
    val reader = stream.getReader() as ReadableStreamDefaultReader

    fun readNext() {
      reader.read().then(
        onFulfilled = { res ->
          val result = res as Value
          val done = result.getMember("done").asBoolean()
          val value = result.getMember("value")

          if (value != null && !value.isNull) {
             // value is likely a TypedArray or UInt8Array
             // We need to reliably convert it to bytes. 
             // Assuming Polyglot access gives us a way or it's a Buffer
             if (value.hasArrayElements()) {
                val size = value.arraySize.toInt()
                for (i in 0 until size) {
                   chunks.write(value.getArrayElement(i.toLong()).asInt())
                }
             }
          }

          if (done) {
            promise.resolve(chunks.toByteArray())
          } else {
            readNext()
          }
        },
        onCatch = { error ->
          promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
        }
      )
    }

    readNext()
    return promise
  }

  override fun toString(): String {
    return "$method $url"
  }

  override fun getMemberKeys(): Array<String> = MemberKeys

  override fun getMember(key: String?): Any? = when (key) {
    MEMBER_BODY -> body
    MEMBER_BODY_USED -> bodyUsed
    MEMBER_CACHE -> cache
    MEMBER_CREDENTIALS -> credentials
    MEMBER_DESTINATION -> destination
    MEMBER_HEADERS -> headers
    MEMBER_INTEGRITY -> integrity
    MEMBER_METHOD -> method
    MEMBER_MODE -> mode
    MEMBER_PRIORITY -> priority
    MEMBER_REDIRECT -> redirect
    MEMBER_REFERRER -> referrer
    MEMBER_REFERRER_POLICY -> referrerPolicy
    MEMBER_MEMBER_URL -> url
    // Add new members
    "arrayBuffer" -> ProxyExecutable { arrayBuffer() }
    "blob" -> ProxyExecutable { blob() }
    "formData" -> ProxyExecutable { formData() }
    "json" -> ProxyExecutable { json() }
    "text" -> ProxyExecutable { text() }
    else -> null
  }
}
