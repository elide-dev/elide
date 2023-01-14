package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * # Guest VM Configuration: GraalVM EE
 *
 * This configuration structure defines options which are only available in GraalVM Enterprise Edition (EE). Options
 * specified within the scope of this object are only applied when running on EE; when running on Community Edition (CE)
 * the options are inert.
 *
 * @see GuestVMEESandboxConfig for sandbox configuration properties.
 * @param sandbox Configuration for VM sandbox and resource limits.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.enterprise")
internal class GuestVMEEConfig(
  var sandbox: GuestVMEESandboxConfig? = null,
)
