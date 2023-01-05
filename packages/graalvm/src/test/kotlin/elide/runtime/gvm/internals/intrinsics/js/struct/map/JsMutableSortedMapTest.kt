package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MapLike
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for the [JsMutableSortedMap] implementation. */
@TestCase internal class JsMutableSortedMapTest : AbstractJsMapTest<JsMutableSortedMap<String, Any?>>() {
  /** @inheritDoc */
  override fun empty(): JsMutableSortedMap<String, Any?> = JsMutableSortedMap.empty()

  /** @inheritDoc */
  override fun spawnGeneric(pairs: Collection<Pair<String, Any?>>): JsMutableSortedMap<String, Any?> =
    JsMutableSortedMap.fromPairs(pairs)

  /** @inheritDoc */
  override fun spawnFromMap(map: Map<String, Any?>): JsMutableSortedMap<String, Any?> =
    JsMutableSortedMap.copyOf(map)

  /** @inheritDoc */
  override fun spawnFromEntries(entries: Collection<Map.Entry<String, Any?>>): JsMutableSortedMap<String, Any?> =
    JsMutableSortedMap.fromEntries(entries)

  /** @inheritDoc */
  override fun spawnFromJsEntries(entries: Collection<MapLike.Entry<String, Any?>>): JsMutableSortedMap<String, Any?> =
    JsMutableSortedMap.from(entries)

  /** @inheritDoc */
  override fun spawnUnbounded(pairs: Iterable<Pair<String, Any?>>): JsMutableSortedMap<String, Any?> =
    JsMutableSortedMap.unboundedPairs(pairs)

  /** @inheritDoc */
  override fun spawnUnboundedEntries(entries: Iterable<Map.Entry<String, Any?>>): JsMutableSortedMap<String, Any?> =
    JsMutableSortedMap.unboundedEntries(entries)

  /** @inheritDoc */
  override fun spawnUnboundedJsEntries(entries: Iterable<MapLike.Entry<String, Any?>>): JsMutableSortedMap<String, Any?> =
    JsMutableSortedMap.unbounded(entries)

  /** @inheritDoc */
  override fun implName(): String = "JsSortedMap"

  @Test fun testBasicConstructor() {
    val entry = JsMutableSortedMap<String, Any?>()
    assertNotNull(entry, "should be able to construct an empty map via the constructor")
  }

  @Test fun testToString() {
    val entry = JsMutableSortedMap<String, Any?>()
    assertEquals("Map(mutable, sorted, size=0)", entry.toString())
  }
}
