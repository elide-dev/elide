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
public interface ModuleRegistrar {
  /**
   * Register a module with the module registry.
   *
   * @param module The module to register.
   * @param impl An instance of the registered module.
   */
  public fun register(module: ModuleInfo, impl: Any)

  /**
   * Register a module with the module registry.
   *
   * @param module The module to register.
   * @param producer The factory to create the module.
   */
  public fun deferred(module: ModuleInfo, producer: ModuleFactory)
}
