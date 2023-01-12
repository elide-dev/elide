package elide.runtime.gvm.internals

/** Abstract base interface for a guest VM configuration property. */
public sealed interface VMProperty : Comparable<VMProperty> {
  /** Symbol to use for this property with the guest VM. */
  public val symbol: String

  /** @return Resolved value for this property. */
  public fun value(): String?

  /** @return Indication of whether a value is present for this property. */
  public fun active(): Boolean = when (value()) {
    "true", "yes", "on", "active", "enabled" -> true
    "false", "no", "off", "inactive", "disabled", "", " " -> false
    null -> false
    else -> true
  }

  override fun compareTo(other: VMProperty): Int {
    return this.symbol.compareTo(other.symbol)
  }
}
