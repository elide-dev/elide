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

package elide.tool.cli

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Disabled
import elide.testing.annotations.Test

/** Common test utilities for Elide Tool sub-commands. */
abstract class AbstractSubtoolTest {
  /**
   * Return the sub-command implementation under test.
   */
  abstract fun subcommand(): Runnable

  protected open fun runCommand() {
    subcommand().run()
  }

  // needs a way to interactively pause
  @Test @Disabled open fun testRunPlain() {
    assertDoesNotThrow {
      runCommand()
    }
  }
}
