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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.cfg

import elide.runtime.plugins.js.JavaScriptVersion
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import java.nio.charset.Charset
import java.time.ZoneId
import java.util.*
import elide.runtime.core.DelicateElideApi

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
    public const val DEFAULT_V8_COMPAT: Boolean = true

    /** Default enablement of WASM support, where available. */
    public const val DEFAULT_WASM: Boolean = true

    /** Default enablement of ESM features. */
    public const val DEFAULT_ESM: Boolean = true

    /** Default enablement of Node module resolution features. */
    public const val DEFAULT_NPM: Boolean = true

    /** Default location for NPM modules. */
    public const val DEFAULT_NPM_MODULES: String = "."

    /** Default enablement of TypeScript execution support. */
    public const val DEFAULT_TYPESCRIPT: Boolean = false  // temporary default

    /** Default JS language level. */
    public val DEFAULT_JS_LANGUAGE_LEVEL: JavaScriptVersion = JavaScriptVersion.ES2022

    /** Default JS VM locale. */
    public val DEFAULT_LOCALE: Locale get() = LanguageDefaults.DEFAULT_LOCALE

    /** Default JS VM time zone. */
    public val DEFAULT_TIMEZONE: ZoneId get() = LanguageDefaults.DEFAULT_TIMEZONE

    /** Default character set to use with raw data exchanged with the JS VM. */
    public val DEFAULT_CHARSET: Charset get() = LanguageDefaults.DEFAULT_CHARSET
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
   * @return ECMA Script language level to apply within the VM; defaults to [JavaScriptVersion.ES2022].
   */
  public val language: JavaScriptVersion? get() = DEFAULT_JS_LANGUAGE_LEVEL

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

  /**
   * @return Locale to use for embedded JS VMs.
   */
  public val locale: Locale? get() = null
}
