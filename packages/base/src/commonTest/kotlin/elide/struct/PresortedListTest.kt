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

class PresortedListTest {
  @Test fun testEmpty() {
    val list = PresortedList<Int>()
    assertTrue(list.isEmpty())
    assertFalse(list.isNotEmpty())
    assertEquals(0, list.size)
  }

  @Test fun testSingle() {
    val list = PresortedList(listOf(1))
    assertFalse(list.isEmpty())
    assertTrue(list.isNotEmpty())
    assertEquals(1, list.size)
    assertEquals(1, list[0])
  }

  @Test fun testMultiple() {
    val list = PresortedList(listOf(3, 2, 1))
    assertFalse(list.isEmpty())
    assertTrue(list.isNotEmpty())
    assertEquals(3, list.size)
    assertEquals(1, list[0])
    assertEquals(2, list[1])
    assertEquals(3, list[2])
  }

  @Test fun testMutable() {
    val list = MutablePresortedList<Int>()
    assertTrue(list.isEmpty())
    assertFalse(list.isNotEmpty())
    list.add(3)
    assertFalse(list.isEmpty())
    assertTrue(list.isNotEmpty())
    list.add(2)
    list.add(1)
    assertEquals(3, list.size)
    assertEquals(1, list[0])
    assertEquals(2, list[1])
    assertEquals(3, list[2])
  }

  @Test fun testBasicSortedInts() {
    val list = MutablePresortedList(listOf(3, 2, 1))
    assertFalse(list.isEmpty())
    assertTrue(list.isNotEmpty())
    assertEquals(3, list.size)
    assertEquals(1, list[0])
    assertEquals(2, list[1])
    assertEquals(3, list[2])
  }

  @Test fun testSortedStrings() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    assertFalse(list.isEmpty())
    assertEquals(3, list.size)
    assertEquals("hey", list[0])
    assertEquals("hi", list[1])
    assertEquals("yo", list[2])
  }

  @Test fun testClear() {
    val list = MutablePresortedList(listOf(3, 2, 1))
    assertFalse(list.isEmpty())
    list.clear()
    assertTrue(list.isEmpty())
  }

  @Test fun testRemoveElement() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    assertFalse(list.isEmpty())
    list.remove("hi")
    assertFalse(list.isEmpty())
    assertEquals(2, list.size)
    assertEquals("hey", list[0])
    assertEquals("yo", list[1])
  }

  @Test fun testRemoveAtIndex() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    list.removeAt(1)
    assertEquals(2, list.size)
    assertEquals("hey", list[0])
    assertEquals("yo", list[1])
  }

  @Test fun testRemoveAll() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    list.removeAll(listOf("hi", "yo"))
    assertEquals(1, list.size)
    assertEquals("hey", list[0])
  }

  @Test fun testAddAll() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    list.addAll(listOf("hi", "yo"))
    assertEquals(5, list.size)
    assertEquals("hey", list[0])
    assertEquals("hi", list[1])
    assertEquals("hi", list[2])
    assertEquals("yo", list[3])
    assertEquals("yo", list[4])
  }

  @Test fun testCustomComparator() {
    val list = PresortedList(listOf("hi", "hey", "yo"), compareBy { it.length })
    assertEquals(3, list.size)
    assertEquals("hi", list[0])
    assertEquals("yo", list[1])
    assertEquals("hey", list[2])
  }

  @Test fun testCustomComparatorMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"), compareBy { it.length })
    assertEquals(3, list.size)
    assertEquals("hi", list[0])
    assertEquals("yo", list[1])
    assertEquals("hey", list[2])
  }

  @Test fun testCustomComparatorConstructorAdd() {
    val list = MutablePresortedList<String>(compareBy { it.length })
    list.add("hi")
    list.add("yo")
    list.add("hey")
    assertEquals(3, list.size)
    assertEquals("yo", list[0])
    assertEquals("hi", list[1])
    assertEquals("hey", list[2])
  }

  @Test fun testCustomComparatorConstructorAddAll() {
    val list = MutablePresortedList<String>(compareBy { it.length })
    list.addAll(listOf("hi", "hey", "yo"))
    assertEquals(3, list.size)
    assertEquals("yo", list[0])
    assertEquals("hi", list[1])
    assertEquals("hey", list[2])
  }

  @Test fun testCloneFromList() {
    val list = listOf("hi", "hey", "yo")
    val clone = MutablePresortedList(list)
    assertEquals(3, clone.size)
    assertEquals("hey", clone[0])
    assertEquals("hi", clone[1])
    assertEquals("yo", clone[2])
  }

  @Test fun testCloneFromPresortedList() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val clone = MutablePresortedList(list)
    assertEquals(3, clone.size)
    assertEquals("hey", clone[0])
    assertEquals("hi", clone[1])
    assertEquals("yo", clone[2])
  }

  @Test fun testSize() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    assertEquals(3, list.size)
    list.add("sup")
    assertEquals(4, list.size)
    list.removeAt(1)
    assertEquals(3, list.size)
  }

  @Test fun testToMutable() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    assertEquals(3, list.size)
    assertEquals("hey", list[0])
    val mutable = list.toMutableList()
    assertEquals(3, mutable.size)
    assertEquals("hey", mutable[0])
    assertEquals("hi", mutable[1])
    assertEquals("yo", mutable[2])
    mutable.add("sup")
    assertEquals(4, mutable.size)
    assertEquals("sup", mutable[2])
    assertEquals("yo", mutable[3])
  }

  @Test fun testIterator() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val iterator = list.iterator()
    assertTrue(iterator.hasNext())
    assertEquals("hey", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("hi", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("yo", iterator.next())
    assertFalse(iterator.hasNext())
  }

  @Test fun testIteratorMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    val iterator = list.iterator()
    assertTrue(iterator.hasNext())
    assertEquals("hey", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("hi", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("yo", iterator.next())
    assertFalse(iterator.hasNext())
  }

  @Test fun testListIterator() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val iterator = list.listIterator()
    assertTrue(iterator.hasNext())
    assertEquals("hey", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("hi", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("yo", iterator.next())
    assertFalse(iterator.hasNext())
  }

  @Test fun testListIteratorMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    val iterator = list.listIterator()
    assertTrue(iterator.hasNext())
    assertEquals("hey", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("hi", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("yo", iterator.next())
    assertFalse(iterator.hasNext())
  }

  @Test fun testListIteratorIndex() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val iterator = list.listIterator(1)
    assertTrue(iterator.hasNext())
    assertEquals("hi", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("yo", iterator.next())
    assertFalse(iterator.hasNext())
  }

  @Test fun testListIteratorIndexMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    val iterator = list.listIterator(1)
    assertTrue(iterator.hasNext())
    assertEquals("hi", iterator.next())
    assertTrue(iterator.hasNext())
    assertEquals("yo", iterator.next())
    assertFalse(iterator.hasNext())
  }

  @Test fun testSubList() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val subList = list.subList(1, 3)
    assertEquals(2, subList.size)
    assertEquals("hi", subList[0])
    assertEquals("yo", subList[1])
  }

  @Test fun testSubListMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    val subList = list.subList(1, 3)
    assertEquals(2, subList.size)
    assertEquals("hi", subList[0])
    assertEquals("yo", subList[1])
  }

  @Test fun testIndexOf() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    assertEquals(0, list.indexOf("hey"))
    assertEquals(1, list.indexOf("hi"))
    assertEquals(2, list.indexOf("yo"))
  }

  @Test fun testIndexOfMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    assertEquals(0, list.indexOf("hey"))
    assertEquals(1, list.indexOf("hi"))
    assertEquals(2, list.indexOf("yo"))
  }

  @Test fun testLastIndexOf() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    assertEquals(0, list.lastIndexOf("hey"))
    assertEquals(1, list.lastIndexOf("hi"))
    assertEquals(2, list.lastIndexOf("yo"))
  }

  @Test fun testLastIndexOfMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    assertEquals(0, list.lastIndexOf("hey"))
    assertEquals(1, list.lastIndexOf("hi"))
    assertEquals(2, list.lastIndexOf("yo"))
  }

  @Test fun testContains() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    assertTrue(list.contains("hey"))
    assertTrue(list.contains("hi"))
    assertTrue(list.contains("yo"))
    assertFalse(list.contains("sup"))
  }

  @Test fun testContainsMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    assertTrue(list.contains("hey"))
    assertTrue(list.contains("hi"))
    assertTrue(list.contains("yo"))
    assertFalse(list.contains("sup"))
  }

  @Test fun testContainsAll() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    assertTrue(list.containsAll(listOf("hey", "hi")))
    assertFalse(list.containsAll(listOf("hey", "hi", "sup")))
  }

  @Test fun testContainsAllMutable() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    assertTrue(list.containsAll(listOf("hey", "hi")))
    assertFalse(list.containsAll(listOf("hey", "hi", "sup")))
  }

  @Test fun testEquals() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val other = PresortedList(listOf("hi", "hey", "yo"))
    assertEquals(list, other)
  }

  @Test fun testHashCode() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val other = PresortedList(listOf("hi", "hey", "yo"))
    assertEquals(list.hashCode(), other.hashCode())
  }

  @Test fun testToString() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    assertEquals("[hey, hi, yo]", list.toString())
  }

  @Test fun testCopyPresorted() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val copy = PresortedList(list)
    assertEquals(list, copy)
  }

  @Test fun testRetainAll() {
    val list = MutablePresortedList(listOf("hi", "hey", "yo"))
    list.retainAll(listOf("hi", "yo"))
    assertEquals(2, list.size)
    assertEquals("hi", list[0])
    assertEquals("yo", list[1])
  }

  @Test fun testUnsupportedSetAtIndex() {
    assertFailsWith<UnsupportedOperationException> {
      MutablePresortedList(listOf("hi", "hey", "yo"))[1] = "sup"
    }
  }

  @Test fun testUnsupportedAddAtIndex() {
    assertFailsWith<UnsupportedOperationException> {
      MutablePresortedList(listOf("hi", "hey", "yo")).add(1, "sup")
    }
  }

  @Test fun testUnsupportedAddAllAtIndex() {
    assertFailsWith<UnsupportedOperationException> {
      MutablePresortedList(listOf("hi", "hey", "yo")).addAll(1, listOf("sup", "yo"))
    }
  }

  @Test fun testSerializer() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    assertNotNull(PresortedList.serializer(String.serializer()))
    assertNotNull(Json.encodeToString(list))
  }

  @Test fun testSerializeJson() {
    val list = PresortedList(listOf("hi", "hey", "yo"))
    val json = Json.encodeToString(list)
    assertNotNull(json, "should not get null JSON from `encodeToString`")
    val decoded = Json.decodeFromString(PresortedList.serializer(String.serializer()), json)
    assertEquals(list, decoded)
  }
}
