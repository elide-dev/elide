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
package elide.tool.cli.cmd.s3

import com.robothy.s3.rest.LocalS3
import com.robothy.s3.rest.bootstrap.LocalS3Mode
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import picocli.CommandLine
import picocli.CommandLine.Command

/** TBD. */
@Command(
  name = "s3",
  description = ["Run a local S3 server"],
  mixinStandardHelpOptions = true,
)
@Introspected
@ReflectiveAccess
internal class ToolS3Command : ProjectAwareSubcommand<ToolState, CommandContext>() {
  /** Specifies the directory where files should be served. */
  @CommandLine.Option(
    names = ["--directory", "--dir", "-d"],
    description = ["Root directory of the server. Current directory will be used if omitted."],
  )
  internal var directory: String? = null

  /** Specifies the port of the server. */
  @CommandLine.Option(
    names = ["--port", "-P"],
    description = ["Port of the server."],
    defaultValue = "8080",
  )
  internal var port: Int = 8080

  @CommandLine.Option(
    names = ["--in-memory", "--memory", "-m"],
    description = ["Run the server in memory. Files will be read from the directory, but will not be modified."],
    defaultValue = "false",
  )
  internal var memory: Boolean = false

  @CommandLine.Option(
    names = ["--shutdown-after"],
    description = ["Automatically shut down the server after this many seconds."],
    defaultValue = "-1",
  )
  internal var timeout: Int = -1

  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val mode = if (memory) LocalS3Mode.IN_MEMORY else LocalS3Mode.PERSISTENCE
    val userDir = System.getProperty("user.dir")
    val dataPath = directory?.let { if (it.isCharAt(0, '/') || it.isCharAt(1, ':')) it else "$userDir/$it" } ?: userDir
    val server = LocalS3.builder().port(port).mode(mode).dataPath(dataPath).build()
    server.start()
    if (timeout > 0) {
      delay(timeout.seconds)
      server.shutdown()
      return CommandResult.success()
    }
    try {
      awaitCancellation()
    } catch (t: Throwable) {
      return if (t is CancellationException) CommandResult.success() else CommandResult.err(1, exc = t)
    } finally {
      server.shutdown()
    }
  }

  private fun String.isCharAt(index: Int, char: Char): Boolean = length > index && this[index] == char
}
