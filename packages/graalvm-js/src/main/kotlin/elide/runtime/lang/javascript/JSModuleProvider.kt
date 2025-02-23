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

package elide.runtime.lang.javascript

import elide.runtime.gvm.loader.ModuleInfo

/**
 * ## JavaScript Module Provider
 *
 * This interface is implemented for classes which can resolve built-in JavaScript modules; usually, a module provider
 * is paired with (or is also a) [SyntheticJSModule]. The module provider is responsible for bootstrapping or loading
 * the module, as needed, and then providing an instance which satisfies the import request.
 */
public fun interface JSModuleProvider {
  /**
   * ## Resolve Module
   *
   * Given a suite of module info for a built-in module, resolve the module implementation and provide it as the return
   * value.
   *
   * @param info The module info for the built-in module.
   * @return The resolved module instance.
   */
  public fun resolve(info: ModuleInfo): Any
}
