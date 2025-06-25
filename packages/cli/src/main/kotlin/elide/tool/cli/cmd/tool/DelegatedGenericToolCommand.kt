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

package elide.tool.cli.cmd.tool

import elide.tool.cli.AbstractSubcommand
import elide.tooling.Tool
import elide.tooling.AbstractTool

/**
 * ## Delegated Tool Command (Generic)
 *
 * Implements generic support for tools which use the JDK's built-in [java.util.spi.ToolProvider] interface; such tools
 * provide a [java.util.spi.ToolProvider.run] method.
 *
 * @param T Tool adapter implementation
 */
abstract class DelegatedGenericToolCommand<T, C> (
  info: Tool.CommandLineTool,
  configurator: ToolCommandConfigurator<T>? = null,
): DelegatedToolCommand<T, C>(info, configurator) where T: AbstractTool, C: AbstractSubcommand<*, *>
