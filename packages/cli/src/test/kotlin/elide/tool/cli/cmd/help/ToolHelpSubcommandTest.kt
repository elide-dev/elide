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

package elide.tool.cli.cmd.help

import io.micronaut.configuration.picocli.PicocliRunner
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tool.cli.AbstractSubtoolTest

/** Tests for the main CLI tool entrypoint. */
@TestCase class ToolHelpSubcommandTest : AbstractSubtoolTest() {
  @Inject internal lateinit var help: HelpCommand

  override fun subcommand(): Runnable = help

  @Test fun testEntrypoint() {
    assertNotNull(help, "should be able to init and inject info subcommand")
  }

  override fun runCommand() {
    assertDoesNotThrow {
      assertEquals(0, PicocliRunner.execute(HelpCommand::class.java, "--help"))
    }
  }
}
