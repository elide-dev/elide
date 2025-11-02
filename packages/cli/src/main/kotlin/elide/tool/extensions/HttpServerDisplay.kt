/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.tool.extensions

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import elide.runtime.http.server.netty.HttpApplicationStack
import elide.runtime.http.server.netty.assembleUri
import elide.tooling.cli.Statics.terminal

fun HttpApplicationStack.echoStartMessage() {
  val startupMessages = services.map { service ->
    service.bindResult.fold(
      onSuccess = { TextColors.green("︎[${service.label}]: listening at ${it.assembleUri()}") },
      onFailure = { TextColors.red("[${service.label}]: failed with $it") },
    )
  }

  terminal.println("Server started:")
  startupMessages.forEach { message -> terminal.println(message) }

  val failures = services.filter { it.bindResult.isFailure }
  if (failures.isNotEmpty()) {
    val message = failures.joinToString("\n") {
      TextColors.red(" - [${it.label}] ${it.bindResult.exceptionOrNull()?.stackTraceToString()}")
    }

    terminal.println(TextStyles.dim("Some test services failed to start:\n$message"))
  }
}

fun HttpApplicationStack.echoShutdownMessage() {
  val errors = onClose.get()?.joinToString("\n").orEmpty()
  if (errors.isNotEmpty()) terminal.println(TextColors.red("Some services failed to shutdown properly:\n$errors"))
  else terminal.println("Server stopped successfully")
}
