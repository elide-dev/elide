@file:JsQualifier("arith")
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package lib.protobuf

import kotlin.js.*

external open class UInt64(lo: Number, hi: Number) {
    open var lo: Number
    open var hi: Number
    open fun cmp(other: UInt64): Number
    open fun rightShift(): UInt64
    open fun leftShift(): UInt64
    open fun msb(): Boolean
    open fun lsb(): Boolean
    open fun zero(): Boolean
    open fun add(other: UInt64): UInt64
    open fun sub(other: UInt64): UInt64
    open fun mul(a: Number): UInt64
    open fun div(divisor: Number): dynamic /* JsTuple<UInt64, UInt64> */
    override fun toString(): String
    open fun clone(): UInt64

    companion object {
        fun mul32x32(a: Number, b: Number): UInt64
        fun fromString(str: String): UInt64
    }
}

external open class Int64(lo: Number, hi: Number) {
    open var lo: Number
    open var hi: Number
    open fun add(other: Int64): Int64
    open fun sub(other: Int64): Int64
    open fun clone(): Int64
    override fun toString(): String

    companion object {
        fun fromString(str: String): Int64
    }
}