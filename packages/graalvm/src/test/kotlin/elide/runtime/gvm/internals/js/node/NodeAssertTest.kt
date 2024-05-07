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
@file:Suppress("EmptyFunctionBlock", "LargeClass", "LongMethod")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.js.node

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.Value.asValue
import org.graalvm.polyglot.proxy.ProxyObject
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.node.asserts.NodeAssertModule
import elide.runtime.gvm.internals.node.asserts.NodeAssertionError
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.AssertAPI
import elide.testing.annotations.TestCase

/** Tests for the built-in `assert` module. */
@TestCase internal class NodeAssertTest @Inject constructor(internal val assert: AssertAPI)
  : NodeModuleConformanceTest<NodeAssertModule>() {
  override val moduleName: String get() = "assert"
  override fun provide(): NodeAssertModule = NodeAssertModule()

  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("AssertionError")
    yield("ok")
    yield("fail")
    yield("strict")
    yield("deepEqual")
    yield("deepStrictEqual")
    yield("notDeepEqual")
    yield("notDeepStrictEqual")
    yield("match")
    yield("doesNotMatch")
    yield("throws")
    yield("doesNotThrow")
    yield("rejects")
    yield("doesNotReject")
    yield("ifError")
  }

  @Test override fun testInjectable() {
    assertNotNull(assert)
  }

  private val objectWithProperty = object: ProxyObject {
    override fun getMemberKeys(): Array<String> = arrayOf("foo")
    override fun getMember(key: String): Value? = when (key) {
      "foo" -> asValue("bar")
      else -> null
    }
    override fun hasMember(key: String): Boolean = key == "foo"
    override fun putMember(key: String, value: Value?) {}
  }

  private val emptyObject = object: ProxyObject {
    override fun getMemberKeys(): Array<String> = arrayOf()
    override fun getMember(key: String): Value? = null
    override fun hasMember(key: String): Boolean = false
    override fun putMember(key: String, value: Value?) = Unit
  }

  @TestFactory fun `ok() should behave as expected for truthy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("ok(true) should pass") { assert.ok(true) })
    yield(dynamicTest("ok(1) should pass") { assert.ok(1) })
    yield(dynamicTest("ok(1L) should pass") { assert.ok(1L) })
    yield(dynamicTest("ok(1.0) should pass") { assert.ok(1.0) })
    yield(dynamicTest("ok(1.0f) should pass") { assert.ok(1.0f) })
    yield(dynamicTest("ok(\"hi\") should pass") { assert.ok("hi") })
    yield(dynamicTest("ok([\"hi\"]) should pass") { assert.ok(arrayOf("hi")) })
    yield(dynamicTest("ok(listOf(\"hi\")) should pass") { assert.ok(listOf("hi")) })
    yield(dynamicTest("ok({\"foo\": \"bar\"}) should pass") { assert.ok(objectWithProperty) })
    yield(dynamicTest("ok(mapOf(\"foo\" to \"bar\")) should pass") { assert.ok(mapOf("foo" to "bar")) })
  }.asStream()

  @TestFactory fun `assert() should behave as expected for truthy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("assert(true) should pass") { assert.assert(true) })
    yield(dynamicTest("assert(1) should pass") { assert.assert(1) })
    yield(dynamicTest("assert(1L) should pass") { assert.assert(1L) })
    yield(dynamicTest("assert(1.0) should pass") { assert.assert(1.0) })
    yield(dynamicTest("assert(1.0f) should pass") { assert.assert(1.0f) })
    yield(dynamicTest("assert(\"hi\") should pass") { assert.assert("hi") })
    yield(dynamicTest("assert([\"hi\"]) should pass") { assert.assert(arrayOf("hi")) })
    yield(dynamicTest("assert(listOf(\"hi\")) should pass") { assert.assert(listOf("hi")) })
    yield(dynamicTest("assert({\"foo\": \"bar\"}) should pass") { assert.assert(objectWithProperty) })
    yield(dynamicTest("assert(mapOf(\"foo\" to \"bar\")) should pass") {
      assert.assert(mapOf("foo" to "bar"))
    })
  }.asStream()

  @TestFactory fun `ok() should behave as expected for guest truthy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("ok(true) should pass") { assert.ok(asValue(true)) })
    yield(dynamicTest("ok(1) should pass") { assert.ok(asValue(1)) })
    yield(dynamicTest("ok(1L) should pass") { assert.ok(asValue(1L)) })
    yield(dynamicTest("ok(1.0) should pass") { assert.ok(asValue(1.0)) })
    yield(dynamicTest("ok(1.0f) should pass") { assert.ok(asValue(1.0f)) })
    yield(dynamicTest("ok(\"hi\") should pass") { assert.ok(asValue("hi")) })
    yield(dynamicTest("ok([\"hi\"]) should pass") { assert.ok(asValue(arrayOf("hi"))) })
    yield(dynamicTest("ok(listOf(\"hi\")) should pass") { assert.ok(asValue(listOf("hi"))) })
    yield(dynamicTest("ok(mapOf(\"foo\" to \"bar\")) should pass") {
      assert.ok(asValue(mapOf("foo" to "bar")))
    })
  }.asStream()

  @TestFactory fun `assert() should behave as expected for guest truthy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("assert(true) should pass") { assert.assert(asValue(true)) })
    yield(dynamicTest("assert(1) should pass") { assert.assert(asValue(1)) })
    yield(dynamicTest("assert(1L) should pass") { assert.assert(asValue(1L)) })
    yield(dynamicTest("assert(1.0) should pass") { assert.assert(asValue(1.0)) })
    yield(dynamicTest("assert(1.0f) should pass") { assert.assert(asValue(1.0f)) })
    yield(dynamicTest("assert(\"hi\") should pass") { assert.assert(asValue("hi")) })
    yield(dynamicTest("assert([\"hi\"]) should pass") { assert.assert(asValue(arrayOf("hi"))) })
    yield(dynamicTest("assert(listOf(\"hi\")) should pass") { assert.assert(asValue(listOf("hi"))) })
    yield(dynamicTest("assert(mapOf(\"foo\" to \"bar\")) should pass") {
      assert.ok(asValue(mapOf("foo" to "bar")))
    })
  }.asStream()

  @TestFactory fun `ok() should behave as expected for falsy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("ok(false) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(false) }
    })
    yield(dynamicTest("ok(0) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(0) }
    })
    yield(dynamicTest("ok(-1) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(-1) }
    })
    yield(dynamicTest("ok(0L) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(0L) }
    })
    yield(dynamicTest("ok(-1L) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(-1L) }
    })
    yield(dynamicTest("ok(0.0) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(0.0) }
    })
    yield(dynamicTest("ok(-1.0) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(-1.0) }
    })
    yield(dynamicTest("ok(0.0f) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(0.0f) }
    })
    yield(dynamicTest("ok(-1.0f) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(-1.0f) }
    })
    yield(dynamicTest("ok(\"\") should fail") {
      assertThrows<NodeAssertionError> { assert.ok("") }
    })
    yield(dynamicTest("ok([]) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(arrayOf<String>()) }
    })
    yield(dynamicTest("ok(emptyList()) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(listOf<String>()) }
    })
    yield(dynamicTest("ok({}) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(emptyObject) }
    })
    yield(dynamicTest("ok(emptyMap()) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(emptyMap<String, String>()) }
    })
  }.asStream()

  @TestFactory fun `assert() should behave as expected for falsy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("assert(false) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(false) }
    })
    yield(dynamicTest("assert(0) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(0) }
    })
    yield(dynamicTest("assert(-1) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(-1) }
    })
    yield(dynamicTest("assert(0L) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(0L) }
    })
    yield(dynamicTest("assert(-1L) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(-1L) }
    })
    yield(dynamicTest("assert(0.0) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(0.0) }
    })
    yield(dynamicTest("assert(-1.0) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(-1.0) }
    })
    yield(dynamicTest("assert(0.0f) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(0.0f) }
    })
    yield(dynamicTest("assert(-1.0f) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(-1.0f) }
    })
    yield(dynamicTest("assert(\"\") should fail") {
      assertThrows<NodeAssertionError> { assert.assert("") }
    })
    yield(dynamicTest("assert([]) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(arrayOf<String>()) }
    })
    yield(dynamicTest("assert(emptyList()) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(listOf<String>()) }
    })
    yield(dynamicTest("assert({}) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(emptyObject) }
    })
    yield(dynamicTest("assert(emptyMap()) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(emptyMap<String, String>()) }
    })
  }.asStream()

  @TestFactory fun `ok() should behave as expected for guest falsy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("ok(false) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(false)) }
    })
    yield(dynamicTest("ok(0) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(0)) }
    })
    yield(dynamicTest("ok(-1) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(-1)) }
    })
    yield(dynamicTest("ok(0L) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(0L)) }
    })
    yield(dynamicTest("ok(-1L) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(-1L)) }
    })
    yield(dynamicTest("ok(0.0) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(0.0)) }
    })
    yield(dynamicTest("ok(-1.0) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(-1.0)) }
    })
    yield(dynamicTest("ok(0.0f) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(0.0f)) }
    })
    yield(dynamicTest("ok(-1.0f) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(-1.0f)) }
    })
    yield(dynamicTest("ok(\"\") should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue("")) }
    })
    yield(dynamicTest("ok([]) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(arrayOf<String>())) }
    })
    yield(dynamicTest("ok(emptyList()) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(listOf<String>())) }
    })
    yield(dynamicTest("ok({}) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(emptyObject)) }
    })
    yield(dynamicTest("ok(emptyMap()) should fail") {
      assertThrows<NodeAssertionError> { assert.ok(asValue(emptyMap<String, String>())) }
    })
  }.asStream()

  @TestFactory fun `assert() should behave as expected for guest falsy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("assert(false) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(false)) }
    })
    yield(dynamicTest("assert(0) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(0)) }
    })
    yield(dynamicTest("assert(-1) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(-1)) }
    })
    yield(dynamicTest("assert(0L) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(0L)) }
    })
    yield(dynamicTest("assert(-1L) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(-1L)) }
    })
    yield(dynamicTest("assert(0.0) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(0.0)) }
    })
    yield(dynamicTest("assert(-1.0) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(-1.0)) }
    })
    yield(dynamicTest("assert(0.0f) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(0.0f)) }
    })
    yield(dynamicTest("assert(-1.0f) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(-1.0f)) }
    })
    yield(dynamicTest("assert(\"\") should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue("")) }
    })
    yield(dynamicTest("assert([]) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(arrayOf<String>())) }
    })
    yield(dynamicTest("assert(emptyList()) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(listOf<String>())) }
    })
    yield(dynamicTest("assert({}) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(emptyObject)) }
    })
    yield(dynamicTest("assert(emptyMap()) should fail") {
      assertThrows<NodeAssertionError> { assert.assert(asValue(emptyMap<String, String>())) }
    })
  }.asStream()

  @Test fun `ok() passing guest tests`() = dual {
    assert.ok(true)
    assert.ok(1)
    assert.ok(1L)
    assert.ok(1.0)
    assert.ok(1.0f)
    assert.ok("hi")
    assert.ok(mapOf("hi" to "hello"))
    assert.ok(arrayOf("hi"))
    assert.ok(listOf("hi"))
  }.guest {
    // language=javascript
    """
      const { ok } = require("node:assert");
      ok(true);
      ok(1);
      ok(1.0);
      ok('hi');
      ok({"hi": "hello"});
      ok(['hi']);
    """
  }

  @Test fun `assert() passing guest tests`() = dual {
    assert.assert(true)
    assert.assert(1)
    assert.assert(1L)
    assert.assert(1.0)
    assert.assert(1.0f)
    assert.assert("hi")
    assert.assert(mapOf("hi" to "hello"))
    assert.assert(arrayOf("hi"))
    assert.assert(listOf("hi"))
  }.guest {
    // language=javascript
    """
      const { assert } = require("node:assert");
      assert(true);
      assert(1);
      assert(1.0);
      assert('hi');
      assert({"hi": "hello"});
      assert(['hi']);
    """
  }

  @Test @Disabled("TODO: Bug in GraalVM with default exports")
  fun `assert() passing guest tests (default entry)`() = dual {
    assert.assert(true)
    assert.assert(1)
    assert.assert(1L)
    assert.assert(1.0)
    assert.assert(1.0f)
    assert.assert("hi")
    assert.assert(mapOf("hi" to "hello"))
    assert.assert(arrayOf("hi"))
    assert.assert(listOf("hi"))
  }.guest {
    // language=javascript
    """
      const assert = require("node:assert");
      assert(true);
      assert(1);
      assert(1.0);
      assert('hi');
      assert({"hi": "hello"});
      assert(['hi']);
    """
  }

  @Test fun `ok() rejected guest tests`() = dual {
    assertThrows<NodeAssertionError> { assert.ok(false) }
    assertThrows<NodeAssertionError> { assert.ok(0) }
    assertThrows<NodeAssertionError> { assert.ok(0L) }
    assertThrows<NodeAssertionError> { assert.ok(0.0) }
    assertThrows<NodeAssertionError> { assert.ok(0.0f) }
    assertThrows<NodeAssertionError> { assert.ok("") }
    assertThrows<NodeAssertionError> { assert.ok(emptyMap<String, String>()) }
    assertThrows<NodeAssertionError> { assert.ok(emptyList<String>()) }
    assertThrows<NodeAssertionError> { assert.ok(emptyArray<String>()) }
  }.guest {
    // language=javascript
    """
      const { throws, ok } = require("node:assert");
      throws(() => ok(false));
      throws(() => ok(0));
      throws(() => ok(0.0));
      throws(() => ok(''));
      throws(() => ok({}));
      throws(() => ok([]));
    """
  }

  @Test fun `assert() rejected guest tests`() = dual {
    assertThrows<NodeAssertionError> { assert.assert(false) }
    assertThrows<NodeAssertionError> { assert.assert(0) }
    assertThrows<NodeAssertionError> { assert.assert(0L) }
    assertThrows<NodeAssertionError> { assert.assert(0.0) }
    assertThrows<NodeAssertionError> { assert.assert(0.0f) }
    assertThrows<NodeAssertionError> { assert.assert("") }
    assertThrows<NodeAssertionError> { assert.assert(emptyMap<String, String>()) }
    assertThrows<NodeAssertionError> { assert.assert(emptyList<String>()) }
    assertThrows<NodeAssertionError> { assert.assert(emptyArray<String>()) }
  }.guest {
    // language=javascript
    """
      const { throws, assert } = require("node:assert");
      throws(() => assert(false));
      throws(() => assert(0));
      throws(() => assert(0.0));
      throws(() => assert(''));
      throws(() => assert({}));
      throws(() => assert([]));
    """
  }

  @Disabled("TODO: Bug in GraalVM with default exports")
  @Test fun `assert() rejected guest tests (default entry)`() = dual {
    assertThrows<NodeAssertionError> { assert.assert(false) }
    assertThrows<NodeAssertionError> { assert.assert(0) }
    assertThrows<NodeAssertionError> { assert.assert(0L) }
    assertThrows<NodeAssertionError> { assert.assert(0.0) }
    assertThrows<NodeAssertionError> { assert.assert(0.0f) }
    assertThrows<NodeAssertionError> { assert.assert("") }
    assertThrows<NodeAssertionError> { assert.assert(emptyMap<String, String>()) }
    assertThrows<NodeAssertionError> { assert.assert(emptyList<String>()) }
    assertThrows<NodeAssertionError> { assert.assert(emptyArray<String>()) }
  }.guest {
    // language=javascript
    """
      const assert = require("node:assert");
      assert.throws(() => assert(false));
      assert.throws(() => assert(0));
      assert.throws(() => assert(0.0));
      assert.throws(() => assert(''));
      assert.throws(() => assert({}));
      assert.throws(() => assert([]));
    """
  }

  @Test fun `throws() should behave correctly`() = dual {
    assertThrows<NodeAssertionError> {
      assert.ok(false)
    }
    assertDoesNotThrow {
      assert.throws {
        error("Example host-side exception")
      }
    }
    assertThrows<NodeAssertionError> {
      assert.throws {
        // i do not throw
      }
    }
  }.guest {
    // language=javascript
    """
      const { throws, ok } = require("node:assert");
      throws(() => { throw new Error("oh no!") });
      throws(() => { ok(false) });
      throws(() => {
        throws(() => {
          // i do not throw
        });
      });
    """
  }

  @Test fun `doesNotThrow() should behave correctly`() = dual {
    assertThrows<NodeAssertionError> {
      assert.ok(false)
    }
    assertThrows<NodeAssertionError> {
      assert.doesNotThrow {
        error("Example host-side exception")
      }
    }
    assertDoesNotThrow {
      assert.doesNotThrow {
        // i do not throw
      }
    }
  }.guest {
    // language=javascript
    """
      const { doesNotThrow, ok } = require("node:assert");
      doesNotThrow(() => { ok(true) });
      doesNotThrow(() => {
        doesNotThrow(() => {
          // i do not throw
        });
      });
    """
  }

  @Test fun `fail() should behave correctly`() = dual {
    assertThrows<NodeAssertionError> {
      assert.fail("Example failure")
    }
    assertThrows<NodeAssertionError> {
      assert.fail("Example failure", "Expected", "Actual", "==")
    }
    assertThrows<NodeAssertionError> {
      assert.fail("Example failure", "Expected", "Actual", "==", { })
    }
  }.guest {
    // language=javascript
    """
      const { fail, throws } = require("node:assert");
      throws(() => fail("Example failure"));
    """
  }

  @Test fun `match() should behave correctly with RegExp`() = dual {
    assertThrows<NodeAssertionError> {
      assert.match("foo", Regex("bar"))
    }
    assertDoesNotThrow {
      assert.match("foo", Regex("foo"))
    }
  }.guest {
    // language=javascript
    """
      const { match, throws } = require("node:assert");
      throws(() => match("foo", new RegExp('bar')));
      match("foo", new RegExp('foo'));
    """
  }

  @Test fun `match() should behave correctly with literal regex`() = dual {
    assertThrows<NodeAssertionError> {
      assert.match("foo", Regex("bar"))
    }
    assertDoesNotThrow {
      assert.match("foo", Regex("foo"))
    }
  }.guest {
    // language=javascript
    """
      const { match, throws } = require("node:assert");
      throws(() => match("foo", /bar/));
      match("foo", /foo/);
    """
  }

  @Test fun `doesNotMatch() should behave correctly with literal regex`() = dual {
    assertThrows<NodeAssertionError> {
      assert.doesNotMatch("foo", Regex("foo"))
    }
    assertDoesNotThrow {
      assert.doesNotMatch("foo", Regex("bar"))
    }
  }.guest {
    // language=javascript
    """
      const { doesNotMatch, throws } = require("node:assert");
      throws(() => doesNotMatch("foo", /foo/));
      doesNotMatch("foo", /bar/);
    """
  }

  @Test fun `doesNotMatch() should behave correctly with RegExp`() = dual {
    assertThrows<NodeAssertionError> {
      assert.doesNotMatch("foo", Regex("foo"))
    }
    assertDoesNotThrow {
      assert.doesNotMatch("foo", Regex("bar"))
    }
  }.guest {
    // language=javascript
    """
      const { doesNotMatch, throws } = require("node:assert");
      throws(() => doesNotMatch("foo", new RegExp('foo')));
      doesNotMatch("foo", new RegExp('bar'));
    """
  }

  @Test fun `ifError() should properly detect error types`() = dual {
    assertThrows<NodeAssertionError> {
      assert.ifError(Error("Example error"))
    }
    assertDoesNotThrow {
      assert.ifError(null)
    }
  }.guest {
    // language=javascript
    """
      const { throws, doesNotThrow, ifError } = require("node:assert");
      throws(() => ifError(new Error("Example error")));
      doesNotThrow(() => ifError(null));
    """
  }

  @TestFactory fun `equal() should pass correctly with host types`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("equal() should pass for equal strings") {
      assert.equal("hi", "hi")
      assert.equal("hello", "hello")
      assert.equal("", "")
    })
    yield(dynamicTest("equal() should pass for equal integers") {
      assert.equal(1, 1)
      assert.equal(2, 2)
      assert.equal(20000, 20000)
      assert.equal(0, 0)
      assert.equal(-1, -1)
    })
    yield(dynamicTest("equal() should pass for equal longs") {
      assert.equal(1L, 1L)
      assert.equal(2L, 2L)
      assert.equal(20000L, 20000L)
      assert.equal(0L, 0L)
      assert.equal(-1L, -1L)
    })
    yield(dynamicTest("equal() should pass for equal integers/longs") {
      assert.equal(1, 1L)
      assert.equal(2, 2L)
      assert.equal(20000, 20000L)
      assert.equal(0, 0L)
      assert.equal(-1, -1L)
    })
    yield(dynamicTest("equal() should pass for equal floats") {
      assert.equal(1.0f, 1.0f)
      assert.equal(2.0f, 2.0f)
      assert.equal(2.3333f, 2.3333f)
      assert.equal(20000.0f, 20000.0f)
      assert.equal(0.0f, 0.0f)
      assert.equal(-1.0f, -1.0f)
    })
    yield(dynamicTest("equal() should pass for equal integers/floats") {
      assert.equal(1.0f, 1)
      assert.equal(2.0f, 2)
      assert.equal(20000.0f, 20000)
      assert.equal(0.0f, 0)
      assert.equal(-1.0f, -1)
    })
    yield(dynamicTest("equal() should pass for equal longs/floats") {
      assert.equal(1.0f, 1L)
      assert.equal(2.0f, 2L)
      assert.equal(20000.0f, 20000L)
      assert.equal(0.0f, 0L)
      assert.equal(-1.0f, -1L)
    })
    yield(dynamicTest("equal() should pass for equal doubles") {
      assert.equal(1.0, 1.0)
      assert.equal(2.0, 2.0)
      assert.equal(2.3333, 2.3333)
      assert.equal(20000.0, 20000.0)
      assert.equal(0.0, 0.0)
      assert.equal(-1.0, -1.0)
    })
    yield(dynamicTest("equal() should pass for equal integers/doubles") {
      assert.equal(1.0, 1)
      assert.equal(2.0, 2)
      assert.equal(20000.0, 20000)
      assert.equal(0.0, 0)
      assert.equal(-1.0, -1)
    })
    yield(dynamicTest("equal() should pass for equal longs/doubles") {
      assert.equal(1.0, 1L)
      assert.equal(2.0, 2L)
      assert.equal(20000.0, 20000L)
      assert.equal(0.0, 0L)
      assert.equal(-1.0, -1L)
    })
    yield(dynamicTest("equal() should pass for equal floats/doubles") {
      assert.equal(1.0, 1.0f)
      assert.equal(2.0, 2.0f)
      assert.equal(2.3333, 2.3333f)
      assert.equal(20000.0, 20000.0f)
      assert.equal(0.0, 0.0f)
      assert.equal(-1.0, -1.0f)
    })
    yield(dynamicTest("equal() should pass for equal uints") {
      assert.equal(1u, 1u)
      assert.equal(2u, 2u)
      assert.equal(20000u, 20000u)
      assert.equal(0u, 0u)
    })
    yield(dynamicTest("equal() should pass for equal bools or nulls") {
      assert.equal(null, null)
      assert.equal(actual = true, expected = true)
      assert.equal(actual = false, expected = false)
    })
  }.asStream()

  @TestFactory fun `equal() should fail correctly with host types`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("equal() should fail for unequal strings") {
      assertThrows<NodeAssertionError> {
        assert.equal("hi", "hello")
      }
      assertThrows<NodeAssertionError> {
        assert.equal("hello", "hi")
      }
      assertThrows<NodeAssertionError> {
        assert.equal("", "hi")
      }
      assertThrows<NodeAssertionError> {
        assert.equal("hi", "")
      }
    })
    yield(dynamicTest("equal() should fail for unequal integers") {
      assertThrows<NodeAssertionError> {
        assert.equal(1, 2)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000, 20001)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20001, 20000)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1, -2)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-2, -1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(1, 0)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0, -1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1, 0)
      }
    })
    yield(dynamicTest("equal() should fail for unequal longs") {
      assertThrows<NodeAssertionError> {
        assert.equal(1L, 2L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2L, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000L, 20001L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20001L, 20000L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1L, -2L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-2L, -1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0L, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(1L, 0L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0L, -1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1L, 0L)
      }
    })
    yield(dynamicTest("equal() should fail for unequal integers/longs") {
      assertThrows<NodeAssertionError> {
        assert.equal(1, 2L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2L, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000, 20001L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1, 1L)
      }
    })
    yield(dynamicTest("equal() should fail for unequal floats") {
      assertThrows<NodeAssertionError> {
        assert.equal(1.0f, 1.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.0f, 2.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.3333f, 2.4444f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000.0f, 20001.0f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0.0f, 0.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1.0f, -1.1f)
      }
    })
    yield(dynamicTest("equal() should fail for unequal integers/floats") {
      assertThrows<NodeAssertionError> {
        assert.equal(1.0f, 2)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.0f, 0)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000.0f, 20001)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0.0f, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1.0f, -2)
      }
    })
    yield(dynamicTest("equal() should fail for unequal longs/floats") {
      assertThrows<NodeAssertionError> {
        assert.equal(1.0f, 2L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.0f, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000.0f, 20001L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0.0f, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1.0f, 0L)
      }
    })
    yield(dynamicTest("equal() should fail for unequal doubles") {
      assertThrows<NodeAssertionError> {
        assert.equal(1.0, 1.1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.0, 2.1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.3333, 2.4444)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000.0, 20001.0)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0.0, 0.1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1.0, -1.1)
      }
    })
    yield(dynamicTest("equal() should fail for unequal integers/doubles") {
      assertThrows<NodeAssertionError> {
        assert.equal(1.0, 2)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.0, 3)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000.0, 20001)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0.0, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1.0, -2)
      }
    })
    yield(dynamicTest("equal() should fail for unequal longs/doubles") {
      assertThrows<NodeAssertionError> {
        assert.equal(1.0, 2L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.0, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000.0, 20001L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0.0, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1.0, -2L)
      }
    })
    yield(dynamicTest("equal() should fail for unequal floats/doubles") {
      assertThrows<NodeAssertionError> {
        assert.equal(1.0, 1.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.0, 2.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2.3333, 2.4444f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000.0, 20001.0f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0.0, 0.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(-1.0, -1.1f)
      }
    })
    yield(dynamicTest("equal() should fail for unequal uints") {
      assertThrows<NodeAssertionError> {
        assert.equal(1u, 2u)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(2u, 1u)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(20000u, 20001u)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0u, 1u)
      }
    })
    yield(dynamicTest("equal() should fail for unequal bools or nulls") {
      assertThrows<NodeAssertionError> {
        assert.equal(null, "hi")
      }
      assertThrows<NodeAssertionError> {
        assert.equal(null, true)
      }
      assertThrows<NodeAssertionError> {
        assert.equal("hi", null)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(true, null)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(null, 0)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(null, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(0, null)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(1, null)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(actual = true, expected = false)
      }
      assertThrows<NodeAssertionError> {
        assert.equal(actual = false, expected = true)
      }
    })
  }.asStream()

  @TestFactory fun `strict() should fail correctly with host types`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("strict() should fail for unequal strings") {
      assertThrows<NodeAssertionError> {
        assert.strict("hi", "hello")
      }
      assertThrows<NodeAssertionError> {
        assert.strict("hello", "hi")
      }
      assertThrows<NodeAssertionError> {
        assert.strict("", "hi")
      }
      assertThrows<NodeAssertionError> {
        assert.strict("hi", "")
      }
    })
    yield(dynamicTest("strict() should fail for unequal integers") {
      assertThrows<NodeAssertionError> {
        assert.strict(1, 2)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000, 20001)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20001, 20000)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1, -2)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-2, -1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(1, 0)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0, -1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1, 0)
      }
    })
    yield(dynamicTest("strict() should fail for unequal longs") {
      assertThrows<NodeAssertionError> {
        assert.strict(1L, 2L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2L, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000L, 20001L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20001L, 20000L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1L, -2L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-2L, -1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0L, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(1L, 0L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0L, -1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1L, 0L)
      }
    })
    yield(dynamicTest("strict() should fail for unequal integers/longs") {
      assertThrows<NodeAssertionError> {
        assert.strict(1, 2L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2L, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000, 20001L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1, 1L)
      }
    })
    yield(dynamicTest("strict() should fail for unequal floats") {
      assertThrows<NodeAssertionError> {
        assert.strict(1.0f, 1.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.0f, 2.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.3333f, 2.4444f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000.0f, 20001.0f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0.0f, 0.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1.0f, -1.1f)
      }
    })
    yield(dynamicTest("strict() should fail for unequal integers/floats") {
      assertThrows<NodeAssertionError> {
        assert.strict(1.0f, 2)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.0f, 0)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000.0f, 20001)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0.0f, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1.0f, -2)
      }
    })
    yield(dynamicTest("strict() should fail for unequal longs/floats") {
      assertThrows<NodeAssertionError> {
        assert.strict(1.0f, 2L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.0f, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000.0f, 20001L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0.0f, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1.0f, 0L)
      }
    })
    yield(dynamicTest("strict() should fail for unequal doubles") {
      assertThrows<NodeAssertionError> {
        assert.strict(1.0, 1.1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.0, 2.1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.3333, 2.4444)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000.0, 20001.0)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0.0, 0.1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1.0, -1.1)
      }
    })
    yield(dynamicTest("strict() should fail for unequal integers/doubles") {
      assertThrows<NodeAssertionError> {
        assert.strict(1.0, 2)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.0, 3)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000.0, 20001)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0.0, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1.0, -2)
      }
    })
    yield(dynamicTest("strict() should fail for unequal longs/doubles") {
      assertThrows<NodeAssertionError> {
        assert.strict(1.0, 2L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.0, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000.0, 20001L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0.0, 1L)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1.0, -2L)
      }
    })
    yield(dynamicTest("strict() should fail for unequal floats/doubles") {
      assertThrows<NodeAssertionError> {
        assert.strict(1.0, 1.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.0, 2.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2.3333, 2.4444f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000.0, 20001.0f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0.0, 0.1f)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(-1.0, -1.1f)
      }
    })
    yield(dynamicTest("strict() should fail for unequal uints") {
      assertThrows<NodeAssertionError> {
        assert.strict(1u, 2u)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(2u, 1u)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(20000u, 20001u)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0u, 1u)
      }
    })
    yield(dynamicTest("strict() should fail for unequal bools or nulls") {
      assertThrows<NodeAssertionError> {
        assert.strict(null, "hi")
      }
      assertThrows<NodeAssertionError> {
        assert.strict(null, true)
      }
      assertThrows<NodeAssertionError> {
        assert.strict("hi", null)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(true, null)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(null, 0)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(null, 1)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(0, null)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(1, null)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(actual = true, expected = false)
      }
      assertThrows<NodeAssertionError> {
        assert.strict(actual = false, expected = true)
      }
    })
  }.asStream()

  @TestFactory fun `equal() should fail correctly with guest types`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("equal() should fail for unequal strings") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue("hi"), asValue("hello"))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue("hello"), asValue("hi"))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(""), asValue("hi"))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue("hi"), asValue(""))
      }
    })
    yield(dynamicTest("equal() should fail for unequal integers") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1), asValue(2))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000), asValue(20001))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20001), asValue(20000))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1), asValue(-2))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-2), asValue(-1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1), asValue(0))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0), asValue(-1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1), asValue(0))
      }
    })
    yield(dynamicTest("equal() should fail for unequal longs") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1L), asValue(2L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2L), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000L), asValue(20001L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20001L), asValue(20000L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1L), asValue(-2L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-2L), asValue(-1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0L), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1L), asValue(0L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0L), asValue(-1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1L), asValue(0L))
      }
    })
    yield(dynamicTest("equal() should fail for unequal integers/longs") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1), asValue(2L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2L), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000), asValue(20001L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1), asValue(1L))
      }
    })
    yield(dynamicTest("equal() should fail for unequal floats") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1.0f), asValue(1.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.0f), asValue(2.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.3333f), asValue(2.4444f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000.0f), asValue(20001.0f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0.0f), asValue(0.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1.0f), asValue(-1.1f))
      }
    })
    yield(dynamicTest("equal() should fail for unequal integers/floats") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1.0f), asValue(2))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.0f), asValue(0))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000.0f), asValue(20001))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0.0f), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1.0f), asValue(-2))
      }
    })
    yield(dynamicTest("equal() should fail for unequal longs/floats") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1.0f), asValue(2L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.0f), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000.0f), asValue(20001L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0.0f), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1.0f), asValue(0L))
      }
    })
    yield(dynamicTest("equal() should fail for unequal doubles") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1.0), asValue(1.1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.0), asValue(2.1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.3333), asValue(2.4444))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000.0), asValue(20001.0))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0.0), asValue(0.1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1.0), asValue(-1.1))
      }
    })
    yield(dynamicTest("equal() should fail for unequal integers/doubles") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1.0), asValue(2))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.0), asValue(3))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000.0), asValue(20001))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0.0), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1.0), asValue(-2))
      }
    })
    yield(dynamicTest("equal() should fail for unequal longs/doubles") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1.0), asValue(2L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.0), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000.0), asValue(20001L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0.0), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1.0), asValue(-2L))
      }
    })
    yield(dynamicTest("equal() should fail for unequal floats/doubles") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1.0), asValue(1.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.0), asValue(2.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(2.3333), asValue(2.4444f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(20000.0), asValue(20001.0f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0.0), asValue(0.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(-1.0), asValue(-1.1f))
      }
    })
    yield(dynamicTest("equal() should fail for unequal bools or nulls") {
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(null), asValue("hi"))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(null), asValue(true))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue("hi"), asValue(null))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(true), asValue(null))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(null), asValue(0))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(null), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(0), asValue(null))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(asValue(1), asValue(null))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(actual = asValue(true), expected = asValue(false))
      }
      assertThrows<NodeAssertionError> {
        assert.equal(actual = asValue(false), expected = asValue(true))
      }
    })
  }.asStream()

  @TestFactory fun `strict() should fail correctly with guest types`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("strict() should fail for unequal strings") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue("hi"), asValue("hello"))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue("hello"), asValue("hi"))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(""), asValue("hi"))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue("hi"), asValue(""))
      }
    })
    yield(dynamicTest("strict() should fail for unequal integers") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1), asValue(2))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000), asValue(20001))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20001), asValue(20000))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1), asValue(-2))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-2), asValue(-1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1), asValue(0))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0), asValue(-1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1), asValue(0))
      }
    })
    yield(dynamicTest("strict() should fail for unequal longs") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1L), asValue(2L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2L), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000L), asValue(20001L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20001L), asValue(20000L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1L), asValue(-2L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-2L), asValue(-1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0L), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1L), asValue(0L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0L), asValue(-1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1L), asValue(0L))
      }
    })
    yield(dynamicTest("strict() should fail for unequal integers/longs") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1), asValue(2L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2L), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000), asValue(20001L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1), asValue(1L))
      }
    })
    yield(dynamicTest("strict() should fail for unequal floats") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1.0f), asValue(1.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.0f), asValue(2.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.3333f), asValue(2.4444f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000.0f), asValue(20001.0f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0.0f), asValue(0.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1.0f), asValue(-1.1f))
      }
    })
    yield(dynamicTest("strict() should fail for unequal integers/floats") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1.0f), asValue(2))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.0f), asValue(0))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000.0f), asValue(20001))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0.0f), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1.0f), asValue(-2))
      }
    })
    yield(dynamicTest("strict() should fail for unequal longs/floats") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1.0f), asValue(2L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.0f), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000.0f), asValue(20001L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0.0f), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1.0f), asValue(0L))
      }
    })
    yield(dynamicTest("strict() should fail for unequal doubles") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1.0), asValue(1.1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.0), asValue(2.1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.3333), asValue(2.4444))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000.0), asValue(20001.0))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0.0), asValue(0.1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1.0), asValue(-1.1))
      }
    })
    yield(dynamicTest("strict() should fail for unequal integers/doubles") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1.0), asValue(2))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.0), asValue(3))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000.0), asValue(20001))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0.0), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1.0), asValue(-2))
      }
    })
    yield(dynamicTest("strict() should fail for unequal longs/doubles") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1.0), asValue(2L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.0), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000.0), asValue(20001L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0.0), asValue(1L))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1.0), asValue(-2L))
      }
    })
    yield(dynamicTest("strict() should fail for unequal floats/doubles") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1.0), asValue(1.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.0), asValue(2.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(2.3333), asValue(2.4444f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(20000.0), asValue(20001.0f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0.0), asValue(0.1f))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(-1.0), asValue(-1.1f))
      }
    })
    yield(dynamicTest("strict() should fail for unequal bools or nulls") {
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(null), asValue("hi"))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(null), asValue(true))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue("hi"), asValue(null))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(true), asValue(null))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(null), asValue(0))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(null), asValue(1))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(0), asValue(null))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(asValue(1), asValue(null))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(actual = asValue(true), expected = asValue(false))
      }
      assertThrows<NodeAssertionError> {
        assert.strict(actual = asValue(false), expected = asValue(true))
      }
    })
  }.asStream()

  @TestFactory fun `equal() should pass correctly with guest types`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("equal() should pass for equal strings") {
      assert.equal(asValue("hi"), asValue("hi"))
      assert.equal(asValue("hello"), asValue("hello"))
      assert.equal(asValue(""), asValue(""))
    })
    yield(dynamicTest("equal() should pass for equal integers") {
      assert.equal(asValue(1), asValue(1))
      assert.equal(asValue(2), asValue(2))
      assert.equal(asValue(20000), asValue(20000))
      assert.equal(asValue(0), asValue(0))
      assert.equal(asValue(-1), asValue(-1))
    })
    yield(dynamicTest("equal() should pass for equal longs") {
      assert.equal(asValue(1L), asValue(1L))
      assert.equal(asValue(2L), asValue(2L))
      assert.equal(asValue(20000L), asValue(20000L))
      assert.equal(asValue(0L), asValue(0L))
      assert.equal(asValue(-1L), asValue(-1L))
    })
    yield(dynamicTest("equal() should pass for equal integers/longs") {
      assert.equal(asValue(1), asValue(1L))
      assert.equal(asValue(2), asValue(2L))
      assert.equal(asValue(20000), asValue(20000L))
      assert.equal(asValue(0), asValue(0L))
      assert.equal(asValue(-1), asValue(-1L))
    })
    yield(dynamicTest("equal() should pass for equal floats") {
      assert.equal(asValue(1.0f), asValue(1.0f))
      assert.equal(asValue(2.0f), asValue(2.0f))
      assert.equal(asValue(2.3333f), asValue(2.3333f))
      assert.equal(asValue(20000.0f), asValue(20000.0f))
      assert.equal(asValue(0.0f), asValue(0.0f))
      assert.equal(asValue(-1.0f), asValue(-1.0f))
    })
    yield(dynamicTest("equal() should pass for equal integers/floats") {
      assert.equal(asValue(1.0f), asValue(1))
      assert.equal(asValue(2.0f), asValue(2))
      assert.equal(asValue(20000.0f), asValue(20000))
      assert.equal(asValue(0.0f), asValue(0))
      assert.equal(asValue(-1.0f), asValue(-1))
    })
    yield(dynamicTest("equal() should pass for equal longs/floats") {
      assert.equal(asValue(1.0f), asValue(1L))
      assert.equal(asValue(2.0f), asValue(2L))
      assert.equal(asValue(20000.0f), asValue(20000L))
      assert.equal(asValue(0.0f), asValue(0L))
      assert.equal(asValue(-1.0f), asValue(-1L))
    })
    yield(dynamicTest("equal() should pass for equal doubles") {
      assert.equal(asValue(1.0), asValue(1.0))
      assert.equal(asValue(2.0), asValue(2.0))
      assert.equal(asValue(2.3333), asValue(2.3333))
      assert.equal(asValue(20000.0), asValue(20000.0))
      assert.equal(asValue(0.0), asValue(0.0))
      assert.equal(asValue(-1.0), asValue(-1.0))
    })
    yield(dynamicTest("equal() should pass for equal integers/doubles") {
      assert.equal(asValue(1.0), asValue(1))
      assert.equal(asValue(2.0), asValue(2))
      assert.equal(asValue(20000.0), asValue(20000))
      assert.equal(asValue(0.0), asValue(0))
      assert.equal(asValue(-1.0), asValue(-1))
    })
    yield(dynamicTest("equal() should pass for equal longs/doubles") {
      assert.equal(asValue(1.0), asValue(1L))
      assert.equal(asValue(2.0), asValue(2L))
      assert.equal(asValue(20000.0), asValue(20000L))
      assert.equal(asValue(0.0), asValue(0L))
      assert.equal(asValue(-1.0), asValue(-1L))
    })
    yield(dynamicTest("equal() should pass for equal floats/doubles") {
      assert.equal(asValue(1.0), asValue(1.0f))
      assert.equal(asValue(2.0), asValue(2.0f))
      // assert.equal(asValue(2.3333), asValue(2.3333f))  @TODO(sgammon): file with gvm for precision issue
      assert.equal(asValue(20000.0), asValue(20000.0f))
      assert.equal(asValue(0.0), asValue(0.0f))
      assert.equal(asValue(-1.0), asValue(-1.0f))
    })
    yield(dynamicTest("equal() should pass for equal bools or nulls") {
      assert.equal(asValue(null), asValue(null))
      assert.equal(actual = asValue(true), expected = asValue(true))
      assert.equal(actual = asValue(false), expected = asValue(false))
    })
  }.asStream()

  @TestFactory fun `strict() should pass correctly with host types`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("strict() should pass for equal strings") {
      assert.strict("hi", "hi")
      assert.strict("hello", "hello")
      assert.strict("", "")
    })
    yield(dynamicTest("strict() should pass for equal integers/longs") {
      assert.strict(1, 1L)
      assert.strict(2, 2L)
      assert.strict(20000, 20000L)
      assert.strict(0, 0L)
      assert.strict(-1, -1L)
    })
    yield(dynamicTest("strict() should pass for equal integers/floats") {
      assert.strict(1.0f, 1)
      assert.strict(2.0f, 2)
      assert.strict(20000.0f, 20000)
      assert.strict(0.0f, 0)
      assert.strict(-1.0f, -1)
    })
    yield(dynamicTest("strict() should pass for equal longs/floats") {
      assert.strict(1.0f, 1L)
      assert.strict(2.0f, 2L)
      assert.strict(20000.0f, 20000L)
      assert.strict(0.0f, 0L)
      assert.strict(-1.0f, -1L)
    })
    yield(dynamicTest("strict() should pass for equal integers/doubles") {
      assert.strict(1.0, 1)
      assert.strict(2.0, 2)
      assert.strict(20000.0, 20000)
      assert.strict(0.0, 0)
      assert.strict(-1.0, -1)
    })
    yield(dynamicTest("strict() should pass for equal longs/doubles") {
      assert.strict(1.0, 1L)
      assert.strict(2.0, 2L)
      assert.strict(20000.0, 20000L)
      assert.strict(0.0, 0L)
      assert.strict(-1.0, -1L)
    })
    yield(dynamicTest("strict() should pass for equal floats/doubles") {
      assert.strict(1.0, 1.0f)
      assert.strict(2.0, 2.0f)
      // assert.strict(2.3333, 2.3333f) @TODO(sgammon): file with gvm for precision issue
      assert.strict(20000.0, 20000.0f)
      assert.strict(0.0, 0.0f)
      assert.strict(-1.0, -1.0f)
    })
    yield(dynamicTest("strict() should pass for equal strings") {
      assert.strict("hi", "hi")
      assert.strict("hello", "hello")
      assert.strict("", "")
    })
    yield(dynamicTest("strict() should pass for equal integers") {
      assert.strict(1, 1)
      assert.strict(2, 2)
      assert.strict(20000, 20000)
      assert.strict(0, 0)
      assert.strict(-1, -1)
    })
    yield(dynamicTest("strict() should pass for equal longs") {
      assert.strict(1L, 1L)
      assert.strict(2L, 2L)
      assert.strict(20000L, 20000L)
      assert.strict(0L, 0L)
      assert.strict(-1L, -1L)
    })
    yield(dynamicTest("strict() should pass for equal floats") {
      assert.strict(1.0f, 1.0f)
      assert.strict(2.0f, 2.0f)
      assert.strict(2.3333f, 2.3333f)
      assert.strict(20000.0f, 20000.0f)
      assert.strict(0.0f, 0.0f)
      assert.strict(-1.0f, -1.0f)
    })
    yield(dynamicTest("strict() should pass for equal doubles") {
      assert.strict(1.0, 1.0)
      assert.strict(2.0, 2.0)
      assert.strict(2.3333, 2.3333)
      assert.strict(20000.0, 20000.0)
      assert.strict(0.0, 0.0)
      assert.strict(-1.0, -1.0)
    })
    yield(dynamicTest("strict() should pass for equal uints") {
      assert.strict(1u, 1u)
      assert.strict(2u, 2u)
      assert.strict(20000u, 20000u)
      assert.strict(0u, 0u)
    })
    yield(dynamicTest("strict() should pass for equal bools or nulls") {
      assert.strict(null, null)
      assert.strict(actual = true, expected = true)
      assert.strict(actual = false, expected = false)
    })
  }.asStream()

  @TestFactory fun `strict() should pass correctly with guest types`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("strict() should pass for equal strings") {
      assert.strict(asValue("hi"), asValue("hi"))
      assert.strict(asValue("hello"), asValue("hello"))
      assert.strict(asValue(""), asValue(""))
    })
    yield(dynamicTest("strict() should pass for equal integers") {
      assert.strict(asValue(1), asValue(1))
      assert.strict(asValue(2), asValue(2))
      assert.strict(asValue(20000), asValue(20000))
      assert.strict(asValue(0), asValue(0))
      assert.strict(asValue(-1), asValue(-1))
    })
    yield(dynamicTest("strict() should pass for equal longs") {
      assert.strict(asValue(1L), asValue(1L))
      assert.strict(asValue(2L), asValue(2L))
      assert.strict(asValue(20000L), asValue(20000L))
      assert.strict(asValue(0L), asValue(0L))
      assert.strict(asValue(-1L), asValue(-1L))
    })
    yield(dynamicTest("strict() should pass for equal integers/longs") {
      assert.strict(asValue(1), asValue(1L))
      assert.strict(asValue(2), asValue(2L))
      assert.strict(asValue(20000), asValue(20000L))
      assert.strict(asValue(0), asValue(0L))
      assert.strict(asValue(-1), asValue(-1L))
    })
    yield(dynamicTest("strict() should pass for equal floats") {
      assert.strict(asValue(1.0f), asValue(1.0f))
      assert.strict(asValue(2.0f), asValue(2.0f))
      assert.strict(asValue(2.3333f), asValue(2.3333f))
      assert.strict(asValue(20000.0f), asValue(20000.0f))
      assert.strict(asValue(0.0f), asValue(0.0f))
      assert.strict(asValue(-1.0f), asValue(-1.0f))
    })
    yield(dynamicTest("strict() should pass for equal integers/floats") {
      assert.strict(asValue(1.0f), asValue(1))
      assert.strict(asValue(2.0f), asValue(2))
      assert.strict(asValue(20000.0f), asValue(20000))
      assert.strict(asValue(0.0f), asValue(0))
      assert.strict(asValue(-1.0f), asValue(-1))
    })
    yield(dynamicTest("strict() should pass for equal longs/floats") {
      assert.strict(asValue(1.0f), asValue(1L))
      assert.strict(asValue(2.0f), asValue(2L))
      assert.strict(asValue(20000.0f), asValue(20000L))
      assert.strict(asValue(0.0f), asValue(0L))
      assert.strict(asValue(-1.0f), asValue(-1L))
    })
    yield(dynamicTest("strict() should pass for equal doubles") {
      assert.strict(asValue(1.0), asValue(1.0))
      assert.strict(asValue(2.0), asValue(2.0))
      assert.strict(asValue(2.3333), asValue(2.3333))
      assert.strict(asValue(20000.0), asValue(20000.0))
      assert.strict(asValue(0.0), asValue(0.0))
      assert.strict(asValue(-1.0), asValue(-1.0))
    })
    yield(dynamicTest("strict() should pass for equal integers/doubles") {
      assert.strict(asValue(1.0), asValue(1))
      assert.strict(asValue(2.0), asValue(2))
      assert.strict(asValue(20000.0), asValue(20000))
      assert.strict(asValue(0.0), asValue(0))
      assert.strict(asValue(-1.0), asValue(-1))
    })
    yield(dynamicTest("strict() should pass for equal longs/doubles") {
      assert.strict(asValue(1.0), asValue(1L))
      assert.strict(asValue(2.0), asValue(2L))
      assert.strict(asValue(20000.0), asValue(20000L))
      assert.strict(asValue(0.0), asValue(0L))
      assert.strict(asValue(-1.0), asValue(-1L))
    })
    yield(dynamicTest("strict() should pass for equal floats/doubles") {
      assert.strict(asValue(1.0), asValue(1.0f))
      assert.strict(asValue(2.0), asValue(2.0f))
      // assert.strict(asValue(2.3333), asValue(2.3333f))  @TODO(sgammon): file with gvm for precision issue
      assert.strict(asValue(20000.0), asValue(20000.0f))
      assert.strict(asValue(0.0), asValue(0.0f))
      assert.strict(asValue(-1.0), asValue(-1.0f))
    })
    yield(dynamicTest("strict() should pass for equal bools or nulls") {
      assert.strict(asValue(null), asValue(null))
      assert.strict(actual = asValue(true), expected = asValue(true))
      assert.strict(actual = asValue(false), expected = asValue(false))
    })
  }.asStream()

//  @TestFactory fun `strict() should fail correctly with host types`(): Stream<DynamicTest> = sequence {
//
//  }.asStream()
}
