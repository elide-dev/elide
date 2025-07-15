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
package elide.runtime.node

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyNativeObject
import org.graalvm.polyglot.proxy.ProxyObject
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import elide.annotations.Context
import elide.annotations.Inject
import elide.runtime.node.querystring.NodeQuerystringModule
import elide.runtime.intrinsics.js.node.querystring.QueryParams
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `querystring` built-in module. */
@TestCase internal class NodeQuerystringTest : NodeModuleConformanceTest<NodeQuerystringModule>() {
  @Inject private lateinit var querystring: NodeQuerystringModule

  override val moduleName: String get() = "querystring"
  override fun provide(): NodeQuerystringModule = NodeQuerystringModule()

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("decode")
    yield("encode")
    yield("escape")
    yield("parse")
    yield("stringify")
    yield("unescape")
  }

  @Test override fun testInjectable() {
    assertNotNull(querystring, "should be able to inject instance of querystring module")
  }

  @Test fun `escape should encode strings correctly`() = conforms {
    val qs = querystring.provide()

    // Basic string encoding
    assertEquals("5", qs.escape(Value.asValue(5)))
    assertEquals("test", qs.escape(Value.asValue("test")))

    // Objects should be encoded using toString
    assertEquals("%5Bobject%20Object%5D", qs.escape(Value.asValue(ProxyObject.fromMap(emptyMap<String, Any>()))))
//    assertEquals("5%2C10", qs.escape(Value.asValue(ProxyArray.fromList(listOf(5,10)))))

    // Unicode characters should be percent-encoded
    assertEquals("%C5%8A%C5%8D%C4%91%C4%95", qs.escape(Value.asValue("Ŋōđĕ")))
    assertEquals("test%C5%8A%C5%8D%C4%91%C4%95", qs.escape(Value.asValue("testŊōđĕ")))

    // Surrogate pairs should be encoded
    // assertEquals("%F0%90%91%B4est", qs.escape(Value.asValue("${String(Character.toChars(0xD800 + 1))}test")))
  }.guest {
    // language=javascript
    """
        const { equal, throws } = require('assert');
        const qs = require('querystring');

        equal(qs.escape(5), '5');
        equal(qs.escape('test'), 'test');
        equal(qs.escape({}), '%5Bobject%20Object%5D');
        equal(qs.escape([5, 10]), '5%2C10');
        equal(qs.escape('Ŋōđĕ'), '%C5%8A%C5%8D%C4%91%C4%95');
        equal(qs.escape('testŊōđĕ'), 'test%C5%8A%C5%8D%C4%91%C4%95');
        
//        equal(qs.escape(`${'$'}{String.fromCharCode(0xD800 + 1)}test`),
//                   '%F0%90%91%B4est');
//
//        throws(
//          () => qs.escape(String.fromCharCode(0xD800 + 1)),
//          {
//            code: 'ERR_INVALID_URI',
//            name: 'URIError',
//            message: 'URI malformed'
//          }
//        );
        
        // Using toString for objects
        equal(
          qs.escape({ test: 5, toString: () => 'test', valueOf: () => 10 }),
          'test'
        );
        
        // `toString` is not callable, must throw an error.
        // Error message will vary between different JavaScript engines, so only check
        // that it is a `TypeError`.
        throws(() => qs.escape({ toString: 5 }), TypeError);
        
        // Should use valueOf instead of non-callable toString.
        equal(qs.escape({ toString: 5, valueOf: () => 'test' }), 'test');
        
        // Error message will vary between different JavaScript engines, so only check
        // that it is a `TypeError`.
//        throws(() => qs.escape(Symbol('test')), TypeError);
    """
  }

  @Test fun `unescape should decode strings correctly`() = conforms {
    val qs = querystring.provide()
    // Basic string with nothing to unescape
    assertEquals(
      "there is nothing to unescape here",
      qs.unescape(Value.asValue("there is nothing to unescape here")),
    )

    // Multiple spaces that need to be unescaped
    assertEquals(
      "there are several spaces that need to be unescaped",
      qs.unescape(Value.asValue("there%20are%20several%20spaces%20that%20need%20to%20be%20unescaped")),
    )

    // Malformed percent sequences should be left as-is
    assertEquals(
      "there%2Qare%0-fake%escaped values in%%%%this%9Hstring",
      qs.unescape(Value.asValue("there%2Qare%0-fake%escaped values in%%%%this%9Hstring")),
    )

    // Full range of percent-encoded characters
    assertEquals(
      " !\"#$%&'()*+,-./01234567",
      qs.unescape(Value.asValue("%20%21%22%23%24%25%26%27%28%29%2A%2B%2C%2D%2E%2F%30%31%32%33%34%35%36%37")),
    )

    // Double percent sequences
    assertEquals("%*", qs.unescape(Value.asValue("%%2a")))

    // Mixed percent sequences
    assertEquals("%2sf*", qs.unescape(Value.asValue("%2sf%2a")))

    // Multiple mixed percent sequences
    assertEquals("%2*f*", qs.unescape(Value.asValue("%2%2af%2a")))
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // Basic string with nothing to unescape
        equal(qs.unescape('there is nothing to unescape here'), 'there is nothing to unescape here');
        
        // Multiple spaces that need to be unescaped
        equal(qs.unescape('there%20are%20several%20spaces%20that%20need%20to%20be%20unescaped'), 'there are several spaces that need to be unescaped');
        
        // Malformed percent sequences should be left as-is
        equal(qs.unescape('there%2Qare%0-fake%escaped values in%%%%this%9Hstring'), 'there%2Qare%0-fake%escaped values in%%%%this%9Hstring');
        
        // Full range of percent-encoded characters
        equal(qs.unescape('%20%21%22%23%24%25%26%27%28%29%2A%2B%2C%2D%2E%2F%30%31%32%33%34%35%36%37'), ' !"#$%&\'()*+,-./01234567');
        
        // Double percent sequences
        equal(qs.unescape('%%2a'), '%*');
        
        // Mixed percent sequences
        equal(qs.unescape('%2sf%2a'), '%2sf*');
        
        // Multiple mixed percent sequences
        equal(qs.unescape('%2%2af%2a'), '%2*f*');
    """
  }

  @Test fun `parse should handle basic parsing`() = conforms {
    val qs = querystring.provide()
    // Proto property handling
    val result1 = qs.parse(Value.asValue("__proto__=1"))

    assertEquals("1", result1.getMember("__proto__").toString())

    // Multiple values for same key
    val result2 = qs.parse(Value.asValue("foo=bar&foo=quux"))
    val fooArray = result2.getMember("foo") as ProxyArray
    assertEquals(2, fooArray.getSize())
    assertEquals("bar", fooArray.get(0))
    assertEquals("quux", fooArray.get(1))

    // URL encoded values
    val result3 = qs.parse(Value.asValue("my+weird+field=q1%212%22%27w%245%267%2Fz8%29%3F"))
    assertEquals("q1!2\"'w$5&7/z8)?", result3.getMember("my weird field").toString())
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // Proto property handling
        const result1 = qs.parse('__proto__=1');
        equal(result1['__proto__'], '1');
        
        // Multiple values for same key
        const result2 = qs.parse('foo=bar&foo=quux');
        equal(Array.isArray(result2.foo), true);
        equal(result2.foo.length, 2);
        equal(result2.foo[0], 'bar');
        equal(result2.foo[1], 'quux');
        
        // URL encoded values
        const result3 = qs.parse('my+weird+field=q1%212%22%27w%245%267%2Fz8%29%3F');
        equal(result3['my weird field'], 'q1!2"\'w$5&7/z8)?');
    """
  }

  @Test fun `parse should handle empty values and whitespace`() = conforms {
    val qs = querystring.provide()
    // Empty values
    val result1 = qs.parse(Value.asValue("str=foo&arr=1&arr=2&arr=3&somenull=&undef="))
    assertEquals("foo", result1.getMember("str").toString())
    val arrArray = result1.getMember("arr") as ProxyArray
    assertEquals(3, arrArray.getSize())
    assertEquals("1", arrArray.get(0))
    assertEquals("2", arrArray.get(1))
    assertEquals("3", arrArray.get(2))
    assertEquals("", result1.getMember("somenull").toString())
    assertEquals("", result1.getMember("undef").toString())

    // Whitespace handling
    val result2 = qs.parse(Value.asValue(" foo = bar "))
    assertEquals(" bar ", result2.getMember(" foo ").toString())
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // Empty values
        const result1 = qs.parse('str=foo&arr=1&arr=2&arr=3&somenull=&undef=');
        equal(result1.str, 'foo');
        equal(Array.isArray(result1.arr), true);
        equal(result1.arr.length, 3);
        equal(result1.arr[0], '1');
        equal(result1.arr[1], '2');
        equal(result1.arr[2], '3');
        equal(result1.somenull, '');
        equal(result1.undef, '');
        
        // Whitespace handling
        const result2 = qs.parse(' foo = bar ');
        equal(result2[' foo '], ' bar ');
    """
  }

  @Test fun `parse should handle nested parsing and custom separators`() = conforms {
    val qs = querystring.provide()
    // Nested parsing
    val result1 = qs.parse(Value.asValue("a=b&q=x%3Dy%26y%3Dz"))
    assertEquals("b", result1.getMember("a").toString())
    assertEquals("x=y&y=z", result1.getMember("q").toString())

    // Parse the nested query string
    val nested = qs.parse(Value.asValue("x=y&y=z"))
    assertEquals("y", nested.getMember("x").toString())
    assertEquals("z", nested.getMember("y").toString())
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // Nested parsing
        const result = qs.parse('a=b&q=x%3Dy%26y%3Dz');
        equal(result.a, 'b');
        equal(result.q, 'x=y&y=z');
        
        // Parse the nested query string
        const nested = qs.parse(result.q);
        equal(nested.x, 'y');
        equal(nested.y, 'z');
    """
  }

  @Test fun `decode should call parse properly`() = conforms {
    val qs = querystring.provide()
    // decode is an alias for parse, so behavior should be identical
    val parseResult = qs.parse(Value.asValue("foo=bar&baz=qux"))
    val decodeResult = qs.decode(Value.asValue("foo=bar&baz=qux"))

    assertEquals("bar", parseResult.getMember("foo").toString())
    assertEquals("qux", parseResult.getMember("baz").toString())
    assertEquals("bar", decodeResult.getMember("foo").toString())
    assertEquals("qux", decodeResult.getMember("baz").toString())

    // Test with URL encoded values
    val parseEncoded = qs.parse(Value.asValue("my+field=hello%20world"))
    val decodeEncoded = qs.decode(Value.asValue("my+field=hello%20world"))

    assertEquals("hello world", parseEncoded.getMember("my field").toString())
    assertEquals("hello world", decodeEncoded.getMember("my field").toString())
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // decode is an alias for parse, so behavior should be identical
        const parseResult = qs.parse('foo=bar&baz=qux');
        const decodeResult = qs.decode('foo=bar&baz=qux');
        
        equal(parseResult.foo, 'bar');
        equal(parseResult.baz, 'qux');
        equal(decodeResult.foo, 'bar');
        equal(decodeResult.baz, 'qux');
        
        // Test with URL encoded values
        const parseEncoded = qs.parse('my+field=hello%20world');
        const decodeEncoded = qs.decode('my+field=hello%20world');
        
        equal(parseEncoded['my field'], 'hello world');
        equal(decodeEncoded['my field'], 'hello world');
    """
  }

  @Test fun `stringify should handle basic objects`() = conforms {
    val qs = querystring.provide()
    // Basic object stringification
    val obj1 = Value.asValue(mapOf("foo" to "bar", "baz" to "qux"))
    assertEquals("foo=bar&baz=qux", qs.stringify(obj1))

    // Single key-value pair
    val obj2 = Value.asValue(mapOf("key" to "value"))
    assertEquals("key=value", qs.stringify(obj2))

    // Numbers and booleans
    val obj3 = Value.asValue(mapOf("num" to 42, "bool" to true))
    assertEquals("num=42&bool=true", qs.stringify(obj3))
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // Basic object stringification
        equal(qs.stringify({ foo: 'bar', baz: 'qux' }), 'foo=bar&baz=qux');
        
        // Single key-value pair
        equal(qs.stringify({ key: 'value' }), 'key=value');
        
        // Numbers and booleans
        equal(qs.stringify({ num: 42, bool: true }), 'num=42&bool=true');
    """
  }

  @Test fun `stringify should handle arrays`() = conforms {
    val qs = querystring.provide()

    val obj1 = Value.asValue(mapOf("arr" to arrayOf("1", "2", "3")))
    assertEquals("arr=1&arr=2&arr=3", qs.stringify(obj1))

    // Mixed arrays
    val obj2 = Value.asValue(mapOf("mixed" to arrayOf<Any>("foo", 42, true)))
    assertEquals("mixed=foo&mixed=42&mixed=true", qs.stringify(obj2))

    // Empty array
    val obj3 = Value.asValue(mapOf("empty" to emptyArray<String>()))
    assertEquals("", qs.stringify(obj3))
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // Array values
        equal(qs.stringify({ arr: ['1', '2', '3'] }), 'arr=1&arr=2&arr=3');
        
        // Mixed arrays
        equal(qs.stringify({ mixed: ['foo', 42, true] }), 'mixed=foo&mixed=42&mixed=true');
        
        // Empty array
        equal(qs.stringify({ empty: [] }), '');
    """
  }

  @Test fun `stringify should handle special characters and encoding`() = conforms {
    val qs = querystring.provide()
    // Special characters that need encoding
    val obj1 = Value.asValue(mapOf("my weird field" to "q1!2\"'w$5&7/z8)?"))
    assertEquals("my%20weird%20field=q1!2%22'w%245%267%2Fz8)%3F", qs.stringify(obj1))

    // Unicode characters
    val obj2 = Value.asValue(mapOf("unicode" to "Ŋōđĕ"))
    assertEquals("unicode=%C5%8A%C5%8D%C4%91%C4%95", qs.stringify(obj2))

    // Proto property
    val obj3 = Value.asValue(mapOf("__proto__" to "1"))
    assertEquals("__proto__=1", qs.stringify(obj3))
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // Special characters that need encoding
        equal(qs.stringify({ 'my weird field': 'q1!2"\'w$5&7/z8)?' }), 'my%20weird%20field=q1!2%22\'w%245%267%2Fz8)%3F');
        
        // Unicode characters
        equal(qs.stringify({ unicode: 'Ŋōđĕ' }), 'unicode=%C5%8A%C5%8D%C4%91%C4%95');
        
        // Proto property
        equal(qs.stringify({ __proto__: '1' }), '');
    """
  }

  @Test fun `stringify should handle null and empty values`() = conforms {
    val qs = querystring.provide()
    // Empty strings
    val obj1 = Value.asValue(mapOf("empty" to "", "another" to "value"))
    assertEquals("empty=&another=value", qs.stringify(obj1))

    // Non-object inputs should return empty string
    assertEquals("", qs.stringify(Value.asValue("not an object")))
    assertEquals("", qs.stringify(Value.asValue(42)))
    assertEquals("", qs.stringify(Value.asValue(true)))
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // Empty strings
        equal(qs.stringify({ empty: '', another: 'value' }), 'empty=&another=value');
        
        // Non-object inputs should return empty string
        equal(qs.stringify('not an object'), '');
        equal(qs.stringify(42), '');
        equal(qs.stringify(true), '');
    """
  }

  @Test fun `stringify should handle custom separators`() = conforms {
    val qs = querystring.provide()
    // Custom separator and equals
    val obj = Value.asValue(mapOf("foo" to "bar", "baz" to "qux"))
    assertEquals("foo:bar;baz:qux", qs.stringify(obj, Value.asValue(";"), Value.asValue(":")))

    // Custom separator only
    assertEquals("foo=bar|baz=qux", qs.stringify(obj, Value.asValue("|"), null))

    // Custom equals only
    assertEquals("foo-bar&baz-qux", qs.stringify(obj, null, Value.asValue("-")))
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        const obj = { foo: 'bar', baz: 'qux' };
        
        // Custom separator and equals
        equal(qs.stringify(obj, ';', ':'), 'foo:bar;baz:qux');
        
        // Custom separator only
        equal(qs.stringify(obj, '|'), 'foo=bar|baz=qux');
        
        // Custom equals only
        equal(qs.stringify(obj, '&', '-'), 'foo-bar&baz-qux');
    """
  }

  @Test fun `encode should call stringify properly`() = conforms {
    val qs = querystring.provide()
    // encode is an alias for stringify, so behavior should be identical
    val obj = Value.asValue(mapOf("foo" to "bar", "baz" to "qux"))
    val stringifyResult = qs.stringify(obj)
    val encodeResult = qs.encode(obj)

    assertEquals("foo=bar&baz=qux", stringifyResult)
    assertEquals("foo=bar&baz=qux", encodeResult)

    // Test with array values
    val objWithArray = Value.asValue(mapOf("arr" to arrayOf("1", "2", "3")))
    val stringifyArray = qs.stringify(objWithArray)
    val encodeArray = qs.encode(objWithArray)

    assertEquals("arr=1&arr=2&arr=3", stringifyArray)
    assertEquals("arr=1&arr=2&arr=3", encodeArray)
  }.guest {
    // language=javascript
    """
        const { equal } = require('assert');
        const qs = require('querystring');

        // encode is an alias for stringify, so behavior should be identical
        const obj = { foo: 'bar', baz: 'qux' };
        equal(qs.stringify(obj), 'foo=bar&baz=qux');
        equal(qs.encode(obj), 'foo=bar&baz=qux');
        
        // Test with array values
        const objWithArray = { arr: ['1', '2', '3'] };
        equal(qs.stringify(objWithArray), 'arr=1&arr=2&arr=3');
        equal(qs.encode(objWithArray), 'arr=1&arr=2&arr=3');
    """
  }
}
