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
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * # Options: Telemetry
 *
 * Defines common options which govern Elide's built-in telemetry features.
 */
@Introspected @ReflectiveAccess class TelemetryOptions : OptionsMixin<TelemetryOptions> {
  /** Disables all telemetry features. */
  @Option(
    names = ["--no-telemetry"],
    description = ["Disable all telemetry features"],
  )
  var noTelemetry: Boolean = false

  // Decide at startup whether telemetry should be disabled.
  private val shouldDisableTelemetry by lazy {
    (
      // the user can pass `--no-telemetry`
      noTelemetry ||

      // the user can set `ELIDE_NO_TELEMETRY` to anything
      System.getenv("ELIDE_NO_TELEMETRY")?.ifBlank { null } != null ||

      // the user can put a file in `$HOME/elide/.no-telemetry` to disable all telemetry by default
      Path.of(System.getProperty("user.home")).resolve("elide").resolve(".no-telemetry").exists()
    )
  }

  /**
   * Decide whether to disable telemetry.
   *
   * @return Whether telemetry should be disabled.
   */
  fun shouldDisable(): Boolean = shouldDisableTelemetry
}
