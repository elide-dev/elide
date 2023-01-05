package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MapLike
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for the [JsSortedMap] implementation. */
@TestCase internal class JsSortedMapTest : AbstractJsMapTest<JsSortedMap<String, Any?>>() {
  /** @inheritDoc */
  override fun empty(): JsSortedMap<String, Any?> = JsSortedMap.empty()

  /** @inheritDoc */
  override fun spawnGeneric(pairs: Collection<Pair<String, Any?>>): JsSortedMap<String, Any?> =
    JsSortedMap.fromPairs(pairs)

  /** @inheritDoc */
  override fun spawnFromMap(map: Map<String, Any?>): JsSortedMap<String, Any?> =
    JsSortedMap.copyOf(map)

  /** @inheritDoc */
  override fun spawnFromEntries(entries: Collection<Map.Entry<String, Any?>>): JsSortedMap<String, Any?> =
    JsSortedMap.fromEntries(entries)

  /** @inheritDoc */
  override fun spawnFromJsEntries(entries: Collection<MapLike.Entry<String, Any?>>): JsSortedMap<String, Any?> =
    JsSortedMap.from(entries)

  /** @inheritDoc */
  override fun spawnUnbounded(pairs: Iterable<Pair<String, Any?>>): JsSortedMap<String, Any?> =
    JsSortedMap.unboundedPairs(pairs)

  /** @inheritDoc */
  override fun spawnUnboundedEntries(entries: Iterable<Map.Entry<String, Any?>>): JsSortedMap<String, Any?> =
    JsSortedMap.unboundedEntries(entries)

  /** @inheritDoc */
  override fun spawnUnboundedJsEntries(entries: Iterable<MapLike.Entry<String, Any?>>): JsSortedMap<String, Any?> =
    JsSortedMap.unbounded(entries)

  /** @inheritDoc */
  override fun implName(): String = "JsSortedMap"

  @Test fun testBasicConstructor() {
    val entry = JsSortedMap<String, Any?>()
    assertNotNull(entry, "should be able to construct an empty map via the constructor")
  }

  @Test fun testToString() {
    val entry = JsSortedMap<String, Any?>()
    assertEquals("Map(immutable, sorted, size=0)", entry.toString())
  }
}
