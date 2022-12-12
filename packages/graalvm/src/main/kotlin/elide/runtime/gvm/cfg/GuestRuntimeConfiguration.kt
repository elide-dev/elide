package elide.runtime.gvm.cfg

import io.micronaut.core.util.Toggleable

/**
 * # Configuration: Guest VMs
 *
 * Defines the surface area of expected configuration for each guest runtime / VM.
 *
 * Note that each guest runtime VM configuration also implements [Toggleable]. If a VM runtime is shut off, it does not
 * load at server startup and is not available to execute user code.
 *
 * @see GuestVMConfiguration for concrete configuration properties.
 */
public interface GuestRuntimeConfiguration : Toggleable {
  // Nothing at this time.
}
