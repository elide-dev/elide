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
@file:Suppress("LongMethod", "LargeClass", "JSUnresolvedReference", "JSUnusedLocalSymbols")

package elide.runtime.node

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.Value.asValue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.InputStream
import java.io.OutputStream
import java.util.stream.Stream
import kotlin.time.Clock
import kotlin.reflect.full.isSubclassOf
import kotlin.streams.asStream
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.ChildProcessAPI
import elide.runtime.intrinsics.js.node.childProcess.*
import elide.runtime.intrinsics.js.node.childProcess.ChildProcessDefaults.decodeEnvMap
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols.IGNORE
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols.INHERIT
import elide.runtime.node.childProcess.*
import elide.testing.annotations.TestCase

/** Testing for Node's built-in `child_process` module. */
@TestCase internal class NodeChildProcessTest : NodeModuleConformanceTest<NodeChildProcessModule>() {
  private val pathToBin = arrayOf("", "bin", "echo").joinToString("/")
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

  @Test fun `child process event names`() {
    assertNotNull(ChildProcessEvents.toString())
    assertEquals("close", assertNotNull(ChildProcessEvents.CLOSE))
    assertEquals("exit", assertNotNull(ChildProcessEvents.EXIT))
    assertEquals("error", assertNotNull(ChildProcessEvents.ERROR))
    assertEquals("message", assertNotNull(ChildProcessEvents.MESSAGE))
    assertEquals("disconnect", assertNotNull(ChildProcessEvents.DISCONNECT))
  }

  @Test fun `standard streams provider`() {
    (object : StandardStreamsProvider {}).let {
      assertNull(it.stdin())
      assertNull(it.stdout())
      assertNull(it.stderr())
    }
    (object : StandardStreamsProvider {
      override fun stderr(): OutputStream? = System.err
      override fun stdin(): InputStream? = System.`in`
      override fun stdout(): OutputStream? = System.out
    }).let {
      assertNotNull(it.stdin())
      assertNotNull(it.stdout())
      assertNotNull(it.stderr())
    }
  }

  @Test fun `env map - guest null`() {
    assertNull(decodeEnvMap(asValue(null)))
  }

  @Test fun `env map - empty host type`() {
    val map = emptyMap<String, String>()
    val decoded = assertNotNull(decodeEnvMap(asValue(map)))
    assertTrue(decoded.isEmpty())
  }

  @Test fun `env map - non-empty host type`() {
    val map = mapOf("EXAMPLE" to "hello")
    val decoded = assertNotNull(decodeEnvMap(asValue(map)))
    assertTrue(decoded.isNotEmpty())
    assertEquals("hello", decoded["EXAMPLE"])
  }

  @Test fun `env map - invalid host type`() {
    assertThrows<TypeError> { decodeEnvMap(asValue(false)) }
  }

  @Test fun `env map - invalid guest type`() {
    executeGuest {
      // language=JavaScript
      """
        5
      """
    }.thenAssert {
      val value = assertNotNull(it.returnValue())
      assertThrows<TypeError> { decodeEnvMap(value) }
    }
  }

  @Test fun `sanity - process spawn`() {
    val proc = ProcessBuilder(listOf("echo", "hello")).start()
    val exit = proc.waitFor()
    assertEquals(0, exit)
    val result = proc.inputStream.bufferedReader().readText()
    assertEquals("hello\n", result)
  }

  // Test option objects used by the `child_process` module.
  private suspend inline fun <reified T : ProcOptions> SequenceScope<DynamicTest>.testProcOptionsType(
    crossinline optionsFromGuest: (Value) -> T,
    crossinline assertDefaults: (T, Set<String>) -> Unit,
  ) {
    yield(
      dynamicTest("from invalid type") {
        executeGuest {
          // language=JavaScript
          """
            5
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertThrows<TypeError> { optionsFromGuest(value) }
        }
      },
    )

    // cwd string
    yield(
      dynamicTest("cwd (string)") {
        executeGuest {
          // language=JavaScript
          """
            ({cwd: '/some/path'})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertNotNull(options.cwdString)
          assertEquals("/some/path", options.cwdString)
          assertDefaults(options, setOf("cwdString", "cwdUrl"))
        }
      },
    )

    yield(
      dynamicTest("cwd (invalid type)") {
        executeGuest {
          // language=JavaScript
          """
            ({cwd: true})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertNull(options.cwdString)
          assertDefaults(options, emptySet())
        }
      },
    )

    // encoding
    yield(
      dynamicTest("encoding") {
        executeGuest {
          // language=JavaScript
          """
            ({encoding: 'utf-8'})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertEquals("utf-8", options.encoding)
          assertDefaults(options, setOf("encoding"))
        }
      },
    )

    yield(
      dynamicTest("encoding (invalid type)") {
        executeGuest {
          // language=JavaScript
          """
            ({encoding: true})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertEquals(ChildProcessDefaults.ENCODING, options.encoding)
          assertDefaults(options, emptySet())
        }
      },
    )

    // shell
    yield(
      dynamicTest("shell") {
        executeGuest {
          // language=JavaScript
          """
            ({shell: 'bash'})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertEquals("bash", options.shell)
          assertDefaults(options, setOf("shell"))
        }
      },
    )

    // shell (invalid type)
    yield(
      dynamicTest("shell (invalid type)") {
        executeGuest {
          // language=JavaScript
          """
            ({shell: 5})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertDefaults(options, emptySet())
        }
      },
    )

    // uid
    if (T::class.isSubclassOf(IdentityProcOptions::class)) yield(
      dynamicTest("uid") {
        executeGuest {
          // language=JavaScript
          """
            ({uid: 1001})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertIs<IdentityProcOptions>(options)
          assertEquals(1001, options.uid)
          assertDefaults(options, setOf("uid"))
        }
      },
    )

    // uid (invalid type)
    if (T::class.isSubclassOf(IdentityProcOptions::class)) yield(
      dynamicTest("uid (invalid type)") {
        executeGuest {
          // language=JavaScript
          """
            ({uid: true})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertDefaults(options, emptySet())
        }
      },
    )

    // gid
    if (T::class.isSubclassOf(IdentityProcOptions::class)) yield(
      dynamicTest("gid") {
        executeGuest {
          // language=JavaScript
          """
            ({gid: 1001})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertIs<IdentityProcOptions>(options)
          assertEquals(1001, options.gid)
          assertDefaults(options, setOf("gid"))
        }
      },
    )

    // gid (invalid type)
    if (T::class.isSubclassOf(IdentityProcOptions::class)) yield(
      dynamicTest("gid (invalid type)") {
        executeGuest {
          // language=JavaScript
          """
            ({gid: true})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertDefaults(options, emptySet())
        }
      },
    )

    // timeout
    yield(
      dynamicTest("timeout") {
        executeGuest {
          // language=JavaScript
          """
            ({timeout: 3000})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertEquals(3000, options.timeout?.inWholeMilliseconds)
          assertDefaults(options, setOf("timeout"))
        }
      },
    )

    // timeout (invalid type)
    yield(
      dynamicTest("timeout (invalid type)") {
        executeGuest {
          // language=JavaScript
          """
            ({timeout: true})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertDefaults(options, emptySet())
        }
      },
    )

    // kill signal
    if (T::class == ExecOptions::class) yield(
      dynamicTest("killSignal") {
        executeGuest {
          // language=JavaScript
          """
            ({killSignal: "sample"})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertIs<ExecOptions>(options)
          assertEquals("sample", options.killSignal)
          assertDefaults(options, setOf("killSignal"))
        }
      },
    )

    // kill signal (invalid type)
    yield(
      dynamicTest("killSignal (invalid type)") {
        executeGuest {
          // language=JavaScript
          """
            ({killSignal: true})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertDefaults(options, emptySet())
        }
      },
    )

    // env
    yield(
      dynamicTest("env") {
        executeGuest {
          // language=JavaScript
          """
            ({env: {TEST: "hello"}})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertNotNull(options.env)
          assertTrue(options.env!!.isNotEmpty())
          assertEquals("hello", options.env!!["TEST"])
          assertDefaults(options, setOf("env"))
        }
      },
    )

    // env (null)
    yield(
      dynamicTest("env (null)") {
        executeGuest {
          // language=JavaScript
          """
            ({env: null})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertNull(options.env)
          assertDefaults(options, setOf("env"))
        }
      },
    )

    // env (empty)
    yield(
      dynamicTest("env (empty)") {
        executeGuest {
          // language=JavaScript
          """
            ({env: {}})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertNotNull(options.env)
          assertTrue(options.env!!.isEmpty())
          assertDefaults(options, setOf("env"))
        }
      },
    )

    // maxBuffer
    yield(
      dynamicTest("maxBuffer") {
        executeGuest {
          // language=JavaScript
          """
            ({maxBuffer: 1000})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertNotNull(options.maxBuffer)
          assertEquals(1000, options.maxBuffer)
          assertDefaults(options, setOf("maxBuffer"))
        }
      },
    )

    // maxBuffer (invalid type)
    yield(
      dynamicTest("maxBuffer (invalid type)") {
        executeGuest {
          // language=JavaScript
          """
            ({maxBuffer: true})
        """
        }.thenAssert {
          val value = assertNotNull(it.returnValue())
          assertTrue(value.hasMembers())
          val options = assertNotNull(assertDoesNotThrow { optionsFromGuest(value) })
          assertNotNull(options.maxBuffer)
          assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, options.maxBuffer)
          assertDefaults(options, emptySet())
        }
      },
    )
  }

  @TestFactory fun `execSync - options properties`(): Stream<DynamicTest> = sequence {
    val defaults = ExecSyncOptions.DEFAULTS
    assertNotNull(ExecSyncDefaults.toString())

    // defaults
    yield(
      dynamicTest("default should stringify without error") {
        assertNotNull(assertNotNull(defaults).toString())
      },
    )
    yield(
      dynamicTest("default should correctly equal itself") {
        assertEquals(defaults, defaults)
      },
    )
    yield(
      dynamicTest("default should correctly not equal a modified copy") {
        assertNotEquals(defaults, defaults.copy(encoding = "some-non-default"))
      },
    )
    yield(
      dynamicTest("default `uid` should be `null`") {
        assertNull(defaults.uid)
      },
    )
    yield(
      dynamicTest("default `gid` should be `null`") {
        assertNull(defaults.gid)
      },
    )
    yield(
      dynamicTest("default `cwdUrl` should be `null`") {
        assertNull(defaults.cwdUrl)
      },
    )
    yield(
      dynamicTest("default `cwdString` should be `null`") {
        assertNull(defaults.cwdString)
      },
    )
    yield(
      dynamicTest("default `input` should be `null`") {
        assertNull(defaults.input)
      },
    )
    yield(
      dynamicTest("default `shell` should be `null`") {
        assertNull(defaults.shell)
      },
    )
    yield(
      dynamicTest("default `env` should be `null`") {
        assertNull(defaults.env)
      },
    )
    yield(
      dynamicTest("default `timeout` should be `null`") {
        assertNull(defaults.timeout)
      },
    )
    yield(
      dynamicTest("default `encoding` should be non-`null`") {
        assertEquals(ChildProcessDefaults.ENCODING, assertNotNull(defaults.encoding))
      },
    )
    yield(
      dynamicTest("default `windowsHide` should be non-`null`") {
        assertEquals(ChildProcessDefaults.WINDOWS_HIDE, assertNotNull(defaults.windowsHide))
      },
    )
    yield(
      dynamicTest("default `maxBufferSize` should be non-`null`") {
        assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, assertNotNull(defaults.maxBuffer))
      },
    )
    yield(
      dynamicTest("default `killSignal` should be non-`null`") {
        assertEquals(ChildProcessDefaults.SIGNAL_SIGKILL, assertNotNull(defaults.killSignal))
      },
    )

    // from self/`null`
    yield(
      dynamicTest("settings should be inflatable from self") {
        assertEquals(assertNotNull(defaults), ExecSyncOptions.from(asValue(defaults)))
      },
    )
    yield(
      dynamicTest("settings should default from host `null`") {
        assertEquals(assertNotNull(defaults), ExecSyncOptions.from(null))
      },
    )
    yield(
      dynamicTest("settings should default from guest `null`") {
        assertEquals(assertNotNull(defaults), ExecSyncOptions.from(asValue(null)))
      },
    )
  }.asStream()

  @TestFactory fun `execSync - options properties from guest`(): Stream<DynamicTest> = sequence {
    testProcOptionsType<ExecSyncOptions>({ ExecSyncOptions.from(it) }) { options, skip ->
      if ("uid" !in skip) assertNull(options.uid)
      if ("gid" !in skip) assertNull(options.gid)
      if ("cwdUrl" !in skip) assertNull(options.cwdUrl)
      if ("cwdString" !in skip) assertNull(options.cwdString)
      if ("shell" !in skip) assertNull(options.shell)
      if ("env" !in skip) assertNull(options.env)
      if ("timeout" !in skip) assertNull(options.timeout)
      if ("killSignal" !in skip) assertNotNull(options.killSignal)
      if ("encoding" !in skip) assertEquals(ChildProcessDefaults.ENCODING, options.encoding)
      assertEquals(options, options)
      assertEquals(options, options.copy())
      assertNotNull(options.toString())
    }
  }.asStream()

  @Test fun `execSync - run with timeout which does not trigger`() {
    val result = assertNotNull(
      childProc().hostExecSync(
        "echo hello",
        ExecSyncOptions.DEFAULTS
          .withTimeout(5.seconds)
          .copy(encoding = "utf8"),
      ),
    )

    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execSync - run with timeout which triggers`() {
    assertThrows<ChildProcessTimeout> {
      assertNotNull(
        childProc().hostExecSync(
          "sleep 5",
          ExecSyncOptions.DEFAULTS
            .withTimeout(2.seconds)
            .copy(encoding = "utf8"),
        ),
      )
    }.let { exception ->
      assertNotNull(exception.message)
    }
  }

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
    val result = assertNotNull(
      childProc().hostExecSync(
        "echo hello",
        ExecSyncOptions.DEFAULTS.copy(
          encoding = null,
        ),
      ),
    )
    assertIs<ByteArray>(result)
  }

  @Test fun `execSync - host with inherited stdout and stderr`() {
    assertNull(
      childProc().hostExecSync(
        "echo hello",
        ExecSyncOptions.DEFAULTS.copy(
          stdio = StdioConfig.DEFAULTS.copy(stdout = INHERIT, stderr = INHERIT),
        ),
      ),
    )
  }

  @Test fun `execSync - host with discarded stdout and stderr`() {
    assertNull(
      childProc().hostExecSync(
        "echo hello",
        ExecSyncOptions.DEFAULTS.copy(
          stdio = StdioConfig.DEFAULTS.copy(stdout = IGNORE, stderr = IGNORE),
        ),
      ),
    )
  }

  @Test fun `execSync - host with inherited stdout but piped stderr`() {
    assertNull(
      childProc().hostExecSync(
        "echo hello",
        ExecSyncOptions.DEFAULTS.copy(
          stdio = StdioConfig.DEFAULTS.copy(stdout = INHERIT),
        ),
      ),
    )
  }

  @Test fun `execSync - host simple process spawn with utf8`() {
    val result = assertNotNull(childProc().hostExecSync("echo hello", ExecSyncOptions.of(encoding = "utf8")))
    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execSync - host simple process with double quoted args`() {
    val result = assertNotNull(
      childProc().hostExecSync(
        "echo \"hello world\"",
        ExecSyncOptions.of(
          encoding = "utf8",
        ),
      ),
    )
    assertIs<String>(result)
    assertEquals("hello world\n", result)
  }

  @Test fun `execSync - host simple process with single quoted args`() {
    val result = assertNotNull(
      childProc().hostExecSync(
        "echo 'hello world'",
        ExecSyncOptions.of(
          encoding = "utf8",
        ),
      ),
    )
    assertIs<String>(result)
    assertEquals("hello world\n", result)
  }

  @Test fun `execSync - with shell enabled as 'bash'`() {
    val result = assertNotNull(
      childProc().hostExecSync(
        "echo \"hello\"",
        ExecSyncOptions.of(
          encoding = "utf8",
          shell = "bash",
        ),
      ),
    )
    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execSync - host with injected environment value`() {
    val result = assertNotNull(
      childProc().hostExecSync(
        "echo \"${'$'}ENV_SUBPROC_TEST\"",
        ExecSyncOptions.of(
          encoding = "utf8",
          shell = "bash",
          env = mapOf("ENV_SUBPROC_TEST" to "hi"),
        ),
      ),
    )
    assertIs<String>(result)
    assertEquals("hi\n", result)
  }

  @Test fun `execSync - host with withheld environment value`() {
    val result = assertNotNull(
      childProc().hostExecSync(
        "echo \"${'$'}JAVA_HOME\"",
        ExecSyncOptions.of(
          encoding = "utf8",
          shell = "bash",
          env = emptyMap(),
        ),
      ),
    )
    assertIs<String>(result)
    assertEquals("\n", result)
  }

  @Test fun `execSync - guest simple process spawn without options`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      execSync("echo hello");
    """
  }.doesNotFail()

  @Test fun `execSync - guest simple process spawn`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      execSync("echo hello", {});
    """
  }.doesNotFail()

  @Test fun `execSync - guest simple process spawn with utf8`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      const result = execSync("echo hello", { encoding: "utf8" });
      test(result).equals("hello\n");
    """
  }.doesNotFail()

  @Test fun `execSync - guest simple process with double quoted args`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      const result = execSync(`echo "hello world"`, { encoding: "utf8" });
      test(result).equals("hello world\n");
    """
  }.doesNotFail()

  @Test fun `execSync - guest simple process with single quoted args`() = executeGuest {
    // language=JavaScript
    """
      const { execSync } = require("node:child_process");
      const result = execSync(`echo 'hello world'`, { encoding: "utf8" });
      test(result).equals("hello world\n");
    """
  }.doesNotFail()

  // ---

  @Test fun `execFileSync - host simple process spawn`() {
    val result = assertNotNull(childProc().execFileSync(asValue(pathToBin), asValue(arrayOf("hello")), null))
    assertIs<ByteArray>(result)
  }

  @Test fun `execFileSync - host simple process spawn with utf8`() {
    val result = assertNotNull(
      childProc().execFileSync(
        asValue(pathToBin),
        asValue(arrayOf("hello")),
        asValue(ExecSyncOptions.of(encoding = "utf8")),
      ),
    )

    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execFileSync - should not need arg quoting`() {
    val result = assertNotNull(
      childProc().execFileSync(
        asValue(pathToBin),
        asValue(arrayOf("hello world")),
        asValue(ExecSyncOptions.of(encoding = "utf8")),
      ),
    )

    assertIs<String>(result)
    assertEquals("hello world\n", result)
  }

  @Test fun `execFileSync - host simple passthrough for double quoted args`() {
    val result = assertNotNull(
      childProc().execFileSync(
        asValue(pathToBin),
        asValue(arrayOf("\"hello world\"")),
        asValue(ExecSyncOptions.of(encoding = "utf8")),
      ),
    )

    assertIs<String>(result)
    assertEquals("\"hello world\"\n", result)
  }

  @Test fun `execFileSync - host simple passthrough for single quoted args`() {
    val result = assertNotNull(
      childProc().execFileSync(
        asValue(pathToBin),
        asValue(arrayOf("'hello world'")),
        asValue(ExecSyncOptions.of(encoding = "utf8")),
      ),
    )

    assertIs<String>(result)
    assertEquals("'hello world'\n", result)
  }

  @Test fun `execFileSync - with shell enabled as 'bash'`() {
    val result = assertNotNull(
      childProc().execFileSync(
        asValue(pathToBin),
        asValue(arrayOf("hello")),
        asValue(ExecSyncOptions.of(encoding = "utf8", shell = "bash")),
      ),
    )

    assertIs<String>(result)
    assertEquals("hello\n", result)
  }

  @Test fun `execFileSync - guest simple process spawn with no options`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      execFileSync("$pathToBin", ["hello"]);
    """
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      execFileSync("$pathToBin", ["hello"], {});
    """
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn with and utf8`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      const result = execFileSync("$pathToBin", ["hello"], { encoding: "utf8" });
      test(result).equals("hello\n");
    """
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn with and utf8 and bash as shell`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      const result = execFileSync("$pathToBin", ["hello"], { encoding: "utf8", shell: "bash" });
      test(result).equals("hello\n");
    """
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn with no args and utf8`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      const result = execFileSync("$pathToBin", { encoding: "utf8" });
      test(result).equals("\n");
    """
  }.doesNotFail()

  @Test fun `execFileSync - guest simple process spawn with no args and utf8 and bash as shell`() = executeGuest {
    // language=JavaScript
    """
      const { execFileSync } = require("node:child_process");
      const result = execFileSync("$pathToBin", { encoding: "utf8", shell: "bash" });
      test(result).equals("\n");
    """
  }.doesNotFail()

  // ---

  @Test fun `spawnSync - invalid command type`() {
    assertThrows<TypeError> { childProc().spawnSync(asValue(true), null, null) }
    assertThrows<TypeError> { childProc().spawnSync(asValue(5), null, null) }
  }

  @TestFactory fun `spawnSync - options properties`(): Stream<DynamicTest> = sequence {
    val defaults = SpawnSyncOptions.DEFAULTS
    assertNotNull(SpawnSyncDefaults.toString())

    // defaults
    yield(
      dynamicTest("default should stringify without error") {
        assertNotNull(assertNotNull(defaults).toString())
      },
    )
    yield(
      dynamicTest("default should correctly equal itself") {
        assertEquals(defaults, defaults)
      },
    )
    yield(
      dynamicTest("default should correctly not equal a modified copy") {
        assertNotEquals(defaults, defaults.copy(encoding = "some-non-default"))
      },
    )
    yield(
      dynamicTest("default `uid` should be `null`") {
        assertNull(defaults.uid)
      },
    )
    yield(
      dynamicTest("default `gid` should be `null`") {
        assertNull(defaults.gid)
      },
    )
    yield(
      dynamicTest("default `cwdUrl` should be `null`") {
        assertNull(defaults.cwdUrl)
      },
    )
    yield(
      dynamicTest("default `cwdString` should be `null`") {
        assertNull(defaults.cwdString)
      },
    )
    yield(
      dynamicTest("default `input` should be `null`") {
        assertNull(defaults.input)
      },
    )
    yield(
      dynamicTest("default `shell` should be `null`") {
        assertNull(defaults.shell)
      },
    )
    yield(
      dynamicTest("default `env` should be `null`") {
        assertNull(defaults.env)
      },
    )
    yield(
      dynamicTest("default `timeout` should be `null`") {
        assertNull(defaults.timeout)
      },
    )
    yield(
      dynamicTest("default `encoding` should be non-`null`") {
        assertEquals(ChildProcessDefaults.ENCODING, assertNotNull(defaults.encoding))
      },
    )
    yield(
      dynamicTest("default `windowsHide` should be non-`null`") {
        assertEquals(ChildProcessDefaults.WINDOWS_HIDE, assertNotNull(defaults.windowsHide))
      },
    )
    yield(
      dynamicTest("default `maxBufferSize` should be non-`null`") {
        assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, assertNotNull(defaults.maxBuffer))
      },
    )
    yield(
      dynamicTest("default `killSignal` should be non-`null`") {
        assertEquals(ChildProcessDefaults.SIGNAL_SIGTERM, assertNotNull(defaults.killSignal))
      },
    )

    // from self/`null`
    yield(
      dynamicTest("settings should be inflatable from self") {
        assertEquals(assertNotNull(defaults), SpawnSyncOptions.from(asValue(defaults)))
      },
    )
    yield(
      dynamicTest("settings should default from host `null`") {
        assertEquals(assertNotNull(defaults), SpawnSyncOptions.from(null))
      },
    )
    yield(
      dynamicTest("settings should default from guest `null`") {
        assertEquals(assertNotNull(defaults), SpawnSyncOptions.from(asValue(null)))
      },
    )
  }.asStream()

  @TestFactory fun `spawnSync - options properties from guest`(): Stream<DynamicTest> = sequence {
    testProcOptionsType<SpawnSyncOptions>({ SpawnSyncOptions.from(it) }) { options, skip ->
      if ("uid" !in skip) assertNull(options.uid)
      if ("gid" !in skip) assertNull(options.gid)
      if ("cwdUrl" !in skip) assertNull(options.cwdUrl)
      if ("cwdString" !in skip) assertNull(options.cwdString)
      if ("shell" !in skip) assertNull(options.shell)
      if ("env" !in skip) assertNull(options.env)
      if ("timeout" !in skip) assertNull(options.timeout)
      if ("encoding" !in skip) assertEquals(ChildProcessDefaults.ENCODING, options.encoding)
      assertEquals(options, options)
      assertEquals(options, options.copy())
      assertNotNull(options.toString())
    }
  }.asStream()

  @Test fun `spawnSync - host simple process spawn`() {
    val result = assertNotNull(
      childProc().spawnSync(
        asValue("echo"),
        asValue(arrayOf("hello")),
        null,
      ),
    )
    assertIs<ChildProcessSync>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exitCode = assertNotNull(result.status)
    assertEquals(0, exitCode)
  }

  @Test fun `spawnSync - simple process spawn result`() {
    val result = assertNotNull(
      childProc().spawnSync(
        asValue("echo"),
        asValue(arrayOf("hello")),
        asValue(SpawnSyncOptions.DEFAULTS.copy(encoding = "utf8")),
      ),
    )

    assertIs<ChildProcessSync>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertNotNull(result.stdout)
    assertNotNull(result.stderr)
    assertEquals(0, result.status)
    assertNull(result.signal)
  }

  @Test fun `spawnSync - consume from spawned stdout`() {
    val opts = SpawnSyncOptions.DEFAULTS.copy(encoding = "utf8")
    val result = assertNotNull(
      childProc().spawnSync(
        asValue("echo"),
        asValue(arrayOf("hello")),
        asValue(opts),
      ),
    )

    assertIs<ChildProcessSync>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertNotNull(result.stdout)
    assertNotNull(result.stderr)
    assertEquals(0, result.status)
    assertNull(result.signal)
    assertEquals("hello\n", result.stdout!!.readToStringOrBuffer(opts))
  }

  @Test fun `spawnSync - host simple process spawn with utf8`() {
    val result = assertNotNull(
      childProc().spawnSync(
        asValue("echo"),
        asValue(arrayOf("hello")),
        asValue(SpawnSyncOptions.of(encoding = "utf8")),
      ),
    )
    assertIs<ChildProcessSync>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exitCode = assertNotNull(result.status)
    assertEquals(0, exitCode)
  }

  @Test fun `spawnSync - with shell enabled as 'bash'`() {
    val result = assertNotNull(
      childProc().spawnSync(
        asValue("echo"),
        asValue(arrayOf("hello")),
        asValue(SpawnSyncOptions.of(encoding = "utf8", shell = "bash")),
      ),
    )
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
    """
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      spawnSync("echo", ["hello"], {});
    """
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn with and utf8`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      const result = spawnSync("echo", ["hello"], { encoding: "utf8" });
      test(result.pid).isNotNull();
    """
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn with and utf8 and bash as shell`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      const result = spawnSync("echo", ["hello"], { encoding: "utf8", shell: "bash" });
      test(result.pid).isNotNull();
    """
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn with no args and utf8`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      const result = spawnSync("echo", { encoding: "utf8" });
      test(result.pid).isNotNull();
    """
  }.doesNotFail()

  @Test fun `spawnSync - guest simple process spawn with no args and utf8 and bash as shell`() = executeGuest {
    // language=JavaScript
    """
      const { spawnSync } = require("node:child_process");
      const result = spawnSync("echo", { encoding: "utf8", shell: "bash" });
      test(result.pid).isNotNull();
    """
  }.doesNotFail()

  // ---

  @Test fun `exec - invalid command type`() {
    assertThrows<TypeError> { childProc().exec(asValue(true), null, null) }
    assertThrows<TypeError> { childProc().exec(asValue(5), null, null) }
  }

  @TestFactory fun `exec - options properties`(): Stream<DynamicTest> = sequence {
    val defaults = ExecOptions.DEFAULTS
    assertNotNull(ExecDefaults.toString())

    // defaults
    yield(
      dynamicTest("default should stringify without error") {
        assertNotNull(assertNotNull(defaults).toString())
      },
    )
    yield(
      dynamicTest("default should correctly equal itself") {
        assertEquals(defaults, defaults)
      },
    )
    yield(
      dynamicTest("default should correctly not equal a modified copy") {
        assertNotEquals(defaults, defaults.copy(encoding = "some-non-default"))
      },
    )
    yield(
      dynamicTest("default `uid` should be `null`") {
        assertNull(defaults.uid)
      },
    )
    yield(
      dynamicTest("default `gid` should be `null`") {
        assertNull(defaults.gid)
      },
    )
    yield(
      dynamicTest("default `cwdUrl` should be `null`") {
        assertNull(defaults.cwdUrl)
      },
    )
    yield(
      dynamicTest("default `cwdString` should be `null`") {
        assertNull(defaults.cwdString)
      },
    )
    yield(
      dynamicTest("default `shell` should be `null`") {
        assertNull(defaults.shell)
      },
    )
    yield(
      dynamicTest("default `env` should be `null`") {
        assertNull(defaults.env)
      },
    )
    yield(
      dynamicTest("default `timeout` should be `null`") {
        assertNull(defaults.timeout)
      },
    )
    yield(
      dynamicTest("default `encoding` should be non-`null`") {
        assertEquals(ChildProcessDefaults.ENCODING, assertNotNull(defaults.encoding))
      },
    )
    yield(
      dynamicTest("default `windowsHide` should be non-`null`") {
        assertEquals(ChildProcessDefaults.WINDOWS_HIDE, assertNotNull(defaults.windowsHide))
      },
    )
    yield(
      dynamicTest("default `maxBufferSize` should be non-`null`") {
        assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, assertNotNull(defaults.maxBuffer))
      },
    )
    yield(
      dynamicTest("default `killSignal` should be non-`null`") {
        assertEquals(ChildProcessDefaults.SIGNAL_SIGKILL, assertNotNull(defaults.killSignal))
      },
    )

    // from self/`null`
    yield(
      dynamicTest("settings should be inflatable from self") {
        assertEquals(assertNotNull(defaults), ExecOptions.from(asValue(defaults)))
      },
    )
    yield(
      dynamicTest("settings should default from host `null`") {
        assertEquals(assertNotNull(defaults), ExecOptions.from(null))
      },
    )
    yield(
      dynamicTest("settings should default from guest `null`") {
        assertEquals(assertNotNull(defaults), ExecOptions.from(asValue(null)))
      },
    )
  }.asStream()

  @TestFactory fun `exec - options properties from guest`(): Stream<DynamicTest> = sequence {
    testProcOptionsType<ExecOptions>({ ExecOptions.from(it) }) { options, skip ->
      if ("uid" !in skip) assertNull(options.uid)
      if ("gid" !in skip) assertNull(options.gid)
      if ("cwdUrl" !in skip) assertNull(options.cwdUrl)
      if ("cwdString" !in skip) assertNull(options.cwdString)
      if ("shell" !in skip) assertNull(options.shell)
      if ("env" !in skip) assertNull(options.env)
      if ("timeout" !in skip) assertNull(options.timeout)
      if ("encoding" !in skip) assertEquals(ChildProcessDefaults.ENCODING, options.encoding)
      assertEquals(options, options)
      assertEquals(options, options.copy())
      assertNotNull(options.toString())
    }
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

  @Test fun `exec - host simple process spawn with callback`() {
    executeGuest {
      // language=JavaScript
      """
        const fn = (err) => {
          if (err) throw err;
        };
        fn;
      """
    }.thenAssert {
      val cbk = assertNotNull(it.returnValue())
      assertTrue(cbk.canExecute())
      val result = assertNotNull(childProc().exec(asValue("echo hello"), null, cbk))
      assertIs<ChildProcess>(result)
      assertNotNull(result.pid)
      assertNotEquals(0, result.pid)
      assertTrue(result.pid > 0)
      val exit = assertNotNull(result.wait())
      assertEquals(0, exit)
    }
  }

  @Test fun `exec - host simple process spawn with utf8`() {
    val result = assertNotNull(
      childProc().exec(
        asValue("echo hello"),
        asValue(ExecOptions.of(encoding = "utf8")),
        null,
      ),
    )
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  @Test fun `exec - with shell enabled as 'bash'`() {
    val result = assertNotNull(
      childProc().exec(
        asValue("echo hello"),
        asValue(ExecOptions.of(encoding = "utf8", shell = "bash")),
        null,
      ),
    )
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  // ---

  @Test fun `spawn - invalid command type`() {
    assertThrows<TypeError> { childProc().spawn(asValue(true), null, null) }
    assertThrows<TypeError> { childProc().spawn(asValue(5), null, null) }
  }

  @Test fun `spawn - with timeout which does not trigger`() {
    val now = Clock.System.now()
    val proc = assertNotNull(
      childProc().spawn(
        asValue("sleep"),
        asValue(arrayOf("3")),
        asValue(SpawnOptions.of(timeoutSeconds = 10)),
      ),
    )

    val spawnTime = Clock.System.now() - now
    assertTrue(spawnTime < 2.seconds)
    assertNotNull(proc.pid)
    assertNotEquals(0, proc.pid)
    assertNotNull(proc.stdout)
    assertNotNull(proc.stdin)
    assertNotNull(proc.stderr)
  }

  @Test fun `spawn - with timeout which triggers`() {
    val now = Clock.System.now()
    val proc = assertNotNull(
      childProc().spawn(
        asValue("sleep"),
        asValue(arrayOf("30")),
        asValue(SpawnOptions.of(timeoutSeconds = 10)),
      ),
    )

    val spawnTime = Clock.System.now() - now
    assertTrue(spawnTime < 2.seconds)
    assertNull(proc.exitCode)
    assertNull(proc.signalCode)
    assertFalse(proc.killed)
    assertNotNull(proc.pid)
    assertNotEquals(0, proc.pid)
    assertNotNull(proc.stdout)
    assertNotNull(proc.stdin)
    assertNotNull(proc.stderr)
    assertThrows<ChildProcessTimeout> {
      assertEquals(0, proc.wait())
    }
  }

  @Test fun `spawn - start and then manually kill`() {
    val now = Clock.System.now()
    val proc = assertNotNull(
      childProc().spawn(
        asValue("sleep"),
        asValue(arrayOf("30")),
        asValue(null),
      ),
    )

    val spawnTime = Clock.System.now() - now
    assertTrue(spawnTime < 2.seconds)
    assertNotNull(proc.pid)
    assertNotEquals(0, proc.pid)
    assertNotNull(proc.stdout)
    assertNotNull(proc.stdin)
    assertNotNull(proc.stderr)
    assertDoesNotThrow { proc.kill() }
  }

  @TestFactory fun `spawn - options properties`(): Stream<DynamicTest> = sequence {
    val defaults = SpawnOptions.DEFAULTS
    assertNotNull(SpawnDefaults.toString())

    // defaults
    yield(
      dynamicTest("default should stringify without error") {
        assertNotNull(assertNotNull(defaults).toString())
      },
    )
    yield(
      dynamicTest("default should correctly equal itself") {
        assertEquals(defaults, defaults)
      },
    )
    yield(
      dynamicTest("default should correctly not equal a modified copy") {
        assertNotEquals(defaults, defaults.copy(encoding = "some-non-default"))
      },
    )
    yield(
      dynamicTest("default `uid` should be `null`") {
        assertNull(defaults.uid)
      },
    )
    yield(
      dynamicTest("default `gid` should be `null`") {
        assertNull(defaults.gid)
      },
    )
    yield(
      dynamicTest("default `cwdUrl` should be `null`") {
        assertNull(defaults.cwdUrl)
      },
    )
    yield(
      dynamicTest("default `cwdString` should be `null`") {
        assertNull(defaults.cwdString)
      },
    )
    yield(
      dynamicTest("default `shell` should be `null`") {
        assertNull(defaults.shell)
      },
    )
    yield(
      dynamicTest("default `env` should be `null`") {
        assertNull(defaults.env)
      },
    )
    yield(
      dynamicTest("default `timeout` should be `null`") {
        assertNull(defaults.timeout)
      },
    )
    yield(
      dynamicTest("default `encoding` should be non-`null`") {
        assertEquals(ChildProcessDefaults.ENCODING, assertNotNull(defaults.encoding))
      },
    )
    yield(
      dynamicTest("default `windowsHide` should be non-`null`") {
        assertEquals(ChildProcessDefaults.WINDOWS_HIDE, assertNotNull(defaults.windowsHide))
      },
    )
    yield(
      dynamicTest("default `maxBufferSize` should be non-`null`") {
        assertEquals(ChildProcessDefaults.MAX_BUFFER_DEFAULT, assertNotNull(defaults.maxBuffer))
      },
    )
    yield(
      dynamicTest("default `killSignal` should be non-`null`") {
        assertEquals(ChildProcessDefaults.SIGNAL_SIGKILL, assertNotNull(defaults.killSignal))
      },
    )

    // from self/`null`
    yield(
      dynamicTest("settings should be inflatable from self") {
        assertEquals(assertNotNull(defaults), SpawnOptions.from(asValue(defaults)))
      },
    )
    yield(
      dynamicTest("settings should default from host `null`") {
        assertEquals(assertNotNull(defaults), SpawnOptions.from(null))
      },
    )
    yield(
      dynamicTest("settings should default from guest `null`") {
        assertEquals(assertNotNull(defaults), SpawnOptions.from(asValue(null)))
      },
    )
  }.asStream()

  @TestFactory fun `spawn - options properties from guest`(): Stream<DynamicTest> = sequence {
    testProcOptionsType<SpawnOptions>({ SpawnOptions.from(it) }) { options, skip ->
      if ("uid" !in skip) assertNull(options.uid)
      if ("gid" !in skip) assertNull(options.gid)
      if ("cwdUrl" !in skip) assertNull(options.cwdUrl)
      if ("cwdString" !in skip) assertNull(options.cwdString)
      if ("shell" !in skip) assertNull(options.shell)
      if ("env" !in skip) assertNull(options.env)
      if ("timeout" !in skip) assertNull(options.timeout)
      if ("encoding" !in skip) assertEquals(ChildProcessDefaults.ENCODING, options.encoding)
      assertEquals(options, options)
      assertEquals(options, options.copy())
      assertNotNull(options.toString())
    }
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
    val result = assertNotNull(
      childProc().spawn(
        asValue("echo"),
        asValue(arrayOf("hello")),
        asValue(SpawnOptions.of(encoding = "utf8")),
      ),
    )
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  @Test fun `spawn - with shell enabled as 'bash'`() {
    val result = assertNotNull(
      childProc().spawn(
        asValue("echo"),
        asValue(arrayOf("hello")),
        asValue(SpawnOptions.of(encoding = "utf8", shell = "bash")),
      ),
    )
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  // ---

  @Test fun `execFile - host simple process spawn`() {
    val result = assertNotNull(
      childProc().execFile(
        asValue(pathToBin),
        asValue(arrayOf("hello")),
        null,
        null,
      ),
    )
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  @Test fun `execFile - host simple process spawn with utf8`() {
    val result = assertNotNull(
      childProc().execFile(
        asValue(pathToBin),
        asValue(arrayOf("hello")),
        asValue(ExecOptions.of(encoding = "utf8")),
        null,
      ),
    )
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }

  @Test fun `execFile - with shell enabled as 'bash'`() {
    val result = assertNotNull(
      childProc().execFile(
        asValue(pathToBin),
        asValue(arrayOf("hello")),
        asValue(ExecOptions.of(encoding = "utf8", shell = "bash")),
        null,
      ),
    )
    assertIs<ChildProcess>(result)
    assertNotNull(result.pid)
    assertNotEquals(0, result.pid)
    assertTrue(result.pid > 0)
    val exit = assertNotNull(result.wait())
    assertEquals(0, exit)
  }
}
