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

package elide.struct

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class SortedMapTest {
  @Test fun testStubbed() {
    val map = emptySortedMap<String, String>()
    assertTrue(map.isEmpty())
    assertEquals(0, map.size)

    assertTrue(map.keys.isEmpty())
    assertTrue(map.values.isEmpty())
    assertTrue(map.entries.isEmpty())

    assertFalse(map.isNotEmpty())
    assertFalse(map.containsKey("key"))
    assertFalse(map.containsValue("value"))

    assertNull(map["key"])
  }

  @Test fun testEmpty() {
    val map = sortedMapOf<String, String>()
    assertTrue(map.isEmpty())
    assertFalse(map.isNotEmpty())
    assertEquals(0, map.size)
  }

  @Test fun testEmptyMutable() {
    val map = mutableSortedMapOf<String, String>()
    assertTrue(map.isEmpty())
    assertFalse(map.isNotEmpty())
    assertEquals(0, map.size)
  }

  @Test fun testNonEmpty() {
    val map = sortedMapOf("hi" to "hello")
    assertFalse(map.isEmpty())
    assertTrue(map.isNotEmpty())
    assertEquals(1, map.size)
    assertEquals("hello", map["hi"])
  }

  @Test fun testNonEmptyMutable() {
    val map = mutableSortedMapOf("hi" to "hello")
    assertFalse(map.isEmpty())
    assertTrue(map.isNotEmpty())
    assertEquals(1, map.size)
    assertEquals("hello", map["hi"])
  }

  @Test fun testClear() {
    val map = mutableSortedMapOf("hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(1, map.size)
    map.clear()
    assertTrue(map.isEmpty())
    assertEquals(0, map.size)
  }

  @Test fun testClearMutable() {
    val map = mutableSortedMapOf("hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(1, map.size)
    map.clear()
    assertTrue(map.isEmpty())
    assertEquals(0, map.size)
  }

  @Test fun testAdd() {
    val map = mutableSortedMapOf<String, String>()
    assertTrue(map.isEmpty())
    assertEquals(0, map.size)
    map["hi"] = "hello"
    assertFalse(map.isEmpty())
    assertEquals(1, map.size)
    assertEquals("hello", map["hi"])
  }

  @Test fun testAddAll() {
    val map = mutableSortedMapOf<String, String>()
    assertTrue(map.isEmpty())
    assertEquals(0, map.size)
    map.putAll(mapOf("hi" to "hello", "bye" to "goodbye"))
    assertFalse(map.isEmpty())
    assertEquals(2, map.size)
    assertEquals("hello", map["hi"])
    assertEquals("goodbye", map["bye"])
  }

  @Test fun testRemove() {
    val map = mutableSortedMapOf("hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(1, map.size)
    assertEquals("hello", map.remove("hi"))
    assertTrue(map.isEmpty())
    assertEquals(0, map.size)
  }

  @Test fun testContains() {
    val map = sortedMapOf("hi" to "hello")
    assertTrue("hi" in map)
    assertFalse("bye" in map)
  }

  @Test fun testContainsMutable() {
    val map = mutableSortedMapOf("hi" to "hello")
    assertTrue("hi" in map)
    assertFalse("bye" in map)
  }

  @Test fun testContainsValue() {
    val map = sortedMapOf("hi" to "hello")
    assertTrue(map.containsValue("hello"))
    assertFalse(map.containsValue("bye"))
  }

  @Test fun testContainsValueMutable() {
    val map = mutableSortedMapOf("hi" to "hello")
    assertTrue(map.containsValue("hello"))
    assertFalse(map.containsValue("bye"))
  }

  @Test fun testIterator() {
    val map = sortedMapOf("bye" to "goodbye", "hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(2, map.size)

    val iter = map.iterator()
    assertTrue(iter.hasNext())
    assertEquals("bye", iter.next().key)
    assertTrue(iter.hasNext())
    assertEquals("hi", iter.next().key)
    assertFalse(iter.hasNext())
  }

  @Test fun testIteratorMutable() {
    val map = mutableSortedMapOf("bye" to "goodbye", "hi" to "hello")
    val iter = map.iterator()
    assertTrue(iter.hasNext())
    assertEquals("bye", iter.next().key)
    assertTrue(iter.hasNext())
    assertEquals("hi", iter.next().key)
    assertFalse(iter.hasNext())
  }

  @Test fun testEntriesSet() {
    val map = sortedMapOf("bye" to "goodbye", "hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(2, map.size)

    val entries = map.entries
    assertEquals(2, entries.size)
    val firstKey = entries.first().key
    val lastKey = entries.last().key
    assertEquals("bye", firstKey)
    assertEquals("hi", lastKey)
    val firstValue = entries.first().value
    val lastValue = entries.last().value
    assertEquals("goodbye", firstValue)
    assertEquals("hello", lastValue)
  }

  @Test fun testEntriesSetMutable() {
    val map = mutableSortedMapOf("bye" to "goodbye", "hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(2, map.size)

    val entries = map.entries
    assertEquals(2, entries.size)
    val firstKey = entries.first().key
    val lastKey = entries.last().key
    assertEquals("bye", firstKey)
    assertEquals("hi", lastKey)
    val firstValue = entries.first().value
    val lastValue = entries.last().value
    assertEquals("goodbye", firstValue)
    assertEquals("hello", lastValue)
  }

  @Test fun testKeysSet() {
    val map = sortedMapOf("bye" to "goodbye", "hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(2, map.size)

    val keys = map.keys
    assertEquals(2, keys.size)
    val firstKey = keys.first()
    val lastKey = keys.last()
    assertEquals("bye", firstKey)
    assertEquals("hi", lastKey)
  }

  @Test fun testKeysSetMutable() {
    val map = mutableSortedMapOf("bye" to "goodbye", "hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(2, map.size)

    val keys = map.keys
    assertEquals(2, keys.size)
    val firstKey = keys.first()
    val lastKey = keys.last()
    assertEquals("bye", firstKey)
    assertEquals("hi", lastKey)
  }

  @Test fun testValuesSet() {
    val map = sortedMapOf("bye" to "goodbye", "hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(2, map.size)

    val values = map.values
    assertEquals(2, values.size)
    val firstValue = values.first()
    val lastValue = values.last()
    assertEquals("goodbye", firstValue)
    assertEquals("hello", lastValue)
  }

  @Test fun testValuesSetMutable() {
    val map = mutableSortedMapOf("bye" to "goodbye", "hi" to "hello")
    assertFalse(map.isEmpty())
    assertEquals(2, map.size)

    val values = map.values
    assertEquals(2, values.size)
    val firstValue = values.first()
    val lastValue = values.last()
    assertEquals("goodbye", firstValue)
    assertEquals("hello", lastValue)
  }

  @Ignore @Test fun testSerializer() {
    assertNotNull(TreeMap.serializer(String.serializer(), String.serializer()))
    assertNotNull(Json.encodeToString(sortedMapOf("hi" to "hello")))
  }

  @Ignore @Test fun testSerializerMutable() {
    assertNotNull(TreeMap.serializer(String.serializer(), String.serializer()))
    assertNotNull(Json.encodeToString(sortedMapOf("hi" to "hello")))
  }

  @Ignore @Test fun testCodecJson() {
    assertNotNull(TreeMap.serializer(String.serializer(), String.serializer()))
    val serialized = assertNotNull(Json.encodeToString(sortedMapOf("hi" to "hello")))
    val map = Json.decodeFromString(TreeMap.serializer(String.serializer(), String.serializer()), serialized)
    assertFalse(map.isEmpty())
    assertEquals(1, map.size)
    assertEquals("hello", map["hi"])
  }

  @Ignore @Test fun testCodecJsonMutable() {
    assertNotNull(TreeMap.serializer(String.serializer(), String.serializer()))
    val serialized = assertNotNull(Json.encodeToString(sortedMapOf("hi" to "hello")))
    val map = Json.decodeFromString(TreeMap.serializer(String.serializer(), String.serializer()), serialized)
    assertFalse(map.isEmpty())
    assertEquals(1, map.size)
    assertEquals("hello", map["hi"])
  }

  @Ignore @Test fun testCodecJsonInterchangeable() {
    val map = sortedMapOf("hi" to "hello")
    val serialized = Json.encodeToString(map)
    val deserialized =
      Json.decodeFromString(TreeMap.serializer(String.serializer(), String.serializer()), serialized)
    val serialized2 = Json.encodeToString(deserialized)
    assertEquals(serialized, serialized2)
    val deserialized2 = Json.decodeFromString(
      TreeMap.serializer(String.serializer(), String.serializer()),
      serialized,
    )
    assertEquals(map, deserialized2)
  }

  @Test fun testEquals() {
    val one = sortedMapOf("hi" to "hey")
    assertEquals(one, one)  // same object
    assertNotEquals<Map<String, String>?>(one, null)

    val two = sortedMapOf("hi" to "hey")
    assertEquals(two, two)  // same object
    assertEquals(one, two)  // identical maps
    assertNotEquals<Map<String, String>?>(two, null)

    val three = sortedMapOf("hi" to "hello")
    assertEquals(three, three)   // same object
    assertNotEquals(two, three)  // different values

    val four = sortedMapOf("hello" to "hello")
    assertEquals(four, four)      // same object
    assertNotEquals(three, four)  // different keys
    assertNotEquals(one, four)    // different values and keys

    val five = sortedMapOf("hi" to "hello")
    assertEquals(five, five)      // same object
    assertNotEquals(four, five)   // different keys
    assertNotEquals(two, five)    // different values
  }

  @Test fun testEqualsMutable() {
    val one = mutableSortedMapOf("hi" to "hey")
    assertEquals(one, one)  // same object
    assertNotEquals<Map<String, String>?>(one, null)

    val two = sortedMapOf("hi" to "hey")
    assertEquals(two, two)  // same object
    assertEquals(one, two)  // identical maps
    assertNotEquals<Map<String, String>?>(two, null)

    val three = sortedMapOf("hi" to "hello")
    assertEquals(three, three)   // same object
    assertNotEquals(two, three)  // different values

    val four = sortedMapOf("hello" to "hello")
    assertEquals(four, four)      // same object
    assertNotEquals(three, four)  // different keys
    assertNotEquals(one, four)    // different values and keys

    val five = sortedMapOf("hi" to "hello")
    assertEquals(five, five)      // same object
    assertNotEquals(four, five)   // different keys
    assertNotEquals(two, five)    // different values
  }

  @Test fun testEqualsInterchangeable() {
    val one = sortedMapOf("hi" to "hey")
    assertEquals(one, one)  // same object
    assertNotEquals<Map<String, String>?>(one, null)

    val two = sortedMapOf("hi" to "hey")
    assertEquals(two, two)  // same object
    assertEquals<Map<String, String>>(one, two)  // identical maps
    assertNotEquals<Map<String, String>?>(two, null)

    val three = sortedMapOf("hi" to "hello")
    assertEquals(three, three)   // same object
    assertNotEquals<Map<String, String>>(two, three)  // different values

    val four = sortedMapOf("hello" to "hello")
    assertEquals(four, four)      // same object
    assertNotEquals<Map<String, String>>(three, four)  // different keys
    assertNotEquals<Map<String, String>>(one, four)    // different values and keys

    val five = sortedMapOf("hi" to "hello")
    assertEquals(five, five)      // same object
    assertNotEquals<Map<String, String>>(four, five)   // different keys
    assertNotEquals<Map<String, String>>(two, five)    // different values
  }

  @Test fun testHashcode() {
    val one = sortedMapOf("hi" to "hey")
    assertEquals(one.hashCode(), one.hashCode())  // same object
    val two = sortedMapOf("hi" to "hey")
    assertEquals(one.hashCode(), two.hashCode())  // identical maps
    val three = sortedMapOf("hi" to "hello")
    assertNotEquals(one.hashCode(), three.hashCode())  // different values
    val four = sortedMapOf("hello" to "hey")
    assertNotEquals(one.hashCode(), four.hashCode())    // different keys
    assertNotEquals(three.hashCode(), four.hashCode())  // different values and keys
  }

  @Test fun testHashcodeMutable() {
    val one = mutableSortedMapOf("hi" to "hey")
    assertEquals(one.hashCode(), one.hashCode())  // same object
    val two = sortedMapOf("hi" to "hey")
    assertEquals(one.hashCode(), two.hashCode())  // identical maps
    val three = sortedMapOf("hi" to "hello")
    assertNotEquals(one.hashCode(), three.hashCode())  // different values
    val four = sortedMapOf("hello" to "hey")
    assertNotEquals(one.hashCode(), four.hashCode())    // different keys
    assertNotEquals(three.hashCode(), four.hashCode())  // different values and keys
  }

  @Test fun testRebalance() {
    val map = mutableSortedMapOf(
      "hi" to "hello",
      "bye" to "goodbye",
      "hey" to "hello",
      "yo" to "sup",
      "abc" to "xyz",
    )

    assertFalse(map.isEmpty())
    assertTrue(map.isNotEmpty())
    assertEquals(5, map.size)

    map.remove("bye")
    assertEquals(4, map.size)

    map.remove("hey")
    assertEquals(3, map.size)

    map["hey"] = "hello"  // not in the map
    assertEquals(4, map.size)

    map["hey"] = "xyzxyz"  // already in the map
    assertEquals(4, map.size)
  }

  @Test fun testRebalanceRoot() {
    val map = mutableSortedMapOf(
      "hi" to "hello",
      "bye" to "goodbye",
      "hey" to "hello",
      "yo" to "sup",
      "abc" to "xyz",
    )

    assertFalse(map.isEmpty())
    assertTrue(map.isNotEmpty())
    assertEquals(5, map.size)

    map.remove("hi")
    assertEquals(4, map.size)

    map.remove("bye")
    assertEquals(3, map.size)

    map.remove("hey")
    assertEquals(2, map.size)

    map["hey"] = "hello"  // not in the map
    assertEquals(3, map.size)

    map["hey"] = "xyzxyz"  // already in the map
    assertEquals(3, map.size)
  }
}
