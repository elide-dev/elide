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

import com.oracle.truffle.api.TruffleFile

/**
 * ## Common JS Module Provider
 *
 * Augments the [JSModuleProvider] interface with support infrastructure for synthetic CommonJS imports; in this case,
 * modules are rendered to strings, which are loaded through a synthetic [TruffleFile]. This is useful for making
 * built-in modules accessible via a single and uniform interface through both ESM and CJS.
 *
 * When importing an ESM module, the [ElideJsModuleRouter] will call in to pre-render the Truffle file and inject it
 * into the module cache. This allows ESM and CJS to behave interchangeably: it should not matter which module system
 * is used first to load a built-in, as the result will be the same (a generated Truffle file is loaded for CJS and
 * injected into the cache, and, as applicable, a synthetic module is returned directly for use in ESM contexts).
 */
public interface CommonJSModuleProvider<T> {
  /**
   * Resolve this module to an implementation object which is usable in a guest context; this can also be a Truffle
   * compatible file.
   *
   * @return Optional implementation object.
   */
  public fun provide(): T & Any
}
