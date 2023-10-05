/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.plugins.js

import java.nio.charset.Charset
import java.time.ZoneId
import java.util.*
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.AbstractLanguageConfig
import elide.runtime.plugins.js.JavaScriptVersion.ES2022

@DelicateElideApi public class JavaScriptConfig : AbstractLanguageConfig() {
  /** Configuration for NPM features. */
  public inner class NpmConfig {
    /** Whether to enable NPM support. */
    public var enabled: Boolean = true

    /**
     * Path to look for modules at. Defaults to the current working directory.
     *
     * Note that this value does not replace the `node_modules` directory used by NPM for resolving package specifiers.
     *
     * For example: if `my_modules` is specified as a value, the interpreter will look for modules at both
     * `/my_modules` *and* `/my_modules/node_modules`, as well as other pre-defined locations, according to
     * [the Node.js specification](https://nodejs.org/api/modules.html#modules_all_together).
     */
    public var modulesPath: String = "."
  }

  public inner class BuiltInModulesConfig {
    /** Core module replacement map. */
    private val moduleReplacements = mutableMapOf<String, String>()

    internal fun replacements(): Map<String, String> {
      return moduleReplacements
    }

    /** Replace the specified built-in CommonJS [module] with a module at the [replacementPath]. */
    public fun replaceModule(module: String, replacementPath: String) {
      moduleReplacements[module] = replacementPath
    }
  }

  /** Internal NPM configuration holder, see the DSL method for entry point. */
  internal val npmConfig: NpmConfig = NpmConfig()

  /** Internal NPM configuration holder, see the DSL method for entry point. */
  internal val builtinModulesConfig: BuiltInModulesConfig = BuiltInModulesConfig()

  /** Whether to enable source-maps support, which enhances stack-traces, logs, and other system features. */
  public var sourceMaps: Boolean = true

  /**  Whether to enable V8 compatibility mode. This is not recommended for most users. */
  public var v8: Boolean = false

  /** Enable WASM support and related bindings. Defaults to `true`; only active where supported. */
  public var wasm: Boolean = true

  /** Enable experimental built-in runtime support for TypeScript. Defaults to `false`. */
  public var typescript: Boolean = false

  /** ECMA Script language level to apply within the VM; defaults to [JavaScriptVersion.ES2022]. */
  public var language: JavaScriptVersion = ES2022

  /** Default locale to apply to the JS VM. Defaults to the system default. */
  public var defaultLocale: Locale = Locale.getDefault()

  /** Default timezone to apply to the JS VM. Defaults to the system default. */
  public var timezone: ZoneId = ZoneId.systemDefault()

  /** Locale to use for embedded JS VMs. */
  public var locale: Locale = Locale.getDefault()

  /** Whether to run the JS interpreter in interactive shell mode. */
  public var interactive: Boolean = false

  /** Whether to enable ESM support. */
  public var esm: Boolean = true

  /** Configure NPM support. */
  public fun npm(config: NpmConfig.() -> Unit) {
    npmConfig.apply(config)
  }

  /** Configure builtin module replacements. */
  public fun builtinModules(config: BuiltInModulesConfig.() -> Unit) {
    builtinModulesConfig.apply(config)
  }

  /**
   * Default character set to apply when exchanging raw data with the JS VM. Defaults to `UTF-8`. `UTF-8` and `UTF-32`
   * are explicitly supported; other support may vary.
   */
  public var charset: Charset = Charsets.UTF_8

  /** Apply init-time settings to a new [context]. */
  internal fun applyTo(context: PolyglotContext) {
    // register JS intrinsics
    applyBindings(context, JavaScript)
  }
}
