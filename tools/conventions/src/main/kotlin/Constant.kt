import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

/** Build-time constant [value] wrapper. */
@JvmInline value class Constant(
  val value: String
) {
  companion object {
    /** Return a [BuildConstant] for the provided [value]. */
    @JvmStatic fun <T: Any> of(value: T): BuildConstant = when (value) {
      is String -> string(value)
      is Long -> long(value)
      is Int -> int(value)
      else -> ofType(value::class, value)
    }

    /** Return a [BuildConstant] [value] with the provided custom [type]. */
    @JvmStatic fun <T: Any> ofType(type: KClass<T>, value: Any): BuildConstant = BuildConstant(
      type = ConstantType.Impl(type),
      value = Constant(value.toString()),
    )

    /** Return a [BuildConstant] for the provided [String] [value]. */
    @JvmStatic fun string(value: String): BuildConstant = BuildConstant(
      type = ConstantType.STRING,
      value = Constant(value),
    )

    /** Return a [BuildConstant] for the provided [Int] [value]. */
    @JvmStatic fun int(value: Int): BuildConstant = BuildConstant(
      type = ConstantType.INT,
      value = Constant(value.toString()),
    )

    /** Return a [BuildConstant] for the provided [Long] [value]. */
    @JvmStatic fun long(value: Long): BuildConstant = BuildConstant(
      type = ConstantType.LONG,
      value = Constant(value.toString()),
    )

    /** Return a [BuildConstant] for the provided [Boolean] [value]. */
    @JvmStatic fun bool(value: Boolean): BuildConstant = BuildConstant(
      type = ConstantType.BOOL,
      value = Constant(value.toString()),
    )
  }

  /** @return Encoded/wrapped value according to the provided [type]. */
  fun wrap(type: ConstantType): String = when (type) {
    // is it a custom impl class?
    is ConstantType.Impl -> TODO("custom build constant types are not supported yet")

    // otherwise, is it a primitive?
    is ConstantType.Primitive -> when (type) {
      // validate int type and encode
      is ConstantType.Primitive.Int -> (value.toIntOrNull() ?: error(
        "Failed to parse build-constant integer as `kotlin.Int`: '$value'"
      )).toString()

      // validate long type and encode
      is ConstantType.Primitive.Long -> (value.toLongOrNull() ?: error(
        "Failed to parse build-constant integer as `kotlin.Long`: '$value'"
      )).toString()

      // validate bool type and encode
      is ConstantType.Primitive.Boolean -> value.toBoolean().toString()

      // wrap string in quotes
      is ConstantType.Primitive.String -> "\"$value\""
    }
  }
}
