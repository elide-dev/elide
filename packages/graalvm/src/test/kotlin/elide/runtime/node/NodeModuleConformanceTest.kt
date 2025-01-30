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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.node

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.streams.asStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.gvm.internals.AbstractDualTest
import elide.runtime.intrinsics.GuestIntrinsic

/**
 * # Node Conformance Test
 *
 * Extends the default JavaScript module test profile to include a Node conformance test; in this mode, the guest code
 * is run against the embedded Elide guest, and also against Node, so the output can be compared.
 */
internal abstract class NodeModuleConformanceTest<T: GuestIntrinsic> : GenericJsModuleTest<T>() {
  companion object {
    private const val NODE_BIN = "node"
  }

  /** Proxy which wires together a dual-test execution (in the guest and on the host) with a conformance test. */
  abstract inner class ConformanceTestExecutionProxy : DualTestExecutionProxy<JavaScript>()

  /**
   * Whether this is a built-in Node module implementation.
   */
  open val nodeBuiltin: Boolean get() = true

  // Run a Node.js subprocess with the provided inputs.
  private fun runConformanceInner(
      args: List<String>,
      stdin: String? = null,
      env: Map<String, String>? = null,
      bin: String = NODE_BIN,
  ): String {
    // use dynamic path separator by os
    val sysPath = (System.getenv("PATH") ?: "").split(File.pathSeparator ?: ":")
    val nodePath = sysPath.map { Paths.get(it, bin) }.firstOrNull {
      Files.exists(it) && !Files.isDirectory(it) && Files.isExecutable(it)
    } ?: throw IllegalStateException("Node.js binary not found in PATH")

    val finalArgs = listOf(nodePath.toAbsolutePath().toString()).plus(args)
    val processBuilder = ProcessBuilder(finalArgs)
      .redirectErrorStream(true)

    if (env != null) {
      processBuilder.environment().putAll(env)
    }
    val process = processBuilder.start()

    if (stdin != null) {
      process.outputStream.write(stdin.toByteArray())
      process.outputStream.close()
    }

    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()

    val stdinSplit = (stdin ?: "(None)").trimIndent().split("\n")

    // look for `[stdin]:<line>:<col>` in the stacktrace and extract the line number
    val capturedStdinLine = output
      .split("\n")
      .firstOrNull { it.contains("[stdin]:") }?.split(":")?.getOrNull(1)

    val renderedStdin = stdinSplit
      .mapIndexed { index, s ->
        if (index == 0 && s.trim().replace("\n", "").isEmpty()) {
          ""
        } else if (index == stdinSplit.lastIndex && s.trim().replace("\n", "").isEmpty()) {
          ""
        } else {
          "${if (index == 0) "" else "        "}${index + 1} | ${s.trimEnd()}"
        }
      }
      .joinToString("\n") { "  $it" }

    val finalCmd = StringBuilder().apply {
      append("  ")
      if (!stdin.isNullOrBlank()) {
        append("(code) | ")
      }
      append(finalArgs.joinToString(" "))
    }

    val finalLineMarked = if (capturedStdinLine.isNullOrBlank()) renderedStdin else {
      renderedStdin.replace("  $capturedStdinLine |", "→ $capturedStdinLine |")
    }

    if (process.exitValue() != 0) {
      val message = buildString {
        appendLine(
          """Node.js conformance test failed with exit code ${process.exitValue()}.

        Command:
        $finalCmd

        Code:
        $finalLineMarked

        Output from Node:
          ${output.split("\n").first().trim()}
          ${output.split("\n").drop(1).joinToString("\n") { "          $it" }}
        """.trimIndent(),
        )
      }

      throw AssertionError(message)
    }
    assertNotNull(
      output,
      "Node.js conformance test failed with no output",
    )
    return if (output.endsWith("\n")) output.dropLast(1) else output
  }

  // Run a conformance test against Node JS.
  private fun runConformance(code: String, moduleCode: Pair<String, String>? = null): String {
    return if (moduleCode != null) {
      // create a tempdir
      val tmpdir = Files.createTempDirectory("elide-node-module-conformance-test")
      tmpdir.toFile().deleteOnExit()

      // write code to file
      val testScript = tmpdir.resolve("test.js")
      Files.write(testScript, code.toByteArray())

      // create a local `node_modules` dir
      val nodeModules = tmpdir.resolve("node_modules")
      Files.createDirectory(nodeModules)

      // create a directory for this module
      val moduleDir = nodeModules.resolve(moduleName)
      Files.createDirectory(moduleDir)

      // create a package.json for this module
      val pkgJson = mapOf(
        "name" to JsonPrimitive(moduleName),
        "version" to JsonPrimitive("1.0.0"),
        "main" to JsonPrimitive("index.cjs"),
        "module" to JsonPrimitive("index.mjs"),
        "type" to JsonPrimitive("module"),
        "exports" to JsonObject(mapOf(
          "." to JsonObject(mapOf(
            "import" to JsonPrimitive("./index.mjs"),
            "require" to JsonPrimitive("./index.cjs")
          )
        )))
      )

      Files.write(
        moduleDir.resolve("package.json"),
        Json.encodeToString<Map<String, JsonElement>>(pkgJson).toByteArray(StandardCharsets.UTF_8),
      )

      // write the code to the module
      val (cjs, esm) = moduleCode
      Files.write(moduleDir.resolve("index.cjs"), cjs.toByteArray())
      Files.write(moduleDir.resolve("index.mjs"), esm.toByteArray())
      runConformanceInner(
        listOf(testScript.toString()),
      )
    } else {
      // if we are testing a built-in module with no custom sources, we can pass the code directly to node via stdin.
      runConformanceInner(
        emptyList(),
        code,
      )
    }
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun conforms(op: suspend () -> Unit): ConformanceTestExecutionProxy = conforms(true, op)

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun conforms(bind: Boolean, op: suspend () -> Unit): ConformanceTestExecutionProxy {
    val dual = dual(bind, op)

    return object : ConformanceTestExecutionProxy() {
      override fun guest(guestOperation: JavaScript) {
        val code = guestOperation.invoke(polyglotContext)
        dual.guest(guestOperation)
        runConformance(code)
      }

      override fun thenRun(guestOperation: JavaScript): AbstractDualTest<JavaScript>.GuestTestExecution {
        return dual.thenRun(guestOperation)
      }
    }
  }

  fun node(@Language("js") code: String): String {
    val trimmed = code.trimIndent()
    val rendered = StringBuilder().apply {
      appendLine("""
        function output(value) {
          console.log(value);
        }
      """.trimIndent())
      appendLine(trimmed)
    }
    return runConformance(rendered.toString())
  }

  @TestFactory open fun `node api - require(mod) prefixed should specify expected members`(): Stream<DynamicTest> {
    return requiredMembers().map { member ->
      DynamicTest.dynamicTest("$moduleName.$member") {
        val keys = require("node:$moduleName").memberKeys
        if (!expectCompliance() && !keys.contains(member)) {
          Assumptions.abort<Unit>("not yet compliant for member '$member' (module: 'node:$moduleName')")
        } else {
          assertContains(keys, member, "member '$member' should be present on module 'node:$moduleName'")
        }
      }
    }.asStream()
  }

  @Test fun `should be able to require() builtin module (node prefix)`() {
    require("node:$moduleName")
  }

  @Test fun `should be able to import builtin module (node prefix)`() {
    import("node:$moduleName")
  }
}
