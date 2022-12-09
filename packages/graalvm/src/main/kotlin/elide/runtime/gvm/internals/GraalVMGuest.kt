package elide.runtime.gvm.internals

import elide.runtime.gvm.GuestLanguage

/** Enumerates known/supported GraalVM guest languages. */
internal enum class GraalVMGuest constructor (override val symbol: String, override val label: String) : GuestLanguage {
  /**
   * ECMA2022-compliant JavaScript via Graal JS+JVM.
   */
  JAVASCRIPT(symbol = "js", label = "JavaScript")
}
