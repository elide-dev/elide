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
package elide.runtime.gvm.internals.js

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import org.graalvm.polyglot.Context
import elide.runtime.gvm.RequestExecutionInputs
import elide.runtime.gvm.internals.intrinsics.js.JsPromiseImpl
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.*
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.runtime.node.buffer.NodeBlob
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic.URLValue as URL
import elide.vm.annotations.Polyglot

/**
 * Defines an abstract base class for JavaScript inputs based on an HTTP [Request] type, which has been made to be
 * compatible with [FetchRequest].
 *
 * @see JsMicronautRequestExecutionInputs for an implementation of this input shape based on Micronaut types.
 * @param state State ("props") provided for a single execution run against these inputs.
 */
internal abstract class JsServerRequestExecutionInputs<Request: Any> (
  private val state: Any? = null,
) : RequestExecutionInputs<Request>, FetchRequest {
  /** Internal indicator of whether the request body stream has been consumed. */
  protected val consumed: AtomicBoolean = AtomicBoolean(false)

  /**
   * ## Request: Body.
   *
   * Specifies, if any, a [ReadableStream] which holds the body of a [FetchRequest]. Body data may only be provided when
   * the [method] for the request allows a body, such as `POST`, `PUT`, and so forth.
   *
   * From MDN:
   * "The read-only body property of the Request interface contains a ReadableStream with the body contents that have
   * been added to the request. Note that a request using the GET or HEAD method cannot have a body and null is returned
   * in these cases."
   *
   * See also: [MDN, Request.body](https://developer.mozilla.org/en-US/docs/Web/API/Request/body).
   */
  override val body: ReadableStream? get() = if (!hasBody()) {
    null  // no data available for body
  } else {
    ReadableStream.wrap(requestBody())
  }

  /**
   * ## Request: Body usage.
   *
   * If the [body] for this request has already been consumed, this property must return `true`; if `false`, the [body]
   * is still buffered and may be queried.
   *
   * From MDN:
   * "The read-only bodyUsed property of the Request interface is a boolean value that indicates whether the request
   * body has been read yet."
   *
   * See also: [MDN, Request.bodyUsed](https://developer.mozilla.org/en-US/docs/Web/API/Request/bodyUsed).
   */
  override val bodyUsed: Boolean get() = consumed.get()

  /**
   * ## Request: Destination.
   *
   * Describes the destination or use profile for the fetched data in a given fetch request/response cycle.
   *
   * From MDN:
   * "The destination read-only property of the Request interface returns a string describing the type of content being
   * requested. The string must be one of the [following values:] `audio`, `audioworklet`, `document`, `embed`, `font`,
   * `frame`, `iframe`, `image`, `manifest`, `object`, `paintworklet`, `report`, `script`, `sharedworker`, `style`,
   * `track`, `video`, `worker` or `xslt` strings, or the empty string, which is the default value."
   *
   * See also: [MDN, Request.destination](https://developer.mozilla.org/en-US/docs/Web/API/Request/destination).
   */
  override val destination: String get() = "worker"

  /**
   * ## Request: Headers.
   *
   * Provides a typed view of fetch headers on top of this request, via [FetchHeaders], which behaves as a specialized
   * JavaScript-compatible multi-map. Multiple header values can be set per header key, if desired, which can be safely
   * combined into a comma-separated header value group upon render.
   *
   * From MDN:
   * "The headers read-only property of the Request interface contains the Headers object associated with the request."
   *
   * See also: [MDN, Request.headers](https://developer.mozilla.org/en-US/docs/Web/API/Request/headers).
   */
  override val headers: FetchHeaders get() = FetchHeaders.fromMultiMap(
    requestHeaders()
  )

  /**
   * ## Request: URL.
   *
   * Provides the string version of the URL requested as part of this [FetchRequest]; the URL is the full absolute URL
   * path, as best known to the server (when expressed as an input), or the full absolute URL path as requested by the
   * developer (when expressed as an intermediate during a fetch call).
   *
   * From MDN:
   * "The url read-only property of the Request interface contains the URL of the request."
   *
   * See also: [MDN, Request.url](https://developer.mozilla.org/en-US/docs/Web/API/Request/url).
   */
  override val url: String get() = URL.fromURL(getURL()).toString()

  /**
   * JavaScript server execution inputs: URL.
   *
   * Provide, in parsed [URI] form, the request URL as best known at this time, or which backs the [FetchRequest]
   * instance which is under processing.
   *
   * @return Parsed URL instance for this request.
   */
  protected abstract fun getURL(): URI

  /**
   * JavaScript server execution inputs: State.
   *
   * Provide, in literal form, the state value ("props") yielded by the active serving controller, as applicable. Apps
   * may use this mechanism to share a value with a guest execution. For SSR-enabled controllers, which is automatically
   * set to the output of the `state()` method.
   *
   * @return Active state for this request/response cycle, if any, otherwise, `null`.
   */
  protected open fun getState(): Any? = state

  /**
   * JavaScript server execution inputs: Body status.
   *
   * Indicate whether this request carries a suite of body data with it; if so, the body data should be available via
   * the [body] property, which provides a [ReadableStream] of the underlying data. The [ReadableStream] is wrapped by
   * the return value of [requestBody].
   *
   * @return Whether this request has a body.
   */
  protected abstract fun hasBody(): Boolean

  /**
   * JavaScript server execution inputs: Body stream.
   *
   * If available, and applicable within the current context, provide the underlying body data for this request, wrapped
   * for consumption via an [InputStream] or some child thereof.
   *
   * @return Stream of body data associated with this request.
   * @throws IllegalStateException if this request has no present body data, as reported by [hasBody].
   */
  protected abstract fun requestBody(): InputStream

  /**
   * JavaScript server execution inputs: HTTP headers.
   *
   * Produce a map of headers, and their (potentially multiple) values, which are associated with the current fetch
   * request. This method may be called in order to help produce headers objects of different types.
   *
   * @return Map of HTTP request headers to their (potentially multiple) values.
   */
  protected abstract fun requestHeaders(): Map<String, List<String>>

  // -- Interface: Body Mixin -- //

  /** Read the body as an ArrayBuffer. */
  @Polyglot override fun arrayBuffer(): JsPromise<Any> {
    val promise = JsPromiseImpl<Any>()
    val stream = body
    if (stream == null) {
      promise.resolve(ByteBuffer.allocate(0))
      return promise
    }

    val reader = stream.getReader() as ReadableStreamDefaultReader
    val chunks = ByteArrayOutputStream()

    fun readNextChunk() {
      reader.read().then(
        onFulfilled = { result ->
          if (result.value != null) {
            val value = result.value
            if (value.hasArrayElements()) {
              val size = value.arraySize.toInt()
              for (i in 0 until size) {
                chunks.write(value.getArrayElement(i.toLong()).asByte().toInt())
              }
            }
          }
          if (result.done) {
            reader.releaseLock()
            promise.resolve(ByteBuffer.wrap(chunks.toByteArray()))
          } else {
            readNextChunk()
          }
        },
        onCatch = { error ->
          reader.releaseLock()
          promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
        }
      )
    }

    readNextChunk()
    return promise
  }

  /** Read the body as a Blob. */
  @Polyglot override fun blob(): JsPromise<Blob> {
    val promise = JsPromiseImpl<Blob>()
    val stream = body
    if (stream == null) {
      promise.resolve(NodeBlob(ByteArray(0), null))
      return promise
    }

    arrayBuffer().then(
      onFulfilled = { buffer ->
        val byteBuffer = buffer as ByteBuffer
        val byteArray = ByteArray(byteBuffer.remaining())
        byteBuffer.get(byteArray)
        val contentType = headers.get("content-type")
        promise.resolve(NodeBlob(byteArray, contentType))
      },
      onCatch = { error ->
        promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
      }
    )
    return promise
  }

  /** Read the body as FormData. */
  @Polyglot override fun formData(): JsPromise<Any> {
    val promise = JsPromiseImpl<Any>()
    val stream = body
    if (stream == null) {
      promise.resolve(FormData())
      return promise
    }

    val contentType = headers.get("content-type") ?: ""

    text().then(
      onFulfilled = { bodyText ->
        try {
          when {
            contentType.contains("application/x-www-form-urlencoded") -> {
              promise.resolve(FormData.parseUrlEncoded(bodyText))
            }
            contentType.contains("multipart/form-data") -> {
              promise.reject(NotImplementedError("multipart/form-data parsing not yet implemented"))
            }
            else -> {
              promise.resolve(FormData.parseUrlEncoded(bodyText))
            }
          }
        } catch (e: Exception) {
          promise.reject(JsError.typeError("Failed to parse form data: ${e.message}"))
        }
      },
      onCatch = { error ->
        promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
      }
    )
    return promise
  }

  /** Read the body as JSON. */
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

  /** Read the body as text. */
  @Polyglot override fun text(): JsPromise<String> {
    val promise = JsPromiseImpl<String>()
    val stream = body
    if (stream == null) {
      promise.resolve("")
      return promise
    }

    val reader = stream.getReader() as ReadableStreamDefaultReader
    val chunks = ByteArrayOutputStream()

    fun readNextChunk() {
      reader.read().then(
        onFulfilled = { result ->
          if (result.value != null) {
            val value = result.value
            if (value.hasArrayElements()) {
              val size = value.arraySize.toInt()
              for (i in 0 until size) {
                chunks.write(value.getArrayElement(i.toLong()).asByte().toInt())
              }
            }
          }
          if (result.done) {
            val text = chunks.toString(StandardCharsets.UTF_8)
            reader.releaseLock()
            promise.resolve(text)
          } else {
            readNextChunk()
          }
        },
        onCatch = { error ->
          reader.releaseLock()
          promise.reject(error as? Throwable ?: RuntimeException(error.toString()))
        }
      )
    }

    readNextChunk()
    return promise
  }
}
