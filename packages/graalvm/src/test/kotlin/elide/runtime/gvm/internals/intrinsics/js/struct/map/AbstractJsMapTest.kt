package elide.runtime.gvm.internals.intrinsics.js.struct.map

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.test.*
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.intrinsics.js.MutableMapLike
import elide.testing.annotations.Test
import org.junit.jupiter.api.DynamicTest.dynamicTest as test
import elide.runtime.intrinsics.js.MapLike as JsMapLike

/** Test for JavaScript `Map` and `MapLike` intrinsic behaviors. */
@Suppress("UNCHECKED_CAST")
internal abstract class AbstractJsMapTest<MapLike> : AbstractJsTest() where MapLike: AbstractJsMap<String, Any?> {
  // Build a generic map for testing, from test data.
  private fun allocateGeneric(): MapLike = spawnGeneric(testDataForGenericMap())

  // Build a mutable map for testing, from test data.
  private fun allocateMutable(): MapLike = spawnGeneric(testDataForMutableMap())

  // Build a sorted map for testing, from test data.
  private fun allocateSorted(): MapLike = spawnGeneric(testDataForSortedMap())

  // Build a multi-map for testing, from test data.
  private fun allocateMulti(): MapLike = spawnGeneric(testDataForMultiMap())

  // Build a concurrent map for testing, from test data.
  private fun allocateConcurrent(): MapLike = spawnGeneric(testDataForConcurrentMap())

  /** @return Empty map of the implementation type under test. */
  protected abstract fun empty(): MapLike

  /** @return Generic-test map of the implementation type under test (collection of pairs). */
  protected abstract fun spawnGeneric(pairs: Collection<Pair<String, Any?>>): MapLike

  /** @return Generic-test map of the implementation type under test (from a map). */
  protected abstract fun spawnFromMap(map: Map<String, Any?>): MapLike

  /** @return Generic-test map of the implementation type under test (from a collection of entries). */
  protected abstract fun spawnFromEntries(entries: Collection<Map.Entry<String, Any?>>): MapLike
  
  /** @return Generic-test map of the implementation type under test (from a collection of JS map-like entries). */
  protected abstract fun spawnFromJsEntries(entries: Collection<JsMapLike.Entry<String, Any?>>): MapLike

  /** @return Generic-test map of the implementation type under test (iterable of pairs). */
  protected abstract fun spawnUnbounded(pairs: Iterable<Pair<String, Any?>>): MapLike

  /** @return Generic-test map of the implementation type under test (from an iterable of entries). */
  protected abstract fun spawnUnboundedEntries(entries: Iterable<Map.Entry<String, Any?>>): MapLike

  /** @return Generic-test map of the implementation type under test (from an iterable of JS map-like entries). */
  protected abstract fun spawnUnboundedJsEntries(entries: Iterable<JsMapLike.Entry<String, Any?>>): MapLike

  /** @return Name of the implementation under test. */
  abstract fun implName(): String

  /** Plan a set of tests for a given JS map implementation. */
  @TestFactory protected fun mapTests(): Collection<DynamicTest> {
    val map = empty()
    val collection = ArrayList<DynamicTest>()
    val implName = implName()

    collection.addAll(testMapGeneric("$implName: generic map", ::allocateGeneric))
    if (map.mutable) collection.addAll(testMapMutable("$implName: mutable map", ::allocateMutable))
    if (map.multi) collection.addAll(testMapMulti("$implName: multi map", ::allocateMulti))
    if (map.sorted) collection.addAll(testMapSorted("$implName: sorted map", ::allocateSorted))
    if (map.threadsafe) collection.addAll(testMapConcurrent("$implName: concurrent map", ::allocateConcurrent))
    return collection
  }

  // Build test data for generic map testing.
  private fun testDataForGenericMap(): Collection<Pair<String, Any?>> {
    return listOf(
      "hello" to "hi",
      "test" to 5,
      "foo" to 3.14,
      "longvalue" to 10L,
      "somevalue" to false,
    )
  }

  /** Test: An empty map should behave like an empty map. */
  @Test fun testCreateEmpty() {
    assertNotNull(empty(), "should be able to spawn an empty map implementation")
    val empty = empty()
    assertTrue(empty.isEmpty(), "empty map should report as empty")
    assertEquals(0, empty.size, "empty map should report `0` size")
  }

  /** Test: Create from pairs. */
  @Test fun testCreateFromPairs() {
    val testData = testDataForGenericMap()
    val subject = spawnGeneric(testData)
    assertNotNull(subject, "should be able to spawn a map from a collection of pairs")
    assertFalse(subject.isEmpty(), "map should accurately report non-empty status")
    assertTrue(subject.isNotEmpty(), "map should accurately report non-empty status")
    assertEquals(testData.size, subject.size, "size of map should match input data")
  }

  /** Test: Create from Java map. */
  @Test fun testCreateFromMap() {
    val testData = testDataForGenericMap()
    val subject = spawnFromMap(testData.toMap())
    assertNotNull(subject, "should be able to spawn a map from a backing map")
    assertFalse(subject.isEmpty(), "map should accurately report non-empty status")
    assertTrue(subject.isNotEmpty(), "map should accurately report non-empty status")
    assertEquals(testData.size, subject.size, "size of map should match input data")
  }

  /** Test: Create from map entries. */
  @Test fun testCreateFromMapEntries() {
    val testData = testDataForGenericMap()
    val subject = spawnFromEntries(testData.map {
      object: Map.Entry<String, Any?> {
        override val key: String get() = it.first
        override val value: Any? get() = it.second
      }
    })
    assertNotNull(subject, "should be able to spawn a map from a collection of entries")
    assertFalse(subject.isEmpty(), "map should accurately report non-empty status")
    assertTrue(subject.isNotEmpty(), "map should accurately report non-empty status")
    assertEquals(testData.size, subject.size, "size of map should match input data")
  }

  /** Test: Create from JS map entries. */
  @Test fun testCreateFromJSMapEntries() {
    val testData = testDataForGenericMap()
    val subject = spawnFromJsEntries(testData.map {
      BaseJsMap.entry(it.first, it.second)
    })
    assertNotNull(subject, "should be able to spawn a map from a collection of JS entries")
    assertFalse(subject.isEmpty(), "map should accurately report non-empty status")
    assertTrue(subject.isNotEmpty(), "map should accurately report non-empty status")
    assertEquals(testData.size, subject.size, "size of map should match input data")
  }

  /** Test: Create from iterable of pairs. */
  @Test fun testCreateFromIterablePairs() {
    val testData = testDataForGenericMap()
    val subject = spawnUnbounded(testData.asIterable())
    assertNotNull(subject, "should be able to spawn a map from iterable of pairs")
    assertFalse(subject.isEmpty(), "map should accurately report non-empty status")
    assertTrue(subject.isNotEmpty(), "map should accurately report non-empty status")
    assertEquals(testData.size, subject.size, "size of map should match input data")
  }

  /** Test: Create from iterable of entries. */
  @Test fun testCreateFromIterableEntries() {
    val testData = testDataForGenericMap()
    val subject = spawnUnboundedEntries(testData.map {
      object: Map.Entry<String, Any?> {
        override val key: String get() = it.first
        override val value: Any? get() = it.second
      }
    }.asIterable())
    assertNotNull(subject, "should be able to spawn a map from iterable of entries")
    assertFalse(subject.isEmpty(), "map should accurately report non-empty status")
    assertTrue(subject.isNotEmpty(), "map should accurately report non-empty status")
    assertEquals(testData.size, subject.size, "size of map should match input data")
  }

  /** Test: Create from iterable of JS entries. */
  @Test fun testCreateFromIterableJsEntries() {
    val testData = testDataForGenericMap()
    val subject = spawnUnboundedJsEntries(testData.map {
      BaseJsMap.entry(it.first, it.second)
    }.asIterable())
    assertNotNull(subject, "should be able to spawn a map from iterable of JS entries")
    assertFalse(subject.isEmpty(), "map should accurately report non-empty status")
    assertTrue(subject.isNotEmpty(), "map should accurately report non-empty status")
    assertEquals(testData.size, subject.size, "size of map should match input data")
  }

  /** Test: Generic immutable (`get`, etc). */
  @Suppress("ReplaceGetOrSet")
  protected open fun testMapGeneric(prefix: String, factory: () -> MapLike): List<DynamicTest> = listOf(
    // --- Maps: Retrieval --- //

    test("$prefix should be able to get a value via subscript") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      assertNotNull(subject["hello"], "should not get `null` for known-good value from generic map")
      assertEquals("hi", subject["hello"])
    },
    test("$prefix should be able to get a value via method") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      assertNotNull(subject.get("hello"))
      assertEquals("hi", subject.get("hello"))
    },
    test("$prefix missing values should yield `null`") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      assertNull(subject["doesnotexist"], "should get `null` for non-existent map key")
    },
    test("$prefix should support `getOrDefault`") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      assertEquals(5, subject.getOrDefault("test", 10))
      assertEquals(10, subject.getOrDefault("doesnotexist", 10))
      assertNull(subject.getOrDefault("doesnotexist", null))
    },
    test("$prefix should preserve types") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      assertEquals(5, subject["test"])
      assertEquals(3.14, subject["foo"])
      assertEquals(10L, subject["longvalue"])
      assertEquals(false, subject["somevalue"])
    },

    // --- Maps: Presence --- //

    test("$prefix should accurately report key presence") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      assertTrue(subject.has("hello"), "should report key presence")
      assertTrue(subject.containsKey("hello"), "should report key presence")
      assertFalse(subject.has("doesnotexist"), "should report key absence")
      assertFalse(subject.containsKey("doesnotexist"), "should report key absence")
    },
    test("$prefix should accurately report value presence") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      assertTrue(subject.containsValue("hi"), "should report value presence")
      assertFalse(subject.containsKey("doesnotexist"), "should report value absence")
    },

    // --- Maps: Key/Value Sets --- //

    test("$prefix should provide accurate key set") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val keys = subject.keys
      assertEquals(5, keys.size, "should have 5 keys")
    },
    test("$prefix should provide accurate value set") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val values = subject.values
      assertEquals(5, values.size, "should have 5 values")
    },

    // --- Maps: Iteration --- //

    test("$prefix should support iterating over keys") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val iterator = subject.keys()
      val keys = ArrayList<String>()
      while (iterator.hasNext()) {
        keys.add(iterator.next().value!!)
      }
      assertEquals(5, keys.size, "should have 5 keys from key iterator")
    },
    test("$prefix should support iterating over values") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val iterator = subject.values()
      val values = ArrayList<Any?>()
      while (iterator.hasNext()) {
        values.add(iterator.next().value)
      }
      assertEquals(5, values.size, "should have 5 values from value iterator")
    },
    test("$prefix should support iterating over entries") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val iterator = subject.entries()
      val values = ArrayList<elide.runtime.intrinsics.js.MapLike.Entry<String, Any?>>()
      while (iterator.hasNext()) {
        values.add(iterator.next().value!!)
      }
      assertEquals(5, values.size, "should have 5 values from entry iterator")
      values.forEach {
        assertNotNull(it.key)
        assertDoesNotThrow {
          it.value
        }
      }
    },
    test("$prefix should support iterating via `forEach`") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val cycles = AtomicInteger(0)
      subject.forEach { _, _ -> cycles.incrementAndGet() }
      assertEquals(5, cycles.get(), "should have 5 calls to `forEach`")
    },
    test("$prefix should support iterating via `forEach` with JS entries") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val cycles = AtomicInteger(0)
      val doer: (elide.runtime.intrinsics.js.MapLike.Entry<String, Any?>) -> Unit = { cycles.incrementAndGet() }
      subject.forEach(doer)
      assertEquals(5, cycles.get(), "should have 5 calls to `forEach`")
    },

    // --- Maps: Sequences --- //

    test("$prefix should support providing a sequence of keys") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val seq = subject.keysSequence()
      assertNotNull(seq, "should not get `null` for key-sequence from map")
      val keys = seq.toList()
      assertEquals(5, keys.size, "should have 5 keys from sequence")
    },
    test("$prefix should support providing a sequence of values") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val seq = subject.valuesSequence()
      assertNotNull(seq, "should not get `null` for value-sequence from map")
      val vals = seq.toList()
      assertEquals(5, vals.size, "should have 5 values from sequence")
    },

    // --- Maps: Streams --- //

    test("$prefix should support providing a stream of keys") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val seq = subject.keysStream()
      assertNotNull(seq, "should not get `null` for key-stream from map")
      val keys = seq.collect(Collectors.toList())
      assertEquals(5, keys.size, "should have 5 keys from collected stream")
    },
    test("$prefix should support providing a stream of values") {
      val subject = factory()
      assertNotNull(subject, "should not get `null` for test map")
      val seq = subject.valuesStream()
      assertNotNull(seq, "should not get `null` for value-stream from map")
      val vals = seq.collect(Collectors.toList())
      assertEquals(5, vals.size, "should have 5 values from collected stream")
    },
    test("$prefix should support parallel streaming if the map is safe for concurrent access") {
      val subject = factory()
      if (subject.threadsafe) {
        // if the map is threadsafe, it should allow parallel streaming
        val keys = subject.keysStream(true).collect(Collectors.toList())
        assertNotNull(keys, "should not get `null` from collected parallel stream")
        assertEquals(5, keys.size, "should have 5 keys from collected parallel stream")
        val values = subject.valuesStream(true).collect(Collectors.toList())
        assertNotNull(values, "should not get `null` from collected parallel stream")
        assertEquals(5, values.size, "should have 5 keys from collected parallel stream")
      } else {
        // if the map is not threadsafe, it should throw an error to ask for a parallel stream
        assertFailsWith<IllegalStateException> {
          subject.keysStream(true)
        }
        assertFailsWith<IllegalStateException> {
          subject.valuesStream(true)
        }
      }
    },
  )

  // Build test data for mutable map testing.
  private fun testDataForMutableMap(): Collection<Pair<String, Any?>> {
    return testDataForGenericMap().plus(listOf(
      "newkey" to "newvalue",
      "another" to 1,
    ))
  }

  /** Test: Generic mutable (`put`, etc). */
  protected open fun testMapMutable(prefix: String, factory: () -> MapLike): List<DynamicTest> = listOf(
    // --- Mutable Maps: Storage --- //

    test("$prefix should be able to put a new value via subscript") {
      val subject = factory() as MutableMap<String, Any?>
      subject["freshKey"] = 10L

      assertNotNull(subject, "should not get `null` for test map")
      assertNotNull(subject["hello"], "should not get `null` for known-good value from generic map")
      assertEquals("hi", subject["hello"])
      assertNotNull(subject["newkey"])
      assertNotNull(subject["another"])
      assertNotNull(subject["freshKey"])
      assertEquals(1, subject["another"])
      assertEquals(10L, subject["freshKey"])
    },
    test("$prefix should be able to put a new value via `put()` method") {
      val subject = factory() as MutableMap<String, Any?>
      @Suppress("ReplacePutWithAssignment")
      subject.put("freshKey", 10L)

      assertNotNull(subject, "should not get `null` for test map")
      assertNotNull(subject["hello"], "should not get `null` for known-good value from generic map")
      assertEquals("hi", subject["hello"])
      assertNotNull(subject["newkey"])
      assertNotNull(subject["another"])
      assertNotNull(subject["freshKey"])
      assertEquals(1, subject["another"])
      assertEquals(10L, subject["freshKey"])
    },
    test("$prefix should be able to put a new value via `set()` method") {
      val subject = factory() as MutableMap<String, Any?>
      @Suppress("ReplaceGetOrSet")
      subject.set("freshKey", 10L)
      assertDoesNotThrow {
        when (subject) {
          is BaseMutableJsMap<String, Any?> -> subject.set("freshkey", 10L)
          is BaseMutableJsMultiMap<String, Any?> -> subject.set("freshkey", 10L)
          else -> {}
        }
      }

      assertNotNull(subject, "should not get `null` for test map")
      assertNotNull(subject["hello"], "should not get `null` for known-good value from generic map")
      assertEquals("hi", subject["hello"])
      assertNotNull(subject["newkey"])
      assertNotNull(subject["another"])
      assertNotNull(subject["freshKey"])
      assertEquals(1, subject["another"])
      assertEquals(10L, subject["freshKey"])
    },
    test("$prefix should be able to overwrite a value via subscript") {
      val subject = factory() as MutableMap<String, Any?>

      assertNotNull(subject, "should not get `null` for test map")
      assertNotNull(subject["hello"], "should not get `null` for known-good value from generic map")
      assertEquals("hi", subject["hello"])
      assertNotNull(subject["newkey"])
      assertNotNull(subject["another"])
      assertEquals(1, subject["another"])
      subject["another"] = subject["another"] as Int + 1
      assertEquals(2, subject["another"])
    },
    test("$prefix should be able to overwrite value via `put()` method") {
      val subject = factory() as MutableMap<String, Any?>

      assertNotNull(subject, "should not get `null` for test map")
      assertNotNull(subject["hello"], "should not get `null` for known-good value from generic map")
      assertEquals("hi", subject["hello"])
      assertNotNull(subject["newkey"])
      assertNotNull(subject["another"])
      assertEquals(1, subject["another"])
      @Suppress("ReplacePutWithAssignment")
      subject.put("another", subject["another"] as Int + 1)
      assertEquals(2, subject["another"])
    },
    test("$prefix should be able to overwrite value via `set()` method") {
      val subject = factory() as MutableMap<String, Any?>

      assertNotNull(subject, "should not get `null` for test map")
      assertNotNull(subject["hello"], "should not get `null` for known-good value from generic map")
      assertEquals("hi", subject["hello"])
      assertNotNull(subject["newkey"])
      assertNotNull(subject["another"])
      assertEquals(1, subject["another"])
      @Suppress("ReplaceGetOrSet")
      subject.set("another", subject["another"] as Int + 1)
      assertEquals(2, subject["another"])
    },

    // --- Mutable Maps: Storage (Multi) --- //

    test("$prefix should be able to add new values via `putAll()` method") {
      val otherMap = mapOf("freshKey" to 10L, "keyThree" to false)
      val subject = factory() as MutableMap<String, Any?>

      assertNull(subject["freshKey"])
      subject.putAll(otherMap)
      assertNotNull(subject["freshKey"])
      assertEquals(10L, subject["freshKey"])
      assertNull(subject["keyTwo"])
      assertEquals(false, subject["keyThree"])
    },

    // --- Mutable Maps: Put if Absent --- //

    test("$prefix should be able to add new values if missing via `putIfAbsent()`") {
      val subject = factory() as MutableMap<String, Any?>

      assertEquals("hi", subject["hello"])
      subject.putIfAbsent("hello", "bye")
      assertEquals("hi", subject["hello"])
      subject.putIfAbsent("freshkey", "bye")
      assertEquals("bye", subject["freshkey"])
    },

    // --- Mutable Maps: Remove --- //

    test("$prefix should be able to delete existing values via `remove()`") {
      val subject = factory() as MutableMap<String, Any?>
      val originalSize = subject.size
      assertEquals("hi", subject["hello"])
      assertNotNull(subject.remove("hello"))
      assertNull(subject["hello"])
      assertEquals(5, subject["test"])
      assertEquals(originalSize - 1, subject.size)
      assertTrue(subject.isNotEmpty())
      assertFalse(subject.isEmpty())
    },
    test("$prefix should not fail when deleting a missing value via `remove()`") {
      val subject = factory() as MutableMap<String, Any?>
      val originalSize = subject.size
      assertNull(subject["idonotexist"])
      assertNull(subject.remove("idonotexist"))
      assertEquals(5, subject["test"])
      assertEquals(originalSize, subject.size)
      assertTrue(subject.isNotEmpty())
      assertFalse(subject.isEmpty())
    },
    test("$prefix should not fail when deleting a value twice via `remove()`") {
      val subject = factory() as MutableMap<String, Any?>
      val originalSize = subject.size
      assertNull(subject["idonotexist"])
      assertNull(subject.remove("idonotexist"))
      assertNull(subject.remove("idonotexist"))
      assertEquals("hi", subject["hello"])
      assertNotNull(subject.remove("hello"))
      assertNull(subject.remove("hello"))
      assertNull(subject["hello"])
      assertEquals(5, subject["test"])
      assertEquals(originalSize - 1, subject.size)
      assertTrue(subject.isNotEmpty())
      assertFalse(subject.isEmpty())
    },

    // --- Mutable Maps: Delete --- //

    test("$prefix should be able to delete existing values via `delete()`") {
      val subject = factory() as MutableMapLike<String, Any?>
      val originalSize = subject.size
      assertEquals("hi", subject["hello"])
      subject.delete("hello")
      assertNull(subject["hello"])
      assertEquals(5, subject["test"])
      assertEquals(originalSize - 1, subject.size)
      assertTrue(subject.isNotEmpty())
      assertFalse(subject.isEmpty())
    },
    test("$prefix should not fail when deleting a missing value via `delete()`") {
      val subject = factory() as MutableMapLike<String, Any?>
      val originalSize = subject.size
      assertNull(subject["idonotexist"])
      subject.delete("idonotexist")
      assertEquals(5, subject["test"])
      assertEquals(originalSize, subject.size)
      assertTrue(subject.isNotEmpty())
      assertFalse(subject.isEmpty())
    },
    test("$prefix should not fail when deleting a value twice via `delete()`") {
      val subject = factory() as MutableMapLike<String, Any?>
      val originalSize = subject.size
      assertNull(subject["idonotexist"])
      subject.delete("idonotexist")
      subject.delete("idonotexist")
      assertEquals("hi", subject["hello"])
      subject.delete("hello")
      subject.delete("hello")
      assertNull(subject["hello"])
      assertEquals(5, subject["test"])
      assertEquals(originalSize - 1, subject.size)
      assertTrue(subject.isNotEmpty())
      assertFalse(subject.isEmpty())
    },

    // --- Mutable Maps: Clear --- //

    test("$prefix should be able to drop all values via `clear()`") {
      val subject = factory() as MutableMap<String, Any?>
      assertEquals("hi", subject["hello"])
      assertEquals(5, subject["test"])
      subject.clear()
      assertNull(subject["hello"])
      assertNull(subject["test"])
      assertEquals(0, subject.size)
      assertTrue(subject.isEmpty())
      assertFalse(subject.isNotEmpty())
    },
    test("$prefix should not fail when `clear()`-ing an empty map") {
      val subject = empty() as MutableMap<String, Any?>
      subject.clear()
      subject.clear()
    },

    // --- Mutable Maps: Sort-in-Place --- //

    test("$prefix should be able to sort-in-place") {
      val subject = factory() as AbstractJsMap<String, Any?>
      if (subject.sorted) return@test  // no-op
      subject as MutableMapLike<String, Any?>

      // make sure we have un-sorted keys
      val keys = subject.keys
      val sortedKeys = keys.sorted()
      assertNotEquals(
        sortedKeys.joinToString(", "),
        keys.joinToString(", "),
        "sample keys should not be sorted",
      )

      // sort the map in-place
      assertDoesNotThrow {
        subject.sort()
      }

      // grab the keys again
      val keys2 = subject.keys
      assertEquals(
        sortedKeys.joinToString(", "),
        keys2.joinToString(", "),
        "map keys should be sorted after sort-in-place operation",
      )
    },
    test("$prefix should not fail for sort-in-place on empty map") {
      val subject = empty() as MutableMapLike<String, Any?>
      subject.sort()
      subject.sort()
    },
    test("$prefix should sort-in-place in stable manner") {
      val subject = factory() as AbstractJsMap<String, Any?>
      if (subject.sorted) return@test  // no-op
      subject as MutableMapLike<String, Any?>

      // make sure we have un-sorted keys
      val keys = subject.keys
      val sortedKeys = keys.sorted()
      assertNotEquals(
        sortedKeys.joinToString(", "),
        keys.joinToString(", "),
        "sample keys should not be sorted",
      )

      // sort the map in-place several times
      assertDoesNotThrow {
        subject.sort()
        subject.sort()
        subject.sort()
      }

      // grab the keys again
      val keys2 = subject.keys
      assertEquals(
        sortedKeys.joinToString(", "),
        keys2.joinToString(", "),
        "map keys should be sorted after sort-in-place operation",
      )
    },

    // --- Mutable Maps: Entries --- //

    test("$prefix should, unfortunately, support mutable entries") {
      val subject = factory() as MutableMap<String, Any?>
      if ((subject as AbstractJsMap<*, *>).threadsafe) return@test

      val mutable = subject.entries.first()
      assertNotNull(mutable)
      assertNotNull(mutable.key)
      assertNotNull(mutable.value)
      assertDoesNotThrow {
        mutable.setValue("another value")
      }
    },
  )

  // Build test data for thread-safe map testing.
  private fun testDataForConcurrentMap(): Collection<Pair<String, Any?>> {
    return testDataForMutableMap()
  }

  /** Test: Concurrent JS map behaviors. */
  protected open fun testMapConcurrent(prefix: String, factory: () -> MapLike): List<DynamicTest> = listOf(
    // --- Concurrent Maps: Remove by Key & Value --- //

    test("$prefix should support removing by key/value pair") {
      val subject = factory() as ConcurrentMap<String, Any?>

      assertNull(subject["hellotest"])
      subject["hellotest"] = "check"
      assertEquals("check", subject["hellotest"])
      assertFalse(subject.remove("hellotest", "something-else"))
      assertNotNull(subject["hellotest"])
      assertTrue(subject.remove("hellotest", "check"))
      assertNull(subject["hellotest"])
    },

    // --- Concurrent Maps: Replace --- //

    test("$prefix should support replacing by key") {
      val subject = factory() as ConcurrentMap<String, Any?>

      assertNull(subject["hellotest"])
      subject["hellotest"] = "check"
      assertEquals("check", subject.replace("hellotest", "check2"))
      assertEquals("check2", subject["hellotest"])
    },
    test("$prefix should support replacing by key/value pair") {
      val subject = factory() as ConcurrentMap<String, Any?>

      assertNull(subject["hellotest"])
      subject["hellotest"] = "check"
      assertFalse(subject.replace("hellotest", "something-else", "check2"))
      assertEquals("check", subject["hellotest"])
      assertTrue(subject.replace("hellotest", "check", "check2"))
      assertEquals("check2", subject["hellotest"])
    },
  )

  // Build test data for sorted map testing.
  private fun testDataForSortedMap(): Collection<Pair<String, Any?>> {
    return testDataForMutableMap()
  }

  /** Test: Sorted JS map behaviors. */
  protected open fun testMapSorted(prefix: String, factory: () -> MapLike): List<DynamicTest> = listOf(
    // --- Sorted Maps: Sort State --- //

    test("$prefix should always have sorted keys") {
      val subject = factory()

      val keys = subject.keys
      val sortedKeys = keys.sorted()
      assertEquals(
        sortedKeys.joinToString(", "),
        keys.joinToString(", "),
        "sorted map keys should always be sorted",
      )
    },
    test("$prefix should be able to cast as `SortedMap`") {
      assertNotNull(
        factory() as? SortedMap<String, Any?>
      )
    },

    // --- Sorted Maps: Comparator --- //

    test("$prefix should provide `null` as default comparator") {
      val subject = factory() as SortedMap<String, Any?>
      assertNull(subject.comparator())
    },
  ).plus(
    // if this sorted map type is mutable, we have additional tests to run
    if ((factory() as AbstractJsMap<*, *>).mutable) {
      listOf(
        test("$prefix should support sub-map slicing") {
          val subject = factory() as SortedMap<String, Any?>

          subject.clear()
          assertEquals(0, subject.size)
          assertTrue(subject.isEmpty())

          subject["a"] = 1L
          subject["b"] = 2L
          subject["c"] = 3L
          subject["d"] = 4L

          // build a sub-map from `b`-`c`
          val subMap = subject.subMap("b", "d")
          assertEquals(2, subMap.size)
          assertEquals(2L, subMap["b"])
          assertEquals(3L, subMap["c"])
          assertNull(subMap["a"])
          assertNull(subMap["d"])
        },
        test("$prefix should support head-map slicing") {
          val subject = factory() as SortedMap<String, Any?>

          subject.clear()
          assertEquals(0, subject.size)
          assertTrue(subject.isEmpty())

          subject["a"] = 1L
          subject["b"] = 2L
          subject["c"] = 3L
          subject["d"] = 4L

          // build a sub-map from `b`-`c`
          val subMap = subject.headMap("c")
          assertEquals(2, subMap.size)
          assertEquals(1L, subMap["a"])
          assertEquals(2L, subMap["b"])
          assertNull(subMap["c"])
          assertNull(subMap["d"])
        },
        test("$prefix should support tail-map slicing") {
          val subject = factory() as SortedMap<String, Any?>

          subject.clear()
          assertEquals(0, subject.size)
          assertTrue(subject.isEmpty())

          subject["a"] = 1L
          subject["b"] = 2L
          subject["c"] = 3L
          subject["d"] = 4L

          // build a sub-map from `b`-`c`
          val subMap = subject.tailMap("c")
          assertEquals(2, subMap.size)
          assertEquals(3L, subMap["c"])
          assertEquals(4L, subMap["d"])
          assertNull(subMap["a"])
          assertNull(subMap["b"])
        },
        test("$prefix should be able to retrieve the first key") {
          val subject = factory() as SortedMap<String, Any?>

          subject.clear()
          assertEquals(0, subject.size)
          assertTrue(subject.isEmpty())

          subject["a"] = 1L
          subject["z"] = 2L
          val first = subject.firstKey()
          assertNotNull(first)
          assertEquals("a", first)
        },
        test("$prefix should be able to retrieve the last key") {
          val subject = factory() as SortedMap<String, Any?>

          subject.clear()
          assertEquals(0, subject.size)
          assertTrue(subject.isEmpty())

          subject["a"] = 1L
          subject["z"] = 2L
          val last = subject.lastKey()
          assertNotNull(last)
          assertEquals("z", last)
        },
        test("$prefix should throw `NoSuchElement` for `firstKey()` when empty") {
          val subject = factory() as SortedMap<String, Any?>
          subject.clear()
          assertFailsWith<NoSuchElementException> {
            subject.firstKey()
          }
        },
        test("$prefix should throw `NoSuchElement` for `lastKey()` when empty") {
          val subject = factory() as SortedMap<String, Any?>
          subject.clear()
          assertFailsWith<NoSuchElementException> {
            subject.lastKey()
          }
        },
      )
    } else {
      emptyList()
    }
  )

  // Build test data for multi-map testing.
  private fun testDataForMultiMap(): Collection<Pair<String, Any?>> {
    return testDataForMutableMap().plus(listOf(
      "hello" to "again",
      "nullvalue" to "another",
    ))
  }

  /** Test: Multi-map behaviors. */
  protected open fun testMapMulti(prefix: String, factory: () -> MapLike): List<DynamicTest> = listOf(
    // -- Multi-Maps: Multiple Values -- //

    test("$prefix should support multiple values per key") {
      val subject = factory() as BaseJsMultiMap<String, Any>
      assertNotNull(subject["hello"])
      assertEquals("hi", subject["hello"])
      val all = subject.getAll("hello")
      assertEquals("hi", all[0])
      assertEquals("again", all[1])
    },
    test("$prefix should return empty list for missing key with `getAll()`") {
      val subject = factory() as BaseJsMultiMap<String, Any>
      val all = subject.getAll("missing")
      assertNotNull(all)
      assertTrue(all.isEmpty())
    },
  )
}
