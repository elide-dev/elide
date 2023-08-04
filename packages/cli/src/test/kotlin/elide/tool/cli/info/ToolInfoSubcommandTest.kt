/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.cli.info

import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tool.cli.AbstractSubtoolTest
import elide.tool.cli.cmd.info.ToolInfoCommand

/** Tests for the main CLI tool entrypoint. */
@TestCase class ToolInfoSubcommandTest : AbstractSubtoolTest() {
  @Inject internal lateinit var info: ToolInfoCommand

  override fun subcommand(): Runnable = info

  @Test fun testEntrypoint() {
    assertNotNull(info, "should be able to init and inject info subcommand")
  }
}
