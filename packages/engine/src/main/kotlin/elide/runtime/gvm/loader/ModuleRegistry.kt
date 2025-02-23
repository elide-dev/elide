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

/**
 * ## Module Registry
 */
public object ModuleRegistry : ModuleRegistrar, ModuleResolver {
  private val registered = sortedMapOf<ModuleInfo, Any>()
  private val factories = sortedMapOf<ModuleInfo, ModuleFactory>()

  override fun register(module: ModuleInfo, impl: Any) {
    assert(module !in registered) { "Module already registered: $module" }
    assert(module !in factories) { "Module already registered as factory: $module" }
    factories[module] = ModuleFactory { _ -> impl }
  }

  override fun deferred(module: ModuleInfo, producer: ModuleFactory) {
    factories[module] = producer
  }

  override operator fun contains(mod: ModuleInfo): Boolean = mod in factories

  override fun load(info: ModuleInfo): Any = when (info) {
    in registered -> registered[info]!!
    in factories -> factories[info]!!.let { fac ->
      fac.load(info).also {
        registered[info] = it
      }
    }
    else -> error("Module not registered: $info")
  }
}
