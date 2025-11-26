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
package elide.tooling.runner

import java.nio.file.Path
import kotlin.test.*
import elide.tooling.runner.ProcessRunner.ProcessOptions
import elide.tooling.runner.ProcessRunner.ProcessShell

class ProcessRunnerTest {
  @Test fun `ProcessOptions should allow specifying a custom working directory`() {
    val customDir = Path.of("/tmp")
    val options = ProcessOptions(
      shell = ProcessShell.None,
      workingDirectory = customDir,
    )

    assertEquals(customDir, options.workingDirectory)
  }

  @Test fun `ProcessRunner build should accept a custom working directory`() {
    val customDir = Path.of("/tmp")
    val exec = Path.of("/bin/ls")

    val builder = ProcessRunner.build(exec) {
      options = ProcessOptions(
        shell = ProcessShell.None,
        workingDirectory = customDir,
      )
    }

    assertEquals(customDir, builder.options.workingDirectory)
    assertEquals(exec, builder.executable)
  }

  @Test fun `ProcessRunner build should allow changing the working directory after building`() {
    val initialDir = Path.of("/tmp")
    val newDir = Path.of("/var")
    val exec = Path.of("/bin/echo")

    val builder = ProcessRunner.build(exec) {
      options = ProcessOptions(
        shell = ProcessShell.None,
        workingDirectory = initialDir,
      )
    }

    assertEquals(initialDir, builder.options.workingDirectory)

    // Change the working directory
    builder.options = ProcessOptions(
      shell = ProcessShell.None,
      workingDirectory = newDir,
    )

    assertEquals(newDir, builder.options.workingDirectory)
  }

  @Test fun `ProcessRunner buildFrom should use current working directory by default`() {
    val exec = Path.of("/bin/echo")
    val args = elide.tooling.Arguments.empty()
    val env = elide.tooling.Environment.empty()

    val builder = ProcessRunner.buildFrom(exec, args, env)

    assertNotNull(builder.options.workingDirectory)
    assertEquals(Path.of(System.getProperty("user.dir")), builder.options.workingDirectory)
  }

  @Test fun `ProcessRunner buildFrom should accept custom working directory`() {
    val exec = Path.of("/bin/echo")
    val args = elide.tooling.Arguments.empty()
    val env = elide.tooling.Environment.empty()
    val customDir = Path.of("/tmp")
    val customOptions = ProcessOptions(
      shell = ProcessShell.None,
      workingDirectory = customDir,
    )

    val builder = ProcessRunner.buildFrom(exec, args, env, options = customOptions)

    assertEquals(customDir, builder.options.workingDirectory)
  }
}
