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
package elide.tooling.testing

import java.nio.file.Path
import java.nio.file.Paths

public enum class TestReportFormat {
  XML,
  HTML
}

/**
 * ## Test Post-Processing Options
 *
 * @property coverageEnabled Whether code coverage instrumentation and reporting is enabled.
 * @property reportingEnabled Whether test and flaw reporting is enabled.
 * @property reportFormat Format for test reports (XML, HTML, etc.).
 * @property reportOutputPath Output directory path for test reports.
 * @property reportSuiteName Name for the test suite in reports.
 */
@JvmRecord public data class TestPostProcessingOptions(
  public val coverageEnabled: Boolean = false,
  public val reportingEnabled: Boolean = false,
  public val reportFormat: TestReportFormat = TestReportFormat.XML, //Default to JUnit XML
  public val reportOutputPath: Path = Paths.get("build", "test-results"),
  public val reportSuiteName: String = "Elide Tests",
)
