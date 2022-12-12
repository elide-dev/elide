package elide.runtime.gvm.internals

/** Abstract base interface for a guest VM configuration property. */
internal sealed interface VMProperty {
  /** Symbol to use for this property with the guest VM. */
  val symbol: String

  /** @return Resolved value for this property. */
  fun value(): String?

  /** @return Indication of whether a value is present for this property. */
  fun active(): Boolean = when (value()) {
    "true", "yes", "on", "active", "enabled" -> true
    "false", "no", "off", "inactive", "disabled", "", " " -> false
    else -> true
  }
}
