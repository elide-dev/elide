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

package elide.tool.cli.options

import ch.qos.logback.classic.Level
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import org.slf4j.LoggerFactory
import picocli.CommandLine.Option
import picocli.CommandLine.ScopeType
import kotlin.properties.Delegates
import elide.tool.cli.Statics

/**
 * # Options: Common
 *
 * Defines common command line options shared by all CLI sub-commands; these are basic flags which control output and
 * input, execution, logging, and other basic CLI facilities.
 */
@Introspected @ReflectiveAccess class CommonOptions : OptionsMixin {
  private val logging by lazy {
    Statics.logging
  }

  private fun setLoggingLevel(level: Level) {
    ((LoggerFactory.getLogger("ROOT")) as ch.qos.logback.classic.Logger).level = level
  }

  /** Verbose logging mode (wins over `--quiet`). */
  @set:Option(
    names = ["-v", "--verbose"],
    description = ["Activate verbose logging. Wins over `--quiet` when both are passed."],
    scope = ScopeType.INHERIT,
    defaultValue = "false",
  )
  var verbose: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      setLoggingLevel(Level.INFO)
      logging.info("Verbose logging enabled.")
    }
  }

  /** Verbose logging mode. */
  @set:Option(
    names = ["-q", "--quiet"],
    description = ["Squelch most logging"],
    scope = ScopeType.INHERIT,
    defaultValue = "false",
  )
  var quiet: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      setLoggingLevel(Level.OFF)
    }
  }

  /** Debug mode. */
  @set:Option(
    names = ["--debug"],
    description = ["Activate debugging features and extra logging"],
    scope = ScopeType.INHERIT,
    defaultValue = "false",
  )
  var debug: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      logging.trace("Debug mode enabled.")
      setLoggingLevel(Level.TRACE)
    }
  }

  /** Whether to activate pretty logging; on by default. */
  @Option(
    names = ["--pretty"],
    negatable = true,
    description = ["Whether to colorize and animate output."],
    defaultValue = "true",
    scope = ScopeType.INHERIT,
  )
  var pretty: Boolean = true
}
