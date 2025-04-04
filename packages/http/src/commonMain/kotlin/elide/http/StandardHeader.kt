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

import elide.core.api.Symbolic

/**
 * ## Standard HTTP Header
 *
 * Provides an enumeration of standard HTTP headers; each header provides its name, use disposition/constraints, and an
 * ability to resolve itself to a string and back again.
 *
 * Standard HTTP headers are typically defined in the HTTP spec, but may also technically be non-standard (through broad
 * or uniform use).
 */
public expect enum class StandardHeader : HeaderName.PlatformHeaderName, Symbolic<String> {
  Accept,
  AcceptCharset,
  AcceptEncoding,
  AcceptLanguage,
  AcceptRanges,
  Age,
  Allow,
  Authorization,
  CacheControl,
  Connection,
  ContentDisposition,
  ContentEncoding,
  ContentLanguage,
  ContentLength,
  ContentRange,
  ContentType,
  Cookie,
  Date,
  ETag,
  Expect,
  Expires,
  From,
  Host,
  IfMatch,
  IfModifiedSince,
  IfNoneMatch,
  IfRange,
  IfUnmodifiedSince,
  LastModified,
  Link,
  Location;

  // Format this header as a string.
  override fun asString(): String

  // Symbol used for this header (normalized).
  override val symbol: String

  // Normalized name (just the symbol).
  override val nameNormalized: HttpHeaderName

  // Whether this header is allowed on requests; defaults to `true`.
  override val allowedOnRequests: Boolean

  // Whether this header is allowed on responses; defaults to `true`.
  override val allowedOnResponses: Boolean
}
