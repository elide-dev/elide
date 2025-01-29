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
package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import tools.elide.assets.EmbeddedScriptLanguage
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import jakarta.annotation.Nullable

/**
 * # Guest VM Configuration
 *
 * Defines configuration structure for guest VMs managed by Elide. Guest VMs are capable of executing user-provided code
 * in a sandbox environment, and potentially in another language.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@ConfigurationProperties("elide.gvm")
public interface GuestVMConfiguration : Toggleable {
  public companion object {
    /** Default enablement status. */
    public const val DEFAULT_ENABLED: Boolean = true

    /** JavaScript language tag. */
    public const val LANGUAGE_JS: String = "js"

    /** WASM language tag. */
    public const val LANGUAGE_WASM: String = "wasm"

    /** Python language tag. */
    public const val LANGUAGE_PYTHON: String = "python"

    /** Ruby language tag. */
    public const val LANGUAGE_RUBY: String = "ruby"

    /** Java language tag. */
    public const val LANGUAGE_JAVA: String = "java"

    /** Default guest languages. */
    public val DEFAULT_LANGUAGES: List<String> = listOf(
      LANGUAGE_JS,
      LANGUAGE_WASM,
      LANGUAGE_PYTHON,
      LANGUAGE_RUBY,
      LANGUAGE_JAVA,
    )

    /** Default character set to use with raw data exchanged with the JS VM. */
    public val DEFAULT_CHARSET: Charset = StandardCharsets.UTF_8

    /** Default configuration instance. */
    public val DEFAULTS: GuestVMConfiguration = object : GuestVMConfiguration {}
  }

  /**
   * @return Permitted guest VM languages. Defaults to the supported set for Elide, which, at the time of this writing,
   *   only includes `js`.
   */
  @Nullable public val languages: List<String>? get() = DEFAULT_LANGUAGES

  /**
   * @return Primary guest VM to boot and use as a server agent. Defaults to `js`.
   */
  public val primary: EmbeddedScriptLanguage get() = EmbeddedScriptLanguage.JS

  /**
   * @return Default character set to apply when exchanging raw data with the JS VM. Defaults to `UTF-8`.
   */
  public val charset: Charset? get() = DEFAULT_CHARSET

  /**
   * @return Guest VM locale. Defaults to `en_US`.
   */
  public val locale: Locale? get() = Locale.US

  /**
   * @return Virtual file system (VFS) configuration for the guest VM.
   */
  public val vfs: GuestIOConfiguration? get() = null

  /**
   * @return Debugger configuration (Chrome Inspector).
   */
  public val inspector: GuestVMInspectConfig? get() = null

  /**
   * @return GraalVM Enterprise Edition-specific configuration.
   */
  public val enterprise: GuestVMEEConfig? get() = null
}
