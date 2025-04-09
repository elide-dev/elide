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
package elide.runtime.intrinsics.server.http

import elide.http.Request
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchRequestIntrinsic
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.js.FetchRequest
import elide.runtime.intrinsics.js.ReadableStream
import elide.vm.annotations.Polyglot

private const val HTTP_REQUEST_PROP_URI = "uri"
private const val HTTP_REQUEST_PROP_URL = "url"
private const val HTTP_REQUEST_PROP_METHOD = "method"
private const val HTTP_REQUEST_PROP_VERSION = "version"
private const val HTTP_REQUEST_PROP_BODY = "body"

// Methods and properties on an HTTP request.
private val httpRequestProps = arrayOf(
  HTTP_REQUEST_PROP_URI,
  HTTP_REQUEST_PROP_URL,
  HTTP_REQUEST_PROP_METHOD,
  HTTP_REQUEST_PROP_VERSION,
  HTTP_REQUEST_PROP_BODY,
)

/** Represents an incoming HTTP request received by the server, accessible by guest code. */
@DelicateElideApi public interface HttpRequest : FetchRequest, ReadOnlyProxyObject {
  /** The URI (path) for this request. */
  @get:Polyglot public val uri: String

  /** The full URL for this request. */
  @get:Polyglot override val url: String

  /** The HTTP method for this request */
  @get:Polyglot override val method: String

  /** The HTTP version for this request */
  @get:Polyglot public val version: String

  /** The HTTP body for this request, or `null` */
  @get:Polyglot override val body: ReadableStream?

  override fun getMemberKeys(): Array<String> = httpRequestProps

  override fun getMember(key: String?): Any? = when (key) {
    HTTP_REQUEST_PROP_URI -> uri
    HTTP_REQUEST_PROP_URL -> url
    HTTP_REQUEST_PROP_METHOD -> method
    HTTP_REQUEST_PROP_VERSION -> version
    HTTP_REQUEST_PROP_BODY -> body
    else -> null
  }

  /** Factories for obtaining an [HttpRequest]. */
  public companion object {
    /** @return Wrapped Elide universal HTTP request from [inner]. */
    @JvmStatic public fun of(inner: Request): HttpRequest = FetchRequestIntrinsic.forRequest(inner)
  }
}
