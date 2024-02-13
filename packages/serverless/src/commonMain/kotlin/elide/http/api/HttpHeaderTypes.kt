/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package elide.http.api

import kotlinx.datetime.Instant
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass
import elide.core.encoding.Encoding
import elide.core.encoding.Encoding.BASE64
import elide.http.api.HttpHeaders.HeaderName

/**
 * ## HTTP Header Information
 *
 * This is an internal-only class which is used to hold basic information about a standard HTTP header, including the
 * header's [name] and [type].
 *
 * @param name Normalized name of the header.
 * @param type Kotlin type of the header's value.
 */
internal open class HeaderMetadata<Value: Any> internal constructor (
  override val name: HeaderName,
  override val type: KClass<Value>,
) : HttpHeader<Value> {
  override fun toString(): String = name.toString()
  override fun equals(other: Any?): Boolean = other is HttpHeader<*> && name == other.name
  override fun hashCode(): Int = name.hashCode()
}

// Default separator to use when parsing tokenized header values.
internal const val DEFAULT_SEPARATOR = ","

/**
 * ## Tokenized HTTP Header
 *
 * This is an internal-only class which is used to hold information about a standard HTTP header which supports split-
 * by-token when reading the header's value.
 *
 * @param name Normalized name of the header.
 * @param type Kotlin type of the header's value.
 * @param separator Separator to use when splitting the header's value into tokens.
 */
internal class TokenizedHeader<Value: Any> internal constructor (
  name: HeaderName,
  type: KClass<Value>,
  internal val separator: HttpString,
) : HeaderMetadata<Value>(name, type)

/**
 * ## Encoded HTTP Header
 *
 * This is an internal-only class which is used to hold information about a standard HTTP header which employs some form
 * of encoding when reading the header's value.
 *
 * @param name Normalized name of the header.
 * @param type Kotlin type of the header's value.
 * @param encoding Encoding to use when reading the header's value.
 */
internal class EncodedHeader<Value: Any> internal constructor (
  name: HeaderName,
  type: KClass<Value>,
  internal val encoding: Encoding,
) : HeaderMetadata<Value>(name, type)

/**
 * ## Header Value Token
 *
 * Specifies a type of header value token, for example `en-US` for `Accept-Language` headers (indicating English as a
 * supported language), or `application/json` for `Content-Type` headers (indicating JSON as a response type).
 */
internal sealed interface HeaderValueTokenType<T: Any> : HttpHeaderValue<T> {
  /** Token value. */
  val token: T

  override val value: T get() = token
  override val asString: HttpString get() = token.toString()
  override val allValues: Collection<T> get() = listOf(token)
}

/**
 * ## Header Value Multi-Token
 *
 * Specifies a type of header value token, for example `en-US` for `Accept-Language` headers (indicating English as a
 * supported language), or `application/json` for `Content-Type` headers (indicating JSON as a response type).
 */
internal sealed interface HeaderValueMultiTokenType<T> : HttpHeaderValue<T> where T: Any {
  /** Token value. */
  val tokens: Collection<T>

  override val value: T get() = tokens.first()
  override val allValues: Collection<T> get() = tokens
}

/**
 * ## Generic Singular Token
 *
 * This is an internal-only class which is used to express a single generic string token, used in some symbolically
 * meaningful manner in the HTTP protocol.
 *
 * @param token Token value.
 */
@JvmInline public value class HttpToken internal constructor (override val token: HttpString) :
  HeaderValueTokenType<HttpString>, HttpHeaderValue<String> {
  override fun toString(): String = asString
}

/**
 * ## Generic Multi-Token
 *
 * This is an internal-only class which is used to express language tokens, as related to the `Accept-Language` header.
 *
 * @param tokens Token values.
 */
@JvmInline public value class HttpTokenList internal constructor (override val tokens: List<String>) :
  HeaderValueMultiTokenType<String>, HttpHeaderValue<String> {
  override val asString: String get() = tokens.joinToString(DEFAULT_SEPARATOR)
  override fun toString(): String = asString
}

/**
 * ## Language Token
 *
 * This is an internal-only class which is used to express language tokens, as related to the `Accept-Language` header.
 *
 * @param token Token value.
 */
@JvmInline public value class Language internal constructor (override val token: String) :
  HeaderValueTokenType<String>, HttpHeaderValue<String> {
  override fun toString(): String = asString
}

/**
 * ## MIME Type Token
 *
 * This is an internal-only class which is used to express MIME type token strings, as related to the `Accept` header
 * and `Content-Type` header.
 *
 * @param token Token value.
 */
@JvmInline public value class Mimetype internal constructor (override val token: String) :
  HeaderValueTokenType<String>, HttpHeaderValue<String> {
    /** Well-known content types. */
    public companion object {
      /** Well-known mimetype for plain text. */
      public val Text: Mimetype = Mimetype("text/plain")

      /** Well-known mimetype for a stream of bytes. */
      public val OctetStream: Mimetype = Mimetype("application/octet-stream")
    }

  override fun toString(): String = asString
}

/**
 * ## HTTP Encoding
 *
 * This is an internal-only class which is used to express encoding schemes over HTTP headers, as relates to the
 * `Accept-Encoding` header and the `Content-Encoding` header.
 *
 * @param spec Pair of a string Encoding name or specification, and an optional pre-resolved matching [Encoding].
 */
@JvmInline public value class HttpEncoding internal constructor (
  private val spec: Pair<String, Encoding?>,
) : HeaderValueTokenType<String> {
  override val token: String get() = spec.first
  override val asString: String get() = token
  override fun toString(): String = asString
}

/**
 * ## Accept HTTP Header
 *
 * This is an internal-only class which is used to hold information about an HTTP header which employs `Accept`-style
 * semantics, with a list of values and their associated quality factors, and an optional quality factor for the default
 * value.
 *
 * @param name Normalized name of the header.
 * @param type Kotlin type of the header's value.
 */
internal class AcceptHeader<Value: HeaderValueTokenType<*>> internal constructor (
  name: HeaderName,
  type: KClass<Value>,
) : HeaderMetadata<Value>(name, type)

// Factory methods used internally to describe standard headers.
@Suppress("unused") internal object HeaderTypes {
  // Wrap the header name and type into a `HttpHeaderInfo` instance.
  @JvmStatic inline fun <reified Value: Any> of(
    name: HeaderName,
    type: KClass<Value> = Value::class
  ): HttpHeader<Value> = HeaderMetadata(name, type)

  // Wrap the header name and type into a `HttpHeaderInfo` instance which supports tokenization.
  @JvmStatic inline fun <reified Value: Any> tokenized(
    name: HeaderName,
    type: KClass<Value>,
    separator: HttpString,
  ): TokenizedHeader<Value> = TokenizedHeader(name, type, separator)

  // Wrap the header name and type into a `HttpHeaderInfo` instance which supports encoding unwrap.
  @JvmStatic inline fun <reified Value: Any> encoded(
    name: HeaderName,
    type: KClass<Value>,
    encoding: Encoding,
  ): EncodedHeader<Value> = EncodedHeader(name, type, encoding)

  // Wrap the header name and set the type to `String`.
  @JvmStatic inline fun str(name: String): HttpHeader<String> = of(HeaderName.std(name))

  // Wrap the header name and set the type to `String`.
  @JvmStatic inline fun tokenized(
    name: HeaderName,
    separator: HttpString,
  ): HttpHeader<String> = tokenized(
    name,
    String::class,
    separator,
  )

  // Wrap the header name and set the type to `String`.
  @JvmStatic inline fun tokenized(
    name: String,
    separator: HttpString = DEFAULT_SEPARATOR,
  ): HttpHeader<String> = tokenized(
    HeaderName.std(name),
    separator,
  )

  // Wrap the header name and set the type to `String`.
  @JvmStatic inline fun encoded(
    name: HeaderName,
    encoding: Encoding,
  ): HttpHeader<String> = encoded(
    name,
    String::class,
    encoding,
  )

  // Wrap the header name and set the type to `String`.
  @JvmStatic inline fun encoded(
    name: String,
    encoding: Encoding = BASE64,
  ): HttpHeader<String> = encoded(
    HeaderName.std(name),
    encoding,
  )

  // Wrap the header name and set the type to `ULong`.
  @JvmStatic inline fun ulong(name: String): HttpHeader<ULong> = of(HeaderName.std(name))

  // Wrap the header name and set the type to `Instant` (used for dates as well).
  @JvmStatic inline fun date(name: String): HttpHeader<Instant> = of(HeaderName.std(name))

  // ----------------------------------------- Special Behavior Area -------------------------------------------------

  // Wrap the `Accept` header and treat it like a tokenized header which produces `Mimetype` instances.
  @JvmStatic inline fun accept(name: String): AcceptHeader<Mimetype> = AcceptHeader(
    HeaderName.std(name),
    Mimetype::class,
  )

  // Wrap the `Accept-Language` header and treat it like a tokenized header which produces `Language` instances.
  @JvmStatic inline fun acceptLanguage(name: String): AcceptHeader<Language> = AcceptHeader(
    HeaderName.std(name),
    Language::class,
  )

  // Wrap the `Accept-Language` header and treat it like a tokenized header which produces `Language` instances.
  @JvmStatic inline fun acceptEncoding(name: String): AcceptHeader<HttpEncoding> = AcceptHeader(
    HeaderName.std(name),
    HttpEncoding::class,
  )
}

// Factory methods used internally to describe standard header values.
@Suppress("unused") internal object HeaderValueTypes {
  // String-type with no other constraints.
  @JvmStatic inline fun str(value: String): HttpHeaderValue<String> = HttpToken(value)

  // Resolve a string with an encoding.
  @JvmStatic inline fun encoding(value: String, encoding: Encoding? = null): HttpEncoding = HttpEncoding(
    value to encoding,
  )

  // Resolve a string with a MIME type.
  @JvmStatic inline fun mime(value: String): Mimetype = Mimetype(value)

  // Resolve a string with a language.
  @JvmStatic inline fun lang(value: String): Language = Language(value)
}
