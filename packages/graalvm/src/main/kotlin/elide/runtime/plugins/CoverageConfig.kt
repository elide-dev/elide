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
package elide.runtime.plugins

import java.nio.file.Path
import elide.runtime.core.DelicateElideApi

/**
 * Defines configuration options for coverage instrumentation and reporting.
 *
 * This container is meant to be used by the [Coverage] plugin.
 */
@DelicateElideApi public class CoverageConfig {
  public var enabled: Boolean = true
  public var outputDirectory: Path? = null
}
