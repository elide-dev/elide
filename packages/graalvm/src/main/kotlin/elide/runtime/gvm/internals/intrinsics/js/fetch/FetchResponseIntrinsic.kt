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
package elide.runtime.gvm.internals.intrinsics.js.fetch

import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.intrinsics.js.FetchHeaders
import elide.runtime.intrinsics.js.FetchMutableResponse
import elide.runtime.intrinsics.js.FetchResponse.Defaults
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.Blob
import elide.runtime.intrinsics.js.FormData
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.internals.intrinsics.js.JsPromiseImpl
import elide.runtime.node.buffer.NodeBlob
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.vm.annotations.Polyglot

/** Implements an intrinsic for the Fetch API `Response` object. */
internal class FetchResponseIntrinsic private constructor (
  responseUrl: String,
  responseStatus: Int,
  responseHeaders: FetchHeaders,
  responseText: String? = null,
  body: ReadableStream? = null,
  responseRedirected: Boolean = false,
) : FetchMutableResponse {
  @Polyglot constructor(): this(
    responseUrl = Defaults.DEFAULT_URL,
    responseStatus = Defaults.DEFAULT_STATUS,
    responseHeaders = FetchHeadersIntrinsic.empty(),
  )

  @Polyglot constructor(body: Any?): this(
    responseUrl = Defaults.DEFAULT_URL,
    responseStatus = Defaults.DEFAULT_STATUS,
    responseHeaders = FetchHeadersIntrinsic.empty(),
    body = FetchResponseOptions.resolveBody(body),
  )

  @Polyglot constructor(body: Any?, options: FetchResponseOptions): this(
    responseUrl = options.url ?: Defaults.DEFAULT_URL,
    responseStatus = options.status ?: Defaults.DEFAULT_STATUS,
    responseText = options.statusText,
    responseHeaders = options.headers ?: FetchHeadersIntrinsic.empty(),
    body = FetchResponseOptions.resolveBody(body),
  )

  @Polyglot constructor(body: ReadableStream): this(
    responseUrl = Defaults.DEFAULT_URL,
    responseStatus = Defaults.DEFAULT_STATUS,
    responseHeaders = FetchHeadersIntrinsic.empty(),
    body = body,
  )

  @Polyglot constructor(body: ReadableStream?, options: FetchResponseOptions): this(
    responseUrl = options.url ?: Defaults.DEFAULT_URL,
    responseStatus = options.status ?: Defaults.DEFAULT_STATUS,
    responseText = options.statusText,
    responseHeaders = options.headers ?: FetchHeadersIntrinsic.empty(),
    body = body,
  )

  data class FetchResponseOptions(
    var url: String? = null,
    var status: Int? = null,
    var statusText: String? = null,
    var headers: FetchHeaders? = null,
  ) {
    companion object {
      internal fun resolveBody(body: Any?): ReadableStream? = when (body) {
        null -> null
        is ReadableStream -> body
        is String -> ReadableStream.wrap(body.toByteArray(StandardCharsets.UTF_8))
        is Value -> when {
            body.isNull -> null
            body.isString -> ReadableStream.wrap(body.asString().toByteArray(StandardCharsets.UTF_8))
            else -> error("Unsupported body type in Value: $body")
        }
        else -> error("Unsupported body type: ${body::class.java}")
      }

      @JvmStatic fun from(value: Value): FetchResponseOptions = when {
        value.hasMembers() -> FetchResponseOptions(
          url = value.getMember("url")?.asString(),
          status = value.getMember("status")?.asInt(),
          statusText = value.getMember("statusText")?.asString(),
          headers = FetchHeadersIntrinsic.from(value.getMember("headers")),
        )
        value.hasHashEntries() -> FetchResponseOptions(
          url = value.getHashValue("url")?.asString(),
          status = value.getHashValue("status")?.asInt(),
          statusText = value.getHashValue("statusText")?.asString(),
          headers = FetchHeadersIntrinsic.from(value.getHashValue("headers")),
        )
        value.isNull -> FetchResponseOptions()
        else -> error("Unrecognized Fetch response constructor options: $value")
      }
    }
  }

  /** Typed constructor methods for `Response` objects. */
  internal object ResponseConstructors {
    /** @return Empty response. */
    @JvmStatic @Polyglot internal fun empty(): FetchResponseIntrinsic = FetchResponseIntrinsic(
      responseUrl = Defaults.DEFAULT_URL,
      responseStatus = Defaults.DEFAULT_STATUS,
      responseHeaders = FetchHeadersIntrinsic.empty(),
    )

    /** @return Manual response. */
    @JvmStatic internal fun create(
      url: String,
      status: Int,
      statusText: String? = null,
      headers: FetchHeaders = FetchHeadersIntrinsic.empty(),
      body: ReadableStream? = null,
      redirected: Boolean = false,
    ): FetchResponseIntrinsic = FetchResponseIntrinsic(
      responseUrl = url,
      responseStatus = status,
      responseText = statusText,
      responseHeaders = headers,
      body = body,
      responseRedirected = redirected,
    )
  }

  // Response (final) URL.
  private val responseUrl: AtomicReference<String> = AtomicReference(responseUrl)

  // Response status code.
  private val responseStatus: AtomicInteger = AtomicInteger(responseStatus)

  // Response status text.
  private val responseText: AtomicReference<String> = AtomicReference(responseText)

  // Response headers.
  private val responseHeaders: AtomicReference<FetchHeaders> = AtomicReference(responseHeaders)

  // Redirection status.
  private val responseRedirected: AtomicBoolean = AtomicBoolean(responseRedirected)

  // Response body.
  private val responseBody: AtomicReference<ReadableStream> = AtomicReference(body)

  // Response body consumption status.
  private val bodyConsumed: AtomicBoolean = AtomicBoolean(false)

  // Indicate whether a body is present.
  internal val bodyPresent: Boolean get() = responseBody.get() != null

  override var headers: FetchHeaders
    @Polyglot get() = responseHeaders.get()
    @Polyglot set(value) = responseHeaders.set(value)

  override var status: Int
    @Polyglot get() = responseStatus.get()
    @Polyglot set(value) = responseStatus.set(value)

  override var statusText: String
    @Polyglot get() = responseText.get() ?: ""
    @Polyglot set(value) = responseText.set(value)

  override var url: String
    @Polyglot get() = responseUrl.get()
    @Polyglot set(value) = responseUrl.set(value)

  @get:Polyglot override val body: ReadableStream? get() = responseBody.get()
  @get:Polyglot override val bodyUsed: Boolean get() = bodyConsumed.get()
  @get:Polyglot override val redirected: Boolean get() = responseRedirected.get()

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

    consumeStream(stream).then(
      onFulfilled = { bytes ->
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
    
    val contentType = headers.get("content-type") ?: ""
    val boundary = FormData.extractBoundary(contentType)

    arrayBuffer().then(
      onFulfilled = { buffer ->
        try {
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

     text().then(
         onFulfilled = { text ->
             try {
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
          val result = res as ReadableStream.ReadResult
          val done = result.done
          val value = result.value

          if (value != null) {
             if (value is ByteArray) {
                chunks.write(value)
             } else if (value is Value && value.hasArrayElements()) {
                val size = value.arraySize.toInt()
                for (i in 0 until size) {
                   chunks.write(value.getArrayElement(i.toLong()).asInt())
                }
             } else if (value is ByteBuffer) {
                val arr = ByteArray(value.remaining())
                value.get(arr)
                chunks.write(arr)
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
    return when {
      statusText.isNotBlank() -> "$status $statusText"
      else -> status.toString()
    }
  }
}
