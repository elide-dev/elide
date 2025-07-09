/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
@file:Suppress("NOTHING_TO_INLINE")

package elide.runtime.node.util

import com.oracle.truffle.js.runtime.builtins.JSPromise
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject
import org.graalvm.polyglot.Value
import java.util.function.Predicate
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.util.NodeTypechecksAPI
import elide.vm.annotations.Polyglot

// Error because a check has not been implemented yet.
private inline fun checkNotImplemented(name: String, value: Value): Boolean = when {
  value.isNull -> false
  else -> TODO("Check function is not implemented yet: '$name'")
}

// Attempt to perform a safe cast of a `Value` to the specified type. If the cast fails, returns `null`.
private inline fun <reified T> Value.safeCast(): T? = runCatching {
  if (isNull) null else `as`(T::class.java)
}.getOrNull()

// Attempt to perform a safe cast of a `Value` to the specified type. If the cast fails, returns `null`.
private inline fun <reified T> Value.safe(predicate: Predicate<Any> = Predicate { true }): T? =
  safeCast<T>()?.takeIf { predicate.test(it) }

// Attempt to perform a safe cast of a `Value` to the specified type. If the cast fails, returns `null`.
private inline fun <reified T> Value.safeIs(predicate: Predicate<Any> = Predicate { true }): Boolean =
  safe<T>(predicate) != null

// Attempt to perform a safe cast of a `Value` to one of two specified types. If the cast fails, returns `null`.
private inline fun <reified T, reified X> Value.safeEither(predicate: Predicate<Any> = Predicate { true }): Boolean =
  safeIs<T>(predicate) || safeIs<X>(predicate)

// Implements typechecks via `util.types`.
internal object NodeTypechecks : NodeTypechecksAPI {
  override fun isBoxedPrimitive(value: Value): Boolean = checkNotImplemented("isBoxedPrimitive", value)
  override fun isCryptoKey(value: Value): Boolean = checkNotImplemented("isCryptoKey", value)
  override fun isDataView(value: Value): Boolean = checkNotImplemented("isDataView", value)
  override fun isExternal(value: Value): Boolean = checkNotImplemented("isExternal", value)
  override fun isGeneratorObject(value: Value): Boolean = checkNotImplemented("isGeneratorObject", value)
  override fun isKeyObject(value: Value): Boolean = checkNotImplemented("isKeyObject", value)
  override fun isMapIterator(value: Value): Boolean = checkNotImplemented("isMapIterator", value)
  override fun isModuleNamespaceObject(value: Value): Boolean = checkNotImplemented("isModuleNamespaceObject", value)
  override fun isNativeError(value: Value): Boolean = checkNotImplemented("isNativeError", value)
  override fun isProxy(value: Value): Boolean = checkNotImplemented("isProxy", value)
  override fun isSetIterator(value: Value): Boolean = checkNotImplemented("isSetIterator", value)

  @Polyglot override fun isPromise(value: Value): Boolean = value.safeEither<JSPromiseObject, JsPromise<*>> {
    (it !is JSPromiseObject || JSPromise.isJSPromise(it))
  }

  @Polyglot override fun isStringObject(value: Value): Boolean = (
    !value.isNull &&
    value.isString &&
    value.metaObject?.metaSimpleName == "String"
  )

  @Polyglot override fun isRegExp(value: Value): Boolean = (
    !value.isNull &&
    value.metaObject?.metaSimpleName == "RegExp"
  )

  @Polyglot override fun isMap(value: Value): Boolean = (
    !value.isNull &&
    value.hasHashEntries()
  )

  @Polyglot override fun isSet(value: Value): Boolean = (
    !value.isNull &&
    value.metaObject?.metaSimpleName == "Set"
  )

  @Polyglot override fun isDate(value: Value): Boolean = (
    !value.isNull &&
    value.isDate
  )

  @Polyglot override fun isBooleanObject(value: Value): Boolean = (
    !value.isNull &&
    value.isBoolean
  )

  @Polyglot override fun isNumberObject(value: Value): Boolean = (
    !value.isNull &&
    value.metaObject?.metaSimpleName == "Number"
  )

  @Polyglot override fun isWeakMap(value: Value): Boolean = (
    !value.isNull &&
    value.metaObject?.metaSimpleName == "WeakMap"
  )

  @Polyglot override fun isWeakSet(value: Value): Boolean = (
    !value.isNull &&
    value.metaObject?.metaSimpleName == "WeakSet"
  )

  @Polyglot override fun isSymbolObject(value: Value): Boolean =
    value.metaObject?.metaSimpleName == "symbol"

  @Polyglot override fun isBigIntObject(value: Value): Boolean = (
    !value.isNull &&
    !value.isNumber &&
    value.metaObject?.metaSimpleName == "BigInt"
  )

  @Polyglot override fun isAsyncFunction(value: Value): Boolean = (
    !value.isNull &&
    value.canExecute() &&
    value.metaObject?.metaSimpleName == "AsyncFunction"
  )

  @Polyglot override fun isGeneratorFunction(value: Value): Boolean = (
    !value.isNull &&
    value.canExecute() &&
    value.metaObject?.metaSimpleName == "GeneratorFunction"
  )

  // Not supported.
  @Polyglot override fun isArgumentsObject(value: Value): Boolean = false

  @Polyglot override fun isArrayBuffer(value: Value): Boolean = (
    !value.isNull &&
    value.hasBufferElements() &&
    value.metaObject?.metaSimpleName == "ArrayBuffer"
  )

  @Polyglot override fun isArrayBufferView(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    isTypedArray(value)
  )

  @Polyglot override fun isTypedArray(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    (
      isUint8Array(value) ||
      isUint8ClampedArray(value) ||
      isUint16Array(value) ||
      isUint32Array(value) ||
      isInt8Array(value) ||
      isInt16Array(value) ||
      isInt32Array(value) ||
      isFloat16Array(value) ||
      isFloat32Array(value) ||
      isFloat64Array(value) ||
      isBigInt64Array(value) ||
      isBigUint64Array(value)
    )
  )

  @Polyglot override fun isAnyArrayBuffer(value: Value): Boolean = (
    !value.isNull &&
    isArrayBuffer(value) ||
    isArrayBufferView(value)
  )

  @Polyglot override fun isUint8Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Uint8Array"
  )

  @Polyglot override fun isUint8ClampedArray(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Uint8ClampedArray"
  )

  @Polyglot override fun isUint16Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Uint16Array"
  )

  @Polyglot override fun isUint32Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Uint32Array"
  )

  @Polyglot override fun isInt8Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Int8Array"
  )

  @Polyglot override fun isInt16Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Int16Array"
  )

  @Polyglot override fun isInt32Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Int32Array"
  )

  // Not supported.
  @Polyglot override fun isFloat16Array(value: Value): Boolean = false

  @Polyglot override fun isFloat32Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Float32Array"
  )

  @Polyglot override fun isFloat64Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "Float64Array"
  )

  @Polyglot override fun isBigInt64Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "BigInt64Array"
  )

  @Polyglot override fun isBigUint64Array(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.metaObject?.metaSimpleName == "BigUint64Array"
  )

  @Polyglot override fun isSharedArrayBuffer(value: Value): Boolean = (
    !value.isNull &&
    value.hasArrayElements() &&
    value.hasBufferElements() &&
    value.metaObject?.metaSimpleName == "SharedArrayBuffer"
  )
}
