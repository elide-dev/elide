/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
  responseText: String,
  responseHeaders: FetchHeaders,
  body: ReadableStream? = null,
  responseRedirected: Boolean = false,
) : FetchMutableResponse {
  /** Typed constructor methods for `Response` objects. */
  internal object ResponseConstructors {
    /** @return Empty response. */
    @JvmStatic @Polyglot internal fun empty(): FetchResponseIntrinsic = FetchResponseIntrinsic(
      responseUrl = Defaults.DEFAULT_URL,
      responseStatus = Defaults.DEFAULT_STATUS,
      responseText = Defaults.DEFAULT_STATUS_TEXT,
      responseHeaders = FetchHeadersIntrinsic.empty(),
    )

    /** @return Manual response. */
    @JvmStatic internal fun create(
      url: String,
      status: Int,
      statusText: String,
      headers: FetchHeaders,
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

  /** @inheritDoc */
  override var headers: FetchHeaders
    get() = responseHeaders.get()
    set(value) = responseHeaders.set(value)

  /** @inheritDoc */
  override var status: Int
    get() = responseStatus.get()
    set(value) = responseStatus.set(value)

  /** @inheritDoc */
  override var statusText: String
    get() = responseText.get()
    set(value) = responseText.set(value)

  /** @inheritDoc */
  override var url: String
    get() = responseUrl.get()
    set(value) = responseUrl.set(value)

  /** @inheritDoc */
  override val body: ReadableStream
    get() = responseBody.get()

  /** @inheritDoc */
  override val bodyUsed: Boolean
    get() = bodyConsumed.get()

  /** @inheritDoc */
  override val redirected: Boolean
    get() = responseRedirected.get()
}
