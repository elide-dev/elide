package elide.runtime.gvm.internals

/**
 * Represents a hard-coded JS Runtime property.
 *
 * @param symbol Symbol to use for the VM property when passing it to a new context.
 * @param staticValue Value for this property.
 */
internal data class VMStaticProperty internal constructor (
  override val symbol: String,
  val staticValue: String,
): VMProperty {
  internal companion object {
    private const val ENABLED_TRUE = "true"
    private const val DISABLED_FALSE = "false"

    /** @return Active setting. */
    @JvmStatic fun of(name: String, value: String): VMStaticProperty = VMStaticProperty(name, value)

    /** @return Active setting. */
    @JvmStatic fun active(name: String): VMStaticProperty = VMStaticProperty(name, ENABLED_TRUE)

    /** @return Active setting. */
    @JvmStatic fun inactive(name: String): VMStaticProperty = VMStaticProperty(name, DISABLED_FALSE)
  }

  override fun value(): String = staticValue
}
