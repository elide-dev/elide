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

package elide.tool.cli.cmd.runner

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.plugins.debug.debug

/** Specifies settings for the Debug Adapter Protocol host. */
@Introspected @ReflectiveAccess class DebugConfig {
  /** Specifies whether the debugger should suspend immediately at execution start. */
  @CommandLine.Option(
    names = ["--debug:suspend"],
    description = ["Whether the debugger should suspend execution immediately."],
    defaultValue = "false",
  )
  internal var suspend: Boolean = false

  /** Specifies whether the debugger should suspend for internal (facade) sources. */
  @CommandLine.Option(
    names = ["--debug:wait"],
    description = ["Whether to wait for the debugger to attach before executing any code at all."],
    defaultValue = "false",
  )
  internal var wait: Boolean = false

  /** Specifies the port the debugger should bind to. */
  @CommandLine.Option(
    names = ["--debug:port"],
    description = ["Set the port the debugger binds to"],
    defaultValue = "4711",
  )
  internal var port: Int = 0

  /** Specifies the host the debugger should bind to. */
  @CommandLine.Option(
    names = ["--debug:host"],
    description = ["Set the host the debugger binds to"],
    defaultValue = "localhost",
  )
  internal var host: String = ""

  /** Apply these settings to the root engine configuration container. */
  internal fun apply(config: PolyglotEngineConfiguration) {
    // install and configure the Debug plugin
    config.debug {
      debugAdapter {
        suspend = this@DebugConfig.suspend
        waitAttached = wait

        host = this@DebugConfig.host
        port = this@DebugConfig.port
      }
    }
  }
}
