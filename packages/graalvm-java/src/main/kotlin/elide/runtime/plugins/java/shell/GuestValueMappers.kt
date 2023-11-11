package elide.runtime.plugins.java.shell

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

@DelicateElideApi internal fun PolyglotValue.asStringOrNull(): String? {
  return takeUnless { isNull }?.asString()
}

@DelicateElideApi internal inline fun <reified T> PolyglotValue.asHostArray(
  mapElement: (PolyglotValue) -> T
): Array<T> {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return Array(arraySize.toInt()) { i -> mapElement(getArrayElement(i.toLong())) }
}

@DelicateElideApi internal fun PolyglotValue.asBooleanArray(): BooleanArray {
  require(hasArrayElements()) { "Expected guest value to have array elements" }
  return BooleanArray(arraySize.toInt()) { i -> getArrayElement(i.toLong()).asBoolean() }
}