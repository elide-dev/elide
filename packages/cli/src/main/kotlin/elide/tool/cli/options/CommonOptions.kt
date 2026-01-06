/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
import java.util.*
import kotlin.properties.Delegates
import elide.tooling.cli.Statics
import elide.tool.cli.output.TOOL_LOGGER_NAME
import elide.versions.VersionsValues

private const val DEFAULT_TIMEOUT_SECONDS: Int = 1

/**
 * # Options: Common
 *
 * Defines common command line options shared by all CLI sub-commands; these are basic flags which control output and
 * input, execution, logging, and other basic CLI facilities.
 */
@Introspected @ReflectiveAccess class CommonOptions : OptionsMixin<CommonOptions> {
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
    defaultValue = "false",
  )
  var verbose: Boolean by Delegates.observable(false) { _, _, active ->
    if (active) {
      if (!LoggerFactory.getLogger(TOOL_LOGGER_NAME).isInfoEnabled) {
        setLoggingLevel(Level.INFO)
        logging.info("Verbose logging enabled.")
      }
    }
  }

  /** Verbose logging mode. */
  @set:Option(
    names = ["-q", "--quiet"],
    description = ["Squelch most logging"],
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
  )
  var pretty: Boolean = true

  /** Sets the default exit timeout for this run. */
  @Option(
    names = ["--timeout"],
    description = ["Timeout to apply when exiting (in seconds)."],
    paramLabel = "secs",
    defaultValue = "$DEFAULT_TIMEOUT_SECONDS",
  )
  var timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS

  /** System options to apply. */
  @Option(
    names = ["-D", "--define"],
    mapFallbackValue = Option.NULL_VALUE,
    hidden = true,
  )
  var systemProperties: Map<String, Optional<String>> = emptyMap()

  /** Internal runtime options to apply. */
  @Option(
    names = ["-XX"],
    mapFallbackValue = Option.NULL_VALUE,
    hidden = true,
  )
  var internalOptions: Map<String, Optional<String>> = emptyMap()

  /** Specifies that `.elideversion` in current directory should be ignored. */
  @Option(
    names = [VersionsValues.IGNORE_VERSION_FLAG],
    description = ["Ignore .elideversion file and ${VersionsValues.USE_VERSION_FLAG}"],
    defaultValue = "false",
  )
  var ignoreVersion: Boolean = false

  /** Specifies a version of Elide to be used, overriding `.elideversion`. */
  @Option(
    names = [VersionsValues.USE_VERSION_FLAG],
    description = ["Version of Elide to run"],
    paramLabel = "version",
  )
  var useVersion: String? = null

  override fun merge(other: CommonOptions?): CommonOptions {
    val options = CommonOptions()
    options.verbose = this.verbose || other?.verbose == true
    options.quiet = this.quiet || other?.quiet == true
    options.debug = this.debug || other?.debug == true
    options.pretty = this.pretty && (other?.pretty ?: true)
    options.timeoutSeconds = this.timeoutSeconds + (other?.timeoutSeconds ?: 0)
    options.systemProperties = this.systemProperties + (other?.systemProperties ?: emptyMap())
    options.internalOptions = this.internalOptions + (other?.internalOptions ?: emptyMap())
    options.ignoreVersion = this.ignoreVersion || other?.ignoreVersion == true
    options.useVersion = other?.useVersion ?: useVersion
    return options
  }
}
