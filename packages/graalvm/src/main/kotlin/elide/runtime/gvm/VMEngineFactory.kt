package elide.runtime.gvm

import elide.runtime.gvm.cfg.GuestRuntimeConfiguration

/**
 * TBD.
 */
internal interface VMEngineFactory<Config: GuestRuntimeConfiguration, Engine: VMEngineImpl<Config>> {
  /**
   * TBD.
   */
  fun acquire(): Engine
}
