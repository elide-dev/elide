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

package elide.tool.cli.cmd.repl

import io.micronaut.configuration.picocli.PicocliRunner
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tool.cli.AbstractSubtoolTest
import elide.tool.cli.cmd.repl.ToolShellCommand

/** Tests for the Elide tool `shell`/`repl` subcommand. */
@TestCase class ToolShellSubcommandTest : AbstractSubtoolTest() {
  @Inject internal lateinit var shell: ToolShellCommand

  override fun subcommand(): Runnable = shell

  override fun runCommand() {
    // inert (temporary)
  }

  @Test fun testEntrypointHelp() {
    assertNotNull(shell, "should be able to init and inject shell subcommand")
    assertDoesNotThrow {
      assertEquals(0, PicocliRunner.execute(ToolShellCommand::class.java, "--help"))
    }
  }

  @Test fun testEntrypointVersion() {
    assertDoesNotThrow {
      assertEquals(0, PicocliRunner.execute(ToolShellCommand::class.java, "--version"))
    }
  }

  @Test fun testEntrypointExecuteSimpleJsImplied() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(ToolShellCommand::class.java, "run", "-c", "'console.log(\"Hello!\");'"),
      )
    }
  }

  @Test fun testEntrypointExecuteSimpleJsAliasNode() {
    assertDoesNotThrow {
      PicocliRunner.execute(ToolShellCommand::class.java, "node", "-c", "'console.log(\"Hello!\");'")
    }
  }

  @Test fun testEntrypointExecuteSimpleJsAliasDeno() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(ToolShellCommand::class.java, "deno", "-c", "'console.log(\"Hello!\");'"),
      )
    }
  }

  @Test fun testEntrypointExecuteSimplePyAlias() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(ToolShellCommand::class.java, "python", "-c", "'print(\"Hello!\")'"),
      )
    }
  }
}
