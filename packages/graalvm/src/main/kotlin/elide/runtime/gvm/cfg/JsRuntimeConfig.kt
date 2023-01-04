package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.util.Locale

/**
 * # JS VM Configuration
 *
 * Defines configuration structure for JavaScript VMs managed by Elide. Guest VMs are capable of executing user-provided
 * code in a sandboxed environment.
 *
 * @param enabled Whether JS VM support is enabled.
 * @param sourceMaps Whether to enable source-maps support, which enhances stack-traces, logs, and other system features
 *   with information about the original source code. Defaults to `true`.
 * @param v8 Run with V8 compatibility mode active. Defaults to `false`; users are not encouraged to activate this.
 * @param wasm Enable WASM support and related bindings. Defaults to `true`; only active where supported.
 * @param esm Settings which apply to ECMAScript Module (ESM) support.
 * @param npm Settings which apply to NPM/`node_modules` support.
 * @param typescript Enable experimental built-in runtime support for TypeScript. Defaults to `false`.
 * @param language ECMA Script language level to apply within the VM; defaults to [JsLanguageLevel.ES2022].
 * @param defaultLocale Default locale to apply to the JS VM. Defaults to the system default.
 * @param timezone Default timezone to apply to the JS VM. Defaults to the system default.
 * @param charset Default character set to apply when exchanging raw data with the JS VM. Defaults to `UTF-8`. `UTF-8`
 *   and `UTF-32` are explicitly supported; other support may vary.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.js")
public class JsRuntimeConfig(
  public var enabled: Boolean = DEFAULT_ENABLED,
  public var sourceMaps: Boolean = DEFAULT_SOURCEMAPS,
  public var v8: Boolean = DEFAULT_V8_COMPAT,
  public var wasm: Boolean = DEFAULT_WASM,
  public var esm: JsEsmConfig = JsEsmConfig.DEFAULTS,
  public var npm: JsNpmConfig = JsNpmConfig.DEFAULTS,
  public var typescript: Boolean = DEFAULT_TYPESCRIPT,
  public var language: JsLanguageLevel = DEFAULT_JS_LANGUAGE_LEVEL,
  public var defaultLocale: Locale = DEFAULT_LOCALE,
  public var timezone: ZoneId = DEFAULT_TIMEZONE,
  public var charset: Charset? = null,
) : Toggleable, GuestRuntimeConfiguration {
  /**
   * ## JS: NPM Configuration
   *
   * Defines configuration structure for NPM/Node module support within the JS VM. This includes enablement of NPM
   * support and the path to look for modules at.
   *
   * @param enabled Whether to enable NPM support (including `require(..)`). Defaults to `true`.
   * @param modules Path to look for modules at. Defaults to `node_modules`.
   */
  @ConfigurationProperties("elide.gvm.js.npm")
  public class JsNpmConfig (
    public var enabled: Boolean = DEFAULT_NPM,
    public var modules: String = DEFAULT_NPM_MODULES,
  ) : Toggleable {
    public companion object {
      /** Default JS NPM settings. */
      @JvmStatic public val DEFAULTS: JsNpmConfig = JsNpmConfig()
    }
  }

  /**
   * ## JS: ESM Configuration
   *
   * Defines configuration structure for modern ECMAScript Module support within the JS VM. This includes enablement of
   * ESM support and the path to look for modules at.
   *
   * @param enabled Whether to enable ESM support (including `import`). Defaults to `true`.
   */
  @ConfigurationProperties("elide.gvm.js.esm")
  public class JsEsmConfig (
    public var enabled: Boolean = DEFAULT_ESM,
  ) : Toggleable {
    public companion object {
      /** Default JS ESM settings. */
      @JvmStatic public val DEFAULTS: JsEsmConfig = JsEsmConfig()
    }
  }

  public companion object {
    /** Default enablement of the JS VM. */
    public const val DEFAULT_ENABLED: Boolean = true

    /** Default enablement of SSR source maps. */
    public const val DEFAULT_SOURCEMAPS: Boolean = true

    /** Default enablement of V8 compatibility shims. */
    public const val DEFAULT_V8_COMPAT: Boolean = false

    /** Default enablement of WASM support, where available. */
    public const val DEFAULT_WASM: Boolean = false  // temporary default

    /** Default enablement of ESM features. */
    public const val DEFAULT_ESM: Boolean = true

    /** Default enablement of Node module resolution features. */
    public const val DEFAULT_NPM: Boolean = true

    /** Default location for NPM modules. */
    public const val DEFAULT_NPM_MODULES: String = "node_modules"

    /** Default enablement of TypeScript execution support. */
    public const val DEFAULT_TYPESCRIPT: Boolean = false  // temporary default

    /** Default JS language level. */
    public val DEFAULT_JS_LANGUAGE_LEVEL: JsLanguageLevel = JsLanguageLevel.ES2022

    /** Default JS VM locale. */
    public val DEFAULT_LOCALE: Locale = Locale.getDefault()

    /** Default JS VM time zone. */
    public val DEFAULT_TIMEZONE: ZoneId = ZoneId.systemDefault()

    /** Default character set to use with raw data exchanged with the JS VM. */
    public val DEFAULT_CHARSET: Charset = StandardCharsets.UTF_8
  }

  /** @inheritDoc */
  override fun isEnabled(): Boolean = enabled
}
