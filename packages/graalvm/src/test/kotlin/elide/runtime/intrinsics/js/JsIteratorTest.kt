package elide.runtime.intrinsics.js

import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsMutableMap
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

/** Tests for [JsIterator]. */
@TestCase class JsIteratorTest {
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
    val val2 = testIter.next()
    assertNotNull(val2)
    val val2inner = val2.value
    assertEquals("two", val2inner)
    assertTrue(val2.done)
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
}
