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

import elide.core.encoding.Encoding

/**
 * # HTTP Headers: Standard Values
 *
 * Defines standard HTTP header values known to the system and to the HTTP protocol at various versions of the HTTP
 * specifications; these values are used to represent known header values and are used by the underlying engine to
 * access typing and parsing information for standard header values.
 *
 * @see HttpHeader for the corresponding header names.
 */
public sealed interface HttpHeaderValue<Value: Any> {
  /** The value of this header. */
  public val value: Value

  /** Standard string form of this primitive value. */
  public val asString: String

  /** All gathered values for this header value, as applicable. Defaults to a single-entry collection. */
  public val allValues: Collection<Value>

  /** Public header value types. */
  public companion object {
    /** Tag value `br`, used in `Accept-Encoding` and `Content-Encoding` headers. */
    public val BROTLI: HttpHeaderValue<String> = HeaderValueTypes.str("br")

    /** Tag value `deflate`, used in `Accept-Encoding` and `Content-Encoding` headers. */
    public val DEFLATE: HttpHeaderValue<String> = HeaderValueTypes.str("deflate")

    /** Tag value `gzip`, used in `Accept-Encoding` and `Content-Encoding` headers. */
    public val GZIP: HttpHeaderValue<String> = HeaderValueTypes.str("gzip")

    /** Tag value `identity`, used in `Accept-Encoding` and `Content-Encoding` headers. */
    public val IDENTITY: HttpHeaderValue<String> = HeaderValueTypes.str("identity")

    /** Tag value `public`, used in `Cache-Control` headers. */
    public val PUBLIC: HttpHeaderValue<String> = HeaderValueTypes.str("public")

    /** Tag value `private`, used in `Cache-Control` headers. */
    public val PRIVATE: HttpHeaderValue<String> = HeaderValueTypes.str("private")

    /** Tag value `no-cache`, used in `Cache-Control` headers. */
    public val NO_CACHE: HttpHeaderValue<String> = HeaderValueTypes.str("no-cache")

    /** Tag value `no-store`, used in `Cache-Control` headers. */
    public val NO_STORE: HttpHeaderValue<String> = HeaderValueTypes.str("no-store")

    // -- Encodings -- //

    /** Tag value `utf-8`, representing UTF-8-encoded content. */
    public val UTF8: HttpEncoding = HeaderValueTypes.encoding("utf-8", Encoding.UTF_8)

    // -- Mimetypes -- //

    /** Encoding for JSON, via `application/json`. */
    public val JSON: Mimetype = ContentType.JSON

    /** Encoding for XML, via `application/xml`. */
    public val XML: Mimetype = ContentType.XML

    /** Encoding for HTML, via `text/html`. */
    public val HTML: Mimetype = ContentType.HTML

    /** Encoding for plain text, via `text/plain`. */
    public val TEXT: Mimetype = ContentType.TEXT

    // -- Languages -- //

    /** English language, via `en`. */
    public val ENGLISH: Language = ContentLanguage.ENGLISH

    /** French language, via `fr`. */
    public val FRENCH: Language = ContentLanguage.FRENCH

    /** German language, via `de`. */
    public val GERMAN: Language = ContentLanguage.GERMAN

    /** Spanish language, via `es`. */
    public val SPANISH: Language = ContentLanguage.SPANISH

    /** All known token values. */
    public val all: Collection<HttpHeaderValue<String>> = listOf(  // keep sorted
      BROTLI,
      DEFLATE,
      GZIP,
      IDENTITY,
      PUBLIC,
      PRIVATE,
      NO_CACHE,
      NO_STORE,
      UTF8,
      JSON,
      XML,
      HTML,
      TEXT,
      ENGLISH,
      FRENCH,
      GERMAN,
      SPANISH,
    )
  }

  /** Common `Content-Type` MIME-type values. */
  public data object ContentType {
    /** Encoding for JSON, via `application/json`. */
    public val JSON: Mimetype = HeaderValueTypes.mime("application/json")

    /** Encoding for XML, via `application/xml`. */
    public val XML: Mimetype = HeaderValueTypes.mime("application/xml")

    /** Encoding for HTML, via `text/html`. */
    public val HTML: Mimetype = HeaderValueTypes.mime("text/html")

    /** Encoding for plain text, via `text/plain`. */
    public val TEXT: Mimetype = HeaderValueTypes.mime("text/plain")

    /** All known MIME types. */
    public val all: Collection<Mimetype> = listOf(  // keep sorted
      HTML,
      JSON,
      TEXT,
      XML,
    )
  }

  /** Common `Content-Language` values. */
  public data object ContentLanguage {
    /** English language, via `en`. */
    public val ENGLISH: Language = HeaderValueTypes.lang("en")

    /** French language, via `fr`. */
    public val FRENCH: Language = HeaderValueTypes.lang("fr")

    /** German language, via `de`. */
    public val GERMAN: Language = HeaderValueTypes.lang("de")

    /** Spanish language, via `es`. */
    public val SPANISH: Language = HeaderValueTypes.lang("es")

    /** All known language values. */
    public val all: Collection<Language> = listOf(  // keep sorted
      ENGLISH,
      FRENCH,
      GERMAN,
      SPANISH,
    )
  }
}
