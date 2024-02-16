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

package elide.http

import kotlin.test.*
import elide.core.encoding.Encoding
import elide.http.api.*
import elide.http.api.HttpHeaders.HeaderName
import elide.http.api.HttpHeaders.HeaderName.Companion.asHeaderName
import elide.http.api.HttpHeaders.HeaderValue
import elide.http.api.HttpHeaders.HeaderValue.MultiValue
import elide.struct.sortedSetOf

/** Tests for HTTP header containers. */
class HttpHeadersTest {
  private fun assertHeader(name: String, header: HttpHeader<*>?, extraAssertions: (HttpHeader<*>) -> Unit = {}) {
    assertNotNull(header, "header should not be `null`")
    assertEquals(name, header.toString(), "header name `toString()` should return name")
    assertEquals(name, header.name.toString(), "header `name.name` `toString()` should return name")
    assertEquals(header, header, "header should equal itself")
    extraAssertions(header)
  }

  private fun <V: Any> assertValue(
    value: HttpHeaderValue<V>?,
    extraAssertions: (HttpHeaderValue<*>) -> Unit = {},
  ) {
    assertNotNull(value, "header value should not be `null`")
    assertEquals(value, value, "header value should equal itself")
    assertNotNull(value.asString, "header value `asString` should never be `null`")
    assertEquals(value.asString, value.asString, "header value `asString` should be stable")
    extraAssertions(value)
  }

  @Test fun testEmpty() {
    assertNotNull(HttpHeaders.empty())
    assertSame(HttpHeaders.empty(), HttpHeaders.empty())
    val headers = HttpHeaders.empty()
    assertTrue(headers.isEmpty())
    assertFalse(headers.isNotEmpty())
  }

  @Test fun testEmptyMutable() {
    val headers = MutableHttpHeaders.create()
    assertTrue(headers.isEmpty())
    assertFalse(headers.isNotEmpty())
  }

  @Test fun testCreate() {
    val headers = HttpHeaders.of("Content-Type" to "application/json")
    assertTrue(headers.isNotEmpty())
    assertFalse(headers.isEmpty())
    assertEquals("application/json", headers["Content-Type"])
    assertEquals("application/json", headers[HeaderName.of("Content-Type")]?.asString)
  }

  @Test fun testCreateMutable() {
    val headers = MutableHttpHeaders.of("Content-Type" to "application/json")
    assertTrue(headers.isNotEmpty())
    assertFalse(headers.isEmpty())
    assertEquals("application/json", headers["Content-Type"])
    assertEquals("application/json", headers[HeaderName.of("Content-Type")]?.asString)
  }

  @Test fun testCreateFromMap() {
    val headers = HttpHeaders.of(mapOf("Content-Type" to "application/json"))
    assertTrue(headers.isNotEmpty())
    assertFalse(headers.isEmpty())
    assertEquals("application/json", headers["Content-Type"])
    assertEquals("application/json", headers[HeaderName.of("Content-Type")]?.asString)
  }

  @Test fun testCreateFromMapMutable() {
    val headers = MutableHttpHeaders.of(mapOf("Content-Type" to "application/json"))
    assertTrue(headers.isNotEmpty())
    assertFalse(headers.isEmpty())
    assertEquals("application/json", headers["Content-Type"])
    assertEquals("application/json", headers["content-type"])
    assertEquals("application/json", headers[HeaderName.of("Content-Type")]?.asString)
  }

  @Test fun testGet() {
    val headers = HttpHeaders.of("Content-Type" to "application/json")
    assertEquals("application/json", headers["Content-Type"])
    assertEquals("application/json", headers[HeaderName.of("Content-Type")]?.asString)
  }

  @Test fun testGetMutable() {
    val headers = MutableHttpHeaders.of("Content-Type" to "application/json")
    assertEquals("application/json", headers["Content-Type"])
    assertEquals("application/json", headers[HeaderName.of("Content-Type")]?.asString)
  }

  @Test fun testGetMissing() {
    val headers = HttpHeaders.of("Content-Type" to "application/json")
    assertEquals(null, headers["Accept"])
    assertEquals(null, headers[HeaderName.of("Accept")]?.asString)
  }

  @Test fun testGetMissingMutable() {
    val headers = MutableHttpHeaders.of("Content-Type" to "application/json")
    assertEquals(null, headers["Accept"])
    assertEquals(null, headers[HeaderName.of("Accept")]?.asString)
  }

  @Test fun testGetEmpty() {
    val headers = HttpHeaders.empty()
    assertEquals(null, headers["Accept"])
    assertEquals(null, headers[HeaderName.of("Accept")]?.asString)
  }

  @Test fun testGetEmptyMutable() {
    val headers = MutableHttpHeaders.create()
    assertEquals(null, headers["Accept"])
    assertEquals(null, headers[HeaderName.of("Accept")]?.asString)
  }

  @Test fun testPutEmptyMutable() {
    val headers = MutableHttpHeaders.create()
    headers["Content-Type"] = "application/json"
    headers[HeaderName.of("Content-Disposition")] = "attachment; filename=\"example.json\""
    assertEquals(null, headers["Accept"])
    assertEquals(null, headers[HeaderName.of("Accept")]?.asString)
    assertEquals("application/json", headers["Content-Type"])
    assertEquals("application/json", headers[HeaderName.of("Content-Type")]?.asString)
    assertEquals("attachment; filename=\"example.json\"", headers["Content-Disposition"])
    assertEquals(
      "attachment; filename=\"example.json\"",
      headers[HeaderName.of("Content-Disposition")]?.asString,
    )
  }

  @Test @Ignore fun testPutExistingMutable() {
    val headers = MutableHttpHeaders.of("Accept" to "hello", "Content-Type" to "application/json")
    headers["Content-Type"] = "application/xml"
    assertEquals("hello", headers["Accept"])
    assertEquals("hello", headers[HeaderName.of("Accept")]?.asString)
    assertEquals("application/xml", headers["Content-Type"])
    assertEquals("application/xml", headers[HeaderName.of("Content-Type")]?.asString)
  }

  @Test @Ignore fun testPutMultiMutable() {
    val headers = MutableHttpHeaders.of("Accept" to "hello", "Content-Type" to "application/json")
    headers["Content-Type"] = "application/xml"
    assertEquals("hello", headers["Accept"])
    assertEquals("hello", headers[HeaderName.of("Accept")]?.asString)
    assertEquals("application/xml", headers["Content-Type"])
    assertEquals("application/xml", headers[HeaderName.of("Content-Type")]?.asString)
    headers.add("Accept", "world")
    assertEquals("hello", headers["Accept"])
    assertEquals("hello", headers[HeaderName.of("Accept")]?.asString)
    assertEquals("world", headers.getAll("Accept").last())
  }

  @Test fun testContains() {
    val headers = HttpHeaders.of("Content-Type" to "application/json")
    assertTrue("Content-Type" in headers)
    assertTrue(HeaderName.of("Content-Type") in headers)
    assertFalse("Accept" in headers)
    assertFalse(HeaderName.of("Accept") in headers)
  }

  @Test fun testContainsMutable() {
    val headers = MutableHttpHeaders.of("Content-Type" to "application/json")
    assertTrue("Content-Type" in headers)
    assertTrue(HeaderName.of("Content-Type") in headers)
    assertFalse("Accept" in headers)
    assertFalse(HeaderName.of("Accept") in headers)
  }

  @Test fun testHeaderName() {
    val name = HeaderName.of("Content-Type")
    assertEquals(name, HeaderName.of("Content-Type"))
    assertEquals(name, "Content-Type".asHeaderName)
    assertEquals<Any>("Content-Type", name.name.original)
    assertTrue(name.equals("Content-Type"))
    assertTrue(name.equals("content-type"))
    assertTrue(name.equals("CoNtEnT-TyPe"))
    assertEquals(name, "CoNtEnT-TyPe".asHeaderName)
    assertEquals(name.compareTo(HeaderName.of("Accept")), 1)
  }

  @Test fun testHeaderNameStrings() {
    assertEquals("content-type", HeaderName.of("content-type").toString())
    assertEquals('c', HeaderName.of("content-type")[0])
    assertEquals("co", HeaderName.of("content-type").subSequence(0, 2))
    assertEquals("content-type".length, HeaderName.of("content-type").length)
  }

  @Test fun testHeaderNameEquals() {
    val name = HeaderName.of("Content-Type")
    assertEquals(name, HeaderName.of("Content-Type"))
    assertEquals(name, HeaderName.of("content-type"))
    assertNotEquals(name, HeaderName.of("Accept"))
    assertFalse(name.equals(null))
    assertFalse(name.equals(5))
  }

  @Test fun testHeaderNameHashCode() {
    val name = HeaderName.of("Content-Type")
    assertEquals(name.hashCode(), HeaderName.of("Content-Type").hashCode())
    val other = HeaderName.of("Accept")
    assertNotEquals(name.hashCode(), other.hashCode())
    // must be stable
    assertEquals(name.hashCode(), name.hashCode())
    // must be tolerant of case
    assertEquals(name.hashCode(), HeaderName.of("content-type").hashCode())
  }

  @Test fun testHeaderValue() {
    val acceptGzip = HeaderValue.of(listOf("gzip"))
    val acceptGzip2 = HeaderValue.of(listOf("gzip"))
    assertNotNull(acceptGzip)
    assertNotNull(acceptGzip2)
    assertEquals(acceptGzip, acceptGzip2)
    assertEquals("gzip", acceptGzip.asString)
  }

  @Test fun testHeaderValueSingle() {
    val acceptGzip = HeaderValue.single("gzip")
    assertNotNull(acceptGzip)
    assertEquals("gzip", acceptGzip.asString)
    assertEquals(1, acceptGzip.size)
    assertEquals("gzip", acceptGzip.allValues.first())
    assertEquals(4, acceptGzip.asString.length)  // `gzip`
    assertEquals('g', acceptGzip.asString[0])  // `[g]zip`
    assertEquals("gz", acceptGzip.asString.subSequence(0, 2))  // `[gz]ip`
  }

  @Test fun testHeaderValueComparableSingle() {
    val left = HeaderValue.single("gzip")
    val right = HeaderValue.single("gzip")
    assertEquals(0, left.compareTo(right))
    assertEquals(0, left.compareTo("gzip"))
    assertEquals(0, left.asString.compareTo(right.asString))
    assertEquals("gzip", right.iterator().next())
    assertTrue(left.contains("gzip"))
    assertTrue(left.containsAll(listOf("gzip")))
    assertFalse(left.contains("something-else"))
    assertFalse(left.containsAll(listOf("something-else")))
    assertFalse(left.isEmpty())
    assertTrue(left.isNotEmpty())
  }

  @Test fun testHeaderValueStrings() {
    assertEquals("gzip", HeaderValue.single("gzip").toString())
    assertEquals("gzip,deflate", HeaderValue.multi("gzip", "deflate").toString())
  }

  @Test fun testHeaderValueSize() {
    assertEquals(1, HeaderValue.single("gzip").size)
    assertEquals(1, HeaderValue.multi("gzip").size)
    assertEquals(2, HeaderValue.multi("gzip", "deflate").size)
    assertEquals(3, HeaderValue.multi("gzip", "deflate", "identity").size)
  }

  @Test fun testHeaderValueMultiMutableAdd() {
    val acceptGzip = HeaderValue.multi("gzip", "deflate")
    assertNotNull(acceptGzip)
    assertEquals("gzip,deflate", acceptGzip.asString)
    assertEquals(2, acceptGzip.size)
    assertEquals("gzip", acceptGzip.allValues.first())
    assertEquals("deflate", acceptGzip.allValues[1])
    assertEquals(12, acceptGzip.asString.length)  // `gzip`
    assertEquals('g', acceptGzip.asString[0])  // `[g]zip`
    assertEquals("gz", acceptGzip.asString.subSequence(0, 2))  // `[gz]ip`
    val newValue = (acceptGzip as MultiValue).add("identity")
    assertNotNull(acceptGzip)
    assertEquals("gzip,deflate", acceptGzip.asString)
    assertEquals(2, acceptGzip.size)
    assertEquals("gzip", acceptGzip.allValues.first())
    assertEquals("deflate", acceptGzip.allValues[1])
    assertEquals(12, acceptGzip.asString.length)  // `gzip`
    assertEquals('g', acceptGzip.asString[0])  // `[g]zip`
    assertEquals("gz", acceptGzip.asString.subSequence(0, 2))  // `[gz]ip`
    assertNotNull(newValue)
    assertEquals("gzip,deflate,identity", newValue.asString)
    assertEquals(3, newValue.size)
    assertEquals("gzip", newValue.allValues.first())
    assertEquals("deflate", newValue.allValues[1])
    assertEquals("identity", newValue.allValues[2])
    assertEquals(21, newValue.asString.length)  // `gzip`
    assertEquals('g', newValue.asString[0])  // `[g]zip`
    assertEquals("gz", newValue.asString.subSequence(0, 2))  // `[gz]ip`
  }

  @Test fun testHeaderValueMultiMutableRemove() {
    val acceptGzip = HeaderValue.multi("gzip", "deflate")
    assertNotNull(acceptGzip)
    assertEquals("gzip,deflate", acceptGzip.asString)
    assertEquals(2, acceptGzip.size)
    assertEquals("gzip", acceptGzip.allValues.first())
    assertEquals("deflate", acceptGzip.allValues[1])
    assertEquals(12, acceptGzip.asString.length)  // `gzip`
    assertEquals('g', acceptGzip.asString[0])  // `[g]zip`
    assertEquals("gz", acceptGzip.asString.subSequence(0, 2))  // `[gz]ip`
    val newValue = (acceptGzip as MultiValue).remove("gzip")
    assertNotNull(acceptGzip)
    assertEquals("gzip,deflate", acceptGzip.asString)
    assertEquals(2, acceptGzip.size)
    assertEquals("gzip", acceptGzip.allValues.first())
    assertEquals("deflate", acceptGzip.allValues[1])
    assertEquals(12, acceptGzip.asString.length)  // `gzip`
    assertEquals('g', acceptGzip.asString[0])  // `[g]zip`
    assertEquals("gz", acceptGzip.asString.subSequence(0, 2))  // `[gz]ip`
    assertNotNull(newValue)
    assertEquals("deflate", newValue.asString)
    assertEquals(1, newValue.size)
    assertEquals("deflate", newValue.allValues.first())
    assertEquals(7, newValue.asString.length)  // `deflate`
    assertEquals('d', newValue.asString[0])  // `[d]eflate`
    assertEquals("de", newValue.asString.subSequence(0, 2))  // `[de]flate`
  }

  @Test fun testHeaderValueComparableMultiple() {
    val left = HeaderValue.multi("gzip", "deflate")
    val right = HeaderValue.multi("gzip", "deflate")
    assertEquals(0, left.compareTo(right))
    assertEquals(0, left.compareTo("gzip,deflate"))
    assertEquals(0, left.asString.compareTo(right.asString))
    assertEquals("gzip", right.iterator().next())
    assertEquals("deflate", right.iterator().apply { next() }.next())
    assertTrue(left.contains("gzip"))
    assertTrue(left.containsAll(listOf("gzip")))
    assertFalse(left.contains("something-else"))
    assertFalse(left.containsAll(listOf("something-else")))
    assertTrue(left.contains("deflate"))
    assertTrue(left.containsAll(listOf("deflate")))
    assertTrue(left.containsAll(listOf("gzip", "deflate")))
    assertTrue(left.containsAll(listOf("deflate", "gzip")))
    assertFalse(left.isEmpty())
    assertTrue(left.isNotEmpty())
  }

  @Test fun testHeaderValueMultiple() {
    // simulates:
    // Accept: gzip
    // Accept: deflate
    val acceptGzip = HeaderValue.multi(listOf("gzip", "deflate"))
    assertNotNull(acceptGzip)
    assertEquals("gzip,deflate", acceptGzip.asString)
    assertEquals(2, acceptGzip.size)
    assertEquals("gzip", acceptGzip.allValues.first())
    assertEquals("deflate", acceptGzip.allValues[1])
    assertEquals(12, acceptGzip.asString.length)  // `gzip`
    assertEquals('g', acceptGzip.asString[0])  // `[g]zip`
    assertEquals("gz", acceptGzip.asString.subSequence(0, 2))  // `[gz]ip`
  }

  @Test fun testSingleGetAll() {
    val headers = HttpHeaders.of("Content-Type" to "application/json")
    assertEquals("application/json", headers.getAll("Content-Type").first())
    assertEquals("application/json", headers.getAll(HeaderName.of("Content-Type")).first())
  }

  @Test fun testSingleGetAllMutable() {
    val headers = MutableHttpHeaders.of("Content-Type" to "application/json")
    assertEquals("application/json", headers.getAll("Content-Type").first())
    assertEquals("application/json", headers.getAll(HeaderName.of("Content-Type")).first())
  }

  @Test fun testMultipleGetAll() {
    val headers = HttpHeaders.of("Accept" to "gzip", "Accept" to "deflate")
    assertEquals("gzip", headers.getAll("Accept").first())
    assertEquals("deflate", headers.getAll("Accept").last())
    assertEquals("gzip", headers.getAll(HeaderName.of("Accept")).first())
    assertEquals("deflate", headers.getAll(HeaderName.of("Accept")).last())
  }

  @Test fun testMultipleGetAllMutable() {
    val headers = MutableHttpHeaders.of("Accept" to "gzip", "Accept" to "deflate")
    assertEquals("gzip", headers.getAll("Accept").first())
    assertEquals("deflate", headers.getAll("Accept").last())
    assertEquals("gzip", headers.getAll(HeaderName.of("Accept")).first())
    assertEquals("deflate", headers.getAll(HeaderName.of("Accept")).last())
  }

  @Test fun testStandardHeaders() {
    HttpHeader.all.forEach {
      assertHeader(it.toString(), it)
    }
  }

  @Test fun testStandardHeadersComparable() {
    val set = sortedSetOf<HttpHeader<*>>(HttpHeader.CONTENT_TYPE, HttpHeader.ACCEPT)
    assertTrue(set.contains(HttpHeader.CONTENT_TYPE))
    assertTrue(set.contains(HttpHeader.ACCEPT))
    assertFalse(set.contains(HttpHeader.ACCEPT_ENCODING))
    assertFalse(set.contains(HttpHeader.CONTENT_LENGTH))
    assertEquals(HttpHeader.ACCEPT, set.first())
    assertEquals(HttpHeader.CONTENT_TYPE, set.last())
  }

  @Test fun testStandardHeadersEqualsHashcode() {
    val ctHashCode = HttpHeader.CONTENT_TYPE.hashCode()
    assertEquals(HttpHeader.CONTENT_TYPE, HttpHeader.CONTENT_TYPE)
    assertEquals(HttpHeader.CONTENT_TYPE.hashCode(), HttpHeader.CONTENT_TYPE.hashCode())
    assertEquals(ctHashCode, HttpHeader.CONTENT_TYPE.hashCode())
    assertNotEquals<Any>(HttpHeader.CONTENT_TYPE, HttpHeader.ACCEPT)
    assertNotEquals(HttpHeader.CONTENT_TYPE.hashCode(), HttpHeader.ACCEPT.hashCode())
  }

  @Test fun testStandardHeadersAsString() {
    assertEquals("content-type", HttpHeader.CONTENT_TYPE.toString())
    assertEquals("content-type", HttpHeader.CONTENT_TYPE.name.toString())
    assertEquals("Content-Type", HttpHeader.CONTENT_TYPE.name.name.original)
    assertEquals("content-type".length, HttpHeader.CONTENT_TYPE.name.length)
    assertEquals('c', HttpHeader.CONTENT_TYPE.name[0])
    assertEquals("co", HttpHeader.CONTENT_TYPE.name.subSequence(0, 2))
  }

  @Test fun testStandardHeadersTypeNotNull() {
    HttpHeader.all.forEach {
      assertNotNull(it.type)
    }
  }

  @Test fun testStandardHeaderValues() {
    HttpHeaderValue.all.forEach {
      assertValue(it)
    }
  }

  @Test fun testCommonContentTypes() {
    HttpHeaderValue.ContentType.all.forEach {
      assertValue(it)
    }
  }

  @Test fun testCommonContentLanguages() {
    HttpHeaderValue.ContentLanguage.all.forEach {
      assertValue(it)
    }
  }

  @Test fun testHeaderFactory() {
    val factory: elide.http.api.HttpHeaders.Factory = HttpHeaders.Companion
    assertNotNull(factory)
    assertNotNull(factory.of("Content-Type" to "application/json"))
    assertNotNull(factory.of(mapOf("Content-Type" to "application/json")))
    assertNotNull(factory.of(listOf("Content-Type" to "application/json")))
  }

  @Test fun testHeaderFactoryMutable() {
    val factory: elide.http.api.HttpHeaders.Factory = MutableHttpHeaders.Companion
    assertNotNull(factory)
    assertNotNull(factory.of("Content-Type" to "application/json"))
    assertNotNull(factory.of(mapOf("Content-Type" to "application/json")))
    assertNotNull(factory.of(listOf("Content-Type" to "application/json")))
  }

  @Test fun testHeaderValueSingularToken() {
    val token = HttpToken("gzip")
    assertEquals("gzip", token.toString())
    assertEquals("gzip", token.asString)
    assertEquals("gzip", token.value)
    assertEquals("gzip", token.allValues.first())
  }

  @Test fun testHeaderValueMultiToken() {
    val token = HttpTokenList(listOf("gzip", "deflate"))
    assertEquals("gzip,deflate", token.toString())
    assertEquals("gzip,deflate", token.asString)
    assertEquals("gzip", token.value)
    assertEquals("gzip", token.allValues.first())
    assertEquals("deflate", token.allValues.last())
  }

  @Test fun testLanguageToken() {
    val token = Language("en")
    assertEquals("en", token.toString())
    assertEquals("en", token.asString)
    assertEquals("en", token.value)
    assertEquals("en", token.allValues.first())
  }

  @Test fun testEncodingToken() {
    val token = HttpEncoding("utf-8" to Encoding.UTF_8)
    assertEquals("utf-8", token.toString())
    assertEquals("utf-8", token.asString)
    assertEquals("utf-8", token.value)
    assertEquals("utf-8", token.allValues.first())
  }

  @Test fun testMimetypeToken() {
    val token = Mimetype("application/octet-stream")
    assertEquals("application/octet-stream", token.toString())
    assertEquals("application/octet-stream", token.asString)
    assertEquals("application/octet-stream", token.value)
    assertEquals("application/octet-stream", token.allValues.first())
  }
}
