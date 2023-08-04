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

@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "unused",
  "FunctionName",
  "DEPRECATION",
)
package lib.tsstdlib

import kotlin.js.Date

public external interface DateConstructor {
  public var prototype: Date
  public fun parse(s: String): Number
  public fun UTC(year: Number, month: Number, date: Number = definedExternally, hours: Number = definedExternally, minutes: Number = definedExternally, seconds: Number = definedExternally, ms: Number = definedExternally): Number
  public fun now(): Number
}

@Suppress("NOTHING_TO_INLINE")
public inline operator fun DateConstructor.invoke(): String {
  return asDynamic()() as String
}

public external interface ArrayLike<T> {
  public var length: Number
}

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> ArrayLike<T>.get(n: Number): T? = asDynamic()[n] as? T

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> ArrayLike<T>.set(n: Number, value: T) {
  asDynamic()[n] = value
}

public external interface ErrorConstructor {
  @nativeInvoke
  public operator fun invoke(message: String = definedExternally): Error
  public var prototype: Error
}

public external interface PromiseLike<T> {
  public fun then(onfulfilled: ((value: T) -> Any?)? = definedExternally, onrejected: ((reason: Any) -> Any?)? = definedExternally): PromiseLike<dynamic /* TResult1 | TResult2 */>
}
