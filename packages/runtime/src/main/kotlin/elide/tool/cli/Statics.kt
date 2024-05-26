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

package elide.tool.cli

import java.io.InputStream
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.Logger
import elide.runtime.Logging

/** Internal static tools and utilities used across the Elide CLI. */
internal object Statics {
  private val delegatedInStream: AtomicReference<InputStream> = AtomicReference()
  private val delegatedOutStream: AtomicReference<PrintStream> = AtomicReference()
  private val delegatedErrStream: AtomicReference<PrintStream> = AtomicReference()

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
    System.getenv("NO_COLOR") != null || args.get().let { args ->
      args.contains("--no-pretty") || args.contains("--no-color")
    }
  }

  /** Invocation args. */
  internal val args: AtomicReference<List<String>> = AtomicReference(emptyList())

  /** Main top-level tool. */
  val base: AtomicReference<ElideTool> = AtomicReference()

  val `in`: InputStream get() = delegatedInStream.get() ?: System.`in`
  val out: PrintStream get() = delegatedOutStream.get() ?: System.out
  val err: PrintStream get() = delegatedErrStream.get() ?: System.err

  internal fun assignStreams(out: PrintStream, err: PrintStream, `in`: InputStream) {
    delegatedOutStream.set(out)
    delegatedErrStream.set(err)
    delegatedInStream.set(`in`)
  }
}
