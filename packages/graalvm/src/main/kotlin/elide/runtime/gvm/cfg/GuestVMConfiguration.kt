package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import jakarta.annotation.Nullable
import tools.elide.assets.EmbeddedScriptLanguage
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * # Guest VM Configuration
 *
 * Defines configuration structure for guest VMs managed by Elide. Guest VMs are capable of executing user-provided code
 * in a sandbox environment, and potentially in another language.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@ConfigurationProperties("elide.gvm")
internal interface GuestVMConfiguration : Toggleable {
  companion object {
    /** Default enablement status. */
    const val DEFAULT_ENABLED: Boolean = true

    /** Default enablement status. */
    const val LANGUAGE_JS: String = "js"

    /** Default guest languages. */
    val DEFAULT_LANGUAGES: List<String> = listOf(LANGUAGE_JS)

    /** Default character set to use with raw data exchanged with the JS VM. */
    val DEFAULT_CHARSET: Charset = StandardCharsets.UTF_8

    /** Default configuration instance. */
    val DEFAULTS: GuestVMConfiguration = object : GuestVMConfiguration {}
  }

  /**
   * @return Permitted guest VM languages. Defaults to the supported set for Elide, which, at the time of this writing,
   *   only includes `js`.
   */
  @Nullable val languages: List<String>? get() = DEFAULT_LANGUAGES

  /**
   * @return Primary guest VM to boot and use as a server agent. Defaults to `js`.
   */
  val primary: EmbeddedScriptLanguage get() = EmbeddedScriptLanguage.JS

  /**
   * @return Default character set to apply when exchanging raw data with the JS VM. Defaults to `UTF-8`.
   */
  val charset: Charset? get() = DEFAULT_CHARSET

  /**
   * @return Virtual file system (VFS) configuration for the guest VM.
   */
  val vfs: GuestIOConfiguration? get() = null

  /**
   * @return Debugger configuration (Chrome Inspector).
   */
  val inspector: GuestVMInspectConfig? get() = null

  /**
   * @return GraalVM Enterprise Edition-specific configuration.
   */
  val enterprise: GuestVMEEConfig? get() = null
}
