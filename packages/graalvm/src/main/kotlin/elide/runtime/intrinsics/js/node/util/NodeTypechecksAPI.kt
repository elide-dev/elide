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
package elide.runtime.intrinsics.js.node.util

import org.graalvm.polyglot.Value
import elide.vm.annotations.Polyglot

/**
 * ## Node Type Checks API
 *
 * Specifies the API surface of the `types` object presented as part of the Node.js `util` module; this API provides
 * small utility methods which check types of values in JavaScript.
 */
public interface NodeTypechecksAPI {
  @Polyglot public fun isAnyArrayBuffer(value: Value): Boolean
  @Polyglot public fun isArrayBufferView(value: Value): Boolean
  @Polyglot public fun isArgumentsObject(value: Value): Boolean
  @Polyglot public fun isArrayBuffer(value: Value): Boolean
  @Polyglot public fun isAsyncFunction(value: Value): Boolean
  @Polyglot public fun isBigInt64Array(value: Value): Boolean
  @Polyglot public fun isBigIntObject(value: Value): Boolean
  @Polyglot public fun isBigUint64Array(value: Value): Boolean
  @Polyglot public fun isBooleanObject(value: Value): Boolean
  @Polyglot public fun isBoxedPrimitive(value: Value): Boolean
  @Polyglot public fun isCryptoKey(value: Value): Boolean
  @Polyglot public fun isDataView(value: Value): Boolean
  @Polyglot public fun isDate(value: Value): Boolean
  @Polyglot public fun isExternal(value: Value): Boolean
  @Polyglot public fun isFloat16Array(value: Value): Boolean
  @Polyglot public fun isFloat32Array(value: Value): Boolean
  @Polyglot public fun isFloat64Array(value: Value): Boolean
  @Polyglot public fun isGeneratorFunction(value: Value): Boolean
  @Polyglot public fun isGeneratorObject(value: Value): Boolean
  @Polyglot public fun isInt8Array(value: Value): Boolean
  @Polyglot public fun isInt16Array(value: Value): Boolean
  @Polyglot public fun isInt32Array(value: Value): Boolean
  @Polyglot public fun isKeyObject(value: Value): Boolean
  @Polyglot public fun isMap(value: Value): Boolean
  @Polyglot public fun isMapIterator(value: Value): Boolean
  @Polyglot public fun isModuleNamespaceObject(value: Value): Boolean
  @Polyglot public fun isNativeError(value: Value): Boolean
  @Polyglot public fun isNumberObject(value: Value): Boolean
  @Polyglot public fun isPromise(value: Value): Boolean
  @Polyglot public fun isProxy(value: Value): Boolean
  @Polyglot public fun isRegExp(value: Value): Boolean
  @Polyglot public fun isSet(value: Value): Boolean
  @Polyglot public fun isSetIterator(value: Value): Boolean
  @Polyglot public fun isSharedArrayBuffer(value: Value): Boolean
  @Polyglot public fun isStringObject(value: Value): Boolean
  @Polyglot public fun isSymbolObject(value: Value): Boolean
  @Polyglot public fun isTypedArray(value: Value): Boolean
  @Polyglot public fun isUint8Array(value: Value): Boolean
  @Polyglot public fun isUint8ClampedArray(value: Value): Boolean
  @Polyglot public fun isUint16Array(value: Value): Boolean
  @Polyglot public fun isUint32Array(value: Value): Boolean
  @Polyglot public fun isWeakMap(value: Value): Boolean
  @Polyglot public fun isWeakSet(value: Value): Boolean
}
