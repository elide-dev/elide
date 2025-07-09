/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.node.util

import com.oracle.truffle.js.runtime.objects.Undefined
import org.graalvm.polyglot.Value
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.util.InspectOptionsAPI

private const val SHOW_HIDDEN_PROP: String = "showHidden"
private const val SHOW_HIDDEN_DEFAULT: Boolean = false
private const val DEPTH_PROP: String = "depth"
private const val DEPTH_DEFAULT: Int = 2
private const val COLORS_PROP: String = "colors"
private const val COLORS_DEFAULT: Boolean = false
private const val CUSTOM_INSPECT_PROP: String = "customInspect"
private const val CUSTOM_INSPECT_DEFAULT: Boolean = true
private const val SHOW_PROXY_PROP: String = "showProxy"
private const val SHOW_PROXY_DEFAULT: Boolean = false
private const val MAX_ARRAY_LENGTH_PROP: String = "maxArrayLength"
private const val MAX_ARRAY_LENGTH_DEFAULT: Int = 100
private const val MAX_STRING_LENGTH_PROP: String = "maxStringLength"
private const val MAX_STRING_LENGTH_DEFAULT: Int = 10000
private const val BREAK_LENGTH_PROP: String = "breakLength"
private const val BREAK_LENGTH_DEFAULT: Int = 80
private const val NUMERIC_SEPARATOR_DEFAULT: Boolean = true
private const val NUMERIC_SEPARATOR_PROP: String = "numericSeparator"
private const val COMPACT_PROP: String = "compact"
private const val SORTED_PROP: String = "sorted"
private const val GETTERS_PROP: String = "getters"

private val allInspectOptionsProps = arrayOf(
  SHOW_HIDDEN_PROP,
  DEPTH_PROP,
  COLORS_PROP,
  CUSTOM_INSPECT_PROP,
  SHOW_PROXY_PROP,
  MAX_ARRAY_LENGTH_PROP,
  MAX_STRING_LENGTH_PROP,
  BREAK_LENGTH_PROP,
  COMPACT_PROP,
  NUMERIC_SEPARATOR_PROP,
  SORTED_PROP,
)

// Options for `inspect`
@JvmRecord internal data class InspectOptions internal constructor (
  override val showHidden: Boolean,
  override val depth: Int,
  override val colors: Boolean,
  override val customInspect: Boolean,
  override val showProxy: Boolean,
  override val maxArrayLength: Int,
  override val maxStringLength: Int,
  override val breakLength: Int,
  override val compact: Value? = null,
  override val sorted: Value? = null,
  override val getters: Value? = null,
  override val numericSeparator: Boolean,
) : InspectOptionsAPI {
  override fun getMemberKeys(): Array<String> = allInspectOptionsProps

  override fun getMember(key: String): Any? = when (key) {
    SHOW_HIDDEN_PROP -> showHidden
    DEPTH_PROP -> depth
    COLORS_PROP -> colors
    CUSTOM_INSPECT_PROP -> customInspect
    SHOW_PROXY_PROP -> showProxy
    MAX_ARRAY_LENGTH_PROP -> maxArrayLength
    MAX_STRING_LENGTH_PROP -> maxStringLength
    BREAK_LENGTH_PROP -> breakLength
    COMPACT_PROP -> compact
    SORTED_PROP -> sorted
    GETTERS_PROP -> getters
    NUMERIC_SEPARATOR_PROP -> numericSeparator
    else -> Undefined.instance
  }

  companion object {
    /** Defaults for inspect options. */
    private val DEFAULTS: InspectOptions by lazy {
      InspectOptions(
        showHidden = SHOW_HIDDEN_DEFAULT,
        depth = DEPTH_DEFAULT,
        colors = COLORS_DEFAULT,
        customInspect = CUSTOM_INSPECT_DEFAULT,
        showProxy = SHOW_PROXY_DEFAULT,
        maxArrayLength = MAX_ARRAY_LENGTH_DEFAULT,
        maxStringLength = MAX_STRING_LENGTH_DEFAULT,
        breakLength = BREAK_LENGTH_DEFAULT,
        compact = null, // default is `null`
        sorted = null, // default is `null`
        getters = null, // default is `null`
        numericSeparator = NUMERIC_SEPARATOR_DEFAULT,
      )
    }

    /**
     * Get the default [InspectOptions] instance.
     *
     * @return A default [InspectOptions] instance with standard values.
     */
    @JvmStatic fun defaults(): InspectOptions = DEFAULTS

    /**
     * Create a new [InspectOptions] instance from a guest value.
     *
     * @param value Guest value to convert to an [InspectOptions] instance.
     * @return A new [InspectOptions] instance.
     */
    @JvmStatic fun from(value: Value): InspectOptions = when {
      value.hasMembers() -> defaults().copy(
        showHidden = value.getMember(SHOW_HIDDEN_PROP)?.asBoolean() ?: SHOW_HIDDEN_DEFAULT,
        depth = value.getMember(DEPTH_PROP)?.asInt() ?: DEPTH_DEFAULT,
        colors = value.getMember(COLORS_PROP)?.asBoolean() ?: COLORS_DEFAULT,
        customInspect = value.getMember(CUSTOM_INSPECT_PROP)?.asBoolean() ?: CUSTOM_INSPECT_DEFAULT,
        showProxy = value.getMember(SHOW_PROXY_PROP)?.asBoolean() ?: SHOW_PROXY_DEFAULT,
        maxArrayLength = value.getMember(MAX_ARRAY_LENGTH_PROP)?.asInt() ?: MAX_ARRAY_LENGTH_DEFAULT,
        maxStringLength = value.getMember(MAX_STRING_LENGTH_PROP)?.asInt() ?: MAX_STRING_LENGTH_DEFAULT,
        breakLength = value.getMember(BREAK_LENGTH_PROP)?.asInt() ?: BREAK_LENGTH_DEFAULT,
        compact = value.getMember(COMPACT_PROP),
        sorted = value.getMember(SORTED_PROP),
        getters = value.getMember(GETTERS_PROP),
        numericSeparator = value.getMember(NUMERIC_SEPARATOR_PROP)?.asBoolean() ?: NUMERIC_SEPARATOR_DEFAULT,
      )
      else -> throw JsError.typeError("`InspectOptions` can only be created from an object")
    }
  }
}
