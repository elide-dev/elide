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
package elide.tool.cli.cmd.verify

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import picocli.CommandLine.Command
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.security.MessageDigest
import jakarta.inject.Provider
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.visitFileTree
import kotlin.time.Clock
import elide.annotations.Inject
import elide.manager.*
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.cli.progress.Progress
import elide.tool.cli.progress.TrackedTask
import elide.tooling.cli.Statics

/** TBD. */
@Command(
  name = "verify",
  description = ["Verify integrity of files of this Elide installation with a stampfile"],
  mixinStandardHelpOptions = true,
)
@Introspected
@ReflectiveAccess
internal class ToolVerifyCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var managerProvider: Provider<InstallManager>
  private val manager: InstallManager by lazy { managerProvider.get() }

  /** Specifies if a stampfile should be generated. */
  @CommandLine.Option(
    names = ["--generate"],
    description = ["Generate a stampfile instead of verifying."],
    defaultValue = "false",
    hidden = true,
  )
  private var generate: Boolean = false

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    if (generate) generate() else verify()?.let { return CommandResult.err(1, it) }
    return CommandResult.success()
  }

  @OptIn(ExperimentalStdlibApi::class)
  private fun generate() {
    val home = Statics.elideHome
    val digest = MessageDigest.getInstance("SHA-256")
    Statics.terminal.println("generating stampfile...")
    val paths = mutableListOf<Path>()
    home.visitFileTree {
      onVisitFile { path, _ ->
        if (path.name != "stampfile") paths.add(path)
        FileVisitResult.CONTINUE
      }
    }
    val lines = paths.sortedBy { it.absolutePathString() }.map {
      val relativePath = it.absolutePathString().substringAfter(home.absolutePathString())
      val hash = digest.digest(it.toFile().readBytes()).toHexString()
      "$hash  .$relativePath"
    }
    val stampfile = home.resolve("stampfile").toFile()
    stampfile.writeText(lines.joinToString("\n"))
    Statics.terminal.println("stampfile written to ${stampfile.absolutePath}")
  }

  private suspend fun verify(): String? {
    val home = Statics.elideHome
    val progress = Progress.create("Verify installation files", Statics.terminal) {
      add(TrackedTask("Verify files", 1000))
    }
    manager.verifyInstall(home.absolutePathString()) {
      when (it) {
        is FileVerifyStartEvent -> {
          progress.updateTask(0) {
            copy(position = 0)
          }
          progress.start()
        }
        is FileVerifyProgressEvent -> {
          val time = Clock.System.now().toEpochMilliseconds()
          progress.updateTask(0) { copy(position = (it.progress * 1000).toInt(), output = output + (time to it.name)) }
        }
        FileVerifyCompletedEvent -> {
          progress.updateTask(0) {
            copy(position = target)
          }
        }
        FileVerifyIndeterminateEvent -> {
          progress.updateTask(0) {
            copy(position = target, status = "no stampfile, individual files not verified")
          }
        }
      }
    }.apply {
      if (isNotEmpty()) {
        progress.updateTask(0) {
          copy(position = target, status = "invalid files", failed = true)
        }
        return "The following files did not match the stampfile:\n${joinToString("\n")}"
      }
    }
    return null
  }
}
