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
 * @param threads Maximum number of threads to use for guest VM execution. Defaults to the count of available processors
 *   multiplied by a constant factor (at the time of this writing, the factor is `2`).
 * @param idle Number of idle VMs to keep running when operating at minimal load; defaults to half the value of
 *   [threads] (in other words, the count of available processors).
 * @param languages Permitted guest VM languages. Defaults to the supported set for Elide, which, at the time of this
 *   writing, only includes `js`.
 * @param primary Primary guest VM to boot and use as a server agent. Defaults to `js`.
 * @param charset Default character set to apply when exchanging raw data with the JS VM. Defaults to `UTF-8`.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@ConfigurationProperties("elide.gvm")
internal class GuestVMConfiguration(
  var enabled: Boolean = DEFAULT_ENABLED,
  var threads: Int = DEFAULT_THREADS,
  var idle: Int = DEFAULT_IDLE,
  var languages: Set<String> = DEFAULT_LANGUAGES,
  var primary: EmbeddedScriptLanguage = EmbeddedScriptLanguage.JS,
  var charset: Charset? = null,
  var inspector: GuestVMInspectConfig? = null,
  var enterprise: GuestVMEEConfig? = null,
) : Toggleable {
  internal companion object {
    /** Default enablement status. */
    const val DEFAULT_ENABLED: Boolean = true

    /** Default number of threads. */
    val DEFAULT_THREADS: Int = Runtime.getRuntime().availableProcessors() * 2

    /** Default number of idle VMs. */
    val DEFAULT_IDLE: Int = DEFAULT_THREADS / 2

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
