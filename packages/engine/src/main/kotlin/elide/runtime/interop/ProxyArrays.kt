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
package elide.runtime.interop

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray

/**
 * Wraps this [List] as a read-only [ProxyArray] for guest interop.
 */
public fun List<*>.toProxyArray(): ProxyArray = object : ProxyArray {
  override fun get(index: Long): Any? = this@toProxyArray[index.toInt()]
  override fun set(index: Long, value: Value?) {}
  override fun getSize(): Long = this@toProxyArray.size.toLong()
}

/**
 * Wraps this [Array] of strings as a read-only [ProxyArray] for guest interop.
 */
public fun Array<String>.toProxyArray(): ProxyArray = toList().toProxyArray()

/**
 * Converts a guest [Value] with array elements to a Kotlin [Array] of strings.
 * Returns an empty array if the value is null or has no array elements.
 */
public fun Value?.toStringArray(): Array<String> {
  if (this == null || !hasArrayElements()) return emptyArray()
  return Array(arraySize.toInt()) { getArrayElement(it.toLong()).asString() }
}
