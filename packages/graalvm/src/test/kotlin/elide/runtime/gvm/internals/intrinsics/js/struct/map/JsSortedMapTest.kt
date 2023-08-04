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

package elide.runtime.gvm.internals.intrinsics.js.struct.map

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import elide.runtime.intrinsics.js.MapLike
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

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
    val map = JsSortedMap<String, Any?>()
    assertNotNull(map, "should be able to construct an empty map via the constructor")
  }

  @Test fun testToString() {
    val map = JsSortedMap<String, Any?>()
    assertEquals("Map(mutable, sorted, size=0)", map.toString())
  }

  @Test fun testComparator() {
    val map = JsSortedMap<String, Any?>()
    assertNull(map.comparator())
  }

  @Test fun testSpawn() {
    val regular = mutableMapOf("hi" to "hello")
    val map = JsSortedMap.of(regular)
    assertNotNull(map)
    assertEquals("hello", map["hi"])
  }
}
