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

@file:Suppress("JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE")

package elide.runtime.gvm.internals.js

import io.micronaut.context.annotation.Requires
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream
import elide.annotations.Context
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.RequestExecutionInputs
import elide.runtime.gvm.api.GuestRuntime
import elide.runtime.gvm.cfg.JsRuntimeConfig
import elide.runtime.gvm.internals.*
import elide.runtime.gvm.internals.GraalVMGuest.JAVASCRIPT
import elide.runtime.gvm.internals.context.ContextManager
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Value as GuestValue
import elide.runtime.gvm.internals.VMStaticProperty as StaticProperty

/**
 * # JavaScript Runtime
 *
 * Implements a guest runtime for Elide which is powered by ECMAScript-compliant JavaScript code. The JavaScript runtime
 * is based on GraalJS (as distinguished from GraalVM's Node.js engine). As a pure JavaScript environment, GraalJS does
 * not support the standard set of Node.js intrinsics collectively known as the "Node APIs."
 *
 * Elide's JavaScript engine implementation helps bridge this gap, by adding support for now-standard interfaces (such
 * as CloudFlare's Workers APIs), and by wiring together polyglot support across other guests and the main host app.
 *
 * &nbsp;
 *
 * ## Architecture of the JS VM
 *
 * The JavaScript runtime is based on a set of components which, together, form a cohesive JavaScript runtime. These
 * components are outlined below:
 *
 * - **[JsRuntime] (this class)**: implements [AbstractVMEngine] with JavaScript-specific types, configurations, and
 *   invocation bindings. This class is responsible for directly executing guest JavaScript code, and for consistently
 *   configuring each JavaScript guest environment.
 *
 * - **JS guest implementation types:** this would include [JsRuntimeConfig], [JsExecutableScript], and
 *   [JsInvocationBindings], which together explain to Elide how to configure the VM and invoke guest code.
 *
 * - **JS intrinsics**: implementations of standard APIs in Java/Kotlin, which are exposed for guest code use in JS via
 *   globals or built-in modules. JavaScript intrinsics are managed by the [IntrinsicsManager], which uses a suite of
 *   `IntrinsicsResolver` instances to locate and mount intrinsics within the guest environment.
 *
 * - **JS facade and polyfills**: this code is written in TypeScript or JavaScript, and forms the substrate upon which
 *   guest JavaScript code runs. Sometimes, JS intrinsics are exposed only via JS facade code, which helps smooth over
 *   type inconsistencies across languages and offer a fully native JavaScript dev experience (i.e. TypeScript typings,
 *   source maps, and so forth).
 *
 * Like all Elide guest VMs, the JavaScript VM also supports the use of a virtualized file-system, which can be backed
 * by a tarball or Elide VFS file. Files made available via file-system settings are supported for import via regular
 * Node.js `require(...)` calls, as well as ECMAScript Module (ESM) imports (`import ... from ...`).
 *
 * &nbsp;
 *
 * ### Building a JS context
 *
 * Contexts are managed and acquired through the active [ContextManager] implementation. The [ContextManager] is
 * configured with methods from this implementation in order to configure VM contexts consistently. The context manager
 * is charged with managing safe multithreaded access to the active suite of VM contexts (one or more depending on
 * operating context).
 *
 * &nbsp;
 *
 * ### Execution of guest code
 *
 * In CLI contexts, the VM is designed to spin up only one responding runtime instance, and manage access to it through
 * calling threads. In server contexts, the VM spins up a configurable number of responding VM contexts, each of which
 * are bound to the same VM engine and share a code cache. Requests are dispatched across these instances, each with a
 * dedicated native thread, using a ring-buffer and disruptor pattern.
 *
 * &nbsp;
 *
 * ## Configuring the VM
 *
 * Most configuration of the VM is automatic and needs no user intervention. The VM is configured with sensible and
 * modern defaults, and with strong host app isolation. There is a set of VM properties which are configurable through
 * Micronaut-style app configuration, available to each guest; then, on top of these standard properties, guest VMs can
 * define their own configurable properties which map to VM configurations.
 *
 * Basic properties which must be configured for the JS VM include:
 * - `elide.gvm.enabled`: Must be flipped to `true` (the default value) to enable guest execution at all.
 * - `elide.gvm.js.enabled`: Must be flipped to `true` (the default value) to enable JavaScript guest code execution.
 *
 * Properties which can further be configured by the developer include:
 * - All properties from [elide.runtime.gvm.cfg.GuestRuntimeConfiguration].
 * - All properties from [JsRuntimeConfig].
 */
@Requires(property = "elide.gvm.enabled", value = "true", defaultValue = "true")
@Requires(property = "elide.gvm.js.enabled", value = "true", defaultValue = "true")
@GuestRuntime(engine = "js")
internal class JsRuntime @Inject constructor (
) : AbstractVMEngine<JsRuntimeConfig, JsExecutableScript, JsInvocationBindings>(JAVASCRIPT) {
  companion object {
    private const val DEFAULT_STREAM_ENCODING: String = "UTF-8"
    private const val DEFAULT_JS_LANGUAGE_LEVEL: String = "2022"
    private const val JS_LANGUAGE_LEVEL_STABLE: String = "stable"
    private const val JS_LANGUAGE_LEVEL_LATEST: String = "latest"
    private const val DEFAULT_JS_LOCALE: String = "en-US"
    private const val FUNCTION_CONSTRUCTOR_CACHE_SIZE: String = "256"
    private const val UNHANDLED_REJECTIONS: String = "handler"
    private const val DEBUG_GLOBAL: String = "__ElideDebug__"
    private const val WASI_STD: String = "wasi_snapshot_preview1"

    // Hard-coded JS VM options.
    val baseOptions : List<VMProperty> = listOf(
      StaticProperty.active("js.async-stack-traces"),
      StaticProperty.active("js.atomics"),
      StaticProperty.active("js.bind-member-functions"),
      StaticProperty.active("js.class-fields"),
      StaticProperty.active("js.direct-byte-buffer"),
      StaticProperty.active("js.disable-eval"),
      StaticProperty.active("js.error-cause"),
      StaticProperty.active("js.esm-eval-returns-exports"),
      StaticProperty.active("js.foreign-hash-properties"),
      StaticProperty.active("js.foreign-object-prototype"),
      StaticProperty.active("js.import-assertions"),
      StaticProperty.active("js.intl-402"),
      StaticProperty.active("js.json-modules"),
      StaticProperty.active("js.performance"),
      StaticProperty.active("js.shared-array-buffer"),
      StaticProperty.active("js.strict"),
      StaticProperty.active("js.temporal"),
      StaticProperty.active("js.top-level-await"),
      StaticProperty.active("js.shadow-realm"),
      // @TODO(sgammon): disable once https://github.com/oracle/graaljs/issues/119 is resolved.
      StaticProperty.active("js.nashorn-compat"),
      StaticProperty.inactive("js.operator-overloading"),
      StaticProperty.inactive("js.annex-b"),
      StaticProperty.inactive("js.console"),
      StaticProperty.inactive("js.graal-builtin"),
      StaticProperty.inactive("js.interop-complete-promises"),
      StaticProperty.inactive("js.java-package-globals"),
      StaticProperty.inactive("js.load"),
      StaticProperty.inactive("js.print"),
      StaticProperty.inactive("js.polyglot-builtin"),
      StaticProperty.inactive("js.polyglot-evalfile"),
      StaticProperty.inactive("js.regex-static-result"),
      StaticProperty.inactive("js.scripting"),
      StaticProperty.inactive("js.syntax-extensions"),
      StaticProperty.of("js.unhandled-rejections", UNHANDLED_REJECTIONS),
      StaticProperty.of("js.debug-property-name", DEBUG_GLOBAL),
      StaticProperty.of("js.function-constructor-cache-size", FUNCTION_CONSTRUCTOR_CACHE_SIZE),
    )

    // Hard-coded WASM VM options.
    val wasmOptions : List<VMProperty> = listOf(
      StaticProperty.active("wasm.Memory64"),
      StaticProperty.active("wasm.MultiValue"),
      StaticProperty.active("wasm.UseUnsafeMemory"),
//      StaticProperty.active("wasm.MultiMemory"),
      StaticProperty.active("wasm.BulkMemoryAndRefTypes"),
//      StaticProperty.active("wasm.Threads"),
      StaticProperty.of("wasm.Builtins", WASI_STD),
    )

    // Root where we can find runtime-related files.
    private const val EMBEDDED_ROOT = "/META-INF/elide/embedded/runtime/js"

    // Virtualized file name for the runtime pre-init (facade).
    private const val RUNTIME_PREINIT = "__runtime__.js"

    // String to use for JavaScript MIME type declarations/mappings.
    private const val JS_MIMETYPE = "application/javascript"

    // Whether runtime assets have loaded.
    internal val runtimeReady: AtomicBoolean = AtomicBoolean(false)

    // Info about the runtime, loaded from the runtime bundle manifest.
    internal val runtimeInfo: AtomicReference<RuntimeInfo> = AtomicReference(null)

    // Assembled runtime init code, loaded from `runtimeInfo`.
    internal val runtimeInit: AtomicReference<Source> = AtomicReference(null)

    // Core modules which patch Node.js builtins.
    private val coreModules: Map<String, String> = mapOf(
      "buffer" to "/__runtime__/buffer/buffer.cjs",
      "util" to "/__runtime__/util/util.cjs",
      "fs" to "/__runtime__/fs/fs.cjs",
      "express" to "/__runtime__/express/express.cjs",
    )

    init {
      check(!runtimeReady.get()) {
        "Runtime cannot be prepared more than once (JS runtime must operate as a singleton)"
      }

      resolveRuntimeInfo(JAVASCRIPT.symbol, RUNTIME_PREINIT, JS_MIMETYPE).let { (info, facade) ->
        runtimeInfo.set(info)
        runtimeInit.set(facade)
        runtimeReady.set(true)
      }
    }
  }

  /** Configurator: VFS. Injects JavaScript runtime assets as a VFS component. */
  @Singleton @Context class JsRuntimeVFSConfigurator : GuestVFSConfigurator(
    GraalVMGuest.JAVASCRIPT,
    { runtimeInfo.get() }
  )

  // Resolve the expected configuration symbol for the given ECMA standard level.
  private val JsLanguageLevel.symbol: String get() = when (this) {
    JsLanguageLevel.ES5,
    JsLanguageLevel.ES6,
    JsLanguageLevel.ES2017,
    JsLanguageLevel.ES2018,
    JsLanguageLevel.ES2019,
    JsLanguageLevel.ES2020,
    JsLanguageLevel.ES2021,
    JsLanguageLevel.ES2022 -> this.name.drop(2)
    JsLanguageLevel.STABLE -> JS_LANGUAGE_LEVEL_STABLE
    JsLanguageLevel.LATEST -> JS_LANGUAGE_LEVEL_LATEST
    JsLanguageLevel.UNRECOGNIZED,
    JsLanguageLevel.JS_LANGUAGE_LEVEL_DEFAULT -> DEFAULT_JS_LANGUAGE_LEVEL
  }

  // JS runtime logger.
  private val logger: Logger by lazy {
    Logging.of(JsRuntime::class)
  }

  /** JavaScript engine configuration. */
  @Inject lateinit var jsConfig: JsRuntimeConfig

  override fun resolveConfig(): JsRuntimeConfig = jsConfig

  /** @inheritDoc */
  override fun configure(engine: Engine, context: VMContext.Builder): Stream<VMProperty> = baseOptions.plus(listOf(
    // `vm.locale`: maps to `js.locale` and controls various locale settings in the JS VM
    VMRuntimeProperty.ofConfigurable("vm.locale", "js.locale", DEFAULT_JS_LOCALE) {
      (config.locale ?: guestConfig.locale)?.toString() ?: DEFAULT_JS_LOCALE
    },

    // `vm.charset`: maps to `js.charset` and controls encoding of raw data exchanged with JS VM
    VMRuntimeProperty.ofConfigurable("vm.charset", "js.charset", DEFAULT_STREAM_ENCODING) {
      (config.charset ?: guestConfig.charset)?.name() ?: DEFAULT_STREAM_ENCODING
    },

    // `vm.ecma-version`: maps to `js.ecmascript-version` and controls the JS language level
    VMRuntimeProperty.ofConfigurable("vm.js.ecma", "js.ecmascript-version") {
      config.language?.symbol ?: JsRuntimeConfig.DEFAULT_JS_LANGUAGE_LEVEL.symbol
    },

    // Shell: Enable JS shell features if running interactively.
    VMRuntimeProperty.ofConfigurable("vm.interactive", "js.shell") {
      (System.getProperty("vm.interactive")?.toBoolean() ?: false).toString()
    },

    // `vm.js.esm`: maps to `js.esm-eval-*` to enable/disable ESM import support.
    VMRuntimeProperty.ofBoolean("vm.js.esm", "js.esm-eval-returns-exports") {
      config.esm.isEnabled
    },

    // `vm.js.esm.bare-specifiers`: enables bare-specifier ESM imports.
    VMRuntimeProperty.ofBoolean("vm.js.esm.bare-specifiers", "js.esm-bare-specifier-relative-lookup") {
      config.esm.isEnabled
    },

    // `vm.js.npm`: maps to `js.commonjs-require` to enable/disable commonjs require support.
    VMRuntimeProperty.ofBoolean("vm.js.npm", "js.commonjs-require") {
      config.npm.isEnabled
    },

    // static: configure module replacements.
    StaticProperty.of("js.commonjs-core-modules-replacements", if (config.npm.isEnabled) {
      coreModules.entries.joinToString(",") {
        "${it.key}:${it.value}"
      }
    } else {
      ""  // disabled if NPM support is turned off
    }),

    // `vm.js.nodeModules`: maps to `js.commonjs-require` to enable/disable NPM require support.
    VMRuntimeProperty.ofConfigurable("vm.js.nodeModules", "js.commonjs-require-cwd") {
      config.npm.modules ?: JsRuntimeConfig.DEFAULT_NPM_MODULES
    },

    // `vm.js.wasm`: maps to `js.webassembly` and controls the JS bridge to WASM.
    VMRuntimeProperty.ofBoolean("vm.js.wasm", "js.webassembly") {
      config.wasm ?: false
    },

    // `vm.js.v8-compat`: maps to `js.v8-compat` and controls compatibility shims for V8
    VMRuntimeProperty.ofBoolean("vm.js.v8-compat", "js.v8-compat") {
      config.v8 ?: false
    },
  )).plus(
    if (config.wasm == true) {
      wasmOptions
    } else {
      emptyList()
    }
  ).stream()

  /** @inheritDoc */
  override fun prepare(context: VMContext, globals: GuestValue) {
    if (logger.isEnabled(LogLevel.TRACE))
      logger.trace("Preparing JS VM context: $context")

    // initialize the runtime
    context.enter()
    try {
      context.eval(runtimeInit.get())
    } catch (err: Throwable) {
      logger.error("Fatal error: Failed to initialize JavaScript runtime", err)
      throw err
    } finally {
      context.leave()
    }
  }

  /** @inheritDoc */
  override fun resolve(
    context: VMContext,
    script: JsExecutableScript,
    mode: GVMInvocationBindings.DispatchStyle?,
  ): JsInvocationBindings = JsInvocationBindings.resolve(script, script.evaluate(context))

  /** @inheritDoc */
  @Suppress("UNREACHABLE_CODE")
  override fun <Inputs : ExecutionInputs> execute(
    context: VMContext,
    script: JsExecutableScript,
    bindings: JsInvocationBindings,
    inputs: Inputs,
  ): GuestValue {
    // resolve an execution adapter
    return when (inputs) {
      // if we're using a request as the basis for this execution, use the request-based adapters.
      is RequestExecutionInputs<*> -> when (inputs) {
        // request backed by micronaut
        is JsMicronautRequestExecutionInputs -> JsServerAdapter()
        else -> error("Unrecognized request execution type: '${inputs::class.java.name}'")
      }.bind(
        script,
        bindings,
        inputs
      ).execute().let { _ ->
        TODO("not yet implemented")
      }

      // otherwise, it has to be a basic execution.
//      is BasicExecutionInputs -> TODO("Non-server VM inputs are not supported yet")

      else -> TODO("Non-server VM inputs are not supported yet")
    }
  }
}
