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
 * ## Testing Options
 *
 * Controls CLI options related to testing; this includes test reporting and coverage, as well as test execution
 * controls or filters.
 *
 * @property enableCoverage Whether coverage is enabled.
 * @property coverageFormat Format of coverage report to produce.
 * @property threadedTestMode Whether to use the threaded test runner (experimental).
 */
@Introspected @ReflectiveAccess class TestingOptions : OptionsMixin<TestingOptions> {
  /** Activates coverage in test mode. */
  @Option(
    names = ["--coverage"],
    description = ["Enable or disable coverage during `elide test`"],
    negatable = true,
    defaultValue = "false",
  )
  internal var enableCoverage: Boolean = false

  /** Activates coverage in test mode. */
  @Option(
    names = ["--coverage-format"],
    description = ["Coverage format to emit; defaults to 'json'"],
    defaultValue = "json",
  )
  internal var coverageFormat: String = "json"

  /** Activates coverage in test mode. */
  @Option(
    names = ["--experimental-threaded-testing"],
    description = ["Test in threaded mode (experimental)"],
    negatable = true,
    defaultValue = "false",
  )
  internal var threadedTestMode: Boolean = false

  /** Activates coverage in test mode. */
  @Option(
    names = ["--test-report"],
    description = ["Enable test reports; pass format. Formats are 'xml'."],
  )
  internal var testReports: String? = null
}
