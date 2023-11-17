package elide.runtime.plugins.jvm.interop

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

@DelicateElideApi public fun PolyglotValue.asStringOrNull(): String? {
  return takeUnless { isNull }?.asString()
}

@DelicateElideApi public fun PolyglotValue.asBooleanOrNull(): Boolean? {
  return takeUnless { isNull }?.asBoolean()
}

@DelicateElideApi public fun PolyglotValue.asByteOrNull(): Byte? {
  return takeUnless { isNull }?.asByte()
}

@DelicateElideApi public fun PolyglotValue.asCharOrNull(): Char? {
  return takeUnless { isNull }?.asInt()?.toChar()
}

@DelicateElideApi public fun PolyglotValue.asShortOrNull(): Short? {
  return takeUnless { isNull }?.asShort()
}

@DelicateElideApi public fun PolyglotValue.asIntOrNull(): Int? {
  return takeUnless { isNull }?.asInt()
}

@DelicateElideApi public fun PolyglotValue.asLongOrNull(): Long? {
  return takeUnless { isNull }?.asLong()
}

@DelicateElideApi public fun PolyglotValue.asFloatOrNull(): Float? {
  return takeUnless { isNull }?.asFloat()
}

@DelicateElideApi public fun PolyglotValue.asDoubleOrNull(): Double? {
  return takeUnless { isNull }?.asDouble()
}

@DelicateElideApi public fun PolyglotValue.asBooleanArray(): BooleanArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return BooleanArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asBoolean() }
}

@DelicateElideApi public fun PolyglotValue.asByteArray(): ByteArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return ByteArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asByte() }
}

@DelicateElideApi public fun PolyglotValue.asCharArray(): CharArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return CharArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asInt().toChar() }
}

@DelicateElideApi public fun PolyglotValue.asShortArray(): ShortArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return ShortArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asShort() }
}

@DelicateElideApi public fun PolyglotValue.asIntArray(): IntArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return IntArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asInt() }
}

@DelicateElideApi public fun PolyglotValue.asLongArray(): LongArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return LongArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asLong() }
}

@DelicateElideApi public fun PolyglotValue.asFloatArray(): FloatArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return FloatArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asFloat() }
}

@DelicateElideApi public fun PolyglotValue.asDoubleArray(): DoubleArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return DoubleArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asDouble() }
}

@DelicateElideApi public inline fun <reified T> PolyglotValue.asHostArray(map: (PolyglotValue) -> T): Array<T> {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return Array(arraySize.toInt()) { map(getArrayElement(it.toLong())) }
}