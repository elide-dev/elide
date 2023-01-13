package elide.runtime.gvm

import elide.runtime.gvm.internals.GraalVMGuest
import java.util.EnumSet

/**
 * TBD.
 */
public interface GuestLanguage {
  /**
   * TBD.
   */
  public val symbol: String

  /**
   * TBD.
   */
  public val label: String

  /**
   * TBD.
   */
  public val supportsSSR: Boolean get() = true

  /**
   * TBD.
   */
  public val supportsStreamingSSR: Boolean get() = invocationModes.contains(InvocationMode.STREAMING)

  /**
   * TBD.
   */
  public val invocationModes: EnumSet<InvocationMode> get() = EnumSet.allOf(InvocationMode::class.java)

  /** Well-known guest languages. */
  public companion object {
    /** JavaScript as a guest language. */
    public val JAVASCRIPT: GuestLanguage = GraalVMGuest.JAVASCRIPT
  }
}
