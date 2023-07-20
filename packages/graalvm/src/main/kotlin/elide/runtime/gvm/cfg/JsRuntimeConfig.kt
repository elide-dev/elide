package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.util.*

/**
 * # JS VM Configuration
 *
 * Defines configuration structure for JavaScript VMs managed by Elide. Guest VMs are capable of executing user-provided
 * code in a sandbox environment.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.js")
public interface JsRuntimeConfig : Toggleable, GuestRuntimeConfiguration {
  /**
   * ## JS: NPM Configuration
   *
   * Defines configuration structure for NPM/Node module support within the JS VM. This includes enablement of NPM
   * support and the path to look for modules at.
   */
  @ConfigurationProperties("elide.gvm.js.npm")
  public interface JsNpmConfig : Toggleable {
    public companion object {
      /** Default JS NPM settings. */
      @JvmStatic public val DEFAULTS: JsNpmConfig = object : JsNpmConfig {}
    }

    /**
     * @return Path to look for modules at. Defaults to `node_modules`.
     */
    public val modules: String? get() = DEFAULT_NPM_MODULES
  }

  /**
   * ## JS: ESM Configuration
   *
   * Defines configuration structure for modern ECMAScript Module support within the JS VM. This includes enablement of
   * ESM support and the path to look for modules at.
   */
  @ConfigurationProperties("elide.gvm.js.esm")
  public interface JsEsmConfig : Toggleable {
    public companion object {
      /** Default JS ESM settings. */
      @JvmStatic public val DEFAULTS: JsEsmConfig = object : JsEsmConfig {}
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
    public const val DEFAULT_WASM: Boolean = true

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

  override fun isEnabled(): Boolean = DEFAULT_ENABLED

  /**
   * @return Whether to enable source-maps support, which enhances stack-traces, logs, and other system features with
   */
  public val sourceMaps: Boolean? get() = DEFAULT_SOURCEMAPS

  /**
   * @return Whether to enable V8 compatibility mode. This is not recommended for most users.
   */
  public val v8: Boolean? get() = DEFAULT_V8_COMPAT

  /**
   * @return Enable WASM support and related bindings. Defaults to `true`; only active where supported.
   */
  public val wasm: Boolean? get() = DEFAULT_WASM

  /**
   * @return Settings which apply to ECMAScript Module (ESM) support.
   */
  public val esm: JsEsmConfig get() = JsEsmConfig.DEFAULTS

  /**
   * @return Settings which apply to NPM/`node_modules` support.
   */
  public val npm: JsNpmConfig get() = JsNpmConfig.DEFAULTS

  /**
   * @return Enable experimental built-in runtime support for TypeScript. Defaults to `false`.
   */
  public val typescript: Boolean? get() = DEFAULT_TYPESCRIPT

  /**
   * @return ECMA Script language level to apply within the VM; defaults to [JsLanguageLevel.ES2022].
   */
  public val language: JsLanguageLevel? get() = DEFAULT_JS_LANGUAGE_LEVEL

  /**
   * @return Default locale to apply to the JS VM. Defaults to the system default.
   */
  public val defaultLocale: Locale? get() = DEFAULT_LOCALE

  /**
   * @return Default timezone to apply to the JS VM. Defaults to the system default.
   */
  public val timezone: ZoneId? get() = DEFAULT_TIMEZONE

  /**
   * @return Default character set to apply when exchanging raw data with the JS VM. Defaults to `UTF-8`. `UTF-8` and
   *   `UTF-32` are explicitly supported; other support may vary.
   */
  public val charset: Charset? get() = null
}
