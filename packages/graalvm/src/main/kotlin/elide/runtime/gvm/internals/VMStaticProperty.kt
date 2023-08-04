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
 * Represents a hard-coded JS Runtime property.
 *
 * @param symbol Symbol to use for the VM property when passing it to a new context.
 * @param staticValue Value for this property.
 */
public data class VMStaticProperty internal constructor (
  override val symbol: String,
  val staticValue: String,
): VMProperty {
  public companion object {
    private const val ENABLED_TRUE = "true"
    private const val DISABLED_FALSE = "false"

    /** @return Active setting. */
    @JvmStatic public fun of(name: String, value: String): VMStaticProperty = VMStaticProperty(name, value)

    /** @return Active setting. */
    @JvmStatic public fun active(name: String): VMStaticProperty = VMStaticProperty(name, ENABLED_TRUE)

    /** @return Active setting. */
    @JvmStatic public fun inactive(name: String): VMStaticProperty = VMStaticProperty(name, DISABLED_FALSE)
  }

  override fun value(): String = staticValue
}
