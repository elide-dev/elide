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
package elide.tooling.reporting

import elide.tooling.Tool
import elide.tooling.reporting.xml.JUnitXmlReporter
import elide.tooling.testing.TestPostProcessingOptions
import elide.tooling.testing.TestPostProcessor
import elide.tooling.testing.TestPostProcessorFactory
import elide.tooling.testing.TestReportFormat
import elide.tooling.testing.TestRunResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class TestReportProcessor : TestPostProcessor {
  override suspend fun invoke(options: TestPostProcessingOptions, results: TestRunResult): Tool.Result {
    return runCatching { 
      when (options.reportFormat) {
        TestReportFormat.XML -> generateXmlReport(options, results)
        TestReportFormat.HTML -> generateHtmlReport(options, results)
      }
    }.fold(
      onSuccess = { Tool.Result.Success },
      onFailure = { e ->
        System.err.println("Failed to generate test report: ${e.message}")
        Tool.Result.UnspecifiedFailure
      }
    )
  }

  private fun generateXmlReport(options: TestPostProcessingOptions, results: TestRunResult) {
    val xmlReporter = JUnitXmlReporter()
    val xmlContent = xmlReporter.generateReport(results, suiteName = options.reportSuiteName)
    
    // Create output directory if it doesn't exist
    Files.createDirectories(options.reportOutputPath)
    
    // Write XML report file
    val outputFile = options.reportOutputPath.resolve("TEST-elide-results.xml")
    Files.write(outputFile, xmlContent.toByteArray())
    
    println("Generated XML test report: ${outputFile.toAbsolutePath()}")
  }

  /**
   * TODO: Implement HTML reporter when ready
   */
  private fun generateHtmlReport(options: TestPostProcessingOptions, results: TestRunResult) {}

  // Create a coverage report processor if coverage is enabled.
  class Factory : TestPostProcessorFactory<TestReportProcessor> {
    override fun create(options: TestPostProcessingOptions): TestPostProcessor? = when (options.reportingEnabled) {
      false -> null
      else -> TestReportProcessor()
    }
  }
}
