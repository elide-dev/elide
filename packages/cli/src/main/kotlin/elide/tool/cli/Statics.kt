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

@file:Suppress("FunctionParameterNaming", "ObjectPropertyNaming")

package elide.tool.cli

import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.atomicfu.atomic
import elide.runtime.Logger
import elide.runtime.Logging

/** Internal static tools and utilities used across the Elide CLI. */
internal object Statics {
  val disableStreams = System.getProperty("elide.disableStreams") == "true"
  private val delegatedInStream = atomic<InputStream?>(null)
  private val delegatedOutStream = atomic<PrintStream?>(null)
  private val delegatedErrStream = atomic<PrintStream?>(null)

  /** Main tool logger. */
  internal val logging: Logger by lazy {
    Logging.named("tool")
  }

  /** Server tool logger. */
  internal val serverLogger: Logger by lazy {
    Logging.named("tool:server")
  }

  /** Whether to disable color output and syntax highlighting. */
  internal val noColor: Boolean by lazy {
    System.getenv("NO_COLOR") != null || args.let { args ->
      args.contains("--no-pretty") || args.contains("--no-color")
    }
  }

  @Volatile private var execBinPath: String = ""
  @Volatile private var initialArgs = emptyArray<String>()

  /** Invocation args. */
  internal val args: Array<String> get() = initialArgs

  internal val bin: String get() = execBinPath
  internal val binPath: Path by lazy { Paths.get(bin).toAbsolutePath() }
  internal val elideHome: Path by lazy { binPath.parent }
  internal val resourcesPath: Path by lazy { elideHome.resolve("resources").toAbsolutePath() }

  // Stream which drops all data.
  private val noOpStream by lazy {
    PrintStream(object : OutputStream() {
      override fun write(b: Int) = Unit
    })
  }

  val `in`: InputStream get() =
    delegatedInStream.value ?: System.`in`

  @JvmField var out: PrintStream =
    when (disableStreams) {
      true -> noOpStream
      else -> delegatedOutStream.value ?: System.out
    }

  @JvmField var err: PrintStream =
    when (disableStreams) {
      true -> noOpStream
      else -> delegatedErrStream.value ?: System.err
    }

  internal fun mountArgs(bin: String, args: Array<String>) {
    check(initialArgs.isEmpty()) { "Args are not initialized yet!" }
    execBinPath = bin
    initialArgs = args
  }

  internal fun assignStreams(out: PrintStream, err: PrintStream, `in`: InputStream) {
    if (disableStreams) return
    delegatedOutStream.value = out
    delegatedErrStream.value = err
    delegatedInStream.value = `in`
  }
}
