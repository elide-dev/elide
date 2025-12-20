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
    val baseArgs: List<String>,
    val extraArgs: List<String> = emptyList(),
  ) {
    fun args(): List<String> = baseArgs + extraArgs + listOf(
      file.absolutePathString(),
    )
  }

  private companion object {
    @JvmStatic private fun testFile(file: String, args: List<String>? = null): TestInvocation = TestInvocation(
      file = testScriptsPath.resolve(file),
      baseArgs = args ?: listOf(
        "run",
        "--verbose",
        "--host:allow-env",
        "--env:dotenv",
      ),
    )

    // All tests to run.
    private val allTests = listOf(
      testFile("args.py"),
      testFile("brotli.mjs"),
      testFile("cpu.js"),
      testFile("env.js"),
      testFile("env.py"),
      testFile("env.rb"),
      testFile("fetch.mjs"),
      testFile("fs.mjs"),
      testFile("fs-async.mjs"),
      testFile("fs-promises.mjs"),
      testFile("Hello.java"),
      testFile("main.kts"),
      testFile("main-another.kts"),
      testFile("hello.py"),
      testFile("hello.rb"),
      testFile("hello.js"),
      testFile("hello.swift"),
      testFile("hello.swift.wasm"),
      testFile("hello.ts"),
      testFile("hello-fn.mts"),
      testFile("hello-import.mts"),
      testFile("hello-import-js.mts"),
      testFile("import-prefix-test.ts"),
      testFile("paths.cjs"),
      testFile("paths.mjs"),
      testFile("paths-default.mjs"),
      testFile("react-imports.mjs"),
      testFile("react-render.tsx"),
      testFile("say_hello.mts"),
      testFile("say_hello_raw.mts"),
      testFile("simple.js"),
      testFile("sqlite.mts"),
      testFile("sqlite.cjs"),
      testFile("sqlite.mjs"),
      testFile("sqlite.ts"),
      testFile("sqlite-json.mts"),
      testFile("stdlib.cjs"),
      testFile("stdlib.mjs"),
      testFile("py_json.py"),
      testFile("fibgen.mts"),
      testFile("sample.test.mts", listOf("test")),
    )

    private val knownBroken = sortedSetOf<String>(
      "main.kts",
      "main-another.kts",
      "react-imports.mjs",  // requires react/react-dom npm packages
      "react-render.tsx",   // requires react/react-dom npm packages
    )
  }

  @TestFactory fun `smoke test`(): Stream<DynamicTest> = sequence<DynamicTest> {
    allTests.forEach {
      yield(DynamicTest.dynamicTest(it.file.name) {
        val isPython = it.file.name.endsWith(".py")
        val isRuby = it.file.name.endsWith(".rb")
        val isJava = it.file.name.endsWith(".java")
        val isKotlin = it.file.name.endsWith(".kts") || it.file.name.endsWith(".kt")
        val isSwift = it.file.name.endsWith(".swift")
        val isWasm = it.file.name.endsWith(".wasm")

        Assumptions.assumeTrue(
          (testPython && isPython) ||
          (testRuby && isRuby) ||
          (testJava && isJava) ||
          (testKotlin && isKotlin) ||
          (testSwift && isSwift) ||
          (testWasm && isWasm) ||
          (!isPython && !isRuby && !isJava && !isKotlin && !isSwift && !isWasm)
        )
        Assumptions.assumeTrue(
          it.file.nameWithoutExtension !in knownBroken && it.file.name !in knownBroken
        )

        assertToolExitsWithCode(0, *it.args().toTypedArray())
      })
    }
  }.asStream()
}
