
/** Binds a [ConstantType] to a [Constant] value. */
data class BuildConstant(
  val type: ConstantType,
  val value: Constant,
) {
  /** @return Wrapped/encoded value based on [type]. */
  fun wrap(): String = value.wrap(type)
}
