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
package elide.runtime.plugins.js

import java.nio.charset.Charset
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
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
    public var modulesPath: String? = null
  }

  /** Configuration of experimental features. */
  public inner class LabsConfig {
    /** Disable polyfills in JavaScript/TypeScript contexts. */
    public var disablePolyfills: Boolean = false

    /** Disable VFS provided for JavaScript builtins. */
    public var disableVfs: Boolean = false
  }

  public inner class BuiltInModulesConfig {
    private val closed: AtomicBoolean = AtomicBoolean(false)

    internal fun finalize() {
      closed.set(true)
    }

    /** Injected Elide API modules. */
    private val elideModules: MutableMap<String, String> = listOf(
      "sqlite",
    ).flatMap { listOf(it, "elide:$it") }.associateWith {
      val mod = if (!it.startsWith("elide:")) "elide:$it" else it
      "/__runtime__/$mod"
    }.toMutableMap()

    /** Core module replacement map. */
    private val moduleReplacements: MutableMap<String, String> = listOf(
      "assert",
      "assert/strict",
      "buffer",
      "child_process",
      "cluster",
      "console",
      "crypto",
      "dgram",
      "diagnostics_channel",
      "dns",
      "dns/promises",
      "domain",
      "events",
      "express",
      "fs",
      "fs/promises",
      "http",
      "http2",
      "https",
      "inspector",
      "inspector/promises",
      "module",
      "net",
      "os",
      "path",
      "perf_hooks",
      "process",
      "querystring",
      "readline",
      "readline/promises",
      "stream",
      "stream/consumers",
      "stream/promises",
      "stream/web",
      "string_decoder",
      "test",
      "timers",
      "timers/promises",
      "tls",
      "trace_events",
      "tty",
      "url",
      "util",
      "v8",
      "vm",
      "wasi",
      "worker",
      "zlib",
    ).associateWith {
      "/__runtime__/$it"
    }.toMutableMap().also {
      it.putAll(elideModules)
    }

    internal fun replacements(): Map<String, String> {
      return moduleReplacements
    }

    /** Replace the specified built-in CommonJS [module] with a module at the [replacementPath]. */
    public fun replaceModule(module: String, replacementPath: String) {
      if (!closed.get()) error("Cannot replace module after modules have been finalized")
      moduleReplacements[module] = replacementPath
    }
  }

  /** Internal NPM configuration holder, see the DSL method for entry point. */
  internal val npmConfig: NpmConfig = NpmConfig()

  /** Experimental options for use with JavaScript. */
  internal val labsConfig: LabsConfig = LabsConfig()

  /** Internal NPM configuration holder, see the DSL method for entry point. */
  internal val builtinModulesConfig: BuiltInModulesConfig = BuiltInModulesConfig()

  /** Whether to enable source-maps support, which enhances stack-traces, logs, and other system features. */
  public var sourceMaps: Boolean = true

  /** Whether JS strict mode is active by default. */
  public var strict: Boolean = true

  /**  Whether to enable V8 compatibility mode. */
  public var v8: Boolean = true

  /** Enable WASM support and related bindings. Defaults to `true`; only active where supported. */
  public var wasm: Boolean = true

  /** Enable experimental built-in runtime support for TypeScript. Defaults to `false`. */
  public var typescript: Boolean = true

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

  /** Whether to enable interop features (imports, etc). */
  public var interop: Boolean = true

  /** Configure NPM support. */
  public fun npm(config: NpmConfig.() -> Unit) {
    npmConfig.apply(config)
  }

  /** Configure experimental options. */
  public fun experimental(config: LabsConfig.() -> Unit) {
    labsConfig.apply(config)
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
