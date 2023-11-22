/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.plugins.jvm.interop

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/** Returns this value as a [String], or `null` if the value is `null` or not a [String]. */
@DelicateElideApi public fun PolyglotValue.asStringOrNull(): String? {
  return takeUnless { isNull || !isString }?.asString()
}

/** Returns this value as a [Boolean], or `null` if the value is `null` or not a [Boolean]. */
@DelicateElideApi public fun PolyglotValue.asBooleanOrNull(): Boolean? {
  return takeUnless { isNull || !isBoolean }?.asBoolean()
}

/** Returns this value as a [Byte], or `null` if the value is `null` or not a number. */
@DelicateElideApi public fun PolyglotValue.asByteOrNull(): Byte? {
  return takeUnless { isNull || !isNumber }?.asByte()
}

/** Returns this value as a [Char], or `null` if the value is `null` or not a number. */
@DelicateElideApi public fun PolyglotValue.asCharOrNull(): Char? {
  return takeUnless { isNull || !isNumber }?.asInt()?.toChar()
}

/** Returns this value as a [Short], or `null` if the value is `null` or not a number. */
@DelicateElideApi public fun PolyglotValue.asShortOrNull(): Short? {
  return takeUnless { isNull || !isNumber }?.asShort()
}

/** Returns this value as a [Int], or `null` if the value is `null` or not a number. */
@DelicateElideApi public fun PolyglotValue.asIntOrNull(): Int? {
  return takeUnless { isNull || !isNumber }?.asInt()
}

/** Returns this value as a [Long], or `null` if the value is `null` or not a number. */
@DelicateElideApi public fun PolyglotValue.asLongOrNull(): Long? {
  return takeUnless { isNull || !isNumber }?.asLong()
}

/** Returns this value as a [Float], or `null` if the value is `null` or not a number. */
@DelicateElideApi public fun PolyglotValue.asFloatOrNull(): Float? {
  return takeUnless { isNull || !isNumber }?.asFloat()
}

/** Returns this value as a [Double], or `null` if the value is `null` or not a number. */
@DelicateElideApi public fun PolyglotValue.asDoubleOrNull(): Double? {
  return takeUnless { isNull || !isNumber }?.asDouble()
}

/** Maps this value to a [BooleanArray] by copying its contents, failing if it is not a guest array. */
@DelicateElideApi public fun PolyglotValue.asBooleanArray(): BooleanArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return BooleanArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asBoolean() }
}

/** Maps this value to a [ByteArray] by copying its contents, failing if it is not a guest array. */
@DelicateElideApi public fun PolyglotValue.asByteArray(): ByteArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return ByteArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asByte() }
}

/** Maps this value to a [CharArray] by copying its contents, failing if it is not a guest array. */
@DelicateElideApi public fun PolyglotValue.asCharArray(): CharArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return CharArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asInt().toChar() }
}

/** Maps this value to a [ShortArray] by copying its contents, failing if it is not a guest array. */
@DelicateElideApi public fun PolyglotValue.asShortArray(): ShortArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return ShortArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asShort() }
}

/** Maps this value to a [IntArray] by copying its contents, failing if it is not a guest array. */
@DelicateElideApi public fun PolyglotValue.asIntArray(): IntArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return IntArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asInt() }
}

/** Maps this value to a [LongArray] by copying its contents, failing if it is not a guest array. */
@DelicateElideApi public fun PolyglotValue.asLongArray(): LongArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return LongArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asLong() }
}

/** Maps this value to a [FloatArray] by copying its contents, failing if it is not a guest array. */
@DelicateElideApi public fun PolyglotValue.asFloatArray(): FloatArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return FloatArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asFloat() }
}

/** Maps this value to a [DoubleArray] by copying its contents, failing if it is not a guest array. */
@DelicateElideApi public fun PolyglotValue.asDoubleArray(): DoubleArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return DoubleArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asDouble() }
}

/** Maps this value to a host array using the [map] function on each element, failing if it is not a guest array. */
@DelicateElideApi public inline fun <reified T> PolyglotValue.asHostArray(map: (PolyglotValue) -> T): Array<T> {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return Array(arraySize.toInt()) { map(getArrayElement(it.toLong())) }
}
