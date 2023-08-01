package elide.runtime.gvm.internals.intrinsics.js.struct.map

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.runtime.intrinsics.js.MapLike
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the baseline (non-mutable) [JsMap] implementation. */
@TestCase internal class JsMapTest : AbstractJsMapTest<JsMap<String, Any?>>() {
  /** @inheritDoc */
  override fun empty(): JsMap<String, Any?> = JsMap.empty()

  /** @inheritDoc */
  override fun spawnGeneric(pairs: Collection<Pair<String, Any?>>): JsMap<String, Any?> =
    JsMap.fromPairs(pairs)

  /** @inheritDoc */
  override fun spawnFromMap(map: Map<String, Any?>): JsMap<String, Any?> =
    JsMap.copyOf(map)

  /** @inheritDoc */
  override fun spawnFromEntries(entries: Collection<Map.Entry<String, Any?>>): JsMap<String, Any?> =
    JsMap.fromEntries(entries)

  /** @inheritDoc */
  override fun spawnFromJsEntries(entries: Collection<MapLike.Entry<String, Any?>>): JsMap<String, Any?> =
    JsMap.from(entries)

  /** @inheritDoc */
  override fun spawnUnbounded(pairs: Iterable<Pair<String, Any?>>): JsMap<String, Any?> =
    JsMap.unboundedPairs(pairs)

  /** @inheritDoc */
  override fun spawnUnboundedEntries(entries: Iterable<Map.Entry<String, Any?>>): JsMap<String, Any?> =
    JsMap.unboundedEntries(entries)

  /** @inheritDoc */
  override fun spawnUnboundedJsEntries(entries: Iterable<MapLike.Entry<String, Any?>>): JsMap<String, Any?> =
    JsMap.unbounded(entries)

  /** @inheritDoc */
  override fun implName(): String = "JsMap"

  @Test fun testMapEntry() {
    val entry = BaseJsMap.entry("hello", 5)
    assertNotNull(entry, "should be able to setup map entry")
    assertEquals("hello", entry.key)
    assertEquals(5, entry.value)
  }

  @Test fun testBasicConstructor() {
    val entry = JsMap<String, Any?>()
    assertNotNull(entry, "should be able to construct an empty map via the constructor")
  }

  @Test fun testPresizedConstructor() {
    val entry = JsMap<String, Any?>(5)
    assertNotNull(entry, "should be able to construct an empty map via the pre-sized constructor")
    val entry2: JsMap<String, Any?> = JsMap.empty(5)
    assertNotNull(entry2, "should be able to construct an empty map via the pre-sized constructor")
  }

  @Test fun testToString() {
    val entry = JsMap<String, Any?>()
    assertEquals("Map(immutable, unsorted, size=0)", entry.toString())
  }

  @Test fun testSpawnMapDirect() {
    val example = mutableMapOf("hi" to "hello")
    val wrap = JsMap.of(example)
    example["yo"] = "hey"
    assertEquals("hey", wrap["yo"], "should be able to wrap a mutable map")
  }
}
