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
package elide.tool.cli.options

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Option

/**
 * ## MCP Options
 *
 * Specifies options which relate to MCP (Model Context Protocol) support in the Elide CLI.
 */
@Introspected @ReflectiveAccess class McpOptions : OptionsMixin<McpOptions> {
  /** Debug mode for MCP usage. */
  @Option(
    names = ["--mcp:debug"],
    description = ["Activate debugging for MCP services."],
  )
  var mcpDebug: Boolean = false
}
