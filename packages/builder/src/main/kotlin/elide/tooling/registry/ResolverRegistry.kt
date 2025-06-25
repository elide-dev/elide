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

package elide.tooling.registry

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import elide.exec.Registry
import elide.tooling.deps.DependencyResolver

public class ResolverRegistry : Registry.Mutable<KClass<out DependencyResolver>, DependencyResolver> {
  // Registered resolvers.
  private val mut: MutableMap<KClass<out DependencyResolver>, DependencyResolver> = mutableMapOf()

  override val count: UInt get() = mut.size.toUInt()
  override fun contains(key: KClass<out DependencyResolver>): Boolean = mut.containsKey(key)
  override fun get(key: KClass<out DependencyResolver>): DependencyResolver? = mut[key]
  override fun getValue(thisRef: Any?, property: KProperty<*>): DependencyResolver? {
    error("Binding not supported for resolvers")
  }

  override fun set(key: KClass<out DependencyResolver>, value: DependencyResolver) {
    mut[key] = value
  }

  override fun remove(key: KClass<out DependencyResolver>): DependencyResolver? {
    return mut.remove(key)
  }

  override fun clear() {
    mut.clear()
  }

  override fun all(): Sequence<Pair<KClass<out DependencyResolver>, DependencyResolver>> = sequence {
    for ((key, value) in mut) {
      yield(key to value)
    }
  }

  /** Factories for creating or obtaining instances of [ResolverRegistry]. */
  public companion object {
    /** @return Empty and mutable resolver registry. */
    @JvmStatic public fun create(): ResolverRegistry = ResolverRegistry()
  }
}
