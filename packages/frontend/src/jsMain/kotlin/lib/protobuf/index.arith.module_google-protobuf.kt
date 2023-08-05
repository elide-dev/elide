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

@file:JsQualifier("arith")
@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "unused",
)
package lib.protobuf

/**
 * UInt64
 */
public open external class UInt64(lo: Number, hi: Number) {
  public open var lo: Number
  public open var hi: Number
  public open fun cmp(other: UInt64): Number
  public open fun rightShift(): UInt64
  public open fun leftShift(): UInt64
  public open fun msb(): Boolean
  public open fun lsb(): Boolean
  public open fun zero(): Boolean
  public open fun add(other: UInt64): UInt64
  public open fun sub(other: UInt64): UInt64
  public open fun mul(a: Number): UInt64
  public open fun div(divisor: Number): dynamic /* JsTuple<UInt64, UInt64> */
  public override fun toString(): String
  public open fun clone(): UInt64

  public companion object {
    public fun mul32x32(a: Number, b: Number): UInt64
    public fun fromString(str: String): UInt64
    }
}

/**
 * Int64
 */
public open external class Int64(lo: Number, hi: Number) {
  public open var lo: Number
  public open var hi: Number
  public open fun add(other: Int64): Int64
  public open fun sub(other: Int64): Int64
  public open fun clone(): Int64
  public override fun toString(): String

  public companion object {
    public fun fromString(str: String): Int64
  }
}
