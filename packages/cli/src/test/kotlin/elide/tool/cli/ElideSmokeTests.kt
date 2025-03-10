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

package elide.tool.cli

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.asStream
import elide.testing.annotations.TestCase

@TestCase class ElideSmokeTests : AbstractEntryTest() {
  @JvmRecord private data class TestInvocation(
    val file: Path,
    val extraArgs: List<String> = emptyList(),
  ) {
    fun args(): List<String> = listOf(
      "run",
      "--verbose",
      "--host:allow-env",
      "--env:dotenv"
    ) + extraArgs + listOf(
      file.absolutePathString(),
    )
  }

  private companion object {
    @JvmStatic private fun testFile(file: String): TestInvocation = TestInvocation(
      file = testScriptsPath.resolve(file)
    )

    // All tests to run.
    private val allTests = listOf(
      testFile("hello.js"),
      testFile("hello.ts"),
      testFile("hello.py"),
      testFile("paths.cjs"),
      testFile("paths.mjs"),
      testFile("paths-default.mjs"),
      testFile("cpu.js"),
      testFile("fs.mjs"),
      testFile("fs-async.mjs"),
      testFile("brotli.mjs"),
      testFile("sqlite.cjs"),
      testFile("sqlite.mjs"),
      testFile("sqlite.ts"),
      testFile("sqlite.mts"),
      testFile("stdlib.cjs"),
      testFile("stdlib.mjs"),
      testFile("simple.js"),
      testFile("args.py"),
      testFile("env.py"),
      testFile("env.js"),
      testFile("fetch.mjs"),
    )

    private val knownBroken = sortedSetOf(
      "brotli",
      "stdlib",
      "paths-default",
    )
  }

  @TestFactory fun `smoke test`(): Stream<DynamicTest> = sequence<DynamicTest> {
    allTests.forEach {
      yield(DynamicTest.dynamicTest(it.file.name) {
        val isPython = it.file.name.endsWith(".py")
        val isRuby = it.file.name.endsWith(".rb")
        Assumptions.assumeTrue(
          (testPython && isPython) || (testRuby && isRuby) || (!isPython && !isRuby)
        )
        Assumptions.assumeTrue(
          it.file.nameWithoutExtension !in knownBroken
        )

        assertToolExitsWithCode(0, *it.args().toTypedArray())
      })
    }
  }.asStream()
}
