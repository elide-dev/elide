/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
 * Represents a user-configurable JS Runtime property; binds a JS VM property to an Elide configuration property.
 *
 * @param name Name of the property within Elide's configuration system.
 * @param symbol Symbol to use for the VM property when passing it to a new context.
 * @param defaultValue If no configured value is available, this value should be passed instead. If null, pass no
 *   value at all.
 */
public data class VMRuntimeProperty internal constructor (
  private val name: String,
  override val symbol: String,
  private val defaultValue: String? = null,
  private val getter: (() -> String?)? = null,
): VMProperty {
  public companion object {
    private fun booleanToSymbol(boolean: Boolean?): String? = when (boolean) {
      null -> null
      true -> "true"
      false -> "false"
    }

    /** @return Fully-configurable runtime property. */
    @JvmStatic public fun ofConfigurable(
      name: String,
      symbol: String,
      defaultValue: String? = null,
      getter: (() -> String?)? = null,
    ): VMRuntimeProperty = VMRuntimeProperty(name, symbol, defaultValue, getter)

    /** @return Fully-configurable runtime property, backed by a Boolean return value. */
    @JvmStatic public fun ofBoolean(
      name: String,
      symbol: String,
      defaultValue: Boolean? = null,
      getter: (() -> Boolean?)? = null,
    ): VMRuntimeProperty = VMRuntimeProperty(
      name,
      symbol,
      booleanToSymbol(defaultValue)
    ) {
      booleanToSymbol(getter?.invoke())
    }
  }

  override fun value(): String? = getter?.invoke() ?: defaultValue
}
