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

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle

/**
 * ## Node Util: Inspect Styling
 *
 * Governs the style mappings used when rendering values via `util.inspect`, when `colors` are enabled via rendering
 * options (see [InspectOptions]).
 */
public interface InspectStyling {
  /** Static factories for obtaining instances of [InspectStyling]. */
  public companion object {
    /**
     * Default instance of [InspectStyling].
     *
     * This is the default styling used when no specific styling is provided.
     *
     * @return A default instance of [InspectStyling].
     */
    @JvmStatic public fun default(): InspectStyling = DefaultInspectStyling

    private fun noDefaultStyle(): TextStyle? = null
  }

  /**
   * Text style to use for string values.
   */
  public val stringValue: TextStyle? get() = noDefaultStyle()

  /**
   * Text style to use for null or undefined values.
   */
  public val nullValue: TextStyle? get() = noDefaultStyle()

  /**
   * Text style to use for complex string values (dates, durations, etc., rendered as strings).
   */
  public val complexStringValue: TextStyle? get() = noDefaultStyle()

  /**
   * Text style to use for primitive non-string values.
   */
  public val primitiveValue: TextStyle? get() = noDefaultStyle()
}

// Default implementation of `InspectStyling`.
internal object DefaultInspectStyling : InspectStyling {
  override val nullValue: TextStyle = TextColors.brightWhite
  override val stringValue: TextStyle = TextColors.green
  override val primitiveValue: TextStyle = TextColors.yellow
  override val complexStringValue: TextStyle = stringValue
}
