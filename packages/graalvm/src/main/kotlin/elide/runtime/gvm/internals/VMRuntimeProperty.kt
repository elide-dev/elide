package elide.runtime.gvm.internals

/**
 * Represents a user-configurable JS Runtime property; binds a JS VM property to an Elide configuration property.
 *
 * @param name Name of the property within Elide's configuration system.
 * @param symbol Symbol to use for the VM property when passing it to a new context.
 * @param defaultValue If no configured value is available, this value should be passed instead. If null, pass no
 *   value at all.
 */
internal data class VMRuntimeProperty internal constructor (
  private val name: String,
  override val symbol: String,
  private val defaultValue: String? = null,
  private val getter: (() -> String?)? = null,
): VMProperty {
  internal companion object {
    private fun booleanToSymbol(boolean: Boolean?): String? = when (boolean) {
      null -> null
      true -> "true"
      false -> "false"
    }

    /** @return Fully-configurable runtime property. */
    @JvmStatic fun ofConfigurable(
      name: String,
      symbol: String,
      defaultValue: String? = null,
      getter: (() -> String?)? = null,
    ): VMRuntimeProperty = VMRuntimeProperty(name, symbol, defaultValue, getter)

    /** @return Fully-configurable runtime property, backed by a Boolean return value. */
    @JvmStatic fun ofBoolean(
      name: String,
      symbol: String,
      defaultValue: Boolean? = null,
      getter: (() -> Boolean?)? = null,
    ): VMRuntimeProperty = VMRuntimeProperty(
      name,
      symbol,
      booleanToSymbol(defaultValue)
    ) {
      booleanToSymbol(getter?.invoke())
    }
  }

  override fun value(): String? = getter?.invoke() ?: defaultValue
}
