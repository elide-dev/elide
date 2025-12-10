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
package elide.tool.exec

import java.nio.file.Path
import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import elide.tool.cli.CommandContext
import elide.tool.cli.state.CommandOptions
import elide.tool.cli.state.CommandState
import elide.tool.exec.SubprocessRunner.stringToTask
import elide.tool.exec.SubprocessRunner.subprocess
import elide.tooling.runner.ProcessRunner

class SubprocessRunnerTest {
  private fun testContext(): CommandContext {
    val options = CommandOptions.of(
      args = emptyList(),
      debug = false,
      verbose = false,
      quiet = false,
      pretty = false,
    )
    val state = CommandState.of(options)
    return CommandContext.default(state, Dispatchers.Default)
  }

  @Test fun `stringToTask should use current working directory by default`() = runTest {
    val expectedCwd = Path.of(System.getProperty("user.dir"))
    val context = testContext()

    with(context) {
      val builder = stringToTask("echo test")
      assertEquals(expectedCwd, builder.options.workingDirectory)
    }
  }

  @Test fun `stringToTask should accept custom working directory`() = runTest {
    val customDir = Path.of("/tmp")
    val context = testContext()

    with(context) {
      val builder = stringToTask("echo test", workingDirectory = customDir)
      assertEquals(customDir, builder.options.workingDirectory)
    }
  }

  @Test fun `should use current working directory by default when creating subprocess`() {
    val expectedCwd = Path.of(System.getProperty("user.dir"))
    val context = testContext()
    val exec = Path.of("/bin/echo")

    with(context) {
      val builder = subprocess(exec) {}
      assertEquals(expectedCwd, builder.options.workingDirectory)
    }
  }

  @Test fun `should be able to pass custom working directory when creating subprocess`() {
    val customDir = Path.of("/var")
    val context = testContext()
    val exec = Path.of("/bin/ls")

    with(context) {
      val builder = subprocess(exec, workingDirectory = customDir) {}
      assertEquals(customDir, builder.options.workingDirectory)
    }
  }

  @Test fun `should be able to pass custom working directory through to ProcessRunner options`() {
    val customDir = Path.of("/usr")
    val context = testContext()
    val exec = Path.of("/bin/pwd")

    with(context) {
      val builder = subprocess(exec, workingDirectory = customDir) {
        args.addAllStrings(listOf("arg1"))
      }

      assertEquals(customDir, builder.options.workingDirectory)
      assertEquals(exec, builder.executable)
      assertTrue(builder.args.asArgumentList().contains("arg1"))
    }
  }
}
