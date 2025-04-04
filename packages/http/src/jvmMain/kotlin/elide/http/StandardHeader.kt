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
import elide.http.HeaderName.PlatformHeaderName

// Constant standard HTTP header names.
private const val HTTP_HEADER_ACCEPT = "accept"
private const val HTTP_HEADER_ACCEPT_CHARSET = "accept-charset"
private const val HTTP_HEADER_ACCEPT_ENCODING = "accept-encoding"
private const val HTTP_HEADER_ACCEPT_LANGUAGE = "accept-language"
private const val HTTP_HEADER_ACCEPT_RANGES = "accept-ranges"
private const val HTTP_HEADER_AGE = "age"
private const val HTTP_HEADER_ALLOW = "allow"
private const val HTTP_HEADER_AUTHORIZATION = "authorization"
private const val HTTP_HEADER_CACHE_CONTROL = "cache-control"
private const val HTTP_HEADER_CONNECTION = "connection"
private const val HTTP_HEADER_CONTENT_DISPOSITION = "content-disposition"
private const val HTTP_HEADER_CONTENT_ENCODING = "content-encoding"
private const val HTTP_HEADER_CONTENT_LANGUAGE = "content-language"
private const val HTTP_HEADER_CONTENT_LENGTH = "content-length"
private const val HTTP_HEADER_CONTENT_RANGE = "content-range"
private const val HTTP_HEADER_CONTENT_TYPE = "content-type"
private const val HTTP_HEADER_COOKIE = "cookie"
private const val HTTP_HEADER_DATE = "date"
private const val HTTP_HEADER_ETAG = "etag"
private const val HTTP_HEADER_EXPECT = "expect"
private const val HTTP_HEADER_EXPIRES = "expires"
private const val HTTP_HEADER_FROM = "from"
private const val HTTP_HEADER_HOST = "host"
private const val HTTP_HEADER_IF_MATCH = "if-match"
private const val HTTP_HEADER_IF_MODIFIED_SINCE = "if-modified-since"
private const val HTTP_HEADER_IF_NONE_MATCH = "if-none-match"
private const val HTTP_HEADER_IF_RANGE = "if-range"
private const val HTTP_HEADER_IF_UNMODIFIED_SINCE = "if-unmodified-since"
private const val HTTP_HEADER_LAST_MODIFIED = "last-modified"
private const val HTTP_HEADER_LINK = "link"
private const val HTTP_HEADER_LOCATION = "location"

/**
 * ## Standard HTTP Header
 *
 * Provides an enumeration of standard HTTP headers; each header provides its name, use disposition/constraints, and an
 * ability to resolve itself to a string and back again.
 *
 * Standard HTTP headers are typically defined in the HTTP spec, but may also technically be non-standard (through broad
 * or uniform use).
 */
public actual enum class StandardHeader (actual override val symbol: String, ): PlatformHeaderName, Symbolic<String> {
  Accept(HTTP_HEADER_ACCEPT) {
    override val allowedOnResponses: Boolean = false
  },
  AcceptCharset(HTTP_HEADER_ACCEPT_CHARSET) {
    override val allowedOnResponses: Boolean = false
  },
  AcceptEncoding(HTTP_HEADER_ACCEPT_ENCODING) {
    override val allowedOnResponses: Boolean = false
  },
  AcceptLanguage(HTTP_HEADER_ACCEPT_LANGUAGE) {
    override val allowedOnResponses: Boolean = false
  },
  AcceptRanges(HTTP_HEADER_ACCEPT_RANGES) {
    override val allowedOnResponses: Boolean = false
  },
  Age(HTTP_HEADER_AGE) {
    override val allowedOnRequests: Boolean = false
  },
  Allow(HTTP_HEADER_ALLOW),
  Authorization(HTTP_HEADER_AUTHORIZATION),
  CacheControl(HTTP_HEADER_CACHE_CONTROL),
  Connection(HTTP_HEADER_CONNECTION),
  ContentDisposition(HTTP_HEADER_CONTENT_DISPOSITION) {
    override val allowedOnRequests: Boolean = false
  },
  ContentEncoding(HTTP_HEADER_CONTENT_ENCODING) {
    override val allowedOnRequests: Boolean = false
  },
  ContentLanguage(HTTP_HEADER_CONTENT_LANGUAGE) {
    override val allowedOnRequests: Boolean = false
  },
  ContentLength(HTTP_HEADER_CONTENT_LENGTH),
  ContentRange(HTTP_HEADER_CONTENT_RANGE) {
    override val allowedOnRequests: Boolean = false
  },
  ContentType(HTTP_HEADER_CONTENT_TYPE),
  Cookie(HTTP_HEADER_COOKIE),
  Date(HTTP_HEADER_DATE),
  ETag(HTTP_HEADER_ETAG),
  Expect(HTTP_HEADER_EXPECT),
  Expires(HTTP_HEADER_EXPIRES),
  From(HTTP_HEADER_FROM),
  Host(HTTP_HEADER_HOST),
  IfMatch(HTTP_HEADER_IF_MATCH),
  IfModifiedSince(HTTP_HEADER_IF_MODIFIED_SINCE),
  IfNoneMatch(HTTP_HEADER_IF_NONE_MATCH),
  IfRange(HTTP_HEADER_IF_RANGE),
  IfUnmodifiedSince(HTTP_HEADER_IF_UNMODIFIED_SINCE),
  LastModified(HTTP_HEADER_LAST_MODIFIED),
  Link(HTTP_HEADER_LINK),
  Location(HTTP_HEADER_LOCATION);

  actual override val allowedOnRequests: Boolean = true
  actual override val allowedOnResponses: Boolean = true
  actual override val nameNormalized: String get() = symbol
  actual override fun asString(): String = nameNormalized

  public companion object : Symbolic.SealedResolver<String, StandardHeader> {
    public val all: Sequence<StandardHeader> = sequence {
      yieldAll(StandardHeader.entries)
    }

    override fun resolve(symbol: String): StandardHeader = when (symbol) {
      HTTP_HEADER_ACCEPT -> Accept
      HTTP_HEADER_ACCEPT_CHARSET -> AcceptCharset
      HTTP_HEADER_ACCEPT_ENCODING -> AcceptEncoding
      HTTP_HEADER_ACCEPT_LANGUAGE -> AcceptLanguage
      HTTP_HEADER_ACCEPT_RANGES -> AcceptRanges
      HTTP_HEADER_AGE -> Age
      HTTP_HEADER_ALLOW -> Allow
      HTTP_HEADER_AUTHORIZATION -> Authorization
      HTTP_HEADER_CACHE_CONTROL -> CacheControl
      HTTP_HEADER_CONNECTION -> Connection
      HTTP_HEADER_CONTENT_DISPOSITION -> ContentDisposition
      HTTP_HEADER_CONTENT_ENCODING -> ContentEncoding
      HTTP_HEADER_CONTENT_LANGUAGE -> ContentLanguage
      HTTP_HEADER_CONTENT_LENGTH -> ContentLength
      HTTP_HEADER_CONTENT_RANGE -> ContentRange
      HTTP_HEADER_CONTENT_TYPE -> ContentType
      HTTP_HEADER_COOKIE -> Cookie
      HTTP_HEADER_DATE -> Date
      HTTP_HEADER_ETAG -> ETag
      HTTP_HEADER_EXPECT -> Expect
      HTTP_HEADER_EXPIRES -> Expires
      HTTP_HEADER_FROM -> From
      HTTP_HEADER_HOST -> Host
      HTTP_HEADER_IF_MATCH -> IfMatch
      HTTP_HEADER_IF_MODIFIED_SINCE -> IfModifiedSince
      HTTP_HEADER_IF_NONE_MATCH -> IfNoneMatch
      HTTP_HEADER_IF_RANGE -> IfRange
      HTTP_HEADER_IF_UNMODIFIED_SINCE -> IfUnmodifiedSince
      HTTP_HEADER_LAST_MODIFIED -> LastModified
      HTTP_HEADER_LINK -> Link
      HTTP_HEADER_LOCATION -> Location
      else -> throw unresolved("Unknown standard HTTP header: $symbol")
    }
  }
}
