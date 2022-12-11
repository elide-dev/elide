package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable

/**
 * # Guest VM Configuration
 *
 * Defines configuration structure for guest VMs managed by Elide. Guest VMs are capable of executing user-provided code
 * in a sandboxed environment, and potentially in another language.
 *
 * @param enabled Whether guest VM support is enabled.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm")
public class GuestVMConfiguration(
  public var enabled: Boolean = true,
) : Toggleable {
  /** @inheritDoc */
  override fun isEnabled(): Boolean = enabled
}
