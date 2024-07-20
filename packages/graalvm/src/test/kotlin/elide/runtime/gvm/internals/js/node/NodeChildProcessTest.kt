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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.js.node

import org.graalvm.polyglot.Value.asValue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.node.childProcess.NodeChildProcess
import elide.runtime.gvm.internals.node.childProcess.NodeChildProcessModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.ChildProcessAPI
import elide.runtime.intrinsics.js.node.childProcess.*
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols.IGNORE
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols.INHERIT
import elide.testing.annotations.TestCase

/** Testing for Node's built-in `child_process` module. */
@TestCase internal class NodeChildProcessTest : NodeModuleConformanceTest<NodeChildProcessModule>() {
  private val pathToBin = "/bin/echo"
  @Inject internal lateinit var childProcess: ChildProcessAPI
  @Inject internal lateinit var module: NodeChildProcessModule

  override fun provide(): NodeChildProcessModule = module
  override val moduleName: String get() = "child_process"

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("ChildProcess")
    yield("exec")
    yield("execSync")
    yield("execFile")
    yield("execFileSync")
    yield("fork")
    yield("spawn")
    yield("spawnSync")
  }

  // Access to the implementation module.
  private fun childProc(): NodeChildProcess = assertNotNull(childProcess as? NodeChildProcess)

  @Test override fun testInjectable() {
    assertNotNull(childProcess, "should be able to inject host-side `child_process` module")
  }

  @Test fun `sanity - process spawn`() {
    val proc = ProcessBuilder(listOf("echo", "hello")).start()
    val exit = proc.waitFor()
    assertEquals(0, exit)
    val result = proc.inputStream.bufferedReader().readText()
    assertEquals("hello\n", result)
  }

  @TestFactory fun `execSync - options properties`(): Stream<DynamicTest> = sequence {
    val defaults = ExecSyncOptions.DEFAULTS

    // defaults
    yield(dynamicTest("default should stringify without error") {
      assertNotNull(assertNotNull(defaults).toString())
    })
    yield(dynamicTest("default should correctly equal itself") {
      assertEquals(defaults, defaults)
    })
    yield(dynamicTest("default should correctly not equal a modified copy") {
      assertNotEquals(defaults, defaults.copy(encoding = "some-non-default"))
    })
    yield(dynamicTest("default `uid` should be `null`") {
      assertNull(defaults.uid)
    })
    yield(dynamicTest("default `gid` should be `null`") {
      assertNull(defaults.gid)
    })
    yield(dynamicTest("default `cwdUrl` should be `null`") {
      assertNull(defaults.cwdUrl)
    })
    yield(dynamicTest("default `cwdString` should be `null`") {
      assertNull(defaults.cwdString)
    })
    yield(dynamicTest("default `input` should be `null`") {
      assertNull(defaults.input)
    })
    yield(dynamicTest("default `shell` should be `null`") {
      assertNull(defaults.shell)
    })
    yield(dynamicTest("default `env` should be `null`") {
      assertNull(defaults.env)
    })
    yield(dynamicTest("default `timeout` should be `null`") {
      assertNull(defaults.timeout)
    })
    yield(dynamicTest("default `encoding` should be non-`null`") {
      assertEquals(ChildProcessDefaults.ENCODING, assertNotNull(defaults.encoding))
    })
    yield(dynamicTest("default `windowsHide` should be non-`null`") {
      assertEquals(ChildProcessDefaults.WINDOWS_HIDE, assertNotNull(defaults.windowsHide))
    })
    yield(dynamicTest("default `maxBufferSize` should be non-`null`") {
      assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, assertNotNull(defaults.maxBuffer))
    })
    yield(dynamicTest("default `killSignal` should be non-`null`") {
      assertEquals(ChildProcessDefaults.SIGNAL_SIGKILL, assertNotNull(defaults.killSignal))
    })

    // from self/`null`
    yield(dynamicTest("settings should be inflatable from self") {
      assertEquals(assertNotNull(defaults), ExecSyncOptions.from(asValue(defaults)))
    })
    yield(dynamicTest("settings should default from host `null`") {
      assertEquals(assertNotNull(defaults), ExecSyncOptions.from(null))
    })
    yield(dynamicTest("settings should default from guest `null`") {
      assertEquals(assertNotNull(defaults), ExecSyncOptions.from(asValue(null)))
    })
  }.asStream()

  @Test fun `execSync - should reject unbalanced double quotes`() {
    assertThrows<Throwable> {
      childProc().hostExecSync("echo \"hello", ExecSyncOptions.of(encoding = "utf8"))
    }
    assertThrows<Throwable> {
      childProc().execSync(asValue("echo \"hello"), null)
    }
  }

  @Test fun `execSync - should reject unbalanced single quotes`() {
    assertThrows<Throwable> { childProc().hostExecSync("echo 'hello", ExecSyncOptions.of(encoding = "utf8")) }
    assertThrows<Throwable> { childProc().execSync(asValue("echo 'hello"), null) }
  }

  @Test fun `execSync - invalid argument throws a TypeError`() {
    assertThrows<TypeError> { childProc().execSync(asValue(5), null) }
    assertThrows<TypeError> { childProc().execSync(asValue(true), null) }
  }

  @Test fun `execSync - host simple process spawn`() {
    val result = assertNotNull(childProc().hostExecSync("echo hello", ExecSyncOptions.DEFAULTS))
    assertIs<ByteArray>(result)
  }

  @Test fun `execSync - host simple process spawn with null encoding`() {
    val result = assertNotNull(childProc().hostExecSync("echo hello", ExecSyncOptions.DEFAULTS.copy(
      encoding = null,
    )))
    assertIs<ByteArray>(result)
  }

  @Test fun `execSync - host with inherited stdout and stderr`() {
    assertNull(childProc().hostExecSync("echo hello", ExecSyncOptions.DEFAULTS.copy(
      stdio = StdioConfig.DEFAULTS.copy(stdout = INHERIT, stderr = INHERIT),
    )))
  }

  @Test fun `execSync - host with discarded stdout and stderr`() {
    assertNull(childProc().hostExecSync("echo hello", ExecSyncOptions.DEFAULTS.copy(
      stdio = StdioConfig.DEFAULTS.copy(stdout = IGNORE, stderr = IGNORE),
    )))
  }

  @Test fun `execSync - host with inherited stdout but piped stderr`() {
    assertNull(childProc().hostExecSync("echo hello", ExecSyncOptions.DEFAULTS.copy(
      stdio = StdioConfig.DEFAULTS.copy(stdout = INHERIT),
    )))
  }

  @Test fun `execSync - host simple process spawn with utf8`() {
    val result = assertNotNull(childProc().hostExecSync("echo hello", ExecSyncOptions.of(encoding = "utf8")))
    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execSync - host simple process with double quoted args`() {
    val result = assertNotNull(childProc().hostExecSync("echo \"hello world\"", ExecSyncOptions.of(
      encoding = "utf8",
    )))
    assertIs<String>(result)
    assertEquals("hello world\n", result)
  }

  @Test fun `execSync - host simple process with single quoted args`() {
    val result = assertNotNull(childProc().hostExecSync("echo 'hello world'", ExecSyncOptions.of(
      encoding = "utf8",
    )))
    assertIs<String>(result)
    assertEquals("hello world\n", result)
  }

  @Test fun `execSync - with shell enabled as 'bash'`() {
    val result = assertNotNull(childProc().hostExecSync("echo \"hello\"", ExecSyncOptions.of(
      encoding = "utf8",
      shell = "bash",
    )))
    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execSync - host with injected environment value`() {
    val result = assertNotNull(childProc().hostExecSync("echo \"${'$'}ENV_SUBPROC_TEST\"", ExecSyncOptions.of(
      encoding = "utf8",
      shell = "bash",
      env = mapOf("ENV_SUBPROC_TEST" to "hi"),
    )))
    assertIs<String>(result)
    assertEquals("hi\n", result)
  }

  @Test fun `execSync - host with withheld environment value`() {
    val result = assertNotNull(childProc().hostExecSync("echo \"${'$'}JAVA_HOME\"", ExecSyncOptions.of(
      encoding = "utf8",
      shell = "bash",
      env = emptyMap(),
    )))
    assertIs<String>(result)
    assertEquals("\n", result)
  }

  @Test fun `execSync - guest simple process spawn without options`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      execSync("echo hello");
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execSync - guest simple process spawn`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      execSync("echo hello", {});
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execSync - guest simple process spawn with utf8`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      const result = execSync("echo hello", { encoding: "utf8" });
      test(result).equals("hello\n");
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execSync - guest simple process with double quoted args`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      const result = execSync(`echo "hello world"`, { encoding: "utf8" });
      test(result).equals("hello world\n");
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execSync - guest simple process with single quoted args`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      const result = execSync(`echo 'hello world'`, { encoding: "utf8" });
      test(result).equals("hello world\n");
    """.trimIndent()
  }.doesNotFail()

  // ---

  @Test fun `execFileSync - host simple process spawn`() {
    val result = assertNotNull(childProc().execFileSync(asValue(pathToBin), asValue(arrayOf("hello")), null))
    assertIs<ByteArray>(result)
  }

  @Test fun `execFileSync - host simple process spawn with utf8`() {
    val result = assertNotNull(childProc().execFileSync(
      asValue(pathToBin),
      asValue(arrayOf("hello")),
      asValue(ExecSyncOptions.of(encoding = "utf8"))))

    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execFileSync - should not need arg quoting`() {
    val result = assertNotNull(childProc().execFileSync(
      asValue(pathToBin),
      asValue(arrayOf("hello world")),
      asValue(ExecSyncOptions.of(encoding = "utf8"))))

    assertIs<String>(result)
    assertEquals("hello world\n", result)
  }

  @Test fun `execFileSync - host simple passthrough for double quoted args`() {
    val result = assertNotNull(childProc().execFileSync(
      asValue(pathToBin),
      asValue(arrayOf("\"hello world\"")),
      asValue(ExecSyncOptions.of(encoding = "utf8"))))

    assertIs<String>(result)
    assertEquals("\"hello world\"\n", result)
  }

  @Test fun `execFileSync - host simple passthrough for single quoted args`() {
    val result = assertNotNull(childProc().execFileSync(
      asValue(pathToBin),
      asValue(arrayOf("'hello world'")),
      asValue(ExecSyncOptions.of(encoding = "utf8"))))

    assertIs<String>(result)
    assertEquals("'hello world'\n", result)
  }

  @Test fun `execFileSync - with shell enabled as 'bash'`() {
    val result = assertNotNull(childProc().execFileSync(
      asValue(pathToBin),
      asValue(arrayOf("hello")),
      asValue(ExecSyncOptions.of(encoding = "utf8", shell = "bash"))))

    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execFileSync - guest simple process spawn with no options`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      execFileSync("$pathToBin", ["hello"]);
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      execFileSync("$pathToBin", ["hello"], {});
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn with and utf8`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      const result = execFileSync("$pathToBin", ["hello"], { encoding: "utf8" });
      test(result).equals("hello\n");
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn with and utf8 and bash as shell`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      const result = execFileSync("$pathToBin", ["hello"], { encoding: "utf8", shell: "bash" });
      test(result).equals("hello\n");
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn with no args and utf8`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      const result = execFileSync("$pathToBin", { encoding: "utf8" });
      test(result).equals("\n");
    """.trimIndent()
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn with no args and utf8 and bash as shell`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      const result = execFileSync("$pathToBin", { encoding: "utf8", shell: "bash" });
      test(result).equals("\n");
    """.trimIndent()
  }.doesNotFail()

  // ---

  @TestFactory fun `spawnSync - options properties`(): Stream<DynamicTest> = sequence {
    val defaults = SpawnSyncOptions.DEFAULTS

    // defaults
    yield(dynamicTest("default should stringify without error") {
      assertNotNull(assertNotNull(defaults).toString())
    })
    yield(dynamicTest("default should correctly equal itself") {
      assertEquals(defaults, defaults)
    })
    yield(dynamicTest("default should correctly not equal a modified copy") {
      assertNotEquals(defaults, defaults.copy(encoding = "some-non-default"))
    })
    yield(dynamicTest("default `uid` should be `null`") {
      assertNull(defaults.uid)
    })
    yield(dynamicTest("default `gid` should be `null`") {
      assertNull(defaults.gid)
    })
    yield(dynamicTest("default `cwdUrl` should be `null`") {
      assertNull(defaults.cwdUrl)
    })
    yield(dynamicTest("default `cwdString` should be `null`") {
      assertNull(defaults.cwdString)
    })
    yield(dynamicTest("default `input` should be `null`") {
      assertNull(defaults.input)
    })
    yield(dynamicTest("default `shell` should be `null`") {
      assertNull(defaults.shell)
    })
    yield(dynamicTest("default `env` should be `null`") {
      assertNull(defaults.env)
    })
    yield(dynamicTest("default `timeout` should be `null`") {
      assertNull(defaults.timeout)
    })
    yield(dynamicTest("default `encoding` should be non-`null`") {
      assertEquals(ChildProcessDefaults.ENCODING, assertNotNull(defaults.encoding))
    })
    yield(dynamicTest("default `windowsHide` should be non-`null`") {
      assertEquals(ChildProcessDefaults.WINDOWS_HIDE, assertNotNull(defaults.windowsHide))
    })
    yield(dynamicTest("default `maxBufferSize` should be non-`null`") {
      assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, assertNotNull(defaults.maxBuffer))
    })
    yield(dynamicTest("default `killSignal` should be non-`null`") {
      assertEquals(ChildProcessDefaults.SIGNAL_SIGTERM, assertNotNull(defaults.killSignal))
    })

    // from self/`null`
    yield(dynamicTest("settings should be inflatable from self") {
      assertEquals(assertNotNull(defaults), SpawnSyncOptions.from(asValue(defaults)))
    })
    yield(dynamicTest("settings should default from host `null`") {
      assertEquals(assertNotNull(defaults), SpawnSyncOptions.from(null))
    })
    yield(dynamicTest("settings should default from guest `null`") {
      assertEquals(assertNotNull(defaults), SpawnSyncOptions.from(asValue(null)))
    })
  }.asStream()

  @Test fun `spawnSync - host simple process spawn`() {
    val result = assertNotNull(childProc().spawnSync(
      asValue("echo"),
      asValue(arrayOf("hello")),
      null))
    assertIs<ChildProcessSync>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exitCode = assertNotNull(result.status)
    assertEquals(0, exitCode)
  }

  @Test fun `spawnSync - host simple process spawn with utf8`() {
    val result = assertNotNull(childProc().spawnSync(
      asValue("echo"),
      asValue(arrayOf("hello")),
      asValue(SpawnSyncOptions.of(encoding = "utf8"))))
    assertIs<ChildProcessSync>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exitCode = assertNotNull(result.status)
    assertEquals(0, exitCode)
  }

  @Test fun `spawnSync - with shell enabled as 'bash'`() {
    val result = assertNotNull(childProc().spawnSync(
      asValue("echo"),
      asValue(arrayOf("hello")),
      asValue(SpawnSyncOptions.of(encoding = "utf8", shell = "bash"))))
    assertIs<ChildProcessSync>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exitCode = assertNotNull(result.status)
    assertEquals(0, exitCode)
  }

  @Test fun `spawnSync - guest simple process spawn with no options`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      spawnSync("echo", ["hello"]);
    """.trimIndent()
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      spawnSync("echo", ["hello"], {});
    """.trimIndent()
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn with and utf8`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      const result = spawnSync("echo", ["hello"], { encoding: "utf8" });
      test(result.pid).isNotNull();
    """.trimIndent()
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn with and utf8 and bash as shell`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      const result = spawnSync("echo", ["hello"], { encoding: "utf8", shell: "bash" });
      test(result.pid).isNotNull();
    """.trimIndent()
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn with no args and utf8`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      const result = spawnSync("echo", { encoding: "utf8" });
      test(result.pid).isNotNull();
    """.trimIndent()
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn with no args and utf8 and bash as shell`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      const result = spawnSync("echo", { encoding: "utf8", shell: "bash" });
      test(result.pid).isNotNull();
    """.trimIndent()
  }.doesNotFail()

  // ---

  @TestFactory fun `exec - options properties`(): Stream<DynamicTest> = sequence {
    val defaults = ExecOptions.DEFAULTS

    // defaults
    yield(dynamicTest("default should stringify without error") {
      assertNotNull(assertNotNull(defaults).toString())
    })
    yield(dynamicTest("default should correctly equal itself") {
      assertEquals(defaults, defaults)
    })
    yield(dynamicTest("default should correctly not equal a modified copy") {
      assertNotEquals(defaults, defaults.copy(encoding = "some-non-default"))
    })
    yield(dynamicTest("default `uid` should be `null`") {
      assertNull(defaults.uid)
    })
    yield(dynamicTest("default `gid` should be `null`") {
      assertNull(defaults.gid)
    })
    yield(dynamicTest("default `cwdUrl` should be `null`") {
      assertNull(defaults.cwdUrl)
    })
    yield(dynamicTest("default `cwdString` should be `null`") {
      assertNull(defaults.cwdString)
    })
    yield(dynamicTest("default `shell` should be `null`") {
      assertNull(defaults.shell)
    })
    yield(dynamicTest("default `env` should be `null`") {
      assertNull(defaults.env)
    })
    yield(dynamicTest("default `timeout` should be `null`") {
      assertNull(defaults.timeout)
    })
    yield(dynamicTest("default `encoding` should be non-`null`") {
      assertEquals(ChildProcessDefaults.ENCODING, assertNotNull(defaults.encoding))
    })
    yield(dynamicTest("default `windowsHide` should be non-`null`") {
      assertEquals(ChildProcessDefaults.WINDOWS_HIDE, assertNotNull(defaults.windowsHide))
    })
    yield(dynamicTest("default `maxBufferSize` should be non-`null`") {
      assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, assertNotNull(defaults.maxBuffer))
    })
    yield(dynamicTest("default `killSignal` should be non-`null`") {
      assertEquals(ChildProcessDefaults.SIGNAL_SIGKILL, assertNotNull(defaults.killSignal))
    })

    // from self/`null`
    yield(dynamicTest("settings should be inflatable from self") {
      assertEquals(assertNotNull(defaults), ExecOptions.from(asValue(defaults)))
    })
    yield(dynamicTest("settings should default from host `null`") {
      assertEquals(assertNotNull(defaults), ExecOptions.from(null))
    })
    yield(dynamicTest("settings should default from guest `null`") {
      assertEquals(assertNotNull(defaults), ExecOptions.from(asValue(null)))
    })
  }.asStream()

  @Test fun `exec - host simple process spawn`() {
    val result = assertNotNull(childProc().exec(asValue("echo hello"), null, null))
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  @Test fun `exec - host simple process spawn with utf8`() {
    val result = assertNotNull(childProc().exec(
      asValue("echo hello"),
      asValue(ExecOptions.of(encoding = "utf8")),
      null))
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  @Test fun `exec - with shell enabled as 'bash'`() {
    val result = assertNotNull(childProc().exec(
      asValue("echo hello"),
      asValue(ExecOptions.of(encoding = "utf8", shell = "bash")),
      null))
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  // ---

  @TestFactory fun `spawn - options properties`(): Stream<DynamicTest> = sequence {
    val defaults = SpawnOptions.DEFAULTS

    // defaults
    yield(dynamicTest("default should stringify without error") {
      assertNotNull(assertNotNull(defaults).toString())
    })
    yield(dynamicTest("default should correctly equal itself") {
      assertEquals(defaults, defaults)
    })
    yield(dynamicTest("default should correctly not equal a modified copy") {
      assertNotEquals(defaults, defaults.copy(encoding = "some-non-default"))
    })
    yield(dynamicTest("default `uid` should be `null`") {
      assertNull(defaults.uid)
    })
    yield(dynamicTest("default `gid` should be `null`") {
      assertNull(defaults.gid)
    })
    yield(dynamicTest("default `cwdUrl` should be `null`") {
      assertNull(defaults.cwdUrl)
    })
    yield(dynamicTest("default `cwdString` should be `null`") {
      assertNull(defaults.cwdString)
    })
    yield(dynamicTest("default `shell` should be `null`") {
      assertNull(defaults.shell)
    })
    yield(dynamicTest("default `env` should be `null`") {
      assertNull(defaults.env)
    })
    yield(dynamicTest("default `timeout` should be `null`") {
      assertNull(defaults.timeout)
    })
    yield(dynamicTest("default `encoding` should be non-`null`") {
      assertEquals(ChildProcessDefaults.ENCODING, assertNotNull(defaults.encoding))
    })
    yield(dynamicTest("default `windowsHide` should be non-`null`") {
      assertEquals(ChildProcessDefaults.WINDOWS_HIDE, assertNotNull(defaults.windowsHide))
    })
    yield(dynamicTest("default `maxBufferSize` should be non-`null`") {
      assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, assertNotNull(defaults.maxBuffer))
    })
    yield(dynamicTest("default `killSignal` should be non-`null`") {
      assertEquals(ChildProcessDefaults.SIGNAL_SIGKILL, assertNotNull(defaults.killSignal))
    })

    // from self/`null`
    yield(dynamicTest("settings should be inflatable from self") {
      assertEquals(assertNotNull(defaults), SpawnOptions.from(asValue(defaults)))
    })
    yield(dynamicTest("settings should default from host `null`") {
      assertEquals(assertNotNull(defaults), SpawnOptions.from(null))
    })
    yield(dynamicTest("settings should default from guest `null`") {
      assertEquals(assertNotNull(defaults), SpawnOptions.from(asValue(null)))
    })
  }.asStream()

  @Test fun `spawn - host simple process spawn`() {
    val result = assertNotNull(childProc().spawn(asValue("echo hello"), null, null))
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  @Test fun `spawn - host simple process spawn with utf8`() {
    val result = assertNotNull(childProc().spawn(
      asValue("echo"),
      asValue(arrayOf("hello")),
      asValue(SpawnOptions.of(encoding = "utf8"))))
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  @Test fun `spawn - with shell enabled as 'bash'`() {
    val result = assertNotNull(childProc().spawn(
      asValue("echo"),
      asValue(arrayOf("hello")),
      asValue(SpawnOptions.of(encoding = "utf8", shell = "bash"))))
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }
}
