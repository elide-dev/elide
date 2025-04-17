/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.tool

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap

/**
 * ## Mutable Environment
 */
public sealed interface MutableEnvironment : Environment {
  /**
   * Build this suite of mutable environment properties into a finalized immutable environment.
   *
   * @return Finalized immutable environment.
   */
  public fun build(): Environment

  /**
   * ## Persistent Environment Map
   *
   * An environment variable map backed by a [PersistentMap]; usable as both a mutable and immutable object.
   */
  @JvmInline public value class PersistentEnvMap internal constructor (
    private val held: PersistentMap<String, String>,
  ) : MutableEnvironment {
    override fun containsKey(key: String): Boolean = held.containsKey(key)
    override fun containsValue(value: String): Boolean = held.containsValue(value)
    override fun get(key: String): String? = held[key]
    override val entries: Set<Map.Entry<String, String>> get() = held.entries
    override val keys: Set<String> get() = held.keys
    override val values: Collection<String> get() = held.values
    override fun isEmpty(): Boolean = held.isEmpty()
    override val size: Int get() = held.size
    override fun toMutable(): MutableEnvironment = this
    override fun build(): Environment = this
  }

  /** Factories for obtaining instances of [MutableEnvironment]. */
  public companion object {
    /** @return Mutable environment which is empty. */
    @JvmStatic public fun empty(): MutableEnvironment = from(persistentMapOf())

    /** @return Mutable environment backed by a copy of a base [environment]. */
    @JvmStatic public fun from(environment: Environment): MutableEnvironment =
      PersistentEnvMap(environment.entries.associate { it.key to it.value }.toPersistentMap())

    /** @return Mutable environment backed by a copy-on-write [PersistentMap]. */
    @JvmStatic public fun from(persistent: PersistentMap<String, String>): MutableEnvironment {
      return PersistentEnvMap(persistent)
    }
  }
}
