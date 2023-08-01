package elide.runtime.gvm.internals

/**
 * Represents a property for the JS Runtime which applies based on some [condition], or falls back to a [defaultValue]
 * at a given [name] in Elide's configuration system.
 *
 * @param name Name of the property within Elide's configuration system.
 * @param symbol Symbol to use for the VM property when passing it to a new context.
 * @param condition Function to execute to determine whether this property should be applied.
 * @param value Runtime value bound to this property, if applicable; otherwise, just pass a [defaultValue].
 * @param defaultValue If the value is disabled, this value should be passed instead. If null, pass no value at all.
 */
internal data class VMConditionalProperty(
  private val name: String,
  override val symbol: String,
  private val condition: () -> Boolean,
  private val value: VMRuntimeProperty? = null,
  private val defaultValue: String? = null,
) : VMProperty {
  override fun value(): String = if (condition.invoke()) {
    value?.value() ?: "true"
  } else {
    defaultValue ?: "false"
  }
}
