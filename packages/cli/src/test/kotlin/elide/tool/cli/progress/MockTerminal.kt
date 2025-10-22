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
package elide.tool.cli.progress

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.PrintRequest
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalInfo
import com.github.ajalt.mordant.terminal.TerminalInterface

/**
 * Mock terminal for progress tests.
 *
 * @author Lauri Heino <datafox>
 */
object MockTerminal {
  operator fun invoke(width: Int): Terminal = Terminal(width = width, terminalInterface = Interface())

  class Interface : TerminalInterface {
    override fun info(
      ansiLevel: AnsiLevel?,
      hyperlinks: Boolean?,
      outputInteractive: Boolean?,
      inputInteractive: Boolean?
    ): TerminalInfo = TerminalInfo(AnsiLevel.ANSI16, false, false, false, false)

    override fun completePrintRequest(request: PrintRequest) = Unit

    override fun readLineOrNull(hideInput: Boolean): String? = null
  }
}
