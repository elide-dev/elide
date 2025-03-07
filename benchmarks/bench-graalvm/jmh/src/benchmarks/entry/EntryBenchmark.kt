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

@file:Suppress("unused", "FunctionName")

package benchmarks.entry

import java.io.PrintStream
import java.util.concurrent.TimeUnit
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup

/** Benchmarks for Elide's entrypoint and related DI loading. */
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@Warmup(iterations = 10)
class EntryBenchmark {
  companion object {
    @JvmStatic var originalOut: PrintStream? = null
    @JvmStatic var originalErr: PrintStream? = null
    @JvmStatic val projectDir: String =
      requireNotNull(System.getProperty("elide.project.root")) { "Please set -Delide.project.root" }

    @JvmStatic val jsHello = "$projectDir/tools/scripts/hello.js"
    @JvmStatic val tsHello = "$projectDir/tools/scripts/hello.ts"
  }

  @Setup fun patchStdout() {
    System.setProperty("elide.disableStreams", "true")
    originalOut = System.out
    originalErr = System.err
    System.setOut(PrintStream(object : PrintStream(originalOut) {
      override fun println(x: Any?) {
        // Do nothing
      }
    }))
    System.setErr(PrintStream(object : PrintStream(originalErr) {
      override fun println(x: Any?) {
        // Do nothing
      }
    }))
  }

  @Setup fun unpatchStdout() {
    System.setOut(originalOut)
    System.setErr(originalErr)
  }

  @Benchmark fun help(): Int {
    return elide.tool.cli.entry(arrayOf("--help"), installStatics = false)
  }

  @Benchmark fun exit(): Int {
    return elide.tool.cli.entry(arrayOf("--exit"), installStatics = false)
  }

  @Benchmark fun js(): Int {
    return elide.tool.cli.entry(arrayOf("run", jsHello), installStatics = false).also {
      assert(it == 0)
    }
  }

  @Benchmark fun ts(): Int {
    return elide.tool.cli.entry(arrayOf("run", tsHello), installStatics = false).also {
      assert(it == 0)
    }
  }
}
