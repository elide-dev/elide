package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable

/**
 * # JS VM Configuration
 *
 * Defines configuration structure for JavaScript VMs managed by Elide. Guest VMs are capable of executing user-provided
 * code in a sandboxed environment.
 *
 * @param enabled Whether JS VM support is enabled.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.js")
public class JsRuntimeConfig(
  public var enabled: Boolean = true,
) : Toggleable, GuestRuntimeConfiguration {
  /** @inheritDoc */
  override fun isEnabled(): Boolean = enabled
}
