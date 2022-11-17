@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "unused",
)
package lib.tsstdlib

import kotlin.js.*

public external interface IteratorYieldResult<TYield> {
  public var done: Boolean?
    get() = definedExternally
    set(value) = definedExternally
  public var value: TYield
}

public external interface IteratorReturnResult<TReturn> {
  public var done: Boolean
  public var value: TReturn
}

public external interface Iterator<T, TReturn, TNext> {
  public fun next(vararg args: Any /* JsTuple<> | JsTuple<TNext> */): dynamic /* IteratorYieldResult<T> | IteratorReturnResult<TReturn> */
  public val `return`: ((value: TReturn) -> dynamic)?
  public val `throw`: ((e: Any) -> dynamic)?
}

public external interface Iterable<T>

public external interface PromiseConstructor {
  public var prototype: Promise<Any>
  public fun all(values: Any /* JsTuple<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?, Any?> | JsTuple<Any?, Any?, Any?> | JsTuple<Any?, Any?> */): Promise<dynamic /* JsTuple<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> | JsTuple<T1, T2, T3, T4, T5, T6, T7, T8, T9> | JsTuple<T1, T2, T3, T4, T5, T6, T7, T8> | JsTuple<T1, T2, T3, T4, T5, T6, T7> | JsTuple<T1, T2, T3, T4, T5, T6> | JsTuple<T1, T2, T3, T4, T5> | JsTuple<T1, T2, T3, T4> | JsTuple<T1, T2, T3> | JsTuple<T1, T2> */>
  public fun <T> all(values: Array<Any? /* T | PromiseLike<T> */>): Promise<Array<T>>
  public fun <T> race(values: Array<T>): Promise<Any>
  public fun <T> reject(reason: Any = definedExternally): Promise<T>
  public fun <T> resolve(value: T): Promise<T>
  public fun <T> resolve(value: PromiseLike<T>): Promise<T>
  public fun resolve(): Promise<Unit>
  public fun <T> all(values: Iterable<Any? /* T | PromiseLike<T> */>): Promise<Array<T>>
  public fun <T> race(values: Iterable<T>): Promise<Any>
  public fun <T> race(values: Iterable<Any? /* T | PromiseLike<T> */>): Promise<T>
}
