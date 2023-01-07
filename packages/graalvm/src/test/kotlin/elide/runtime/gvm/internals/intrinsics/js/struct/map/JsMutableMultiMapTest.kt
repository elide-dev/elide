package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MapLike
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for the [JsMutableMultiMap] implementation. */
@TestCase internal class JsMutableMultiMapTest : AbstractJsMapTest<JsMutableMultiMap<String, Any?>>() {
  /** @inheritDoc */
  override fun empty(): JsMutableMultiMap<String, Any?> = JsMutableMultiMap.empty()

  /** @inheritDoc */
  override fun spawnGeneric(pairs: Collection<Pair<String, Any?>>): JsMutableMultiMap<String, Any?> =
    JsMutableMultiMap.fromPairs(pairs)

  /** @inheritDoc */
  override fun spawnFromMap(map: Map<String, Any?>): JsMutableMultiMap<String, Any?> =
    JsMutableMultiMap.copyOf(map)

  /** @inheritDoc */
  override fun spawnFromEntries(entries: Collection<Map.Entry<String, Any?>>): JsMutableMultiMap<String, Any?> =
    JsMutableMultiMap.fromEntries(entries)

  /** @inheritDoc */
  override fun spawnFromJsEntries(entries: Collection<MapLike.Entry<String, Any?>>): JsMutableMultiMap<String, Any?> =
    JsMutableMultiMap.from(entries)

  /** @inheritDoc */
  override fun spawnUnbounded(pairs: Iterable<Pair<String, Any?>>): JsMutableMultiMap<String, Any?> =
    JsMutableMultiMap.unboundedPairs(pairs)

  /** @inheritDoc */
  override fun spawnUnboundedEntries(entries: Iterable<Map.Entry<String, Any?>>): JsMutableMultiMap<String, Any?> =
    JsMutableMultiMap.unboundedEntries(entries)

  /** @inheritDoc */
  override fun spawnUnboundedJsEntries(entries: Iterable<MapLike.Entry<String, Any?>>): JsMutableMultiMap<String, Any?>
    = JsMutableMultiMap.unbounded(entries)

  /** @inheritDoc */
  override fun implName(): String = "JsMutableMultiMap"

  @Test fun testBasicConstructor() {
    val entry = JsMutableMultiMap<String, Any?>()
    assertNotNull(entry, "should be able to construct an empty map via the constructor")
  }

  @Test fun testPresizedConstructor() {
    val entry = JsMutableMultiMap<String, Any?>(5)
    assertNotNull(entry, "should be able to construct an empty map via the pre-sized constructor")
    val entry2: JsMutableMultiMap<String, Any?> = JsMutableMultiMap.empty(5)
    assertNotNull(entry2, "should be able to construct an empty map via the pre-sized constructor")
  }

  @Test fun testToString() {
    val entry = JsMutableMultiMap<String, Any?>()
    assertEquals("MultiMap(mutable, unsorted, size=0)", entry.toString())
  }

  @Test fun testSpawnMap() {
    val example = mutableMapOf("hi" to "hello")
    val target = JsMutableMultiMap.of(example)
    assertNotNull(target)
    assertEquals("hello", target["hi"])
  }
}
