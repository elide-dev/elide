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

/** Tests for the [JsConcurrentMap] implementation. */
@TestCase internal class JsConcurrentMapTest : AbstractJsMapTest<JsConcurrentMap<String, Any?>>() {
  /** @inheritDoc */
  override fun empty(): JsConcurrentMap<String, Any?> = JsConcurrentMap.empty()

  /** @inheritDoc */
  override fun spawnGeneric(pairs: Collection<Pair<String, Any?>>): JsConcurrentMap<String, Any?> =
    JsConcurrentMap.fromPairs(pairs)

  /** @inheritDoc */
  override fun spawnFromMap(map: Map<String, Any?>): JsConcurrentMap<String, Any?> =
    JsConcurrentMap.copyOf(map)

  /** @inheritDoc */
  override fun spawnFromEntries(entries: Collection<Map.Entry<String, Any?>>): JsConcurrentMap<String, Any?> =
    JsConcurrentMap.fromEntries(entries)

  /** @inheritDoc */
  override fun spawnFromJsEntries(entries: Collection<MapLike.Entry<String, Any?>>): JsConcurrentMap<String, Any?> =
    JsConcurrentMap.from(entries)

  /** @inheritDoc */
  override fun spawnUnbounded(pairs: Iterable<Pair<String, Any?>>): JsConcurrentMap<String, Any?> =
    JsConcurrentMap.unboundedPairs(pairs)

  /** @inheritDoc */
  override fun spawnUnboundedEntries(entries: Iterable<Map.Entry<String, Any?>>): JsConcurrentMap<String, Any?> =
    JsConcurrentMap.unboundedEntries(entries)

  /** @inheritDoc */
  override fun spawnUnboundedJsEntries(entries: Iterable<MapLike.Entry<String, Any?>>): JsConcurrentMap<String, Any?> =
    JsConcurrentMap.unbounded(entries)

  /** @inheritDoc */
  override fun implName(): String = "JsConcurrentMap"

  @Test fun testBasicConstructor() {
    val entry = JsConcurrentMap<String, Any?>()
    assertNotNull(entry, "should be able to construct an empty map via the constructor")
  }

  @Test fun testPresizedConstructor() {
    val entry = JsConcurrentMap<String, Any?>(5)
    assertNotNull(entry, "should be able to construct an empty map via the pre-sized constructor")
    val entry2: JsConcurrentMap<String, Any?> = JsConcurrentMap.empty(5)
    assertNotNull(entry2)
  }

  @Test fun testToString() {
    val entry = JsConcurrentMap<String, Any?>()
    assertEquals("Map(immutable, unsorted, concurrent, size=0)", entry.toString())
  }

  @Test fun testSpawnFromMap() {
    val example = mutableMapOf("hi" to "hello")
    val wrap = JsConcurrentMap.of(example)
    assertNotNull(wrap, "should be able to acquire a new map")
  }
}
