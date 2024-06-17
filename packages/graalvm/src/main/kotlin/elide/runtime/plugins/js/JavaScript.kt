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
package elide.runtime.plugins.js

import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.LogLevel.DEBUG
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.*
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.disableOptions
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.extensions.setOptions
import elide.runtime.core.plugin
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest
import elide.runtime.plugins.env.Environment
import elide.runtime.plugins.js.JavaScriptVersion.*

/** Engine plugin adding support for JavaScript. */
@DelicateElideApi public class JavaScript private constructor(
  private val config: JavaScriptConfig,
  private val resources: LanguagePluginManifest,
  private val environment: Environment? = null,
) {
  // Whether modules have initialized.
  private val modulesInitialized = AtomicBoolean(false)

  // Logger for the JavaScript plugin.
  private val logging by lazy { Logging.of(JavaScript::class) }

  private fun initializeContext(context: PolyglotContext) {
    // if applicable, install the env plugin bindings
    environment?.install(context, JavaScript)

    // apply init-time settings
    config.applyTo(context)

    // run embedded initialization code
    initializeEmbeddedScripts(context, resources)
  }

  private fun generateImports(name: String): Source = Source.newBuilder("js", StringBuilder().apply {
    if (name.startsWith("elide:")) {
      val modname = name.removePrefix("elide:")
      append("import '$modname';")
      append("require('$modname');")
    } else {
      append("import 'node:$name';")
      append("require('node:$name');")
    }
  }.toString(), "__elide-internal-module-import_$name.mjs").apply {
    cached(true)
    interactive(false)
    internal(true)
  }.build()

  @Synchronized private fun finalizeContext(context: PolyglotContext) {
    config.builtinModulesConfig.finalize()
    if (EXPERIMENTAL_MODULE_PRELOADING && !modulesInitialized.get()) {
      modulesInitialized.compareAndSet(false, true)
      config.builtinModulesConfig.replacements().keys.asSequence().flatMap {
        if (it.startsWith("elide:")) it.removePrefix("elide:").let { modname ->
          sequenceOf(
            it to "node_modules/elide:$modname/$modname.cjs",
            it to "node_modules/elide:$modname/$modname.mjs",
          )
        } else sequenceOf(
          it to "__runtime__/$it/$it.cjs",
          it to "__runtime__/$it/$it.mjs",
        )
      }.let { mods ->
        try {
          val logDebug = logging.isEnabled(DEBUG)
          context.enter()
          mods.forEach { (name, path) ->
            if (logDebug) logging.debug("Preloading JS module '$name' from '$path'")

            try {
              context.evaluate(generateImports(name))
            } catch (err: Throwable) {
              // log but otherwise ignore
              logging.error("Failed to preload module '$name' from '$path'", err)
            }
          }
        } finally {
          context.leave()
        }
      }
    }
  }

  private fun configureContext(builder: PolyglotContextBuilder): Unit = with(builder) {
    enableOptions(
      "js.allow-eval",
      "js.async-context",
      "js.async-iterator-helpers",
      "js.async-stack-traces",
      "js.atomics-wait-async",
      "js.bind-member-functions",
      "js.class-fields",
      "js.direct-byte-buffer",
      "js.error-cause",
      "js.foreign-hash-properties",
      "js.foreign-object-prototype",
      "js.import-attributes",
      "js.intl-402",
      "js.iterator-helpers",
      "js.json-modules",
      "js.lazy-translation",
      "js.new-set-methods",
      "js.performance",
      "js.shared-array-buffer",
      "js.strict",
      "js.temporal",
      "js.top-level-await",
      // Experimental:
      "js.annex-b",
      "js.atomics",
      "js.esm-eval-returns-exports",
      "js.scope-optimization",
      "js.shadow-realm",
    )

    disableOptions(
      "js.console",
      "js.graal-builtin",
      "js.interop-complete-promises",
      "js.java-package-globals",
      "js.load",
      "js.polyglot-evalfile",
      "js.print",
      "js.regexp-static-result",
      "js.scripting",
      "js.syntax-extensions",
      // Experimental:
      "js.operator-overloading",
      "js.polyglot-builtin",
    )

    setOptions(
      "js.commonjs-require-cwd" to (config.npmConfig.modulesPath?.ifBlank { null } ?: "."),
      "js.debug-property-name" to DEBUG_GLOBAL,
      "js.ecmascript-version" to config.language.symbol(),
      "js.function-constructor-cache-size" to FUNCTION_CONSTRUCTOR_CACHE_SIZE,
      "js.locale" to config.locale.toString(),
      "js.unhandled-rejections" to UNHANDLED_REJECTIONS,
      // Experimental:
      "js.charset" to config.charset.name(),
    )

    setOptions(
      "js.commonjs-require" to config.npmConfig.enabled,
      "js.esm-bare-specifier-relative-lookup" to config.esm,
      "js.shell" to config.interactive,
      "js.v8-compat" to config.v8,
      // Experimental:
      "js.esm-eval-returns-exports" to config.esm,
      "js.v8-intrinsics" to config.v8,
    )

    if (config.wasm && wasmSupported) enableOptions(
      "wasm.BulkMemoryAndRefTypes",
      "wasm.Memory64",
      "wasm.UseUnsafeMemory",
      // Experimental:
      "wasm.MultiValue",
    ).also {
      option("wasm.Builtins", WASI_STD)
    }

    if (config.npmConfig.enabled) {
      val replacement = config.builtinModulesConfig.replacements().entries.joinToString(",") {
        "${it.key}:${it.value}"
      }

      option("js.commonjs-core-modules-replacements", replacement)
    }
  }

  /**
   * Engine plugin providing support for the JavaScript language, including a limited set of intrinsics and core module
   * replacements for Node.js.
   */
  public companion object Plugin : AbstractLanguagePlugin<JavaScriptConfig, JavaScript>() {
    private const val JS_LANGUAGE_ID = "js"
    private const val JS_PLUGIN_ID = "JavaScript"

    private const val EXPERIMENTAL_MODULE_PRELOADING = true

    private const val WASI_STD = "wasi_snapshot_preview1"
    private const val FUNCTION_CONSTRUCTOR_CACHE_SIZE: String = "256"
    private const val UNHANDLED_REJECTIONS: String = "handler"
    private const val DEBUG_GLOBAL: String = "__ElideDebug__"

    override val languageId: String = JS_LANGUAGE_ID
    override val key: Key<JavaScript> = Key(JS_PLUGIN_ID)

    @JvmStatic internal val wasmSupported by lazy {
      Engine.create().languages.containsKey("wasm")
    }

    override fun install(scope: InstallationScope, configuration: JavaScriptConfig.() -> Unit): JavaScript {
      configureLanguageSupport(scope)

      // resolve the env plugin (if present, otherwise ignore silently)
      val env = scope.configuration.plugin(Environment)

      // apply the configuration and create the plugin instance
      val config = JavaScriptConfig().apply(configuration)
      configureSharedBindings(scope, config)
      
      val embedded = resolveEmbeddedManifest(scope)
      val instance = JavaScript(config, embedded, env)

      // register resources with the VFS
      installEmbeddedBundles(scope, embedded)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      scope.lifecycle.on(ContextInitialized, instance::initializeContext)
      scope.lifecycle.on(ContextFinalized, instance::finalizeContext)
      return instance
    }

    private fun JavaScriptVersion.symbol(): String = when (this) {
      ES5,
      ES6,
      ES2017,
      ES2018,
      ES2019,
      ES2020,
      ES2021,
      ES2022,
      ES2023 -> this.name.drop(2)

      STABLE -> "stable"
      LATEST -> "latest"
    }
  }
}
