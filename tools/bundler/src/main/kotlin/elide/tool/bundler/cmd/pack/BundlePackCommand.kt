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

@file:Suppress("RedundantVisibilityModifier")

package elide.tool.bundler.cmd.pack

import picocli.CommandLine.Command
import elide.tool.bundler.AbstractBundlerSubcommand
import elide.tool.bundler.cfg.ElideBundlerTool.ELIDE_TOOL_VERSION

/** Implements the `bundle pack` command. */
@Command(
  name = BundlePackCommand.CMD_NAME,
  description = ["Pack a VFS bundle"],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
)
public class BundlePackCommand : AbstractBundlerSubcommand() {
  internal companion object {
    internal const val CMD_NAME = "pack"
  }

  override fun invoke() = operation {
    logging.info("Bundle `pack` is not implemented yet")
  }
}
