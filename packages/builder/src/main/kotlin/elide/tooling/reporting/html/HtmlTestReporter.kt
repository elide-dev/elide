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
package elide.tooling.reporting.html

import elide.tooling.testing.TestResult
import elide.tooling.testing.TestRunResult

/**
 * HTML test reporter that converts Elide test results to interactive HTML reports.
 *
 * This reporter creates self-contained HTML files with embedded CSS and JavaScript
 * for modern, interactive test reporting. The generated reports include:
 * - Summary dashboard with pass/fail statistics
 * - Hierarchical test structure (supports nested describes/suites)
 * - Search and filtering capabilities
 * - Collapsible test groups
 * - Detailed failure information with stack traces
 * - Responsive design for mobile and desktop
 *
 * The HTML reporter uses kotlinx-html DSL for HTML generation and follows
 * the same patterns as the existing PageController and HtmlBuilder components.
 */
internal class HtmlTestReporter {
  
  private val templateBuilder = HtmlTemplateBuilder()
  
  /**
   * Generate HTML report content from test run results.
   *
   * @param results The test run summary with aggregate statistics and individual test results
   * @param suiteName Optional name for the test suite (defaults to "Elide Tests")
   * @return HTML content as a string
   */
  internal fun generateReport(
    results: TestRunResult,
    suiteName: String = "Elide Tests"
  ): String {
    // Convert test results to HTML data models
    val summary = HtmlTestModelConverter.convertSummary(results, suiteName)
    val testCases = results.individualTests.map { HtmlTestModelConverter.convertTestCase(it) }
    val testHierarchy = HtmlTestModelConverter.buildTestHierarchy(testCases)

    // Generate HTML using kotlinx-html DSL
    return templateBuilder.generateReport(summary, testHierarchy)
  }
}
