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
package elide.tooling.term

import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedString

/**
 * ## Terminal Utilities
 *
 * General terminal utilities and extension functions.
 */
public object TerminalUtil {
  private const val COLOR_256 = 256
  private const val COLOR_TRUE = 16777216

  /**
   * Force this [AttributedString] to render in 256-color ANSI, returning a string.
   *
   * @receiver Attributed string.
   * @return Rendered string, in true-color.
   */
  public fun AttributedString.to256ColorAnsiString(): String {
    return toAnsi(
      COLOR_256,
      AttributedCharSequence.ForceMode.Force256Colors,
    )
  }

  /**
   * Force this [AttributedString] to render in true-color ANSI, returning a string.
   *
   * @receiver Attributed string.
   * @return Rendered string, in true-color.
   */
  public fun AttributedString.toTrueColorAnsiString(): String {
    return toAnsi(
      COLOR_TRUE,
      AttributedCharSequence.ForceMode.ForceTrueColors,
    )
  }
}
