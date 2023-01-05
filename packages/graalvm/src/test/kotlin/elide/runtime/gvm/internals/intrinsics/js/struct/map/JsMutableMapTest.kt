package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MapLike
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for the [JsMutableMap] implementation. */
@TestCase internal class JsMutableMapTest : AbstractJsMapTest<JsMutableMap<String, Any?>>() {
  /** @inheritDoc */
  override fun empty(): JsMutableMap<String, Any?> = JsMutableMap.empty()

  /** @inheritDoc */
  override fun spawnGeneric(pairs: Collection<Pair<String, Any?>>): JsMutableMap<String, Any?> =
    JsMutableMap.fromPairs(pairs)

  /** @inheritDoc */
  override fun spawnFromMap(map: Map<String, Any?>): JsMutableMap<String, Any?> =
    JsMutableMap.copyOf(map)

  /** @inheritDoc */
  override fun spawnFromEntries(entries: Collection<Map.Entry<String, Any?>>): JsMutableMap<String, Any?> =
    JsMutableMap.fromEntries(entries)

  /** @inheritDoc */
  override fun spawnFromJsEntries(entries: Collection<MapLike.Entry<String, Any?>>): JsMutableMap<String, Any?> =
    JsMutableMap.from(entries)

  /** @inheritDoc */
  override fun spawnUnbounded(pairs: Iterable<Pair<String, Any?>>): JsMutableMap<String, Any?> =
    JsMutableMap.unboundedPairs(pairs)

  /** @inheritDoc */
  override fun spawnUnboundedEntries(entries: Iterable<Map.Entry<String, Any?>>): JsMutableMap<String, Any?> =
    JsMutableMap.unboundedEntries(entries)

  /** @inheritDoc */
  override fun spawnUnboundedJsEntries(entries: Iterable<MapLike.Entry<String, Any?>>): JsMutableMap<String, Any?> =
    JsMutableMap.unbounded(entries)

  /** @inheritDoc */
  override fun implName(): String = "JsMutableMap"

  @Test fun testBasicConstructor() {
    val entry = JsMutableMap<String, Any?>()
    assertNotNull(entry, "should be able to construct an empty map via the constructor")
  }

  @Test fun testPresizedConstructor() {
    val entry = JsMutableMap<String, Any?>(5)
    assertNotNull(entry, "should be able to construct an empty map via the pre-sized constructor")
  }

  @Test fun testToString() {
    val entry = JsMutableMap<String, Any?>()
    assertEquals("Map(mutable, unsorted, size=0)", entry.toString())
  }

  @Test fun testSpawnMapDirect() {
    val example = mutableMapOf("hi" to "hello")
    val wrap = JsMutableMap.of(example)
    example["yo"] = "hey"
    assertEquals("hey", wrap["yo"], "should be able to wrap a mutable map")
  }
}
