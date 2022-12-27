package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import tools.elide.assets.EmbeddedScriptLanguage
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * # Guest VM Configuration
 *
 * Defines configuration structure for guest VMs managed by Elide. Guest VMs are capable of executing user-provided code
 * in a sandboxed environment, and potentially in another language.
 *
 * @param enabled Whether guest VM support is enabled.
 * @param languages Permitted guest VM languages. Defaults to the supported set for Elide, which, at the time of this
 *   writing, only includes `js`.
 * @param primary Primary guest VM to boot and use as a server agent. Defaults to `js`.
 * @param charset Default character set to apply when exchanging raw data with the JS VM. Defaults to `UTF-8`.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@ConfigurationProperties("elide.gvm")
internal class GuestVMConfiguration(
  var enabled: Boolean = DEFAULT_ENABLED,
  var languages: Set<String> = DEFAULT_LANGUAGES,
  var primary: EmbeddedScriptLanguage = EmbeddedScriptLanguage.JS,
  var charset: Charset? = null,
  var inspector: GuestVMInspectConfig? = null,
  var enterprise: GuestVMEEConfig? = null,
) : Toggleable {
  internal companion object {
    /** Default enablement status. */
    const val DEFAULT_ENABLED: Boolean = true

    /** Default guest languages. */
    val DEFAULT_LANGUAGES: Set<String> = setOf("js")

    /** Default character set to use with raw data exchanged with the JS VM. */
    val DEFAULT_CHARSET: Charset = StandardCharsets.UTF_8

    /** Default configuration instance. */
    val DEFAULTS: GuestVMConfiguration = GuestVMConfiguration()
  }

  /** @inheritDoc */
  override fun isEnabled(): Boolean = enabled
}
