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

package elide.runtime.lang.javascript

import com.oracle.truffle.api.source.Source
import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.objects.JSModuleLoader
import java.util.function.Predicate
import elide.runtime.lang.javascript.DelegatedModuleLoaderRegistry.DelegatedModuleRequest

/**
 * ## Delegated Module Loader Registry
 *
 * Registry for module loaders which are used as "delegates" during the module loading process; such delegates filter
 * based on the requested module names or paths.
 */
public object DelegatedModuleLoaderRegistry : Predicate<DelegatedModuleRequest> {
  /**
   * Represents a request for a delegated JavaScript module load.
   *
   * @property source The source requesting the import.
   * @property label The label of the request (e.g. the module name).
   */
  @JvmRecord
  public data class DelegatedModuleRequest private constructor (
    val source: Source?,
    val label: String,
  ) {
    public companion object {
      public fun of(source: Source): DelegatedModuleRequest = DelegatedModuleRequest(source, source.name)
      public fun of(name: String): DelegatedModuleRequest = DelegatedModuleRequest(null, name)
    }
  }

  /**
   * ### Delegate Factory
   *
   * Utility interface which functions both as a [Predicate] and a factory for creating a delegated module loader.
   */
  public interface DelegateFactory : Predicate<DelegatedModuleRequest> {
    /**
     * Invoke this factory to procure or otherwise obtain the [JSModuleLoader].
     *
     * @param realm JavaScript realm for this loader.
     * @return The module loader for the provided realm.
     */
    public operator fun invoke(realm: JSRealm): JSModuleLoader
  }

  // Registry of all delegate factories.
  @JvmStatic private val registry: MutableList<DelegateFactory> = mutableListOf()

  // Ask delegate factories if they accept this.
  override fun test(t: DelegatedModuleRequest): Boolean = registry.any { it.test(t) }

  /**
   * Register a new delegate factory.
   *
   * @param factory The factory to register.
   */
  @JvmStatic public fun register(factory: DelegateFactory) {
    registry.add(factory)
  }

  /**
   * Resolve a module request to a module loader.
   *
   * @param request The request to resolve.
   * @return The module loader, if any.
   */
  @JvmStatic public fun resolve(request: DelegatedModuleRequest, realm: JSRealm): JSModuleLoader {
    return requireNotNull(registry.firstOrNull { it.test(request) }?.invoke(realm)) {
      "Expected a module loader to resolve the request, but none was found."
    }
  }
}
