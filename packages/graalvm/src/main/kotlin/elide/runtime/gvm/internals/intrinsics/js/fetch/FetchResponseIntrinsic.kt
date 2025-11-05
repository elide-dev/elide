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

import org.graalvm.polyglot.Value
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.intrinsics.js.FetchHeaders
import elide.runtime.intrinsics.js.FetchMutableResponse
import elide.runtime.intrinsics.js.FetchResponse.Defaults
import elide.runtime.intrinsics.js.ReadableStream
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

  constructor(bodyContent: String?): this(
    responseUrl = Defaults.DEFAULT_URL,
    responseStatus = Defaults.DEFAULT_STATUS,
    responseHeaders = FetchHeadersIntrinsic.empty(),
     body = bodyContent?.let { ReadableStream.wrap(it.toByteArray(StandardCharsets.UTF_8)) },
  )

  constructor(bodyContent: String?, options: FetchResponseOptions): this(
    responseUrl = options.url ?: Defaults.DEFAULT_URL,
    responseStatus = options.status ?: Defaults.DEFAULT_STATUS,
    responseText = options.statusText,
    responseHeaders = options.headers ?: FetchHeadersIntrinsic.empty(),
    body = bodyContent?.let { ReadableStream.wrap(it.toByteArray(StandardCharsets.UTF_8)) },
  )

  data class FetchResponseOptions(
    var url: String? = null,
    var status: Int? = null,
    var statusText: String? = null,
    var headers: FetchHeaders? = null,
  ) {
    companion object {
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

  override fun toString(): String {
    return when {
      statusText.isNotBlank() -> "$status $statusText"
      else -> status.toString()
    }
  }
}
