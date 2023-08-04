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
import elide.runtime.intrinsics.js.MapLike
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the [JsConcurrentSortedMap] implementation. */
@TestCase internal class JsConcurrentSortedMapTest : AbstractJsMapTest<JsConcurrentSortedMap<String, Any?>>() {
  /** @inheritDoc */
  override fun empty(): JsConcurrentSortedMap<String, Any?> = JsConcurrentSortedMap.empty()

  /** @inheritDoc */
  override fun spawnGeneric(pairs: Collection<Pair<String, Any?>>): JsConcurrentSortedMap<String, Any?> =
    JsConcurrentSortedMap.fromPairs(pairs)

  /** @inheritDoc */
  override fun spawnFromMap(map: Map<String, Any?>): JsConcurrentSortedMap<String, Any?> =
    JsConcurrentSortedMap.copyOf(map)

  /** @inheritDoc */
  override fun spawnFromEntries(entries: Collection<Map.Entry<String, Any?>>): JsConcurrentSortedMap<String, Any?> =
    JsConcurrentSortedMap.fromEntries(entries)

  /** @inheritDoc */
  override fun spawnFromJsEntries(entries: Collection<MapLike.Entry<String, Any?>>): JsConcurrentSortedMap<String, Any?>
    = JsConcurrentSortedMap.from(entries)

  /** @inheritDoc */
  override fun spawnUnbounded(pairs: Iterable<Pair<String, Any?>>): JsConcurrentSortedMap<String, Any?> =
    JsConcurrentSortedMap.unboundedPairs(pairs)

  /** @inheritDoc */
  override fun spawnUnboundedEntries(entries: Iterable<Map.Entry<String, Any?>>): JsConcurrentSortedMap<String, Any?> =
    JsConcurrentSortedMap.unboundedEntries(entries)

  /** @inheritDoc */
  override fun spawnUnboundedJsEntries(
    entries: Iterable<MapLike.Entry<String, Any?>>
  ): JsConcurrentSortedMap<String, Any?> = JsConcurrentSortedMap.unbounded(entries)

  /** @inheritDoc */
  override fun implName(): String = "JsConcurrentSortedMap"

  @Test fun testBasicConstructor() {
    val entry = JsConcurrentSortedMap<String, Any?>()
    assertNotNull(entry, "should be able to construct an empty map via the constructor")
  }

  @Test fun testToString() {
    val entry = JsConcurrentSortedMap<String, Any?>()
    assertEquals("Map(immutable, unsorted, concurrent, size=0)", entry.toString())
  }

  @Test fun testSpawn() {
    val regular = mutableMapOf("hi" to "hello")
    val map = JsConcurrentSortedMap.of(regular)
    assertNotNull(map)
    assertEquals("hello", map["hi"])
  }
}
