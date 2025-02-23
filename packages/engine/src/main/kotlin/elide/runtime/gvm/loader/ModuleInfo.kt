/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.gvm.loader

import java.util.concurrent.ConcurrentSkipListMap

/**
 * Assigned string name/ID for a code module.
 */
public typealias ModuleId = String

/**
 * ## Module Info
 *
 * Describes information about a code module of some kind; the module is addressed by a simple [name].
 *
 * @property name The name of the module.
 * @property dependencies The list of module names that this module depends on.
 */
@ConsistentCopyVisibility
@JvmRecord public data class ModuleInfo private constructor (
  public val name: ModuleId,
  public val dependencies: List<ModuleId> = emptyList(),
) : Comparable<ModuleInfo> {
  override fun compareTo(other: ModuleInfo): Int = name.compareTo(other.name)

  public companion object {
    public val allModuleInfos: MutableMap<ModuleId, ModuleInfo> = ConcurrentSkipListMap<ModuleId, ModuleInfo>()

    // Register a module info record.
    @JvmStatic private fun register(name: String, vararg deps: String): ModuleInfo {
      assert(name !in allModuleInfos) { "Module $name already registered" }
      return ModuleInfo(
        name = name,
        dependencies = deps.toList(),
      ).also {
        allModuleInfos[name] = it
      }
    }

    // Obtain a module info record, registering if needed.
    @JvmStatic public fun of(name: String, vararg deps: String): ModuleInfo = allModuleInfos.computeIfAbsent(name) {
      register(
        name = name,
        deps = deps,
      )
    }

    // Obtain a module info record, or return `null` if not found.
    @JvmStatic public fun find(name: String): ModuleInfo? = allModuleInfos[name]
  }
}
