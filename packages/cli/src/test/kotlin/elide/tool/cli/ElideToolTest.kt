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
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tool.cli.cmd.repl.ToolShellCommand

/** Tests for the main CLI tool entrypoint. */
@TestCase class ElideToolTest {
  @Inject lateinit var tool: ElideTool

  @Test fun testEntrypoint() {
    assertNotNull(tool, "should be able to init and inject entrypoint")
  }

  @Test fun testEntrypointHelp() {
    assertDoesNotThrow {
      assertEquals(0, PicocliRunner.execute(ElideTool::class.java, "--help"))
    }
  }

  @Test fun testEntrypointVersion() {
    assertDoesNotThrow {
      assertEquals(0, PicocliRunner.execute(ElideTool::class.java, "--version"))
    }
  }

  @Test fun testRootEntrypointExecuteJsCode() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(ElideTool::class.java, "run", "-c", "'console.log(\"Hello!\");'"),
      )
    }
  }

  @Test fun testEntrypointExecuteSimpleJsExplicit() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(ElideTool::class.java, "run", "--javascript", "-c", "'console.log(\"Hello!\");'"),
      )
    }
  }

  @Test fun testEntrypointExecuteSimplePyCode() {
    assertDoesNotThrow {
      assertEquals(
        0,
        PicocliRunner.execute(ElideTool::class.java, "run", "--python", "-c", "'print(\"Hello!\")'"),
      )
    }
  }
}
