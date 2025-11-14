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

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Files
import kotlin.test.assertNotNull
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the main CLI tool entrypoint. */
@TestCase class ElideToolTest : AbstractEntryTest() {
  @Test fun testEntrypoint() {
    assertNotNull(tool, "should be able to init and inject entrypoint")
  }

  @Test fun testEntrypointHelp() {
    assertDoesNotThrow {
      assertToolRunsWith("--help")
    }
  }

  @Test fun testEntrypointVersion() {
    assertDoesNotThrow {
      assertToolRunsWith("--version")
    }
  }

  @Test fun testRootEntrypointExecuteJsCode() {
    assertDoesNotThrow {
      assertToolRunsWith(
        "run",
        "-c",
        "'console.log(\"Hello!\");'",
      )
    }
  }

  @Test fun testRootEntrypointExecuteTsCode() {
    assertDoesNotThrow {
      assertToolRunsWith(
        "run",
        "--typescript",
        "-c",
        "'console.log(\"Hello!\");'",
      )
    }
  }

  @Test fun testRootEntrypointExecuteJsFile() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.js").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertToolExitsWithCode(
        0,
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecuteTsFile() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.ts").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertToolExitsWithCode(
        0,
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecutePyFile() {
    Assumptions.assumeTrue(testPython)
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.py").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertToolRunsWith(
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecuteRbFile() {
    Assumptions.assumeTrue(testRuby)
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.rb").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertToolRunsWith(
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecuteJsFileWithRun() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.js").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertToolRunsWith(
        "run",
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecutePyFileWithRun() {
    Assumptions.assumeTrue(testPython)
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.py").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertToolRunsWith(
        "run",
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecuteRbFileWithRun() {
    Assumptions.assumeTrue(testRuby)
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.rb").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      assertToolRunsWith(
        "run",
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecuteJsFileExplicit() {
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.js").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      // `elide run --javascript tools/scripts/hello.js`
      assertToolRunsWith(
        "run",
        "--javascript",
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecutePyFileExplicit() {
    Assumptions.assumeTrue(testPython)
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.py").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      // `elide run --python tools/scripts/hello.py`
      assertToolRunsWith(
        "run",
        "--python",
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecuteRbFileExplicit() {
    Assumptions.assumeTrue(testRuby)
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.rb").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      // `elide run --ruby tools/scripts/hello.rb`
      assertToolRunsWith(
        "run",
        "--ruby",
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecutePyFileAlias() {
    Assumptions.assumeTrue(testPython)
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.py").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      // `elide python tools/scripts/hello.py`
      assertToolRunsWith(
        "python",
        scriptPath.toString(),
      )
    }
  }

  @Test fun testRootEntrypointExecuteRbFileAlias() {
    Assumptions.assumeTrue(testRuby)
    Assumptions.assumeTrue(Files.exists(testScriptsPath))
    val scriptPath = testScriptsPath.resolve("hello.rb").toAbsolutePath()
    Assumptions.assumeTrue(Files.exists(scriptPath))

    assertDoesNotThrow {
      // `elide ruby tools/scripts/hello.rb`
      assertToolRunsWith(
        "ruby",
        scriptPath.toString(),
      )
    }
  }

  @Test fun testEntrypointExecuteSimpleJsExplicit() {
    assertDoesNotThrow {
      assertToolRunsWith(
        "run",
        "--javascript",
        "-c",
        "'console.log(\"Hello!\");'",
      )
    }
  }

  @Test fun testEntrypointExecuteSimplePyCode() {
    assertDoesNotThrow {
      assertToolRunsWith(
        "run",
        "--python",
        "-c",
        "'print(\"Hello!\")'",
      )
    }
  }

  @Test fun testEntrypointExecuteSimpleRbCode() {
    assertDoesNotThrow {
      assertToolRunsWith(
        "run",
        "--ruby",
        "-c",
        "'puts \"Hello!\"'",
      )
    }
  }

  @Test fun testEntrypointS3() {
    assertDoesNotThrow {
      assertToolRunsWith(
        "s3",
        "--in-memory",
        "--shutdown-after",
        "5"
      )
    }
  }
}
