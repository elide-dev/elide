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

package elide.internal.conventions.kotlin

/**
 * Defines a target platform supported by the Kotlin Gradle Plugin. This sealed hierarchy is meant to be used with
 * the [kotlin][elide.internal.conventions.ElideBuildExtension.Kotlin] convention DSL.
 *
 * Pure projects may use individual objects like [KotlinTarget.JVM]. To create a multiplatform target, simply add
 * multiple targets:
 *
 * ```kotlin
 * // will produce a KMP project with Jvm and Native targets
 * target = JVM + Native
 *
 * // will produce a KMP project with both JS targets
 * target = JsBrowser + JsNode
 * ```
 *
 * For KMP projects targeting every available platform, use the [KotlinTarget.All] property, which is computed lazily.
 */
public sealed interface KotlinTarget {
  /** Kotlin Multiplatform target for the JavaScript backend, specifically browser platforms. */
  public data object JsBrowser : KotlinTarget

  /** Kotlin Multiplatform target for the JavaScript backend, specifically the Node.js platform. */
  public data object JsNode : KotlinTarget

  /** Kotlin Multiplatform target for the native backend. This will target every available native platform. */
  public data object Native : KotlinTarget

  /** Kotlin target for the Jvm backend. */
  public data object JVM : KotlinTarget

  /** Kotlin target for the experimental WASM backend. */
  public data object WASM : KotlinTarget

  /**
   * Multiplatform target consisting of a combination of other targets. A [JVM] target wrapped by a [Multiplatform]
   * target will produce a KMP project with a single target (JVM).
   *
   * The easiest way to obtain a [Multiplatform] target is to [add][KotlinTarget.plus] several targets together, such
   * as `JVM + Native`. Note that adding a [Multiplatform] target with any other target will result in a merge rather
   * than nesting (which would be pointless).
   */
  @JvmInline public value class Multiplatform(public val targets: Array<KotlinTarget>) : KotlinTarget

  /**
   * Merge two [targets][KotlinTarget] into a single, [Multiplatform] target. This method correctly handles merging
   * [Multiplatform] targets with other targets (single or otherwise).
   */
  public operator fun plus(other: KotlinTarget): Multiplatform = when(this){
    is Multiplatform -> when(other) {
      is Multiplatform -> Multiplatform(targets + other.targets)
      else -> Multiplatform(targets + other)
    }
    else -> when(other) {
      is Multiplatform -> Multiplatform(other.targets + this)
      else -> Multiplatform(arrayOf(this, other))
    }
  }

  /**
   * Returns whether this target contains another. For [Multiplatform] targets, this returns `true` when the [other]
   * target is contained in its wrapped array; for single targets, it returns `true` only when both are equal.
   */
  public operator fun contains(other: KotlinTarget): Boolean = when (this) {
    is Multiplatform -> other in targets
    else -> other == this
  }

  public companion object {
    /** Lazy target containing every avialable platform. */
    public val All: KotlinTarget by lazy { Multiplatform(arrayOf(JVM, JsBrowser, JsNode, Native)) }
  }
}
