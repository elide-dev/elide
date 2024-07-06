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

import io.micronaut.configuration.picocli.PicocliRunner
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the main CLI tool entrypoint. */
@TestCase class ElideToolTest {
  private val rootProjectPath = Paths.get(System.getProperty("user.dir"))
    .parent
    .parent

  private val testScriptsPath = rootProjectPath
    .resolve("tools")
    .resolve("scripts")
    .toAbsolutePath()

  @Inject lateinit var tool: Elide

  @Test fun testEntrypoint() {
    assertNotNull(tool, "should be able to init and inject entrypoint")
  }

  @Test fun testEntrypointHelp() {
    assertDoesNotThrow {
      assertEquals(0, PicocliRunner.execute(Elide::class.java, "--help"))
    }
  }

  @Test fun testEntrypointVersion() {
    assertDoesNotThrow {
      assertEquals(0, PicocliRunner.execute(Elide::class.java, "--version"))
    }
  }

  @Test fun testRootEntrypointExecuteJsCode() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(Elide::class.java, "run", "-c", "'console.log(\"Hello!\");'"),
      )
    }
  }

  @Test fun testRootEntrypointExecuteJsFile() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.js").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertEquals(
        0,
        // `elide tools/scripts/hello.js`
        PicocliRunner.execute(Elide::class.java, scriptPath.toString()),
      )
    }
  }

  @Ignore @Test fun testRootEntrypointExecutePyFile() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.py").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertEquals(
        0,
        // `elide tools/scripts/hello.py`
        PicocliRunner.execute(Elide::class.java, scriptPath.toString()),
      )
    }
  }

  @Test fun testRootEntrypointExecuteJsFileWithRun() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.js").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertEquals(
        0,
        // `elide run tools/scripts/hello.js`
        PicocliRunner.execute(Elide::class.java, "run", scriptPath.toString()),
      )
    }
  }

  @Ignore @Test fun testRootEntrypointExecutePyFileWithRun() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.py").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertEquals(
        0,
        // `elide run tools/scripts/hello.py`
        PicocliRunner.execute(Elide::class.java, "run", scriptPath.toString()),
      )
    }
  }

  @Test fun testRootEntrypointExecuteJsFileExplicit() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.js").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertEquals(
        0,
        // `elide run --javascript tools/scripts/hello.js`
        PicocliRunner.execute(Elide::class.java, "run", "--javascript", scriptPath.toString()),
      )
    }
  }

  @Ignore @Test fun testRootEntrypointExecutePyFileExplicit() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.py").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertEquals(
        0,
        // `elide run --python tools/scripts/hello.py`
        PicocliRunner.execute(Elide::class.java, "run", "--python", scriptPath.toString()),
      )
    }
  }

  @Test fun testRootEntrypointExecutePyFileAlias() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.py").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertEquals(
        0,
        // `elide python tools/scripts/hello.py`
        PicocliRunner.execute(Elide::class.java, "python", scriptPath.toString()),
      )
    }
  }

  @Test fun testEntrypointExecuteSimpleJsExplicit() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(Elide::class.java, "run", "--javascript", "-c", "'console.log(\"Hello!\");'"),
      )
    }
  }

  @Test fun testEntrypointExecuteSimplePyCode() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(Elide::class.java, "run", "--python", "-c", "'print(\"Hello!\")'"),
      )
    }
  }
}
