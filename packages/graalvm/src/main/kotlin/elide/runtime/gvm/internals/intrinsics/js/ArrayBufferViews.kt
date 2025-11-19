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
package elide.runtime.gvm.internals.intrinsics.js

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.nio.ByteBuffer
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews.getBackingBuffer
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews.getLength
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews.getOffset
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews.getViewType
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews.newView
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.plugins.js.JavaScript

/**
 * Alias for a guest [Value] representing a JavaScript `ArrayBuffer`. Values using this alias _must_ be checked before
 * being returned from a function or property, to ensure they have [buffer elements][Value.hasBufferElements].
 *
 * @see ArrayBufferViews.getBackingBuffer
 */
internal typealias ArrayBufferValue = Value

/**
 * Represents a type of array buffer view (TypedArray or DataView), identified by the name of its corresponding
 * metaobject in a guest context.
 */
internal enum class ArrayBufferViewType {
  Int8Array,
  Uint8Array,
  Uint8ClampedArray,
  Int16Array,
  Uint16Array,
  Int32Array,
  Uint32Array,
  Float16Array,
  Float32Array,
  Float64Array,
  Int64Array,
  Uint64Array,
  DataView,
}

/**
 * A collection of interoperability helpers for JavaScript array buffer views (i.e. typed arrays and data views).
 *
 * Use [newView] as a simple one-in-all constructor for buffer views, and [getViewType], [getBackingBuffer],
 * [getOffset], and [getLength] to inspect values for known properties.
 *
 * Note that most of the functions require an active (entered) context in the calling thread, in order to locate the
 * necessary metaobjects that act as constructors and contain static methods.
 */
@OptIn(DelicateElideApi::class)
internal object ArrayBufferViews {
  private const val ARRAY_ELEMENT_SIZE_MEMBER = "BYTES_PER_ELEMENT"
  private const val BYTE_LENGTH_MEMBER = "byteLength"
  private const val BYTE_OFFSET_MEMBER = "byteOffset"
  private const val BACKING_BUFFER_MEMBER = "buffer"

  /**
   * Retrieve the metaobject (constructor) for a given array view [type] from the JavaScript bindings of a [context],
   * throwing an exception if it cannot be found. The returned value is guaranteed to be a metaobject.
   */
  private fun getViewConstructor(type: ArrayBufferViewType, context: Context): Value {
    val constructor = context.getBindings(JavaScript.languageId).getMember(type.name)
      ?: throw TypeError.create("Unknown array buffer view type '$type'")

    check(constructor.isMetaObject) { "Expected view constructor to be a metaobject" }
    return constructor
  }

  /**
   * Construct a new array buffer view of the given [type] and [length], wrapping [buffer] starting at [offset]. A
   * guest metaobject resolved from the specified [context] will be used as constructor for the instance.
   *
   * The NIO [buffer] will be converted to an [ArrayBufferValue] before being wrapped.
   */
  internal fun newView(
    type: ArrayBufferViewType,
    buffer: ByteBuffer,
    offset: Long = 0,
    length: Long = buffer.limit().toLong(),
    context: Context = Context.getCurrent()
  ): Value {
    return newView(type, Value.asValue(buffer), offset, length, context)
  }

  /**
   * Construct a new array buffer view of the given [type] and [length], wrapping [buffer] starting at [offset]. A
   * guest metaobject resolved from the specified [context] will be used as constructor for the instance.
   */
  internal fun newView(
    type: ArrayBufferViewType,
    buffer: ArrayBufferValue,
    offset: Long,
    length: Long,
    context: Context = Context.getCurrent()
  ): Value {
    val constructor = getViewConstructor(type, context)
    return constructor.newInstance(buffer, offset, length)
  }

  /**
   * Given an array buffer [view], return the size in bytes of the view's elements (e.g. `8` for an `Int8Array`).
   *
   * A default value of `1` will be returned if the [view] has no static member indicating the element size, to support
   * the use of `DataView` alongside `TypedArray` instances.
   */
  internal fun getElementSize(view: Value): Int {
    return runCatching { view.getMember(ARRAY_ELEMENT_SIZE_MEMBER).asInt() }.getOrDefault(1)
  }

  /**
   * Given an arbitrary array buffer [view], resolve its [type][ArrayBufferViewType] using the name of its metaobject,
   * or throw an exception if it is not found.
   */
  internal fun getViewType(view: Value): ArrayBufferViewType {
    return getViewTypeOrNull(view) ?: throw TypeError.create("Value $view is not a valid buffer view")
  }

  /**
   * Given an arbitrary array buffer [view], resolve its [type][ArrayBufferViewType] using the name of its metaobject,
   * or return `null` if it is not known.
   */
  internal fun getViewTypeOrNull(view: Value): ArrayBufferViewType? {
    val metaName = view.metaObject?.metaSimpleName ?: return null
    return runCatching { ArrayBufferViewType.valueOf(metaName) }.getOrNull()
  }

  /**
   * Given an array buffer [view], return its backing array buffer. The returned value is guaranteed to be a
   * [buffer][Value.hasBufferElements] for guest interop purposes.
   *
   * If the [view] value has no backing buffer, or the buffer is invalid, an exception will be thrown.
   */
  internal fun getBackingBuffer(view: Value): ArrayBufferValue {
    if (!view.hasMember(BACKING_BUFFER_MEMBER)) throw TypeError.create("Value $view is not a buffer view")
    val value = view.getMember(BACKING_BUFFER_MEMBER)

    if (!value.hasBufferElements()) throw TypeError.create("Invalid backing buffer: $value has no buffer elements")
    return value
  }

  /**
   * Given an array buffer [view], return a [ByteArray] containing the data from the viewed segment of the backing
   * array buffer.
   */
  internal fun readViewedBytes(view: Value): ByteArray {
    val buffer = getBackingBuffer(view)
    val offset = getOffset(view)
    val length = getLength(view)

    val bytes = ByteArray(length.toInt())
    buffer.readBuffer(offset, bytes, 0, bytes.size)

    return bytes
  }

  /**
   * Given an array buffer [view], return its `byteOffset` value, or throw an exception if it cannot be resolved or is
   * not valid.
   */
  internal fun getOffset(view: Value): Long {
    if (!view.hasMember(BYTE_OFFSET_MEMBER)) throw TypeError.create("Value $view is not a buffer view")
    val value = view.getMember(BYTE_OFFSET_MEMBER)

    if (!value.isNumber || !value.fitsInLong()) throw TypeError.create("Invalid byte offset value: $value")
    return value.asLong()
  }

  /**
   * Given an array buffer [view], return its `byteLength` value, or throw an exception if it cannot be resolved or is
   * not valid.
   */
  internal fun getLength(view: Value): Long {
    if (!view.hasMember(BYTE_LENGTH_MEMBER)) throw TypeError.create("Value $view is not a buffer view")
    val value = view.getMember(BYTE_LENGTH_MEMBER)

    if (!value.isNumber || !value.fitsInLong()) throw TypeError.create("Invalid byte length value: $value")
    return value.asLong()
  }
}
