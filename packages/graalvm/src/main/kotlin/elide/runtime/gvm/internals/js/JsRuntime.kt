package elide.runtime.gvm.internals.js

import elide.annotations.Factory
import elide.annotations.Inject
import elide.runtime.gvm.internals.GraalVMGuest.JAVASCRIPT
import elide.annotations.Singleton
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.VMEngineFactory
import elide.runtime.gvm.cfg.JsRuntimeConfig
import elide.runtime.gvm.internals.AbstractVMEngine
import elide.runtime.gvm.internals.InvocationBindings
import elide.runtime.gvm.internals.VMProperty
import elide.runtime.gvm.internals.VMRuntimeProperty
import elide.runtime.gvm.internals.VMStaticProperty as StaticProperty
import elide.runtime.gvm.internals.intrinsics.GuestRuntime
//import elide.server.Application
import io.micronaut.context.annotation.Requires
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.ImageSingletons
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel
import java.util.stream.Stream
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Value as GuestValue

/**
 * TBD.
 */
@GuestRuntime internal class JsRuntime private constructor (config: JsRuntimeConfig) :
  AbstractVMEngine<JsRuntimeConfig, JsExecutableScript>(JAVASCRIPT, config) {
  /**
   * TBD.
   */
  @Requires(property = "elide.gvm.enabled", value = "true", defaultValue = "true")
  @Requires(property = "elide.gvm.js.enabled", value = "true", defaultValue = "true")
  @Factory @Singleton internal class JsRuntimeFactory @Inject constructor (config: JsRuntimeConfig) :
    VMEngineFactory<JsRuntimeConfig, JsRuntime> {
    // Singleton JS runtime instance.
    private val singleton: JsRuntime = JsRuntime(config)

    /** @inheritDoc */
    @Singleton override fun acquire(): JsRuntime = singleton
  }

  private companion object {
    const val DEFAULT_STREAM_ENCODING: String = "UTF-8"
    const val DEFAULT_JS_LANGUAGE_LEVEL: String = "2022"
    private const val FUNCTION_CONSTRUCTOR_CACHE_SIZE: String = "256"
    private const val UNHANDLED_REJECTIONS: String = "handler"
    private const val DEBUG_GLOBAL: String = "ElideDebug"

    // Hard-coded JS VM options.
    internal val baseOptions : List<VMProperty> = listOf(
      StaticProperty.active("js.async-stack-traces"),
      StaticProperty.active("js.atomics"),
      StaticProperty.active("js.bind-member-functions"),
      StaticProperty.active("js.commonjs-require"),
      StaticProperty.active("js.class-properties"),
      StaticProperty.active("js.direct-byte-buffer"),
      StaticProperty.active("js.disable-eval"),
      StaticProperty.active("js.esm-eval-returns-exports"),
      StaticProperty.active("js.foreign-hash-properties"),
      StaticProperty.active("js.foreign-object-prototype"),
      StaticProperty.active("js.intl-402"),
      StaticProperty.active("js.json-modules"),
      StaticProperty.active("js.performance"),
      StaticProperty.active("js.shared-array-buffer"),
      StaticProperty.active("js.strict"),
      StaticProperty.active("js.temporal"),
      StaticProperty.active("js.top-level-await"),
      StaticProperty.inactive("js.annex-b"),
      StaticProperty.inactive("js.console"),
      StaticProperty.inactive("js.graal-builtin"),
      StaticProperty.inactive("js.interop-complete-promises"),
      StaticProperty.inactive("js.java-package-globals"),
      StaticProperty.inactive("js.load"),
      StaticProperty.inactive("js.nashorn-compat"),
      StaticProperty.inactive("js.operator-overloading"),
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
  }

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
    JsLanguageLevel.UNRECOGNIZED,
    JsLanguageLevel.JS_LANGUAGE_LEVEL_DEFAULT -> DEFAULT_JS_LANGUAGE_LEVEL
  }

  // JS runtime logger.
  private val logger: Logger = Logging.of(JsRuntime::class)

  /** @inheritDoc */
//  override fun initialize() {
//    Application.Initialization.initializeWithServer {
//      if (ImageInfo.inImageBuildtimeCode()) {
//        ImageSingletons.add(JsRuntime::class.java, this)
//      }
//    }
//  }

  /** @inheritDoc */
  override fun configure(engine: Engine, context: Context.Builder): Stream<VMProperty> = baseOptions.plus(listOf(
    // `vm.charset`: maps to `js.charset` and controls encoding of raw data exchanged with JS VM
    VMRuntimeProperty.ofConfigurable("vm.charset", "js.charset", DEFAULT_STREAM_ENCODING) {
      (config.charset ?: guestConfig.charset)?.name() ?: DEFAULT_STREAM_ENCODING
    },

    // `vm.ecma-version`: maps to `js.ecmascript-version` and controls the JS language level
    VMRuntimeProperty.ofConfigurable("vm.js.ecma", "js.ecmascript-version") {
      config.language.symbol
    },

    // `vm.wasm`: maps to `js.webassembly` and controls the JS bridge to WASM32.
    VMRuntimeProperty.ofBoolean("vm.js.wasm", "js.webassembly") {
      config.wasm
    },

    // `vm.v8-compat`: maps to `js.v8-compat` and controls compatibility shims for V8
    VMRuntimeProperty.ofBoolean("vm.v8-compat", "js.v8-compat") {
      config.v8
    },
  )).stream()

  /** @inheritDoc */
  override fun prepare(context: VMContext, bindings: GuestValue) {
    if (logger.isEnabled(LogLevel.TRACE))
      logger.trace("Preparing JS VM context: $context")
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun execute(context: VMContext, script: JsExecutableScript, bindings: InvocationBindings): GuestValue {
    TODO("Not yet implemented")
  }
}
