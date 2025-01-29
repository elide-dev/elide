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
@file:Suppress("JSUnresolvedFunction", "JSUnresolvedVariable")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.intrinsics.js.url

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.Value.asValue
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.js.url.URLSearchParamsIntrinsic.ExtractableBackingMap
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.MultiMapLike
import elide.runtime.intrinsics.js.MutableURLSearchParams
import elide.runtime.intrinsics.js.URLSearchParams
import elide.runtime.intrinsics.js.err.TypeError
import elide.testing.annotations.TestCase

/** Tests for the intrinsic `URLSearchParams` implementation provided by Elide. */
@TestCase internal class URLSearchParamsIntrinsicTest : AbstractJsIntrinsicTest<URLSearchParamsIntrinsic>() {
  @Inject lateinit var urlIntrinsic: URLIntrinsic
  @Inject lateinit var urlSearchParamsIntrinsic: URLSearchParamsIntrinsic

  override fun provide(): URLSearchParamsIntrinsic = urlSearchParamsIntrinsic

  @Suppress("UNCHECKED_CAST")
  private fun assertParamsAsHostObject(count: Int, valueCount: Int, params: URLSearchParams) {
    assertEquals(count, params.size, "URLSearchParams should report correct size")
    if (count > 0) {
      assertEquals(count, params.size, "`params.size` should report correct size")
      assertEquals(count, params.keys.size, "`params.keys.size` should report correct size")
    }
    if (valueCount > 0)
      assertEquals(valueCount, params.values.size, "`params.values.size` should report correct size")

    // test backing map
    val backing = assertNotNull((params as ExtractableBackingMap<URLParamsMap, String, MutableList<String>>).asMap())
    assertEquals(count, backing.size, "backing map should have correct size")
    backing.keys.forEach {
      assertContains(params.keys, it, "search param `keys` should contain `$it`")
      assertContains(params, it, "search params should contain `$it`")
      assertTrue(params.has(it), "search params `has('$it')` should return `true`")
    }
  }

  private fun assertParamsAsHashMap(
    count: Int,
    valueCount: Int,
    keys: Set<String>? = null,
    values: List<Pair<String, String>>? = null,
    params: URLSearchParams
  ) {
    assertEquals(count.toLong(), params.hashSize, "URLSearchParams should report correct size as hash map")
    if (count > 0) assertEquals(count, params.keys.size)
    if (valueCount > 0) assertEquals(valueCount, params.values.size)
    params.keys.forEach {
      assertTrue(params.hasHashEntry(asValue(it)), "`hasHashEntry` should report `true` for key '$it'")
    }
    keys?.forEach {
      assertTrue(params.hasHashEntry(asValue(it)), "`hasHashEntry` should report `true` for key '$it'")
      val hasMethod = params.getMember("has")
      val getMethod = params.getMember("get")
      assertIs<ProxyExecutable>(hasMethod, "`has` should be `ProxyExecutable`")
      assertIs<ProxyExecutable>(getMethod, "`get` should be `ProxyExecutable`")
      val doesHave = hasMethod.execute(asValue(it))
      assertTrue(when (doesHave) {
        is Boolean -> doesHave
        is Value -> doesHave.asBoolean()
        else -> error("expected boolean or guest value boolean, got $doesHave, after executing `has`")
      }, "`has` should return `true` for key '$it'")

      assertDoesNotThrow {
        assertNotNull(getMethod.execute(asValue(it)))
      }
    }
    values?.forEach { (key, value) ->
      assertTrue(params.hasHashEntry(asValue(key)), "`hasHashEntry` should report `true` for key '$key'")
      assertEquals(
        value,
        params.getHashValue(asValue(key)),
        "hash entry value should be '$value'"
      )
      val getMethod = params.getMember("get")
      assertIs<ProxyExecutable>(getMethod, "`get` should be `ProxyExecutable`")
      val plucked = getMethod.execute(asValue(key))
      assertIs<String>(plucked, "plucked `get` should return string")
      assertEquals(value, plucked, "plucked `get` should return '$value'")
    }

    // same with arbitrary hash entries if the params are not mutable
    if (params !is MutableURLSearchParams) {
      assertThrows<UnsupportedOperationException> {
        params.putHashEntry(asValue("hello"), asValue("world"))
      }
    }
  }

  private fun assertParamsAsObject(params: URLSearchParams) {
    // check internal consistency
    val members = assertNotNull(params.memberKeys)
    assertTrue(members.isNotEmpty())
    members.forEach {
      assertDoesNotThrow {
        assertNotNull(params.getMember(it))
      }
    }

    // putting a member should always throw
    assertThrows<UnsupportedOperationException> {
      params.putMember("hello", asValue("world"))
    }
  }

  private inline fun URLSearchParams.assertParams(
    count: Int = 0,
    valueCount: Int = 0,
    keys: Set<String>? = null,
    values: List<Pair<String, String>>? = null,
    mutable: Boolean = false,
    asHashMap: Boolean = true,
    asObject: Boolean = true,
    assertions: URLSearchParams.() -> Unit,
  ) {
    assertNotNull(this)
    if (count > 0) assertEquals(count, this.size)

    // testing as hash map and object
    assertParamsAsHostObject(count, valueCount, this)
    if (asHashMap) assertParamsAsHashMap(count, valueCount, keys, values, this)
    if (asObject) assertParamsAsObject(this)
    assertIs<URLSearchParams>(this, "should be instance of `URLSearchParams`")
    if (mutable) assertIs<MutableURLSearchParams>(this, "should be instance of `MutableURLSearchParams`")
    else assertIsNot<MutableURLSearchParams>(this, "should not be instance of `MutableURLSearchParams`")

    // testing for given keys
    keys?.forEach {
      assertContains(this.keys, it, "search param `keys` should contain `$it`")
      assertContains(this, it, "search params should contain `$it`")
      assertTrue(this.has(it), "search params `has('$it')` should return `true`")
    }

    // testing for given values
    values?.forEach { (key, value) ->
      assertEquals(value, this[key], "search param value at key '$key' should be '$value'")
      assertContains(this.getAll(key), value, "search params `getAll('$key')` should contain '$value'")
    }

    assertions()
  }

  private fun URLSearchParams.assertParams(
    count: Int = 0,
    valueCount: Int = 0,
    keys: Set<String>? = null,
    values: List<Pair<String, String>>? = null,
    mutable: Boolean = false,
  ) = assertParams(
    count,
    valueCount,
    keys,
    values,
    mutable,
  ) {
    // defaults
    assertNotNull(this)
  }

  @Test override fun testInjectable() {
    assertNotNull(urlIntrinsic)
    assertNotNull(urlSearchParamsIntrinsic)
  }

  @Test fun testBasicParseToMultimap() {
    val parsed = assertNotNull(parseParamsToMultiMap("hello=hi&cool=yo")).toMap()
    assertEquals(2, parsed.size)
    assertContains(parsed.keys, "hello")
    assertContains(parsed.keys, "cool")
    assertFalse(parsed.containsKey("other"))
    assertEquals("hi", parsed["hello"])
    assertEquals("yo", parsed["cool"])
  }

  @Test fun testBasicParseToMultimapGuest() {
    val parsed = assertNotNull(parseParamsFromGuest(asValue("hello=hi&cool=yo"))).toMap()
    assertEquals(2, parsed.size)
    assertContains(parsed.keys, "hello")
    assertContains(parsed.keys, "cool")
    assertFalse(parsed.containsKey("other"))
    assertEquals("hi", parsed["hello"])
    assertEquals("yo", parsed["cool"])
  }

  @Test fun testBasicMergeFoldQueryParams() {
    val parsed = assertNotNull(parseParamsToMultiMap("hello=hi&cool=yo"))
    val map = assertNotNull(mergeFoldQueryParams(2, parsed))
    assertEquals(2, map.size)
    assertContains(map.keys, "hello")
    assertContains(map.keys, "cool")
    assertFalse(map.containsKey("other"))
    assertEquals("hi", map["hello"]?.first())
    assertEquals("yo", map["cool"]?.first())
  }

  @Test fun testMultiParamMergeFold() {
    val parsed = assertNotNull(parseParamsToMultiMap("hello=hi&cool=yo&hello=bye"))
    val map = assertNotNull(mergeFoldQueryParams(2, parsed))
    assertEquals(2, map.size)
    assertContains(map.keys, "hello")
    assertContains(map.keys, "cool")
    assertFalse(map.containsKey("other"))
    assertEquals(2, map["hello"]?.size)
    assertEquals("hi", map["hello"]?.get(0))
    assertEquals("bye", map["hello"]?.get(1))
    assertEquals("yo", map["cool"]?.first())
  }

  @Test fun testBuildParamMapFromHostString() {
    val map = assertNotNull(buildQueryParamsFromString("hello=hi&cool=yo"))
    assertEquals(2, map.size)
    assertContains(map.keys, "hello")
    assertContains(map.keys, "cool")
    assertFalse(map.containsKey("other"))
    assertEquals("hi", map["hello"]?.first())
    assertEquals("yo", map["cool"]?.first())
  }

  @Test fun testBuildParamMapFromGuestString() {
    val map = assertNotNull(buildQueryParamsFromGuest(asValue("hello=hi&cool=yo")))
    assertEquals(2, map.size)
    assertContains(map.keys, "hello")
    assertContains(map.keys, "cool")
    assertFalse(map.containsKey("other"))
    assertEquals("hi", map["hello"]?.first())
    assertEquals("yo", map["cool"]?.first())
  }

  @Test fun testParseImmutableFromString() {
    val parsed = assertNotNull("hello=hi&cool=yo".urlParams())
    assertEquals(2, parsed.size)
    assertContains(parsed.keys, "hello")
    assertContains(parsed.keys, "cool")
    assertFalse(parsed.containsKey("other"))
    assertEquals("hi", parsed["hello"])
    assertEquals("yo", parsed["cool"])
    assertIs<URLSearchParams>(parsed)
    assertIs<MultiMapLike<String, String>>(parsed)

    parsed.assertParams(
      count = 2,
      keys = setOf("hello", "cool"),
      values = listOf("hello" to "hi", "cool" to "yo"),
      mutable = false,
    )
  }

  @Test fun testParseMutableFromString() {
    val parsed = assertNotNull("hello=hi&cool=yo".mutableUrlParams())
    assertEquals(2, parsed.size)
    assertContains(parsed.keys, "hello")
    assertContains(parsed.keys, "cool")
    assertFalse(parsed.containsKey("other"))
    assertEquals("hi", parsed["hello"])
    assertEquals("yo", parsed["cool"])
    assertIs<URLSearchParams>(parsed)
    assertIs<MutableURLSearchParams>(parsed)
    assertIs<MultiMapLike<String, String>>(parsed)

    parsed.assertParams(
      count = 2,
      keys = setOf("hello", "cool"),
      values = listOf("hello" to "hi", "cool" to "yo"),
      mutable = true,
    )
  }

  @CsvSource(
    "hello=hi&cool=yo, 2, 2",
    "hello=hi&cool=yo&hello=bye, 2, 3",
    "hello=hi&cool=yo&sample=yes, 3, 3",
    "hello=hi&cool=yo&sample=yes&hello=bye, 3, 4",
    "hello=hi&cool=yo&sample=yes&hello=bye&cool=yo, 3, 5",
    "test=test&test2=test2&test3=test3&test4=test4, 4, 4",
    "test=test&test2=test2&test3=test3&test4=test4&test5=test5, 5, 5",
    "test=test&test2=test2&test3=test3&test4=test4&test5=test5&test6=test6, 6, 6",
  )
  @ParameterizedTest
  fun `URLSearchParameter(String) well-formed parse tests`(input: String, expectedCount: Int, expectedValues: Int) {
    val parsed = assertNotNull(input.urlParams(), "`urlParams` parse should not return `null`")
    val mutable = assertNotNull(input.mutableUrlParams(), "`mutableUrlParams` parse should not return `null`")

    parsed.assertParams(count = expectedCount, valueCount = expectedValues, mutable = false)
    mutable.assertParams(count = expectedCount, valueCount = expectedValues, mutable = true)
  }

  @CsvSource(
    "=hi&cool=yo&hey=, 2, 2",
    "hello=hi&cool=yo&=, 2, 2",
    "hello=hi&cool=yo&=yo, 2, 2",
    "hello=hi&cool=yo&=yo&=, 2, 2",
  )
  @ParameterizedTest
  fun `URLSearchParameter(String) malformed parse tests`(input: String, expectedCount: Int, expectedValues: Int) {
    val parsed = assertNotNull(input.urlParams(), "`urlParams` parse should not return `null`")
    val mutable = assertNotNull(input.mutableUrlParams(), "`mutableUrlParams` parse should not return `null`")

    parsed.assertParams(count = expectedCount, valueCount = expectedValues, mutable = false)
    mutable.assertParams(count = expectedCount, valueCount = expectedValues, mutable = true)
  }

  @Test fun testParseDiscardsNonUtf8Chars() {
    val parsed = assertNotNull("hello=hi&cool=yo&\uD83C\uDFC6=smile".urlParams())
    assertEquals(2, parsed.size)
    assertContains(parsed.keys, "hello")
    assertContains(parsed.keys, "cool")
    assertFalse(parsed.containsKey("other"))
    assertEquals("hi", parsed["hello"])
    assertEquals("yo", parsed["cool"])
  }

  @Test fun testSortDoesNotThrow() {
    val parsed = assertNotNull("hello=hi&cool=yo".urlParams())
    assertDoesNotThrow { parsed.sort() }
    val mutable = assertNotNull("hello=hi&cool=yo".mutableUrlParams())
    assertDoesNotThrow { mutable.sort() }
  }

  @Test fun testHostImmutableToString() {
    val parsed = assertNotNull("hello=hi&cool=yo".urlParams())
    val parsedToString = assertNotNull(parsed.toString())
    assertTrue("immutable" in parsedToString)
    assertTrue("2" in parsedToString)
    assertTrue("URLSearchParams(" in parsedToString)
    assertTrue(parsedToString.startsWith("URLSearchParams("))
  }

  @Test fun testHostMutableToString() {
    val parsed = assertNotNull("hello=hi&cool=yo".urlParams())
    val parsedToString = assertNotNull(parsed.toString())
    assertTrue("mutable" in parsedToString)
    assertTrue("2" in parsedToString)
    assertTrue("URLSearchParams(" in parsedToString)
    assertTrue(parsedToString.startsWith("URLSearchParams("))
  }

  @CsvSource(
    "hello=hi&cool=yo, 2, 2",
    "hello=hi&cool=yo&hello=bye, 2, 3",
    "hello=hi&cool=yo&sample=yes, 3, 3",
    "hello=hi&cool=yo&sample=yes&hello=bye, 3, 4",
    "hello=hi&cool=yo&sample=yes&hello=bye&cool=yo, 3, 5",
    "test=test&test2=test2&test3=test3&test4=test4, 4, 4",
    "test=test&test2=test2&test3=test3&test4=test4&test5=test5, 5, 5",
    "test=test&test2=test2&test3=test3&test4=test4&test5=test5&test6=test6, 6, 6",
  )
  @ParameterizedTest
  fun `URLSearchParameter(String) constructor tests`(input: String, expectedCount: Int, expectedValues: Int) {
    // construct host-side with string
    assertNotNull(input.urlParams()).assertParams(
      count = expectedCount,
      valueCount = expectedValues,
      mutable = false,
    )
    assertNotNull(input.mutableUrlParams()).assertParams(
      count = expectedCount,
      valueCount = expectedValues,
      mutable = true,
    )

    // construct guest-side with string
    val parsed = assertNotNull(URLSearchParamsIntrinsic.URLSearchParams(asValue(input)))
    parsed.assertParams(count = expectedCount, valueCount = expectedValues, mutable = false)

    // construct guest-side with string (mutable)
    val mutable = assertNotNull(URLSearchParamsIntrinsic.MutableURLSearchParams(asValue(input)))
    mutable.assertParams(count = expectedCount, valueCount = expectedValues, mutable = true)

    // construct guest-side with string (immutable from mutable)
    val immutable = assertNotNull(URLSearchParamsIntrinsic.URLSearchParams(mutable))
    immutable.assertParams(count = expectedCount, valueCount = expectedValues, mutable = false)

    // construct with other URLSearchParams
    val other = assertNotNull(URLSearchParamsIntrinsic.URLSearchParams(parsed))
    assertNotSame(parsed, other)
    assertEquals(parsed.size, other.size)
    other.assertParams(count = expectedCount, valueCount = expectedValues, mutable = false)

    // construct with other URLSearchParams
    val otherMutable = assertNotNull(URLSearchParamsIntrinsic.MutableURLSearchParams(mutable))
    assertNotSame(parsed, other)
    assertEquals(parsed.size, other.size)
    otherMutable.assertParams(count = expectedCount, valueCount = expectedValues, mutable = true)
  }

  @Test fun testNullConstructor() {
    val assertEmpty: URLSearchParams.() -> Unit = {
      assertTrue(isEmpty())
      assertEquals(0, size)
    }
    assertNotNull(URLSearchParamsIntrinsic.URLSearchParams(null)).assertEmpty()
    assertNotNull(URLSearchParamsIntrinsic.MutableURLSearchParams(null)).assertEmpty()
    assertNotNull(URLSearchParamsIntrinsic.URLSearchParams(asValue(null))).assertEmpty()
    assertNotNull(URLSearchParamsIntrinsic.MutableURLSearchParams(asValue(null))).assertEmpty()
    assertNotNull(URLSearchParamsIntrinsic.URLSearchParams()).assertEmpty()
    assertNotNull(URLSearchParamsIntrinsic.MutableURLSearchParams()).assertEmpty()
  }

  @Test fun testInvalidConstructor() {
    assertThrows<TypeError> {
      URLSearchParamsIntrinsic.URLSearchParams(123)
    }
    assertThrows<TypeError> {
      URLSearchParamsIntrinsic.URLSearchParams(asValue(123))
    }
    assertThrows<TypeError> {
      URLSearchParamsIntrinsic.MutableURLSearchParams(123)
    }
    assertThrows<TypeError> {
      URLSearchParamsIntrinsic.MutableURLSearchParams(asValue(123))
    }
  }

  @Test fun testHostMultimap() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertEquals(2, map.size)
    assertContains(map.keys, "hello")
    assertContains(map.keys, "cool")
    assertFalse(map.containsKey("other"))
    assertEquals("hi", map["hello"])
    assertEquals("yo", map["cool"])
    val all = map.getAll("hello")
    assertEquals(2, all.size)
    assertEquals("hi", all[0])
    assertEquals("again", all[1])
  }

  @Test fun testGuestMultimap() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertEquals(2, map.size)
    assertContains(map.keys, "hello")
    assertContains(map.keys, "cool")
    assertFalse(map.containsKey("other"))
    assertEquals("hi", map["hello"])
    assertEquals("yo", map["cool"])
    val getAllMethod = map.getMember("getAll")
    assertIs<ProxyExecutable>(getAllMethod, "`getAll` should be `ProxyExecutable`")
    val all = getAllMethod.execute(asValue("hello"))
    assertIs<List<String>>(all, "`getAll` should return list")
    assertEquals(2, all.size)
    assertEquals("hi", all[0])
    assertEquals("again", all[1])
  }

  @Test fun testInvalidKeysShouldThrow() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertThrows<IllegalStateException> {
      map.getMember("randomMemberThatDoesNotExist")
    }
  }

  @Test fun testHasInvalidKeyShouldThrow() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertThrows<TypeError> {
      val has = map.getMember("has")
      assertIs<ProxyExecutable>(has, "`has` should be `ProxyExecutable`")
      has.execute()
    }
  }

  @Test fun testGetInvalidKeyShouldThrow() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertThrows<TypeError> {
      val getter = map.getMember("get")
      assertIs<ProxyExecutable>(getter, "`get` should be `ProxyExecutable`")
      getter.execute()
    }
  }

  @Test fun testGetAllInvalidKeyShouldThrow() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertThrows<TypeError> {
      val getter = map.getMember("getAll")
      assertIs<ProxyExecutable>(getter, "`getAll` should be `ProxyExecutable`")
      getter.execute()
    }
  }

  @Test fun testHasNullKeyShouldThrow() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertThrows<TypeError> {
      val has = map.getMember("has")
      assertIs<ProxyExecutable>(has, "`has` should be `ProxyExecutable`")
      has.execute(asValue(null))
    }
  }

  @Test fun testGetNullKeyShouldThrow() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertThrows<TypeError> {
      val getter = map.getMember("get")
      assertIs<ProxyExecutable>(getter, "`get` should be `ProxyExecutable`")
      getter.execute(asValue(null))
    }
  }

  @Test fun testGetAllNullKeyShouldThrow() {
    val map = assertNotNull("hello=hi&cool=yo&hello=again".urlParams())
    assertThrows<TypeError> {
      val getter = map.getMember("getAll")
      assertIs<ProxyExecutable>(getter, "`getAll` should be `ProxyExecutable`")
      getter.execute(asValue(null))
    }
  }

  @Test fun testURLSearchParamsSymbolPresentGlobally() = dual {
    // no-op
  }.guest {
    // language=javascript
    """
      test(URLSearchParams).isNotNull();
    """
  }
}
