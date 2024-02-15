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

@file:Suppress("MemberVisibilityCanBePrivate")

package elide.http.api

import kotlinx.datetime.Instant
import kotlin.reflect.KClass
import elide.http.api.HttpHeaders.HeaderName

/**
 * # Standard HTTP Headers
 *
 * Defines [HeaderName] instances and their associated typed profiles for standard/known HTTP headers; these can be used
 * to easily access a specific header, and are used by the underlying engine to access typing and parsing information
 * for standard headers.
 */
public sealed interface HttpHeader<Value: Any> : Comparable<HttpHeader<*>> {
  override fun compareTo(other: HttpHeader<*>): Int = name.compareTo(other.name)

  /**
   * ## HTTP Header Name
   *
   * Describes the name of an HTTP header, which is case-insensitive and normalized for leading/trailing whitespace.
   */
  public val name: HeaderName

  /**
   * ## Header Value Type
   *
   * Describes the Kotlin type for this header; when parsed, the value will be returned as this type.
   */
  public val type: KClass<Value>

  /** Standard or known HTTP headers. */
  public companion object {
    /** HTTP `Accept` header. */
    public val ACCEPT: HttpHeader<Mimetype> = HeaderTypes.accept("Accept")

    /** HTTP `Accept-Charset` header. */
    public val ACCEPT_CHARSET: HttpHeader<String> = HeaderTypes.str("Accept-Charset")

    /** HTTP `Accept-Encoding` header. */
    public val ACCEPT_ENCODING: HttpHeader<HttpEncoding> = HeaderTypes.acceptEncoding("Accept-Encoding")

    /** HTTP `Accept-Language` header. */
    public val ACCEPT_LANGUAGE: HttpHeader<Language> = HeaderTypes.acceptLanguage("Accept-Language")

    /** HTTP `Accept-Ranges` header. */
    public val ACCEPT_RANGES: HttpHeader<String> = HeaderTypes.str("Accept-Ranges")

    /** HTTP `Age` header. */
    public val AGE: HttpHeader<String> = HeaderTypes.str("Age")

    /** HTTP `Authorization` header. */
    public val AUTHORIZATION: HttpHeader<String> = HeaderTypes.encoded("Authorization")

    /** HTTP `Authorization-Info` header. */
    public val AUTHORIZATION_INFO: HttpHeader<String> = HeaderTypes.str("Authorization-Info")

    /** HTTP `Cache-Control` header. */
    public val CACHE_CONTROL: HttpHeader<String> = HeaderTypes.tokenized("Cache-Control")

    /** HTTP `Content-Type` header. */
    public val CONTENT_TYPE: HttpHeader<String> = HeaderTypes.str("Content-Type")

    /** HTTP `Content-Length` header. */
    public val CONTENT_LENGTH: HttpHeader<ULong> = HeaderTypes.ulong("Content-Length")

    /** HTTP `Content-Encoding` header. */
    public val CONTENT_ENCODING: HttpHeader<String> = HeaderTypes.str("Content-Encoding")

    /** HTTP `Content-Language` header. */
    public val CONTENT_LANGUAGE: HttpHeader<String> = HeaderTypes.str("Content-Language")

    /** HTTP `Cookie` header. */
    public val COOKIE: HttpHeader<String> = HeaderTypes.str("Cookie")

    /** HTTP `Date` header. */
    public val DATE: HttpHeader<Instant> = HeaderTypes.date("Date")

    /** HTTP `ETag` header. */
    public val ETAG: HttpHeader<String> = HeaderTypes.str("ETag")

    /** HTTP `Expires` header. */
    public val EXPIRES: HttpHeader<String> = HeaderTypes.str("Expires")

    /** HTTP `Host` header. */
    public val HOST: HttpHeader<String> = HeaderTypes.str("Host")

    /** HTTP `Last-Modified` header. */
    public val LAST_MODIFIED: HttpHeader<String> = HeaderTypes.str("Last-Modified")

    /** HTTP `Location` header. */
    public val LOCATION: HttpHeader<String> = HeaderTypes.str("Location")

    /** HTTP `Server` header. */
    public val SERVER: HttpHeader<String> = HeaderTypes.str("Server")

    /** HTTP `Set-Cookie` header. */
    public val SET_COOKIE: HttpHeader<String> = HeaderTypes.str("Set-Cookie")

    /** All standard HTTP headers. */
    internal val all: Collection<HttpHeader<*>> = listOf(  // please keep this list sorted
      ACCEPT,
      ACCEPT_CHARSET,
      ACCEPT_ENCODING,
      ACCEPT_LANGUAGE,
      ACCEPT_RANGES,
      AGE,
      AUTHORIZATION,
      AUTHORIZATION_INFO,
      CONTENT_TYPE,
      CONTENT_LENGTH,
      CONTENT_ENCODING,
      CONTENT_LANGUAGE,
      COOKIE,
      DATE,
      ETAG,
      EXPIRES,
      HOST,
      LAST_MODIFIED,
      LOCATION,
      SERVER,
      SET_COOKIE,
    )
  }
}
