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

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.dim
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import elide.runtime.http.server.netty.HttpApplicationStack
import elide.runtime.http.server.netty.assembleUri
import elide.tooling.cli.Statics.terminal

fun HttpApplicationStack.Companion.bindAndDisplayResult(block: () -> HttpApplicationStack): HttpApplicationStack {
  val stack: HttpApplicationStack
  val bindTime = measureTime { stack = block() }.let {
    when {
      it.inWholeSeconds > 0 -> it.toString(DurationUnit.SECONDS, decimals = 2)
      else -> it.toString(DurationUnit.MILLISECONDS, decimals = 0)
    }
  }

  val singleService = stack.services.singleOrNull { it.bindResult.isSuccess }

  if (singleService != null) {
    val uri = singleService.bindResult.getOrThrow().assembleUri()
    val label = serviceDisplayName(singleService.label)
    terminal.println(green("$label listening on: ${cyan(uri.toString())} ($bindTime)"))
  } else {
    val maxLabelSize = stack.services.maxOf { serviceDisplayName(it.label).length } + 1

    val startupMessages = stack.services.map { service ->
      service.bindResult.fold(
        onSuccess = {
          val label = dim("${serviceDisplayName(service.label)}:".padEnd(maxLabelSize))
          "︎$label listening at ${cyan(it.assembleUri())}"
        },
        onFailure = {
          val label = dim("${serviceDisplayName(service.label)}:".padEnd(maxLabelSize))
          "$label ${red("failed with $it")}"
        },
      )
    }

    val summaryColor = when (stack.services.count { it.bindResult.isSuccess }) {
      startupMessages.size -> green
      0 -> red
      else -> yellow
    }

    terminal.println("Started ${summaryColor(startupMessages.size.toString())} services in $bindTime")
    startupMessages.forEach { message -> terminal.println(message) }
  }


  val failures = stack.services.filter { it.bindResult.isFailure }
  if (failures.isNotEmpty()) {
    val message = failures.joinToString("\n") {
      red(" - [${it.label}] ${it.bindResult.exceptionOrNull()?.stackTraceToString()}")
    }

    terminal.println(dim("Some services failed to start:\n$message"))
  }

  return stack
}

fun HttpApplicationStack.echoShutdownMessage() {
  val errors = awaitClose().joinToString("\n")
  if (errors.isNotEmpty()) terminal.println(red("Some services failed to shutdown properly:\n$errors"))
  else terminal.println("Server stopped successfully")
}

fun serviceDisplayName(label: String): String = when (label) {
  "http" -> "HTTP"
  "https" -> "HTTPS"
  "http3" -> "HTTP/3"
  else -> error("Unknown service label $label")
}
