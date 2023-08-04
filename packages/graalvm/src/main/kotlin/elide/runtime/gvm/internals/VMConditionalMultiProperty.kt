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

package elide.runtime.gvm.internals

/**
 * Represents a property for the JS Runtime which applies based on some `condition`, or falls back to a `defaultValue`
 * at a given `name` in Elide's configuration system; this is similar to a [VMConditionalProperty], but allows for
 * multiple properties to be efficiently applied based on a single condition.
 *
 * @param main Conditional property which should trigger this set of properties.
 * @param properties Other property configurations which should apply if this one applies.
 */
internal data class VMConditionalMultiProperty(
  private val main: VMConditionalProperty,
  private val properties: List<VMRuntimeProperty>,
): VMProperty {
  /** @return Main value for this conditional multi-property set. */
  override fun value(): String = main.value()

  /** @return Main property symbol for this conditional multi-property set. */
  override val symbol: String get() = main.symbol

  /** @return Full list of properties that should apply for this set, including the root property. */
  internal fun explode(): List<VMProperty> {
    return listOf(
      main
    ).plus(
      properties
    )
  }
}
