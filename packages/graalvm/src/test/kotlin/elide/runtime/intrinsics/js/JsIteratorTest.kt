/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress("JSUnresolvedVariable", "JSUnresolvedFunction")

package elide.runtime.intrinsics.js

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsMutableMap
import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for [JsIterator]. */
@TestCase internal class JsIteratorTest : AbstractJsTest() {
  @Test fun testJsIterator() {
    val map = JsMutableMap.fromPairs(listOf("hello" to 1, "hey" to 2))
    val iterator = JsIterator.JsIteratorFactory.forIterator(map.iterator())

    val entry = iterator.next()
    assertNotNull(entry)
    assertTrue(iterator.hasNext())
    val entry2 = iterator.next()
    assertNotNull(entry2)
    assertFalse(iterator.hasNext())
  }

  @Test fun testManualIterator() {
    val remaining = ArrayList<String>()
    remaining.add("one")
    remaining.add("two")

    val testIter = object : JsIterator<String> {
      override fun hasNext(): Boolean = remaining.isNotEmpty()

      override fun next(): JsIterator.JsIteratorResult<String> {
        val item = remaining.first()
        remaining.removeAt(0)
        return JsIterator.JsIteratorResult.of(
          item,
          remaining.isEmpty(),
        )
      }
    }

    assertTrue(testIter.hasNext())
    val val1 = testIter.next()
    assertNotNull(val1)
    val val1inner = val1.value
    assertEquals("one", val1inner)
    assertFalse(val1.done)
    val val2 = testIter.next()
    assertNotNull(val2)
    val val2inner = val2.value
    assertEquals("two", val2inner)
    assertTrue(val2.done)
  }

  @Test fun testIteratorReturn() {
    val remaining = ArrayList<String>()
    remaining.add("one")
    remaining.add("two")

    val testIter = object : JsIterator<String> {
      override fun hasNext(): Boolean = remaining.isNotEmpty()

      override fun next(): JsIterator.JsIteratorResult<String> {
        val item = remaining.first()
        remaining.removeAt(0)
        return if (remaining.isEmpty()) {
          `return`(item)
        } else {
          JsIterator.JsIteratorResult.of(
            item,
            false,
          )
        }
      }
    }

    assertTrue(testIter.hasNext())
    val val1 = testIter.next()
    assertNotNull(val1)
    val val1inner = val1.value
    assertEquals("one", val1inner)
    assertFalse(val1.done)
    val val2 = testIter.next
    assertNotNull(val2)
    assertEquals("two", val2)
    assertFalse(testIter.hasNext())
  }

  @Test fun testIteratorThrow() {
    val remaining = ArrayList<String>()
    remaining.add("one")
    remaining.add("two")

    val testIter = object : JsIterator<String> {
      override fun hasNext(): Boolean = remaining.isNotEmpty()

      override fun next(): JsIterator.JsIteratorResult<String> {
        return `throw`(JsError.typeError("testing"))
      }
    }

    assertTrue(testIter.hasNext())
    val failure = assertDoesNotThrow {
      testIter.next()
    }
    assertFails {
      failure.value
    }
  }

  @Test @Disabled fun testGuestIteratorManual() {
    val remaining = ArrayList<String>()
    remaining.add("one")
    remaining.add("two")

    val testIter = JsIterator.JsIteratorFactory.forIterator(
      remaining.iterator()
    )

    executeGuest {
      // inject `testIter`
      getBindings("js").putMember("testIter", testIter)

      // language=javascript
      """
        test(testIter).isNotNull();

        const values = [];
        let result = testIter.next();
        test(result).isNotNull();
        while (!result.done) {
         values.push(result.value);
         result = testIter.next();
        }
        test(values.length).isEqualTo(2);
        test(values[0]).isEqualTo("one");
        test(values[1]).isEqualTo("two");
      """
    }.doesNotFail()
  }

  @Test fun testGuestIterator() {
    val remaining = ArrayList<String>()
    remaining.add("one")
    remaining.add("two")

    val testIter = JsIterator.JsIteratorFactory.forIterator(
      remaining.iterator()
    )

    executeGuest {
      // inject `testIter`
      getBindings("js").putMember("testIter", testIter)

      // language=javascript
      """
        test(testIter).isNotNull();

        const values = [];
        for (const value of testIter) {
          values.push(value);
        }
        test(values.length).isEqualTo(2);
        test(values[0]).isEqualTo("one");
        test(values[1]).isEqualTo("two");
      """
    }.doesNotFail()
  }
}
