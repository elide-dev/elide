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

package elide.runtime.plugins.debug

import elide.runtime.core.DelicateElideApi

/**
 * Defines configuration options for the built-in implementation of the Chrome DevTools Protocol, and the
 * Debug Adapter Protocol.
 *
 * This container is meant to be used by the [Debug].
 */
@DelicateElideApi public class DebugConfig internal constructor() {
  /** Configuration for the Debug Adapter Protocol (DAP) connection.  */
  @DelicateElideApi public class DebugAdapterConfig internal constructor() {
    /**
     * Whether to enable the DAP host, enabled by default if the `debugAdapterProtocol` DSL block is requested during
     * configuration.
     */
    public var enabled: Boolean = false

    /** Whether to suspend execution at the first source line. Defaults to `false` */
    public var suspend: Boolean = false

    /** Whether to wait until the inspector is attached before executing any code. Defaults to `false`. */
    public var waitAttached: Boolean = false

    /** Host where inspection should mount. */
    public var host: String = "localhost"

    /** Port where inspection should mount. */
    public var port: Int = 4711
  }

  /** Configuration for the Chrome Devtools inspector connection. */
  @DelicateElideApi public class InspectorConfig internal constructor() {
    /**
     * Whether to enable the devtools inspector, enabled by default if the `chromeInspector` DSL block is requested
     * during configuration.
     */
    public var enabled: Boolean = false

    /** A custom path to be used as connection URL for the inspector. */
    public var path: String? = null

    /**
     * A list of directories or ZIP/JAR files representing the source path, used to resolve relative references in
     * inspected code.
     */
    public var sourcePaths: List<String>? = null

    /** Whether to suspend execution at the first source line. Defaults to `false` */
    public var suspend: Boolean = false

    /** Whether to wait until the inspector is attached before executing any code. Defaults to `false`. */
    public var waitAttached: Boolean = false

    /** Whether to show internal sources in the inspector. */
    public var internal: Boolean = false

    /** Host where inspection should mount. */
    public var host: String = "localhost"

    /** Port where inspection should mount. */
    public var port: Int = 4200
  }

  /** Debug Adapter Protocol settings. */
  internal val debugger: DebugAdapterConfig = DebugAdapterConfig()

  /** Chrome Devtools inspector settings. */
  internal val inspector: InspectorConfig = InspectorConfig()

  /** Configure the Debug Adapter Protocol host. */
  public fun debugAdapter(block: DebugAdapterConfig.() -> Unit) {
    debugger.enabled = true
    debugger.apply(block)
  }

  /** Configure the Chrome DevTools inspector host. */
  public fun chromeInspector(block: InspectorConfig.() -> Unit) {
    inspector.enabled = true
    inspector.apply(block)
  }
}
