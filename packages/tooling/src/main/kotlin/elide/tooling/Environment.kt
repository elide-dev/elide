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
package elide.tooling

import org.graalvm.nativeimage.ImageInfo
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.serialization.Serializable

/**
 * Raw map type for tracking environment.
 */
public typealias EnvironmentMap = Map<String, String>

/**
 * # Environment
 *
 * Describes the concept of a program's system environment; environment refers to a suite of key-value pairs made
 * available to a program's execution context, and which may be used to influence the behavior of the program. Variables
 * held in the environment behave roughly the same on all operating systems.
 *
 * A single instance of [Environment] may define one or more environment variables, and may or may not refer to system
 * variable values. Types are aware of the origin of each value, allowing enforcement mechanisms to take place before
 * access is granted to held values.
 */
@Serializable
public sealed interface Environment : EnvironmentMap {
  /**
   * Obtain a mutable form of this environment; if the environment object is already mutable, this is a no-op which
   * returns self.
   *
   * @return Mutable environment from this environment.
   */
  public fun toMutable(): MutableEnvironment

  /**
   * Extend this environment with the [other] environment.
   *
   * @param other Environment to extend this environment with.
   * @return Environment combining this and the [other] environment.
   */
  public fun extend(other: Environment): Environment = from(
    toMutable().plus(other.entries.map { it.key to it.value })
  )

  /**
   * Extend this environment with the [other] environment.
   *
   * @param other Environment to extend this environment with.
   * @return Environment combining this and the [other] environment.
   */
  public fun extend(other: Map<String, String>): Environment = from(
    toMutable().plus(other)
  )

  /**
   * Extend this environment with the [other] environment.
   *
   * @param other Environment to extend this environment with.
   * @return Environment combining this and the [other] environment.
   */
  public fun extend(other: Iterable<Pair<String, String>>): Environment = from(
    toMutable().plus(other)
  )

  /**
   * Extend this environment with the [other] environment.
   *
   * @param other Environment to extend this environment with.
   * @return Environment combining this and the [other] environment.
   */
  public fun extend(vararg other: Pair<String, String>): Environment = from(
    toMutable().plus(other)
  )

  /**
   * ## Empty Environment
   *
   * An empty environment profile which is immutable and can never contain values; as a result, it is expressed
   * simultaneously as a sealed type and global singleton object.
   */
  public data object EmptyEnvironment : Environment, EnvironmentMap by EMPTY {
    override fun toMutable(): MutableEnvironment = MutableEnvironment.empty()
  }

  /**
   * ## Host Environment
   *
   * Proxies access directly to the host environment; this environment is inherently immutable. Accessing a mutating
   * version of this environment extends transparently on top of host (system) environment, rather than setting process
   * environment variables.
   */
  public data object HostEnv : Environment, EnvironmentMap by HOST_PROXY {
    override fun toMutable(): MutableEnvironment = MutableEnvironment.from(this)
  }

  /**
   * ## Explicit Environment
   *
   * Specifies a suite of explicit environment variables using a [map] of key-value pairs. This environment does not
   * adopt any host values at all.
   */
  @JvmInline public value class MappedEnv internal constructor (
    private val map: PersistentMap<String, String>,
  ) : Environment, EnvironmentMap by map {
    override fun toMutable(): MutableEnvironment = MutableEnvironment.from(map)
  }

  /**
   * ## Compound Environment
   *
   * Allows one or more [Environment] instances to be combined into a single [Environment]; the specified instances are
   * consulted in order when querying against instances of [CompoundEnv].
   */
  @JvmInline public value class CompoundEnv internal constructor (
    private val state: Pair<UInt, Sequence<Environment>>
  ) : Environment {
    override val size: Int get() = state.first.toInt()
    override fun isEmpty(): Boolean = false
    override val keys: Set<String> get() = state.second.flatMap { it.keys }.toSet()
    override val values: Collection<String> get() = state.second.flatMap { it.values }.toList()
    override val entries: Set<Map.Entry<String, String>> get() = state.second.flatMap { it.entries }.toSet()
    override fun containsKey(key: String): Boolean = state.second.any { it.containsKey(key) }
    override fun containsValue(value: String): Boolean = state.second.any { it.containsValue(value) }
    override fun get(key: String): String? = state.second.firstNotNullOfOrNull { it[key] }
    override fun toMutable(): MutableEnvironment = error("Cannot mutate a compound environment.")
  }

  /** Factories for obtaining [Environment] instances. */
  public companion object {
    /** Empty environment map. */
    private val EMPTY: Map<String, String> = emptyMap()

    /** Host environment map; calculated lazily. */
    private val HOST_PROXY: Map<String, String> = object : Map<String, String> {
      private val deferredHostMap by lazy {
        System.getenv().also {
          check(!ImageInfo.inImageBuildtimeCode()) {
            "Cannot access host environment at build-time"
          }
        }
      }

      override fun containsKey(key: String): Boolean = deferredHostMap.containsKey(key)
      override fun containsValue(value: String): Boolean = deferredHostMap.containsValue(value)
      override fun get(key: String): String? = deferredHostMap[key]
      override fun isEmpty(): Boolean = deferredHostMap.isEmpty()
      override val entries: Set<Map.Entry<String, String>> get() = deferredHostMap.entries
      override val keys: Set<String> get() = deferredHostMap.keys
      override val size: Int get() = deferredHostMap.size
      override val values: Collection<String> get() = deferredHostMap.values
    }

    /** @return Empty environment instance. */
    @JvmStatic public fun empty(): EmptyEnvironment = EmptyEnvironment

    /** @return Host-backed environment instance. */
    @JvmStatic public fun host(): HostEnv = HostEnv

    /** @return Environment backed by the provided [pairs]. */
    @JvmStatic public fun of(vararg pairs: Pair<String, String>): MappedEnv = MappedEnv(pairs.toMap().toPersistentMap())

    /** @return Environment backed by the provided [map]. */
    @JvmStatic public fun of(map: PersistentMap<String, String>): MappedEnv = MappedEnv(map)

    /** @return Environment created from a copy of the provided [map]. */
    @JvmStatic public fun from(map: Map<String, String>): MappedEnv = MappedEnv(map.toPersistentMap())
  }
}
