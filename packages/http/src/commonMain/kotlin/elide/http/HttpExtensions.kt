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

package elide.http

import elide.http.StandardHeader.ContentLength
import elide.http.StandardHeader.ContentType

/** @return Whether this version of HTTP supports trailers. */
public fun ProtocolVersion.supportsTrailers(): Boolean =
  major >= 2u

/** @return Whether this version of HTTP supports pipelining. */
public fun ProtocolVersion.supportsPipelining(): Boolean =
  major == 1u.toUShort() && minor == 1u.toUShort()

/** @return Whether this version of HTTP supports push. */
public fun ProtocolVersion.supportsPush(): Boolean =
  major >= 2u

/** @return Whether this version of HTTP supports chunked transfer encoding. */
public fun ProtocolVersion.supportsChunkedTransferEncoding(): Boolean = when (major) {
  1u.toUShort() -> minor >= 1u
  else -> true
}

/** Provide the `Content-Length` value for a given suite of headers, or `0` if none is present. */
public val Headers.contentLength: ContentLengthValue get() = this[ContentLength]?.asString()?.toULongOrNull() ?: 0uL

/** Provide the `Content-Type` value for a given suite of headers, or `null` if none is present. */
public val Headers.contentType: HeaderValue? get() = this[ContentType]

/** Provide the `Content-Length` value for a given request, or `0` if none is present. */
public val Message.contentLength: ContentLengthValue get() = headers.contentLength

/** Provide the `Content-Type` value for a request, or `null` if none is present. */
public val Message.contentType: HeaderValue? get() = headers.contentType
