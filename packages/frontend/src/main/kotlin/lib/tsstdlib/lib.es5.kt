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

import kotlin.js.*

external interface DateConstructor {
  var prototype: Date
    fun parse(s: String): Number
    fun UTC(year: Number, month: Number, date: Number = definedExternally, hours: Number = definedExternally, minutes: Number = definedExternally, seconds: Number = definedExternally, ms: Number = definedExternally): Number
    fun now(): Number
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun DateConstructor.invoke(): String {
  return asDynamic()() as String
}

external interface ArrayLike<T> {
    var length: Number
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> ArrayLike<T>.get(n: Number): T? = asDynamic()[n] as? T

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> ArrayLike<T>.set(n: Number, value: T) {
  asDynamic()[n] = value
}

external interface ErrorConstructor {
  @nativeInvoke
  operator fun invoke(message: String = definedExternally): Error
  var prototype: Error
}

external interface PromiseLike<T> {
  fun then(onfulfilled: ((value: T) -> Any?)? = definedExternally, onrejected: ((reason: Any) -> Any?)? = definedExternally): PromiseLike<dynamic /* TResult1 | TResult2 */>
}
