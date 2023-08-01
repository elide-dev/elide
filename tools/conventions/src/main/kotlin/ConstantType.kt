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

import kotlin.reflect.KClass

/** Concrete types for build-time constant values. */
sealed class ConstantType private constructor (
  val primitive: ConstantPrimitive,
  val target: KClass<*>,
) {
  /** @return Fully-qualified type reference. */
  fun reference(): String = when (primitive) {
    ConstantPrimitive.STRING -> String::class.qualifiedName
    ConstantPrimitive.INT -> Int::class.qualifiedName
    ConstantPrimitive.LONG -> Long::class.qualifiedName
    ConstantPrimitive.BOOL -> Boolean::class.qualifiedName
    ConstantPrimitive.CLASS -> target::class.qualifiedName
  }!!

  companion object {
    /** String type singleton. */
    val STRING: Primitive.String = Primitive.String()

    /** Integer type singleton. */
    val INT: Primitive.Int = Primitive.Int()

    /** Long-type singleton. */
    val LONG: Primitive.Long = Primitive.Long()

    /** Boolean-type singleton. */
    val BOOL: Primitive.Boolean = Primitive.Boolean()
  }

  /** Class-backed constant type. */
  class Impl(target: KClass<*>): ConstantType(
    primitive = ConstantPrimitive.CLASS,
    target = target,
  )

  /** Primitive-backed constant type. */
  sealed class Primitive private constructor (target: KClass<*>, primitive: ConstantPrimitive): ConstantType(
    primitive = primitive,
    target = target,
  ) {
    /** Primitive build-time string. */
    class String: Primitive(
      target = kotlin.String::class,
      primitive = ConstantPrimitive.STRING,
    )

    /** Primitive build-time integer. */
    class Int: Primitive(
      target = kotlin.Int::class,
      primitive = ConstantPrimitive.INT,
    )

    /** Primitive build-time string. */
    class Long: Primitive(
      target = kotlin.Long::class,
      primitive = ConstantPrimitive.LONG,
    )

    /** Primitive build-time boolean. */
    class Boolean: Primitive(
      target = kotlin.Boolean::class,
      primitive = ConstantPrimitive.BOOL,
    )
  }
}
