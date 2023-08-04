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

package elide.tool.cli

/**
 * TBD.
 */
internal sealed class ToolState(
  val output: OutputSettings = OutputSettings.DEFAULTS,
) {
  /**
   * Output settings for the tool.
   *
   */
  data class OutputSettings(
    val verbose: Boolean = false,
    val quiet: Boolean = false,
    val pretty: Boolean = true,
    val stderr: Boolean = false,
  ) {
    internal companion object {
      // Default output settings.
      val DEFAULTS: OutputSettings = OutputSettings()
    }
  }

  /** Empty/default tool state. */
  private class Empty : ToolState()

  companion object {
    /** Default `Empty` singleton. */
    internal val EMPTY: ToolState = Empty()
  }
}
