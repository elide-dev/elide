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
@file:Suppress("EmptyFunctionBlock", "LargeClass", "LongMethod")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.node

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.Value.asValue
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.math.BigInteger
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.Test
import elide.runtime.core.DelicateElideApi
import elide.runtime.node.asserts.NodeAssertModule
import elide.runtime.node.asserts.NodeAssertionError
import elide.testing.annotations.TestCase

/** Tests for the built-in `assert` module. */
@TestCase internal class NodeAssertTest : NodeModuleConformanceTest<NodeAssertModule>() {
  override val moduleName: String get() = "assert"
  override fun provide(): NodeAssertModule = NodeAssertModule()
  private val assert = NodeAssertModule().provide()

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

  private val objectWithProperty = object : ProxyObject {
    override fun getMemberKeys(): Array<String> = arrayOf("foo")
    override fun getMember(key: String): Value? = when (key) {
      "foo" -> asValue("bar")
      else -> null
    }

    override fun hasMember(key: String): Boolean = key == "foo"
    override fun putMember(key: String, value: Value?) {}
  }

  private val emptyObject = object : ProxyObject {
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

  @TestFactory fun `notOk() should behave as expected for truthy cases`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("notOk(true) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(true) }
      },
    )
    yield(
      dynamicTest("notOk(1) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(1) }
      },
    )
    yield(
      dynamicTest("notOk(1L) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(1L) }
      },
    )
    yield(
      dynamicTest("notOk(1.0) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(1.0) }
      },
    )
    yield(
      dynamicTest("notOk(1.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(1.0f) }
      },
    )
    yield(
      dynamicTest("notOk(\"hi\") should fail") {
        assertThrows<NodeAssertionError> { assert.notOk("hi") }
      },
    )
    yield(
      dynamicTest("notOk([\"hi\"]) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(arrayOf("hi")) }
      },
    )
    yield(
      dynamicTest("notOk(listOf(\"hi\")) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(listOf("hi")) }
      },
    )
    yield(
      dynamicTest("notOk({\"foo\": \"bar\"}) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(objectWithProperty) }
      },
    )
    yield(
      dynamicTest("notOk(mapOf(\"foo\" to \"bar\")) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(mapOf("foo" to "bar")) }
      },
    )
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
    yield(
      dynamicTest("assert(mapOf(\"foo\" to \"bar\")) should pass") {
        assert.assert(mapOf("foo" to "bar"))
      },
    )
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
    yield(
      dynamicTest("ok(mapOf(\"foo\" to \"bar\")) should pass") {
        assert.ok(asValue(mapOf("foo" to "bar")))
      },
    )
  }.asStream()

  @TestFactory fun `notOk() should behave as expected for guest truthy cases`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("notOk(true) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue(true)) }
      },
    )
    yield(
      dynamicTest("notOk(1) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue(1)) }
      },
    )
    yield(
      dynamicTest("notOk(1L) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue(1L)) }
      },
    )
    yield(
      dynamicTest("notOk(1.0) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue(1.0)) }
      },
    )
    yield(
      dynamicTest("notOk(1.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue(1.0f)) }
      },
    )
    yield(
      dynamicTest("notOk(\"hi\") should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue("hi")) }
      },
    )
    yield(
      dynamicTest("notOk([\"hi\"]) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue(arrayOf("hi"))) }
      },
    )
    yield(
      dynamicTest("notOk(listOf(\"hi\")) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue(listOf("hi"))) }
      },
    )
    yield(
      dynamicTest("notOk(mapOf(\"foo\" to \"bar\")) should fail") {
        assertThrows<NodeAssertionError> { assert.notOk(asValue(mapOf("foo" to "bar"))) }
      },
    )
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
    yield(
      dynamicTest("assert(mapOf(\"foo\" to \"bar\")) should pass") {
        assert.ok(asValue(mapOf("foo" to "bar")))
      },
    )
  }.asStream()

  @TestFactory fun `ok() should behave as expected for falsy cases`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("ok(false) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(false) }
      },
    )
    yield(
      dynamicTest("ok(0) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(0) }
      },
    )
    yield(
      dynamicTest("ok(-1) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(-1) }
      },
    )
    yield(
      dynamicTest("ok(0L) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(0L) }
      },
    )
    yield(
      dynamicTest("ok(-1L) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(-1L) }
      },
    )
    yield(
      dynamicTest("ok(0.0) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(0.0) }
      },
    )
    yield(
      dynamicTest("ok(-1.0) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(-1.0) }
      },
    )
    yield(
      dynamicTest("ok(0.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(0.0f) }
      },
    )
    yield(
      dynamicTest("ok(-1.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(-1.0f) }
      },
    )
    yield(
      dynamicTest("ok(\"\") should fail") {
        assertThrows<NodeAssertionError> { assert.ok("") }
      },
    )
    yield(
      dynamicTest("ok([]) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(arrayOf<String>()) }
      },
    )
    yield(
      dynamicTest("ok(emptyList()) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(listOf<String>()) }
      },
    )
    yield(
      dynamicTest("ok({}) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(emptyObject) }
      },
    )
    yield(
      dynamicTest("ok(emptyMap()) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(emptyMap<String, String>()) }
      },
    )
  }.asStream()

  @TestFactory fun `notOk() should behave as expected for falsy cases`(): Stream<DynamicTest> = sequence {
    yield(dynamicTest("notOk(false) should pass") { assert.notOk(false) })
    yield(dynamicTest("notOk(0) should pass") { assert.notOk(0) })
    yield(dynamicTest("notOk(-1) should pass") { assert.notOk(-1) })
    yield(dynamicTest("notOk(0L) should pass") { assert.notOk(0L) })
    yield(dynamicTest("notOk(-1L) should pass") { assert.notOk(-1L) })
    yield(dynamicTest("notOk(0.0) should pass") { assert.notOk(0.0) })
    yield(dynamicTest("notOk(-1.0) should pass") { assert.notOk(-1.0) })
    yield(dynamicTest("notOk(0.0f) should pass") { assert.notOk(0.0f) })
    yield(dynamicTest("notOk(-1.0f) should pass") { assert.notOk(-1.0f) })
    yield(dynamicTest("notOk(\"\") should pass") { assert.notOk("") })
    yield(dynamicTest("notOk([]) should pass") { assert.notOk(arrayOf<String>()) })
    yield(dynamicTest("notOk(emptyList()) should pass") { assert.notOk(listOf<String>()) })
    yield(dynamicTest("notOk({}) should pass") { assert.notOk(emptyObject) })
    yield(dynamicTest("notOk(emptyMap()) should pass") { assert.notOk(emptyMap<String, String>()) })
  }.asStream()

  @TestFactory fun `assert() should behave as expected for falsy cases`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("assert(false) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(false) }
      },
    )
    yield(
      dynamicTest("assert(0) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(0) }
      },
    )
    yield(
      dynamicTest("assert(-1) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(-1) }
      },
    )
    yield(
      dynamicTest("assert(0L) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(0L) }
      },
    )
    yield(
      dynamicTest("assert(-1L) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(-1L) }
      },
    )
    yield(
      dynamicTest("assert(0.0) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(0.0) }
      },
    )
    yield(
      dynamicTest("assert(-1.0) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(-1.0) }
      },
    )
    yield(
      dynamicTest("assert(0.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(0.0f) }
      },
    )
    yield(
      dynamicTest("assert(-1.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(-1.0f) }
      },
    )
    yield(
      dynamicTest("assert(\"\") should fail") {
        assertThrows<NodeAssertionError> { assert.assert("") }
      },
    )
    yield(
      dynamicTest("assert([]) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(arrayOf<String>()) }
      },
    )
    yield(
      dynamicTest("assert(emptyList()) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(listOf<String>()) }
      },
    )
    yield(
      dynamicTest("assert({}) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(emptyObject) }
      },
    )
    yield(
      dynamicTest("assert(emptyMap()) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(emptyMap<String, String>()) }
      },
    )
  }.asStream()

  @TestFactory fun `ok() should behave as expected for guest falsy cases`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("ok(false) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(false)) }
      },
    )
    yield(
      dynamicTest("ok(0) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(0)) }
      },
    )
    yield(
      dynamicTest("ok(-1) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(-1)) }
      },
    )
    yield(
      dynamicTest("ok(0L) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(0L)) }
      },
    )
    yield(
      dynamicTest("ok(-1L) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(-1L)) }
      },
    )
    yield(
      dynamicTest("ok(0.0) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(0.0)) }
      },
    )
    yield(
      dynamicTest("ok(-1.0) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(-1.0)) }
      },
    )
    yield(
      dynamicTest("ok(0.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(0.0f)) }
      },
    )
    yield(
      dynamicTest("ok(-1.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(-1.0f)) }
      },
    )
    yield(
      dynamicTest("ok(\"\") should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue("")) }
      },
    )
    yield(
      dynamicTest("ok([]) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(arrayOf<String>())) }
      },
    )
    yield(
      dynamicTest("ok(emptyList()) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(listOf<String>())) }
      },
    )
    yield(
      dynamicTest("ok({}) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(emptyObject)) }
      },
    )
    yield(
      dynamicTest("ok(emptyMap()) should fail") {
        assertThrows<NodeAssertionError> { assert.ok(asValue(emptyMap<String, String>())) }
      },
    )
  }.asStream()

  @TestFactory fun `assert() should behave as expected for guest falsy cases`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("assert(false) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(false)) }
      },
    )
    yield(
      dynamicTest("assert(0) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(0)) }
      },
    )
    yield(
      dynamicTest("assert(-1) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(-1)) }
      },
    )
    yield(
      dynamicTest("assert(0L) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(0L)) }
      },
    )
    yield(
      dynamicTest("assert(-1L) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(-1L)) }
      },
    )
    yield(
      dynamicTest("assert(0.0) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(0.0)) }
      },
    )
    yield(
      dynamicTest("assert(-1.0) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(-1.0)) }
      },
    )
    yield(
      dynamicTest("assert(0.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(0.0f)) }
      },
    )
    yield(
      dynamicTest("assert(-1.0f) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(-1.0f)) }
      },
    )
    yield(
      dynamicTest("assert(\"\") should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue("")) }
      },
    )
    yield(
      dynamicTest("assert([]) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(arrayOf<String>())) }
      },
    )
    yield(
      dynamicTest("assert(emptyList()) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(listOf<String>())) }
      },
    )
    yield(
      dynamicTest("assert({}) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(emptyObject)) }
      },
    )
    yield(
      dynamicTest("assert(emptyMap()) should fail") {
        assertThrows<NodeAssertionError> { assert.assert(asValue(emptyMap<String, String>())) }
      },
    )
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

  private fun testAssertEqual(left: Any?, right: Any?) {
    assert.equal(left, left)
    assert.equal(left, left, "Custom message with equal()")
    assert.equal(left, right)
    assert.equal(left, right, "Custom message with equal()")
    assert.equal(right, right)
    assert.equal(right, right, "Custom message with equal()")
    assert.equal(right, left)
    assert.equal(right, left, "Custom message with equal()")
    if (left == null || right == null) return
    if ((left is Value && left.isNull) || (right is Value && right.isNull)) return

    assertThrows<NodeAssertionError> { assert.equal(left, null) }
    assertThrows<NodeAssertionError> { assert.equal(null, right) }
    assertThrows<NodeAssertionError> { assert.equal(left, null, "Custom message with equal()") }
    assertThrows<NodeAssertionError> { assert.equal(null, right, "Custom message with equal()") }
    assert.notEqual(left, null)
    assert.notEqual(null, right)
    assert.notEqual(right, null)
    assert.notEqual(null, left)
  }

  @TestFactory fun `equal() range testing`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("equal() should pass for full range of shorts") {
        for (i in Short.MIN_VALUE..Short.MAX_VALUE step 1000) {
          val short = i.toShort()
          assert.equal(short, short)
          assert.equal(asValue(short), short)
          assert.equal(asValue(short), asValue(short))
          if (short != Short.MIN_VALUE) {
            assert.notEqual(short, short - 1)
            assert.notEqual(asValue(short), short - 1)
            assert.notEqual(short, asValue(short - 1))
            assert.notEqual(asValue(short), asValue(short - 1))
          }
          if (short != Short.MAX_VALUE) {
            assert.notEqual(short, short + 1)
            assert.notEqual(asValue(short), short + 1)
            assert.notEqual(short, asValue(short + 1))
            assert.notEqual(asValue(short), asValue(short + 1))
          }
        }
      },
    )
    yield(
      dynamicTest("equal() should pass for full range of integers") {
        for (i in Int.MIN_VALUE..Int.MAX_VALUE step 100000) {
          assert.equal(i, i)
          assert.equal(asValue(i), i)
          assert.equal(i, asValue(i))
          assert.equal(asValue(i), asValue(i))
          if (i != Int.MIN_VALUE) assert.notEqual(i, i - 1)
          if (i != Int.MAX_VALUE) assert.notEqual(i, i + 1)
        }
      },
    )
    yield(
      dynamicTest("equal() should pass for full range of longs") {
        val i = Long.MAX_VALUE
        assert.equal(i, i)
        assert.equal(asValue(i), i)
        assert.equal(i, asValue(i))
        assert.equal(asValue(i), asValue(i))
        assert.notEqual(i, i - 1)
        assert.notEqual(asValue(i), i - 1)
        assert.notEqual(i, asValue(i - 1))
        assert.notEqual(asValue(i), asValue(i - 1))
      },
    )
  }.asStream()

  @TestFactory fun `equal() should pass correctly with host types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("equal() should pass for equal strings") {
        testAssertEqual("hi", "hi")
        testAssertEqual("hello", "hello")
        testAssertEqual("", "")
      },
    )
    yield(
      dynamicTest("equal() should pass for equal bools or nulls") {
        testAssertEqual(null, null)
        testAssertEqual(left = true, right = true)
        testAssertEqual(left = false, right = false)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers") {
        testAssertEqual(1, 1)
        testAssertEqual(2, 2)
        testAssertEqual(20000, 20000)
        testAssertEqual(0, 0)
        testAssertEqual(-1, -1)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integer strings") {
        testAssertEqual(1, "1")
        testAssertEqual(2, "2")
        testAssertEqual(20000, "20000")
        testAssertEqual(0, "0")
        testAssertEqual(-1, "-1")
      },
    )
    yield(
      dynamicTest("equal() should pass for equal longs") {
        testAssertEqual(1L, 1L)
        testAssertEqual(2L, 2L)
        testAssertEqual(20000L, 20000L)
        testAssertEqual(0L, 0L)
        testAssertEqual(-1L, -1L)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal long strings") {
        testAssertEqual(1L, "1")
        testAssertEqual(2L, "2")
        testAssertEqual(20000L, "20000")
        testAssertEqual(0L, "0")
        testAssertEqual(-1L, "-1")
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts") {
        testAssertEqual(1.toShort(), 1.toShort())
        testAssertEqual(2.toShort(), 2.toShort())
        testAssertEqual(0.toShort(), 0.toShort())
        testAssertEqual((-1).toShort(), (-1).toShort())
      },
    )
    yield(
      dynamicTest("equal() should pass for equal short strings") {
        testAssertEqual(1.toShort(), "1")
        testAssertEqual(2.toShort(), "2")
        testAssertEqual(0.toShort(), "0")
        testAssertEqual((-1).toShort(), "-1")
      },
    )
    yield(
      dynamicTest("equal() should pass for equal floats") {
        testAssertEqual(1.0f, 1.0f)
        testAssertEqual(2.0f, 2.0f)
        testAssertEqual(2.3333f, 2.3333f)
        testAssertEqual(20000.0f, 20000.0f)
        testAssertEqual(0.0f, 0.0f)
        testAssertEqual(-1.0f, -1.0f)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal float strings") {
        testAssertEqual(1.0f, "1.0")
        testAssertEqual(2.0f, "2.0")
        testAssertEqual(20000.0f, "20000.0")
        testAssertEqual(0.0f, "0.0")
        testAssertEqual(-1.0f, "-1.0")
      },
    )
    yield(
      dynamicTest("equal() should pass for equal doubles") {
        testAssertEqual(1.0, 1.0)
        testAssertEqual(2.0, 2.0)
        testAssertEqual(2.3333, 2.3333)
        testAssertEqual(20000.0, 20000.0)
        testAssertEqual(0.0, 0.0)
        testAssertEqual(-1.0, -1.0)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal double strings") {
        testAssertEqual(1.0, "1.0")
        testAssertEqual(2.0, "2.0")
        testAssertEqual(20000.0, "20000.0")
        testAssertEqual(0.0, "0.0")
        testAssertEqual(-1.0, "-1.0")
      },
    )
    yield(
      dynamicTest("equal() should pass for equal big integers") {
        testAssertEqual(BigInteger.valueOf(1), BigInteger.valueOf(1))
        testAssertEqual(BigInteger.valueOf(2), BigInteger.valueOf(2))
        testAssertEqual(BigInteger.valueOf(20000), BigInteger.valueOf(20000))
        testAssertEqual(BigInteger.valueOf(0), BigInteger.valueOf(0))
        testAssertEqual(BigInteger.valueOf(0), BigInteger.ZERO)
        testAssertEqual(BigInteger.valueOf(-1), BigInteger.valueOf(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal big integer strings") {
        testAssertEqual(BigInteger.valueOf(1), "1")
        testAssertEqual(BigInteger.valueOf(2), "2")
        testAssertEqual(BigInteger.valueOf(20000), "20000")
        testAssertEqual(BigInteger.valueOf(0), "0")
        testAssertEqual(BigInteger.valueOf(0), "0")
        testAssertEqual(BigInteger.valueOf(-1), "-1")
      },
    )
    yield(
      dynamicTest("equal() should pass for equal uints") {
        testAssertEqual(1u, 1u)
        testAssertEqual(2u, 2u)
        testAssertEqual(20000u, 20000u)
        testAssertEqual(0u, 0u)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal uint strings") {
        testAssertEqual(1u, "1")
        testAssertEqual(2u, "2")
        testAssertEqual(20000u, "20000")
        testAssertEqual(0u, "0")
      },
    )

    // shorts first
    yield(
      dynamicTest("equal() should pass for equal shorts/longs") {
        testAssertEqual(1.toShort(), 1L)
        testAssertEqual(2.toShort(), 2L)
        testAssertEqual(20000.toShort(), 20000L)
        testAssertEqual(0.toShort(), 0L)
        testAssertEqual((-1).toShort(), -1L)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/integers") {
        testAssertEqual(1.toShort(), 1)
        testAssertEqual(2.toShort(), 2)
        testAssertEqual(0.toShort(), 0)
        testAssertEqual((-1).toShort(), -1)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/floats") {
        testAssertEqual(1.toShort(), 1.0f)
        testAssertEqual(2.toShort(), 2.0f)
        testAssertEqual(20000.toShort(), 20000.0f)
        testAssertEqual(0.toShort(), 0.0f)
        testAssertEqual((-1).toShort(), -1.0f)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/doubles") {
        testAssertEqual(1.toShort(), 1.0)
        testAssertEqual(2.toShort(), 2.0)
        testAssertEqual(20000.toShort(), 20000.0)
        testAssertEqual(0.toShort(), 0.0)
        testAssertEqual((-1).toShort(), -1.0)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/big integers") {
        testAssertEqual(1.toShort(), BigInteger.valueOf(1))
        testAssertEqual(2.toShort(), BigInteger.valueOf(2))
        testAssertEqual(20000.toShort(), BigInteger.valueOf(20000))
        testAssertEqual(0.toShort(), BigInteger.valueOf(0))
        testAssertEqual(0.toShort(), BigInteger.ZERO)
        testAssertEqual((-1).toShort(), BigInteger.valueOf(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/uints") {
        testAssertEqual(1.toShort(), 1u)
        testAssertEqual(2.toShort(), 2u)
        testAssertEqual(20000.toShort(), 20000u)
        testAssertEqual(0.toShort(), 0u)
      },
    )

    // integers first
    yield(
      dynamicTest("equal() should pass for equal integers/longs") {
        testAssertEqual(1, 1L)
        testAssertEqual(2, 2L)
        testAssertEqual(20000, 20000L)
        testAssertEqual(0, 0L)
        testAssertEqual(-1, -1L)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers/shorts") {
        testAssertEqual(1, 1.toShort())
        testAssertEqual(1.toShort(), 1)
        testAssertEqual(2, 2.toShort())
        testAssertEqual(2.toShort(), 2)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers/floats") {
        testAssertEqual(1, 1.0f)
        testAssertEqual(2, 2.0f)
        testAssertEqual(20000, 20000.0f)
        testAssertEqual(0, 0.0f)
        testAssertEqual(-1, -1.0f)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers/doubles") {
        testAssertEqual(1, 1.0)
        testAssertEqual(2, 2.0)
        testAssertEqual(20000, 20000.0)
        testAssertEqual(0, 0.0)
        testAssertEqual(-1, -1.0)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers/big integers") {
        testAssertEqual(1, BigInteger.valueOf(1))
        testAssertEqual(2, BigInteger.valueOf(2))
        testAssertEqual(20000, BigInteger.valueOf(20000))
        testAssertEqual(0, BigInteger.valueOf(0))
        testAssertEqual(0, BigInteger.ZERO)
        testAssertEqual(-1, BigInteger.valueOf(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/uints") {
        testAssertEqual(1, 1u)
        testAssertEqual(2, 2u)
        testAssertEqual(20000, 20000u)
        testAssertEqual(0, 0u)
      },
    )

    // uints first
    yield(
      dynamicTest("equal() should pass for equal uints/longs") {
        testAssertEqual(1u, 1L)
        testAssertEqual(2u, 2L)
        testAssertEqual(20000u, 20000L)
        testAssertEqual(0u, 0L)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal uints/shorts") {
        testAssertEqual(1u, 1.toShort())
        testAssertEqual(1.toShort(), 1u)
        testAssertEqual(2u, 2.toShort())
        testAssertEqual(2.toShort(), 2u)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal uints/floats") {
        testAssertEqual(1u, 1.0f)
        testAssertEqual(2u, 2.0f)
        testAssertEqual(20000u, 20000.0f)
        testAssertEqual(0u, 0.0f)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal uints/doubles") {
        testAssertEqual(1u, 1.0)
        testAssertEqual(2u, 2.0)
        testAssertEqual(20000u, 20000.0)
        testAssertEqual(0u, 0.0)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal uints/big integers") {
        testAssertEqual(1u, BigInteger.valueOf(1))
        testAssertEqual(2u, BigInteger.valueOf(2))
        testAssertEqual(20000u, BigInteger.valueOf(20000))
        testAssertEqual(0u, BigInteger.valueOf(0))
        testAssertEqual(0u, BigInteger.ZERO)
      },
    )

    // longs first
    yield(
      dynamicTest("equal() should pass for equal longs/floats") {
        testAssertEqual(1L, 1.0f)
        testAssertEqual(2L, 2.0f)
        testAssertEqual(20000L, 20000.0f)
        testAssertEqual(0L, 0.0f)
        testAssertEqual(-1L, -1.0f)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal longs/shorts") {
        testAssertEqual(1L, 1.toShort())
        testAssertEqual(2L, 2.toShort())
        testAssertEqual(20000L, 20000.toShort())
        testAssertEqual(0L, 0.toShort())
        testAssertEqual(-1L, (-1).toShort())
      },
    )
    yield(
      dynamicTest("equal() should pass for equal longs/doubles") {
        testAssertEqual(1L, 1.0)
        testAssertEqual(2L, 2.0)
        testAssertEqual(20000L, 20000.0)
        testAssertEqual(0L, 0.0)
        testAssertEqual(-1L, -1.0)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal longs/integers") {
        testAssertEqual(1L, 1)
        testAssertEqual(2L, 2)
        testAssertEqual(20000L, 20000)
        testAssertEqual(0L, 0)
        testAssertEqual(-1L, -1)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal longs/big integers") {
        testAssertEqual(1L, BigInteger.valueOf(1))
        testAssertEqual(2L, BigInteger.valueOf(2))
        testAssertEqual(20000L, BigInteger.valueOf(20000))
        testAssertEqual(0L, BigInteger.valueOf(0))
        testAssertEqual(0L, BigInteger.ZERO)
        testAssertEqual(-1L, BigInteger.valueOf(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/uints") {
        testAssertEqual(1L, 1u)
        testAssertEqual(2L, 2u)
        testAssertEqual(20000L, 20000u)
        testAssertEqual(0L, 0u)
      },
    )

    // floats first
    yield(
      dynamicTest("equal() should pass for equal floats/doubles") {
        testAssertEqual(1.0f, 1.0)
        testAssertEqual(2.0f, 2.0)
        testAssertEqual(2.3333f, 2.3333)
        testAssertEqual(20000.0f, 20000.0)
        testAssertEqual(0.0f, 0.0)
        testAssertEqual(-1.0f, -1.0)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal floats/integers") {
        testAssertEqual(1.0f, 1)
        testAssertEqual(2.0f, 2)
        testAssertEqual(20000.0f, 20000)
        testAssertEqual(0.0f, 0)
        testAssertEqual(-1.0f, -1)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal floats/longs") {
        testAssertEqual(1.0f, 1L)
        testAssertEqual(2.0f, 2L)
        testAssertEqual(20000.0f, 20000L)
        testAssertEqual(0.0f, 0L)
        testAssertEqual(-1.0f, -1L)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal floats/shorts") {
        testAssertEqual(1.0f, 1.toShort())
        testAssertEqual(2.0f, 2.toShort())
        testAssertEqual(20000.0f, 20000.toShort())
        testAssertEqual(0.0f, 0.toShort())
        testAssertEqual(-1.0f, (-1).toShort())
      },
    )
    yield(
      dynamicTest("equal() should pass for equal floats/big integers") {
        testAssertEqual(1.0f, BigInteger.valueOf(1))
        testAssertEqual(2.0f, BigInteger.valueOf(2))
        testAssertEqual(20000.0f, BigInteger.valueOf(20000))
        testAssertEqual(0.0f, BigInteger.valueOf(0))
        testAssertEqual(0.0f, BigInteger.ZERO)
        testAssertEqual(-1.0f, BigInteger.valueOf(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/uints") {
        testAssertEqual(1.0f, 1u)
        testAssertEqual(2.0f, 2u)
        testAssertEqual(20000.0f, 20000u)
        testAssertEqual(0.0f, 0u)
      },
    )

    // doubles first
    yield(
      dynamicTest("equal() should pass for equal doubles/shorts") {
        testAssertEqual(1.0, 1.toShort())
        testAssertEqual(2.0, 2.toShort())
        testAssertEqual(20000.0, 20000.toShort())
        testAssertEqual(0.0, 0.toShort())
        testAssertEqual(-1.0, (-1).toShort())
      },
    )
    yield(
      dynamicTest("equal() should pass for equal doubles/longs") {
        testAssertEqual(1.0, 1L)
        testAssertEqual(2.0, 2L)
        testAssertEqual(20000.0, 20000L)
        testAssertEqual(0.0, 0L)
        testAssertEqual(-1.0, -1L)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal doubles/floats") {
        testAssertEqual(1.0, 1.0f)
        testAssertEqual(2.0, 2.0f)
        testAssertEqual(20000.0, 20000.0f)
        testAssertEqual(0.0, 0.0f)
        testAssertEqual(-1.0, -1.0f)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal doubles/integers") {
        testAssertEqual(1.0, 1)
        testAssertEqual(2.0, 2)
        testAssertEqual(20000.0, 20000)
        testAssertEqual(0.0, 0)
        testAssertEqual(-1.0, -1)
      },
    )
    yield(
      dynamicTest("equal() should pass for equal doubles/big integers") {
        testAssertEqual(1.0, BigInteger.valueOf(1))
        testAssertEqual(2.0, BigInteger.valueOf(2))
        testAssertEqual(20000.0, BigInteger.valueOf(20000))
        testAssertEqual(0.0, BigInteger.valueOf(0))
        testAssertEqual(0.0, BigInteger.ZERO)
        testAssertEqual(-1.0, BigInteger.valueOf(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal shorts/uints") {
        testAssertEqual(1.0, 1u)
        testAssertEqual(2.0, 2u)
        testAssertEqual(20000.0, 20000u)
        testAssertEqual(0.0, 0u)
      },
    )
  }.asStream()

  @TestFactory fun `equal() should pass correctly with mixed types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("equal() with mixed types should pass for equal strings") {
        assert.equal("hi", asValue("hi"))
        assert.equal("hello", asValue("hello"))
        assert.equal("", asValue(""))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal bools or nulls") {
        assert.equal(null, asValue(null))
        assert.equal(actual = true, expected = asValue(true))
        assert.equal(actual = false, expected = asValue(false))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal integers") {
        assert.equal(1, asValue(1))
        assert.equal(2, asValue(2))
        assert.equal(20000, asValue(20000))
        assert.equal(0, asValue(0))
        assert.equal(-1, asValue(-1))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal longs") {
        assert.equal(1L, asValue(1L))
        assert.equal(2L, asValue(2L))
        assert.equal(20000L, asValue(20000L))
        assert.equal(0L, asValue(0L))
        assert.equal(-1L, asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal shorts") {
        assert.equal(1.toShort(), asValue(1.toShort()))
        assert.equal(2.toShort(), asValue(2.toShort()))
        assert.equal(0.toShort(), asValue(0.toShort()))
        assert.equal((-1).toShort(), asValue((-1).toShort()))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal floats") {
        assert.equal(1.0f, asValue(1.0f))
        assert.equal(2.0f, asValue(2.0f))
        assert.equal(2.3333f, asValue(2.3333f))
        assert.equal(20000.0f, asValue(20000.0f))
        assert.equal(0.0f, asValue(0.0f))
        assert.equal(-1.0f, asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal doubles") {
        assert.equal(1.0, asValue(1.0))
        assert.equal(2.0, asValue(2.0))
        assert.equal(2.3333, asValue(2.3333))
        assert.equal(20000.0, asValue(20000.0))
        assert.equal(0.0, asValue(0.0))
        assert.equal(-1.0, asValue(-1.0))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal big integers") {
        assert.equal(BigInteger.valueOf(1), asValue(BigInteger.valueOf(1)))
        assert.equal(BigInteger.valueOf(2), asValue(BigInteger.valueOf(2)))
        assert.equal(BigInteger.valueOf(20000), asValue(BigInteger.valueOf(20000)))
        assert.equal(BigInteger.valueOf(0), asValue(BigInteger.valueOf(0)))
        assert.equal(BigInteger.valueOf(0), asValue(BigInteger.ZERO))
        assert.equal(BigInteger.valueOf(-1), asValue(BigInteger.valueOf(-1)))
      },
    )

    // shorts first
    yield(
      dynamicTest("equal() with mixed types should pass for equal shorts/longs") {
        assert.equal(1.toShort(), asValue(1L))
        assert.equal(2.toShort(), asValue(2L))
        assert.equal(20000.toShort(), asValue(20000L))
        assert.equal(0.toShort(), asValue(0L))
        assert.equal((-1).toShort(), asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal shorts/integers") {
        assert.equal(1.toShort(), asValue(1))
        assert.equal(2.toShort(), asValue(2))
        assert.equal(0.toShort(), asValue(0))
        assert.equal((-1).toShort(), asValue(-1))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal shorts/floats") {
        assert.equal(1.toShort(), asValue(1.0f))
        assert.equal(2.toShort(), asValue(2.0f))
        assert.equal(20000.toShort(), asValue(20000.0f))
        assert.equal(0.toShort(), asValue(0.0f))
        assert.equal((-1).toShort(), asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal shorts/doubles") {
        assert.equal(1.toShort(), asValue(1.0))
        assert.equal(2.toShort(), asValue(2.0))
        assert.equal(20000.toShort(), asValue(20000.0))
        assert.equal(0.toShort(), asValue(0.0))
        assert.equal((-1).toShort(), asValue(-1.0))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal shorts/big integers") {
        assert.equal(1.toShort(), asValue(BigInteger.valueOf(1)))
        assert.equal(2.toShort(), asValue(BigInteger.valueOf(2)))
        assert.equal(20000.toShort(), asValue(BigInteger.valueOf(20000)))
        assert.equal(0.toShort(), asValue(BigInteger.valueOf(0)))
        assert.equal(0.toShort(), asValue(BigInteger.ZERO))
        assert.equal((-1).toShort(), asValue(BigInteger.valueOf(-1)))
      },
    )

    // integers first
    yield(
      dynamicTest("equal() with mixed types should pass for equal integers/longs") {
        assert.equal(1, asValue(1L))
        assert.equal(2, asValue(2L))
        assert.equal(20000, asValue(20000L))
        assert.equal(0, asValue(0L))
        assert.equal(-1, asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal integers/shorts") {
        assert.equal(1, asValue(1.toShort()))
        assert.equal(1.toShort(), asValue(1))
        assert.equal(2, asValue(2.toShort()))
        assert.equal(2.toShort(), asValue(2))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal integers/floats") {
        assert.equal(1, asValue(1.0f))
        assert.equal(2, asValue(2.0f))
        assert.equal(20000, asValue(20000.0f))
        assert.equal(0, asValue(0.0f))
        assert.equal(-1, asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal integers/doubles") {
        assert.equal(1, asValue(1.0))
        assert.equal(2, asValue(2.0))
        assert.equal(20000, asValue(20000.0))
        assert.equal(0, asValue(0.0))
        assert.equal(-1, asValue(-1.0))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal integers/big integers") {
        assert.equal(1, asValue(BigInteger.valueOf(1)))
        assert.equal(2, asValue(BigInteger.valueOf(2)))
        assert.equal(20000, asValue(BigInteger.valueOf(20000)))
        assert.equal(0, asValue(BigInteger.valueOf(0)))
        assert.equal(0, asValue(BigInteger.ZERO))
        assert.equal(-1, asValue(BigInteger.valueOf(-1)))
      },
    )

    // longs first
    yield(
      dynamicTest("equal() with mixed types should pass for equal longs/floats") {
        assert.equal(1L, asValue(1.0f))
        assert.equal(2L, asValue(2.0f))
        assert.equal(20000L, asValue(20000.0f))
        assert.equal(0L, asValue(0.0f))
        assert.equal(-1L, asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal longs/shorts") {
        assert.equal(1L, asValue(1.toShort()))
        assert.equal(2L, asValue(2.toShort()))
        assert.equal(20000L, asValue(20000.toShort()))
        assert.equal(0L, asValue(0.toShort()))
        assert.equal(-1L, asValue((-1).toShort()))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal longs/doubles") {
        assert.equal(1L, asValue(1.0))
        assert.equal(2L, asValue(2.0))
        assert.equal(20000L, asValue(20000.0))
        assert.equal(0L, asValue(0.0))
        assert.equal(-1L, asValue(-1.0))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal longs/integers") {
        assert.equal(1L, asValue(1))
        assert.equal(2L, asValue(2))
        assert.equal(20000L, asValue(20000))
        assert.equal(0L, asValue(0))
        assert.equal(-1L, asValue(-1))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal longs/big integers") {
        assert.equal(1L, asValue(BigInteger.valueOf(1)))
        assert.equal(2L, asValue(BigInteger.valueOf(2)))
        assert.equal(20000L, asValue(BigInteger.valueOf(20000)))
        assert.equal(0L, asValue(BigInteger.valueOf(0)))
        assert.equal(0L, asValue(BigInteger.ZERO))
        assert.equal(-1L, asValue(BigInteger.valueOf(-1)))
      },
    )

    // floats first
    yield(
      dynamicTest("equal() with mixed types should pass for equal floats/doubles") {
        assert.equal(1.0f, asValue(1.0))
        assert.equal(2.0f, asValue(2.0))
        // assert.equal(2.3333f, asValue(2.3333))  @TODO(sgammon): file with gvm for precision issue
        assert.equal(20000.0f, asValue(20000.0))
        assert.equal(0.0f, asValue(0.0))
        assert.equal(-1.0f, asValue(-1.0))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal floats/integers") {
        assert.equal(1.0f, asValue(1))
        assert.equal(2.0f, asValue(2))
        assert.equal(20000.0f, asValue(20000))
        assert.equal(0.0f, asValue(0))
        assert.equal(-1.0f, asValue(-1))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal floats/longs") {
        assert.equal(1.0f, asValue(1L))
        assert.equal(2.0f, asValue(2L))
        assert.equal(20000.0f, asValue(20000L))
        assert.equal(0.0f, asValue(0L))
        assert.equal(-1.0f, asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal floats/shorts") {
        assert.equal(1.0f, asValue(1.toShort()))
        assert.equal(2.0f, asValue(2.toShort()))
        assert.equal(20000.0f, asValue(20000.toShort()))
        assert.equal(0.0f, asValue(0.toShort()))
        assert.equal(-1.0f, asValue((-1).toShort()))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal floats/big integers") {
        assert.equal(1.0f, asValue(BigInteger.valueOf(1)))
        assert.equal(2.0f, asValue(BigInteger.valueOf(2)))
        assert.equal(20000.0f, asValue(BigInteger.valueOf(20000)))
        assert.equal(0.0f, asValue(BigInteger.valueOf(0)))
        assert.equal(0.0f, asValue(BigInteger.ZERO))
        assert.equal(-1.0f, asValue(BigInteger.valueOf(-1)))
      },
    )

    // doubles first
    yield(
      dynamicTest("equal() with mixed types should pass for equal doubles/shorts") {
        assert.equal(1.0, asValue(1.toShort()))
        assert.equal(2.0, asValue(2.toShort()))
        assert.equal(20000.0, asValue(20000.toShort()))
        assert.equal(0.0, asValue(0.toShort()))
        assert.equal(-1.0, asValue((-1).toShort()))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal doubles/longs") {
        assert.equal(1.0, asValue(1L))
        assert.equal(2.0, asValue(2L))
        assert.equal(20000.0, asValue(20000L))
        assert.equal(0.0, asValue(0L))
        assert.equal(-1.0, asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal doubles/floats") {
        assert.equal(1.0, asValue(1.0f))
        assert.equal(2.0, asValue(2.0f))
        assert.equal(20000.0, asValue(20000.0f))
        assert.equal(0.0, asValue(0.0f))
        assert.equal(-1.0, asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal doubles/integers") {
        assert.equal(1.0, asValue(1))
        assert.equal(2.0, asValue(2))
        assert.equal(20000.0, asValue(20000))
        assert.equal(0.0, asValue(0))
        assert.equal(-1.0, asValue(-1))
      },
    )
    yield(
      dynamicTest("equal() with mixed types should pass for equal doubles/big integers") {
        assert.equal(1.0, asValue(BigInteger.valueOf(1)))
        assert.equal(2.0, asValue(BigInteger.valueOf(2)))
        assert.equal(20000.0, asValue(BigInteger.valueOf(20000)))
        assert.equal(0.0, asValue(BigInteger.valueOf(0)))
        assert.equal(0.0, asValue(BigInteger.ZERO))
        assert.equal(-1.0, asValue(BigInteger.valueOf(-1)))
      },
    )
  }.asStream()

  @TestFactory fun `equal() should fail correctly with host types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("equal() should fail for unequal strings") {
        assertThrows<NodeAssertionError> { testAssertEqual("hi", "hello") }
        assertThrows<NodeAssertionError> { testAssertEqual("hello", "hi") }
        assertThrows<NodeAssertionError> { testAssertEqual("", "hi") }
        assertThrows<NodeAssertionError> { testAssertEqual("hi", "") }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal bools or nulls") {
        assertThrows<NodeAssertionError> { testAssertEqual(null, "hi") }
        assertThrows<NodeAssertionError> { testAssertEqual(null, true) }
        assertThrows<NodeAssertionError> { testAssertEqual("hi", null) }
        assertThrows<NodeAssertionError> { testAssertEqual(true, null) }
        assertThrows<NodeAssertionError> { testAssertEqual(null, 0) }
        assertThrows<NodeAssertionError> { testAssertEqual(null, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(0, null) }
        assertThrows<NodeAssertionError> { testAssertEqual(1, null) }
        assertThrows<NodeAssertionError> { testAssertEqual(left = true, right = false) }
        assertThrows<NodeAssertionError> { testAssertEqual(left = false, right = true) }
      },
    )

    yield(
      dynamicTest("equal() should fail for unequal integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1, 2) }
        assertThrows<NodeAssertionError> { testAssertEqual(1, "2") }
        assertThrows<NodeAssertionError> { testAssertEqual(2, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(2, "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(20000, 20001) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000, "20001") }
        assertThrows<NodeAssertionError> { testAssertEqual(20001, 20000) }
        assertThrows<NodeAssertionError> { testAssertEqual(20001, "20000") }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, -2) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, "-2") }
        assertThrows<NodeAssertionError> { testAssertEqual(-2, -1) }
        assertThrows<NodeAssertionError> { testAssertEqual(-2, "-1") }
        assertThrows<NodeAssertionError> { testAssertEqual(0, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(0, "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(1, 0) }
        assertThrows<NodeAssertionError> { testAssertEqual(1, "0") }
        assertThrows<NodeAssertionError> { testAssertEqual(0, -1) }
        assertThrows<NodeAssertionError> { testAssertEqual(0, "-1") }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, 0) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, "0") }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal longs") {
        assertThrows<NodeAssertionError> { testAssertEqual(1L, 2L) }
        assertThrows<NodeAssertionError> { testAssertEqual(1L, "2") }
        assertThrows<NodeAssertionError> { testAssertEqual(2L, 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(2L, "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(20000L, 20001L) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000L, "20001") }
        assertThrows<NodeAssertionError> { testAssertEqual(20001L, 20000L) }
        assertThrows<NodeAssertionError> { testAssertEqual(20001L, "20000") }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, -2L) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, "-2") }
        assertThrows<NodeAssertionError> { testAssertEqual(-2L, -1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(-2L, "-1") }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(1L, 0L) }
        assertThrows<NodeAssertionError> { testAssertEqual(1L, "0") }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, -1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, "-1") }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, 0L) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, "0") }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal shorts") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), 2.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), "2") }
        assertThrows<NodeAssertionError> { testAssertEqual(2.toShort(), 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.toShort(), "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.toShort(), 20001.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.toShort(), "20001") }
        assertThrows<NodeAssertionError> { testAssertEqual(20001.toShort(), 20000.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(20001.toShort(), "20000") }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), (-2).toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), "-2") }
        assertThrows<NodeAssertionError> { testAssertEqual((-2).toShort(), (-1).toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual((-2).toShort(), "-1") }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), 0.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), "0") }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), (-1).toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), "-1") }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), 0.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), "0") }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0f, 1.1f) }
        assertThrows<NodeAssertionError> { testAssertEqual(1.0f, "1.1") }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0f, 2.1f) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0f, "2.1") }
        assertThrows<NodeAssertionError> { testAssertEqual(2.3333f, 2.4444f) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.3333f, "2.4444") }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0f, 20001.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0f, "20001.0") }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0f, 0.1f) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0f, "0.1") }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0f, -1.1f) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0f, "-1.1") }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal doubles") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0, 1.1) }
        assertThrows<NodeAssertionError> { testAssertEqual(1.0, "1.1") }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0, 2.1) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0, "2.1") }
        assertThrows<NodeAssertionError> { testAssertEqual(2.3333, 2.4444) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.3333, "2.4444") }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0, 20001.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0, "20001.0") }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0, 0.1) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0, "0.1") }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0, -1.1) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0, "-1.1") }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal uints") {
        assertThrows<NodeAssertionError> { testAssertEqual(1u, 2u) }
        assertThrows<NodeAssertionError> { testAssertEqual(1u, "2") }
        assertThrows<NodeAssertionError> { testAssertEqual(2u, 1u) }
        assertThrows<NodeAssertionError> { testAssertEqual(2u, "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(20000u, 20001u) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000u, "20001") }
        assertThrows<NodeAssertionError> { testAssertEqual(0u, 1u) }
        assertThrows<NodeAssertionError> { testAssertEqual(0u, "1") }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal big integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(1), BigInteger.valueOf(2)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(1), "2") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(2), BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(2), "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(20000), BigInteger.valueOf(20001)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(20000), "20001") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(20001), BigInteger.valueOf(20000)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(20001), "20000") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(-1), BigInteger.valueOf(-2)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(-1), "-2") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(-2), BigInteger.valueOf(-1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(-2), "-1") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(0), BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(0), "1") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(1), BigInteger.valueOf(0)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(1), "0") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(0), BigInteger.valueOf(-1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(0), "-1") }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(-1), BigInteger.valueOf(0)) }
        assertThrows<NodeAssertionError> { testAssertEqual(BigInteger.valueOf(-1), "0") }
      },
    )

    // shorts first
    yield(
      dynamicTest("equal() should fail for unequal shorts/integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), 2) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.toShort(), 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.toShort(), 20001) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), 1) }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), 1) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal shorts/longs") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), 2L) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.toShort(), 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.toShort(), 20001L) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), 1L) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal shorts/floats") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), 2.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.toShort(), 1.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.toShort(), 20001.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), 1.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), 1.0f) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal shorts/doubles") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), 2.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.toShort(), 1.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.toShort(), 20001.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), 1.0) }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), 1.0) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal shorts/big integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.toShort(), BigInteger.valueOf(2)) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.toShort(), BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.toShort(), BigInteger.valueOf(20001)) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.toShort(), BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual((-1).toShort(), BigInteger.valueOf(1)) }
      },
    )

    // integers first
    yield(
      dynamicTest("equal() should fail for unequal integers/shorts") {
        assertThrows<NodeAssertionError> { testAssertEqual(1, 2.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(2, 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000, 20001.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(0, 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, 1.toShort()) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal integers/longs") {
        assertThrows<NodeAssertionError> { testAssertEqual(1, 2L) }
        assertThrows<NodeAssertionError> { testAssertEqual(2, 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000, 20001L) }
        assertThrows<NodeAssertionError> { testAssertEqual(0, 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, 1L) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal integers/floats") {
        assertThrows<NodeAssertionError> { testAssertEqual(1, 2.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(2, 1.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000, 20001.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(0, 1.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, 1.0f) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal integers/doubles") {
        assertThrows<NodeAssertionError> { testAssertEqual(1, 2.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(2, 1.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000, 20001.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(0, 1.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, 1.0) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal integers/big integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1, BigInteger.valueOf(2)) }
        assertThrows<NodeAssertionError> { testAssertEqual(2, BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000, BigInteger.valueOf(20001)) }
        assertThrows<NodeAssertionError> { testAssertEqual(0, BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1, BigInteger.valueOf(1)) }
      },
    )

    // longs first
    yield(
      dynamicTest("equal() should fail for unequal longs/shorts") {
        assertThrows<NodeAssertionError> { testAssertEqual(1L, 2.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(2L, 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000L, 20001.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, 1.toShort()) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal longs/integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1L, 2) }
        assertThrows<NodeAssertionError> { testAssertEqual(2L, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000L, 20001) }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, 1) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal longs/floats") {
        assertThrows<NodeAssertionError> { testAssertEqual(1L, 2.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(2L, 1.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000L, 20001.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, 1.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, 1.0f) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal longs/doubles") {
        assertThrows<NodeAssertionError> { testAssertEqual(1L, 2.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(2L, 1.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000L, 20001.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, 1.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, 1.0) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal longs/big integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1L, BigInteger.valueOf(2)) }
        assertThrows<NodeAssertionError> { testAssertEqual(2L, BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000L, BigInteger.valueOf(20001)) }
        assertThrows<NodeAssertionError> { testAssertEqual(0L, BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1L, BigInteger.valueOf(1)) }
      },
    )

    // floats first
    yield(
      dynamicTest("equal() should fail for unequal floats/shorts") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0f, 2.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0f, 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0f, 20001.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0f, 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0f, 1.toShort()) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats/integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0f, 2) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0f, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0f, 20001) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0f, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0f, 1) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats/longs") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0f, 2L) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0f, 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0f, 20001L) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0f, 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0f, 1L) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats/doubles") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0f, 2.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0f, 1.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0f, 20001.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0f, 1.0) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0f, 1.0) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats/big integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0f, BigInteger.valueOf(2)) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0f, BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0f, BigInteger.valueOf(20001)) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0f, BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0f, BigInteger.valueOf(1)) }
      },
    )

    // doubles first
    yield(
      dynamicTest("equal() should fail for unequal floats/shorts") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0, 2.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0, 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0, 20001.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0, 1.toShort()) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0, 1.toShort()) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats/integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0, 2) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0, 20001) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0, 1) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0, 1) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats/longs") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0, 2L) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0, 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0, 20001L) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0, 1L) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0, 1L) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats/floats") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0, 2.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0, 1.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0, 20001.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0, 1.0f) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0, 1.0f) }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal doubles/big integers") {
        assertThrows<NodeAssertionError> { testAssertEqual(1.0, BigInteger.valueOf(2)) }
        assertThrows<NodeAssertionError> { testAssertEqual(2.0, BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(20000.0, BigInteger.valueOf(20001)) }
        assertThrows<NodeAssertionError> { testAssertEqual(0.0, BigInteger.valueOf(1)) }
        assertThrows<NodeAssertionError> { testAssertEqual(-1.0, BigInteger.valueOf(1)) }
      },
    )
  }.asStream()

  @TestFactory fun `strict() should fail correctly with host types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("strict() should fail for unequal strings") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal integers") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal longs") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal integers/longs") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal floats") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal integers/floats") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal longs/floats") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal doubles") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal integers/doubles") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal longs/doubles") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal floats/doubles") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal uints") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal bools or nulls") {
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
      },
    )
  }.asStream()

  @TestFactory fun `equal() should fail correctly with guest types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("equal() should fail for unequal strings") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue("hi"), asValue("hello"))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue("hello"), asValue("hi"))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(""), asValue("hi"))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue("hi"), asValue(""))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal integers") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1), asValue(2))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2), asValue(1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000), asValue(20001))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20001), asValue(20000))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1), asValue(-2))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-2), asValue(-1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0), asValue(1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1), asValue(0))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0), asValue(-1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1), asValue(0))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal longs") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1L), asValue(2L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2L), asValue(1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000L), asValue(20001L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20001L), asValue(20000L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1L), asValue(-2L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-2L), asValue(-1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0L), asValue(1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1L), asValue(0L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0L), asValue(-1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1L), asValue(0L))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal integers/longs") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1), asValue(2L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2L), asValue(1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000), asValue(20001L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0), asValue(1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1), asValue(1L))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1.0f), asValue(1.1f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.0f), asValue(2.1f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.3333f), asValue(2.4444f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000.0f), asValue(20001.0f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0.0f), asValue(0.1f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1.0f), asValue(-1.1f))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal integers/floats") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1.0f), asValue(2))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.0f), asValue(0))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000.0f), asValue(20001))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0.0f), asValue(1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1.0f), asValue(-2))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal longs/floats") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1.0f), asValue(2L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.0f), asValue(1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000.0f), asValue(20001L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0.0f), asValue(1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1.0f), asValue(0L))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal doubles") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1.0), asValue(1.1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.0), asValue(2.1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.3333), asValue(2.4444))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000.0), asValue(20001.0))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0.0), asValue(0.1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1.0), asValue(-1.1))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal integers/doubles") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1.0), asValue(2))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.0), asValue(3))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000.0), asValue(20001))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0.0), asValue(1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1.0), asValue(-2))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal longs/doubles") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1.0), asValue(2L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.0), asValue(1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000.0), asValue(20001L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0.0), asValue(1L))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1.0), asValue(-2L))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal floats/doubles") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1.0), asValue(1.1f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.0), asValue(2.1f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(2.3333), asValue(2.4444f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(20000.0), asValue(20001.0f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0.0), asValue(0.1f))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(-1.0), asValue(-1.1f))
        }
      },
    )
    yield(
      dynamicTest("equal() should fail for unequal bools or nulls") {
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(null), asValue("hi"))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(null), asValue(true))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue("hi"), asValue(null))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(true), asValue(null))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(null), asValue(0))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(null), asValue(1))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(0), asValue(null))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(asValue(1), asValue(null))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(left = asValue(true), right = asValue(false))
        }
        assertThrows<NodeAssertionError> {
          testAssertEqual(left = asValue(false), right = asValue(true))
        }
      },
    )
  }.asStream()

  @TestFactory fun `strict() should fail correctly with guest types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("strict() should fail for unequal strings") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal integers") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal longs") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal integers/longs") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal floats") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal integers/floats") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal longs/floats") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal doubles") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal integers/doubles") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal longs/doubles") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal floats/doubles") {
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
      },
    )
    yield(
      dynamicTest("strict() should fail for unequal bools or nulls") {
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
      },
    )
  }.asStream()

  @TestFactory fun `equal() should pass correctly with guest types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("equal() should pass for equal strings") {
        testAssertEqual(asValue("hi"), asValue("hi"))
        testAssertEqual(asValue("hello"), asValue("hello"))
        testAssertEqual(asValue(""), asValue(""))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers") {
        testAssertEqual(asValue(1), asValue(1))
        testAssertEqual(asValue(2), asValue(2))
        testAssertEqual(asValue(20000), asValue(20000))
        testAssertEqual(asValue(0), asValue(0))
        testAssertEqual(asValue(-1), asValue(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal longs") {
        testAssertEqual(asValue(1L), asValue(1L))
        testAssertEqual(asValue(2L), asValue(2L))
        testAssertEqual(asValue(20000L), asValue(20000L))
        testAssertEqual(asValue(0L), asValue(0L))
        testAssertEqual(asValue(-1L), asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers/longs") {
        testAssertEqual(asValue(1), asValue(1L))
        testAssertEqual(asValue(2), asValue(2L))
        testAssertEqual(asValue(20000), asValue(20000L))
        testAssertEqual(asValue(0), asValue(0L))
        testAssertEqual(asValue(-1), asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal floats") {
        testAssertEqual(asValue(1.0f), asValue(1.0f))
        testAssertEqual(asValue(2.0f), asValue(2.0f))
        testAssertEqual(asValue(2.3333f), asValue(2.3333f))
        testAssertEqual(asValue(20000.0f), asValue(20000.0f))
        testAssertEqual(asValue(0.0f), asValue(0.0f))
        testAssertEqual(asValue(-1.0f), asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers/floats") {
        testAssertEqual(asValue(1.0f), asValue(1))
        testAssertEqual(asValue(2.0f), asValue(2))
        testAssertEqual(asValue(20000.0f), asValue(20000))
        testAssertEqual(asValue(0.0f), asValue(0))
        testAssertEqual(asValue(-1.0f), asValue(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal longs/floats") {
        testAssertEqual(asValue(1.0f), asValue(1L))
        testAssertEqual(asValue(2.0f), asValue(2L))
        testAssertEqual(asValue(20000.0f), asValue(20000L))
        testAssertEqual(asValue(0.0f), asValue(0L))
        testAssertEqual(asValue(-1.0f), asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal doubles") {
        testAssertEqual(asValue(1.0), asValue(1.0))
        testAssertEqual(asValue(2.0), asValue(2.0))
        testAssertEqual(asValue(2.3333), asValue(2.3333))
        testAssertEqual(asValue(20000.0), asValue(20000.0))
        testAssertEqual(asValue(0.0), asValue(0.0))
        testAssertEqual(asValue(-1.0), asValue(-1.0))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal integers/doubles") {
        testAssertEqual(asValue(1.0), asValue(1))
        testAssertEqual(asValue(2.0), asValue(2))
        testAssertEqual(asValue(20000.0), asValue(20000))
        testAssertEqual(asValue(0.0), asValue(0))
        testAssertEqual(asValue(-1.0), asValue(-1))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal longs/doubles") {
        testAssertEqual(asValue(1.0), asValue(1L))
        testAssertEqual(asValue(2.0), asValue(2L))
        testAssertEqual(asValue(20000.0), asValue(20000L))
        testAssertEqual(asValue(0.0), asValue(0L))
        testAssertEqual(asValue(-1.0), asValue(-1L))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal floats/doubles") {
        testAssertEqual(asValue(1.0), asValue(1.0f))
        testAssertEqual(asValue(2.0), asValue(2.0f))
        // testAssertEqual(asValue(2.3333), asValue(2.3333f))  @TODO(sgammon): file with gvm for precision issue
        testAssertEqual(asValue(20000.0), asValue(20000.0f))
        testAssertEqual(asValue(0.0), asValue(0.0f))
        testAssertEqual(asValue(-1.0), asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("equal() should pass for equal bools or nulls") {
        testAssertEqual(asValue(null), asValue(null))
        testAssertEqual(left = asValue(true), right = asValue(true))
        testAssertEqual(left = asValue(false), right = asValue(false))
      },
    )
  }.asStream()

  @TestFactory fun `strict() should pass correctly with host types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("strict() should pass for equal strings") {
        assert.strict("hi", "hi")
        assert.strict("hello", "hello")
        assert.strict("", "")
      },
    )
    yield(
      dynamicTest("strict() should pass for equal integers/longs") {
        assert.strict(1, 1L)
        assert.strict(2, 2L)
        assert.strict(20000, 20000L)
        assert.strict(0, 0L)
        assert.strict(-1, -1L)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal integers/floats") {
        assert.strict(1.0f, 1)
        assert.strict(2.0f, 2)
        assert.strict(20000.0f, 20000)
        assert.strict(0.0f, 0)
        assert.strict(-1.0f, -1)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal longs/floats") {
        assert.strict(1.0f, 1L)
        assert.strict(2.0f, 2L)
        assert.strict(20000.0f, 20000L)
        assert.strict(0.0f, 0L)
        assert.strict(-1.0f, -1L)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal integers/doubles") {
        assert.strict(1.0, 1)
        assert.strict(2.0, 2)
        assert.strict(20000.0, 20000)
        assert.strict(0.0, 0)
        assert.strict(-1.0, -1)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal longs/doubles") {
        assert.strict(1.0, 1L)
        assert.strict(2.0, 2L)
        assert.strict(20000.0, 20000L)
        assert.strict(0.0, 0L)
        assert.strict(-1.0, -1L)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal floats/doubles") {
        assert.strict(1.0, 1.0f)
        assert.strict(2.0, 2.0f)
        // assert.strict(2.3333, 2.3333f) @TODO(sgammon): file with gvm for precision issue
        assert.strict(20000.0, 20000.0f)
        assert.strict(0.0, 0.0f)
        assert.strict(-1.0, -1.0f)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal strings") {
        assert.strict("hi", "hi")
        assert.strict("hello", "hello")
        assert.strict("", "")
      },
    )
    yield(
      dynamicTest("strict() should pass for equal integers") {
        assert.strict(1, 1)
        assert.strict(2, 2)
        assert.strict(20000, 20000)
        assert.strict(0, 0)
        assert.strict(-1, -1)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal longs") {
        assert.strict(1L, 1L)
        assert.strict(2L, 2L)
        assert.strict(20000L, 20000L)
        assert.strict(0L, 0L)
        assert.strict(-1L, -1L)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal floats") {
        assert.strict(1.0f, 1.0f)
        assert.strict(2.0f, 2.0f)
        assert.strict(2.3333f, 2.3333f)
        assert.strict(20000.0f, 20000.0f)
        assert.strict(0.0f, 0.0f)
        assert.strict(-1.0f, -1.0f)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal doubles") {
        assert.strict(1.0, 1.0)
        assert.strict(2.0, 2.0)
        assert.strict(2.3333, 2.3333)
        assert.strict(20000.0, 20000.0)
        assert.strict(0.0, 0.0)
        assert.strict(-1.0, -1.0)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal uints") {
        assert.strict(1u, 1u)
        assert.strict(2u, 2u)
        assert.strict(20000u, 20000u)
        assert.strict(0u, 0u)
      },
    )
    yield(
      dynamicTest("strict() should pass for equal bools or nulls") {
        assert.strict(null, null)
        assert.strict(actual = true, expected = true)
        assert.strict(actual = false, expected = false)
      },
    )
  }.asStream()

  @TestFactory fun `strict() should pass correctly with guest types`(): Stream<DynamicTest> = sequence {
    yield(
      dynamicTest("strict() should pass for equal strings") {
        assert.strict(asValue("hi"), asValue("hi"))
        assert.strict(asValue("hello"), asValue("hello"))
        assert.strict(asValue(""), asValue(""))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal integers") {
        assert.strict(asValue(1), asValue(1))
        assert.strict(asValue(2), asValue(2))
        assert.strict(asValue(20000), asValue(20000))
        assert.strict(asValue(0), asValue(0))
        assert.strict(asValue(-1), asValue(-1))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal longs") {
        assert.strict(asValue(1L), asValue(1L))
        assert.strict(asValue(2L), asValue(2L))
        assert.strict(asValue(20000L), asValue(20000L))
        assert.strict(asValue(0L), asValue(0L))
        assert.strict(asValue(-1L), asValue(-1L))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal integers/longs") {
        assert.strict(asValue(1), asValue(1L))
        assert.strict(asValue(2), asValue(2L))
        assert.strict(asValue(20000), asValue(20000L))
        assert.strict(asValue(0), asValue(0L))
        assert.strict(asValue(-1), asValue(-1L))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal floats") {
        assert.strict(asValue(1.0f), asValue(1.0f))
        assert.strict(asValue(2.0f), asValue(2.0f))
        assert.strict(asValue(2.3333f), asValue(2.3333f))
        assert.strict(asValue(20000.0f), asValue(20000.0f))
        assert.strict(asValue(0.0f), asValue(0.0f))
        assert.strict(asValue(-1.0f), asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal integers/floats") {
        assert.strict(asValue(1.0f), asValue(1))
        assert.strict(asValue(2.0f), asValue(2))
        assert.strict(asValue(20000.0f), asValue(20000))
        assert.strict(asValue(0.0f), asValue(0))
        assert.strict(asValue(-1.0f), asValue(-1))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal longs/floats") {
        assert.strict(asValue(1.0f), asValue(1L))
        assert.strict(asValue(2.0f), asValue(2L))
        assert.strict(asValue(20000.0f), asValue(20000L))
        assert.strict(asValue(0.0f), asValue(0L))
        assert.strict(asValue(-1.0f), asValue(-1L))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal doubles") {
        assert.strict(asValue(1.0), asValue(1.0))
        assert.strict(asValue(2.0), asValue(2.0))
        assert.strict(asValue(2.3333), asValue(2.3333))
        assert.strict(asValue(20000.0), asValue(20000.0))
        assert.strict(asValue(0.0), asValue(0.0))
        assert.strict(asValue(-1.0), asValue(-1.0))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal integers/doubles") {
        assert.strict(asValue(1.0), asValue(1))
        assert.strict(asValue(2.0), asValue(2))
        assert.strict(asValue(20000.0), asValue(20000))
        assert.strict(asValue(0.0), asValue(0))
        assert.strict(asValue(-1.0), asValue(-1))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal longs/doubles") {
        assert.strict(asValue(1.0), asValue(1L))
        assert.strict(asValue(2.0), asValue(2L))
        assert.strict(asValue(20000.0), asValue(20000L))
        assert.strict(asValue(0.0), asValue(0L))
        assert.strict(asValue(-1.0), asValue(-1L))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal floats/doubles") {
        assert.strict(asValue(1.0), asValue(1.0f))
        assert.strict(asValue(2.0), asValue(2.0f))
        // assert.strict(asValue(2.3333), asValue(2.3333f))  @TODO(sgammon): file with gvm for precision issue
        assert.strict(asValue(20000.0), asValue(20000.0f))
        assert.strict(asValue(0.0), asValue(0.0f))
        assert.strict(asValue(-1.0), asValue(-1.0f))
      },
    )
    yield(
      dynamicTest("strict() should pass for equal bools or nulls") {
        assert.strict(asValue(null), asValue(null))
        assert.strict(actual = asValue(true), expected = asValue(true))
        assert.strict(actual = asValue(false), expected = asValue(false))
      },
    )
  }.asStream()

  @Test fun `ok() should work for falsy host values`() {
    assert.notOk(false)
    assertThrows<NodeAssertionError> {
      assert.ok(false)
    }
    assert.notOk(null)
    assertThrows<NodeAssertionError> {
      assert.ok(null)
    }
    assert.notOk(emptyList<String>())
    assertThrows<NodeAssertionError> {
      assert.ok(emptyList<String>())
    }
    assert.notOk(emptyMap<String, String>())
    assertThrows<NodeAssertionError> {
      assert.ok(emptyMap<String, String>())
    }
    assert.notOk(emptyList<String>().iterator())
    assertThrows<NodeAssertionError> {
      assert.ok(emptyList<String>().iterator())
    }
    assert.notOk(Unit)
    assertThrows<NodeAssertionError> {
      assert.ok(Unit)
    }
    assert.notOk(0.toShort())
    assertThrows<NodeAssertionError> {
      assert.ok(0.toShort())
    }
    assert.notOk(0)
    assertThrows<NodeAssertionError> {
      assert.ok(0)
    }
    assert.notOk(0L)
    assertThrows<NodeAssertionError> {
      assert.ok(0L)
    }
    assert.notOk(BigInteger.ZERO)
    assertThrows<NodeAssertionError> {
      assert.ok(BigInteger.ZERO)
    }
    assert.notOk(0.0f)
    assertThrows<NodeAssertionError> {
      assert.ok(0.0f)
    }
    assert.notOk(0.0)
    assertThrows<NodeAssertionError> {
      assert.ok(0.0)
    }
  }

  @Test fun `ok() should work for falsy guest values`() {
    assert.notOk(asValue(false))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(false))
    }
    assert.notOk(asValue(null))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(null))
    }
    assert.notOk(asValue(emptyList<String>()))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(emptyList<String>()))
    }
    assert.notOk(asValue(emptyMap<String, String>()))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(emptyMap<String, String>()))
    }
    assert.notOk(asValue(emptyList<String>().iterator()))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(emptyList<String>().iterator()))
    }
    assert.notOk(asValue(0.toShort()))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(0.toShort()))
    }
    assert.notOk(asValue(0))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(0))
    }
    assert.notOk(asValue(0L))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(0L))
    }
    assert.notOk(asValue(BigInteger.ZERO))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(BigInteger.ZERO))
    }
    assert.notOk(asValue(0.0f))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(0.0f))
    }
    assert.notOk(asValue(0.0))
    assertThrows<NodeAssertionError> {
      assert.ok(asValue(0.0))
    }
  }

  @Test fun `ok() should work for proxy arrays`() {
    val map = object : ProxyArray {
      override fun get(index: Long): Any? = if (index == 0L) "hello" else null
      override fun set(index: Long, value: Value?) {
        TODO("not needed")
      }

      override fun getSize(): Long = 1
    }
    val empty = object : ProxyArray {
      override fun get(index: Long): Any? = if (index == 0L) "hello" else null
      override fun set(index: Long, value: Value?) {
        TODO("not needed")
      }

      override fun getSize(): Long = 0
    }
    assertDoesNotThrow {
      assert.ok(map)
    }
    assertThrows<NodeAssertionError> {
      assert.ok(empty)
    }
  }

  @Test fun `ok() should work for proxy maps`() {
    val map = object : ProxyHashMap {
      override fun getHashSize(): Long = 1
      override fun hasHashEntry(key: Value?): Boolean = key?.asString() == "hello"
      override fun getHashValue(key: Value?): Any? = if (key?.asString() == "hello") "hi" else null
      override fun putHashEntry(key: Value?, value: Value?) {
        TODO("not needed")
      }

      override fun getHashEntriesIterator(): Any {
        TODO("not needed")
      }
    }
    val empty = object : ProxyHashMap {
      override fun getHashSize(): Long = 0
      override fun hasHashEntry(key: Value?): Boolean = false
      override fun getHashValue(key: Value?): Any? = null
      override fun putHashEntry(key: Value?, value: Value?) {
        TODO("not needed")
      }

      override fun getHashEntriesIterator(): Any {
        TODO("not needed")
      }
    }
    assertDoesNotThrow {
      assert.ok(map)
    }
    assertThrows<NodeAssertionError> {
      assert.ok(empty)
    }
  }

  @Test fun `ok() should work for proxy objects`() {
    val obj = object : ProxyObject {
      override fun getMember(key: String?): Any? = if (key == "hello") "hi" else null
      override fun getMemberKeys(): Any = arrayOf("hello")
      override fun hasMember(key: String?): Boolean = key == "hello"

      override fun putMember(key: String?, value: Value?) {
        TODO("not needed")
      }
    }
    val empty = object : ProxyObject {
      override fun getMember(key: String?): Any? = null
      override fun getMemberKeys(): Any = emptyArray<String>()
      override fun hasMember(key: String?): Boolean = false

      override fun putMember(key: String?, value: Value?) {
        TODO("not needed")
      }
    }
    assertDoesNotThrow {
      assert.ok(obj)
    }
    assertThrows<NodeAssertionError> {
      assert.ok(empty)
    }
  }

  @TestFactory fun `equal() guest testing - pass cases`(): Stream<DynamicTest> = sequence {
    dynamicGuestTest("pass: `true == true`") {
      // language=javascript
      """
        const { equal } = require("node:assert");
        equal(true, true);
      """
    }
    dynamicGuestTest("pass: `false == false`") {
      // language=javascript
      """
        const { equal } = require("node:assert");
        equal(false, false);
      """
    }
    dynamicGuestTest("pass: `null == null`") {
      // language=javascript
      """
        const { equal } = require("node:assert");
        equal(null, null);
      """
    }
    dynamicGuestTest("pass: `undefined == undefined`") {
      // language=javascript
      """
        const { equal } = require("node:assert");
        equal(undefined, undefined);
      """
    }
    dynamicGuestTest("pass: `1 == 1`") {
      // language=javascript
      """
        const { equal } = require("node:assert");
        equal(1, 1);
      """
    }
    dynamicGuestTest("pass: `1 == 1.0`") {
      // language=javascript
      """
        const { equal } = require("node:assert");
        equal(1, 1.0);
      """
    }
    dynamicGuestTest("pass: `'' == ''`") {
      // language=javascript
      """
        const { equal } = require("node:assert");
        equal('', '');
      """
    }
    dynamicGuestTest("pass: `'hello' == 'hello'`") {
      // language=javascript
      """
        const { equal } = require("node:assert");
        equal('hello', 'hello');
      """
    }
  }.asStream()

  @TestFactory fun `equal() guest testing - fail cases`(): Stream<DynamicTest> = sequence {
    dynamicGuestTest("fail: `true != false`") {
      // language=javascript
      """
        const { equal, throws } = require("node:assert");
        throws(() => equal(true, false));
      """
    }
    dynamicGuestTest("fail: `false != true`") {
      // language=javascript
      """
        const { equal, throws } = require("node:assert");
        throws(() => equal(false, true));
      """
    }
    dynamicGuestTest("fail: `null != true`") {
      // language=javascript
      """
        const { equal, throws } = require("node:assert");
        throws(() => equal(null, true));
      """
    }
    dynamicGuestTest("fail: `undefined != true`") {
      // language=javascript
      """
        const { equal, throws } = require("node:assert");
        throws(() => equal(undefined, true));
      """
    }
    dynamicGuestTest("fail: `1 != 2`") {
      // language=javascript
      """
        const { equal, throws } = require("node:assert");
        throws(() => equal(1, 2));
      """
    }
    dynamicGuestTest("fail: `1 != 2.0`") {
      // language=javascript
      """
        const { equal, throws } = require("node:assert");
        throws(() => equal(1, 2.0));
      """
    }
    dynamicGuestTest("fail: `'' != 'hello'`") {
      // language=javascript
      """
        const { equal, throws } = require("node:assert");
        throws(() => equal('', 'hello'));
      """
    }
    dynamicGuestTest("fail: `'hello' != ''`") {
      // language=javascript
      """
        const { equal, throws } = require("node:assert");
        throws(() => equal('hello', ''));
      """
    }
  }.asStream()
}
