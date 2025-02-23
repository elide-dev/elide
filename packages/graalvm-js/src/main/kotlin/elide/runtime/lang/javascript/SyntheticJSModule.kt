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
import elide.runtime.lang.javascript.SyntheticJSModule.ExportKind.*

/**
 * ## Synthetic JavaScript Module
 *
 * Describes a "synthetic" JavaScript module, which is a module that surfaces as a built-in for guest JavaScript code,
 * and which is typically implemented in Kotlin or generally on JVM. Synthetic modules are resolved at their built-in
 * name and routed to the appropriate implementation.
 *
 * This interface defines the API contract which is used to understand the module and to dynamically load it into a
 * suite of exports. It is not a requirement that the synthetic facade for a module be the same as the module's actual
 * implementation.
 *
 * @param T Type/shape of the module.
 * @see JSModuleProvider ESM interface for synthetic modules.
 * @see CommonJSModuleProvider CJS interface for synthetic modules.
 */
public interface SyntheticJSModule<T> : JSModuleProvider, CommonJSModuleProvider<T> {
  /**
   * Provide the module instance.
   *
   * @return Module instance.
   */
  override fun provide(): T & Any

  /**
   * Build a provider for this module.
   *
   * @return Provider for this module.
   */
  public fun provider(): JSModuleProvider = JSModuleProvider { provide() }

  /**
   * Exported symbols provided by this module.
   *
   * @return Array of exported symbols.
   */
  public fun exports(): Array<ExportedSymbol> = emptyArray()

  // Use this provider to resolve the module.
  override fun resolve(info: ModuleInfo): T & Any = provide()

  /**
   * Describes different types of exports.
   *
   * - [CLASS] indicates a constructor export at a given name.
   * - [DEFAULT] is a meta-type which wraps another type as the `default` module export.
   * - [METHOD] indicates a module-level function (or method) at a given name.
   * - [PROPERTY] indicates a module-level property at a given name.
   */
  public enum class ExportKind {
    CLASS,
    DEFAULT,
    METHOD,
    PROPERTY,
;
  }

  /**
   * ## Exported Module Symbol
   *
   * Describes a symbol which is exported from a synthetic module; such symbols are used when initializing the module
   * object in ESM mode, and used for generating bindings in string form for CJS mode.
   *
   * @param name Symbol to export this as; must be a valid identifier in JavaScript.
   * @param kind Kind of export this is.
   * @param from Optional source symbol for this symbol.
   */
  @JvmRecord public data class ExportedSymbol(
    val name: String,
    val kind: ExportKind,
    val from: String? = null,
  ) {
    public companion object {
      @JvmStatic public fun of(name: String, kind: ExportKind = PROPERTY, from: String? = null): ExportedSymbol {
        return ExportedSymbol(name, kind, from)
      }

      @JvmStatic public fun default(from: String? = null): ExportedSymbol = of("default", DEFAULT, from)

      @JvmStatic public fun method(name: String, from: String? = null): ExportedSymbol = of(name, METHOD, from)

      @JvmStatic public fun cls(name: String, from: String? = null): ExportedSymbol = of(name, CLASS, from)
    }
  }
}
