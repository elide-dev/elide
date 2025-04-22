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
import picocli.CommandLine.Option
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.plugins.debug.debug

/** Specifies settings for the Chrome DevTools inspector. */
@Introspected @ReflectiveAccess class InspectorConfig {
  @Option(
    names = ["--inspect"],
    description = ["Whether to enable the Chrome Devtools inspector"],
    defaultValue = "false",
  )
  internal var enabled: Boolean = false

  /** Specifies whether the inspector should suspend immediately at execution start. */
  @Option(
    names = ["--inspect:suspend"],
    description = ["Whether the inspector should suspend execution immediately."],
    defaultValue = "false",
  )
  internal var suspend: Boolean = false

  /** Specifies whether the inspector should suspend for internal (facade) sources. */
  @Option(
    names = ["--inspect:internal"],
    description = ["Specifies whether the inspector should suspend for internal (facade) sources"],
    defaultValue = "false",
    hidden = false,
  )
  internal var `internal`: Boolean = false

  /** Specifies whether the inspector should suspend for internal (facade) sources. */
  @Option(
    names = ["--inspect:wait"],
    description = ["Whether to wait for the inspector to attach before executing any code at all."],
    defaultValue = "false",
  )
  internal var wait: Boolean = false

  /** Specifies the port the inspector should bind to. */
  @Option(
    names = ["--inspect:port"],
    description = ["Set the port the inspector binds to"],
    defaultValue = "4200",
  )
  internal var port: Int = 0

  /** Specifies the host the inspector should bind to. */
  @Option(
    names = ["--inspect:host"],
    description = ["Set the host the inspector binds to"],
    defaultValue = "localhost",
  )
  internal var host: String = ""

  /** Specifies the path the inspector should bind to. */
  @Option(
    names = ["--inspect:path"],
    description = ["Set a custom path for the inspector"],
  )
  internal var path: String? = null

  /** Specifies paths where sources are available. */
  @Option(
    names = ["--inspect:sources"],
    arity = "0..N",
    description = ["Add a source directory to the inspector path. Specify 0-N times."],
  )
  internal var sources: List<String> = emptyList()

  /** Apply these settings to the root engine configuration container. */
  internal fun apply(config: PolyglotEngineConfiguration) {
    if (!enabled) return

    // install and configure the Debug plugin
    config.debug {
      chromeInspector {
        enabled = this@InspectorConfig.enabled

        suspend = this@InspectorConfig.suspend
        internal = this@InspectorConfig.internal
        waitAttached = wait

        host = this@InspectorConfig.host
        port = this@InspectorConfig.port

        path = this@InspectorConfig.path
        sourcePaths = sources
      }
    }
  }
}
