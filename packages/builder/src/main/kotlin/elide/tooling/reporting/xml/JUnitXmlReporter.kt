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
package elide.tooling.reporting.xml

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import elide.tooling.testing.TestCaseResult
import elide.tooling.testing.TestResult
import elide.tooling.testing.TestRunResult
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

/**
 * JUnit XML reporter that converts Elide test results to standard JUnit XML format.
 *
 * This reporter creates XML files compatible with most CI/CD systems, IDEs, and test reporting tools
 * that consume JUnit XML format.
 */
internal class JUnitXmlReporter {
  private val xmlMapper = XmlMapper().apply {
    registerKotlinModule()
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
  }

  private companion object {
    private const val MILLISECONDS_TO_SECONDS = 1000.0
  }

  /**
   * Generate JUnit XML report content from test run results.
   *
   * @param results The test run results to convert to XML
   * @param suiteName Optional name for the test suite (defaults to "Elide Tests")
   * @return XML content as a string
   */
  internal fun generateReport(results: TestRunResult, suiteName: String = "Elide Tests"): String {
    val testSuite = convertToJUnitTestSuite(results, suiteName)
    val testSuites = JUnitTestSuites(
      tests = results.stats.tests.toInt(),
      failures = results.stats.fails.toInt(),
      errors = 0, // Elide doesn't distinguish between failures and errors currently
      skipped = results.stats.skips.toInt(),
      time = formatDuration(results.stats.duration),
      testSuites = listOf(testSuite)
    )
    return xmlMapper.writeValueAsString(testSuites)
  }

  /**
   * Convert Elide TestRunResult to JUnit XML test suite.
   */
  private fun convertToJUnitTestSuite(results: TestRunResult, suiteName: String): JUnitTestSuite {
    val testCases = results.results.map { convertToJUnitTestCase(it) }
    
    return JUnitTestSuite(
      name = suiteName,
      tests = results.stats.tests.toInt(),
      failures = results.stats.fails.toInt(),
      errors = 0, // Elide doesn't distinguish between failures and errors currently
      skipped = results.stats.skips.toInt(),
      time = formatDuration(results.stats.duration),
      timestamp = formatTimestamp(Instant.now()),
      testCases = testCases,
      systemOut = "", // System output capture not yet implemented
      systemErr = ""  // System error capture not yet implemented
    )
  }

  /**
   * Convert Elide TestCaseResult to JUnit XML test case.
   */
  private fun convertToJUnitTestCase(caseResult: TestCaseResult): JUnitTestCase {
    // Extract test name and classname from the test scope
    val testName = extractTestName(caseResult.scope.simpleName)
    val className = extractClassName(caseResult.scope.qualifiedName)
    
    return JUnitTestCase(
      name = testName,
      classname = className,
      time = formatDuration(caseResult.duration),
      failure = when (val result = caseResult.result) {
        is TestResult.Fail -> convertToJUnitFailure(result)
        else -> null
      },
      error = null, // Elide doesn't distinguish between failures and errors currently
      skipped = when (val result = caseResult.result) {
        is TestResult.Skip -> convertToJUnitSkipped(result)
        else -> null
      }
    )
  }

  /**
   * Convert Elide TestResult.Fail to JUnit failure element.
   */
  private fun convertToJUnitFailure(failure: TestResult.Fail): JUnitFailure {
    val cause = failure.cause
    return JUnitFailure(
      message = cause?.message ?: "Test failed",
      type = cause?.javaClass?.simpleName ?: "TestFailure",
      content = cause?.stackTraceToString() ?: "No details available"
    )
  }

  /**
   * Convert Elide TestResult.Skip to JUnit skipped element.
   */
  private fun convertToJUnitSkipped(skip: TestResult.Skip): JUnitSkipped {
    return JUnitSkipped(
      message = "Test was skipped: ${skip.reason}"
    )
  }

  /**
   * Extract a meaningful test name from the simple name.
   * 
   * For example:
   * - "test_addition" -> "test_addition"
   * - "should calculate sum correctly" -> "should calculate sum correctly"
   */
  private fun extractTestName(simpleName: String): String {
    return simpleName.trim().takeIf { it.isNotEmpty() } ?: "unnamed_test"
  }

  /**
   * Extract a meaningful class name from the qualified name.
   * 
   * For example:
   * - "/path/to/test.js > describe block > test" -> "test.js"
   * - "com.example.MyTest > testMethod" -> "com.example.MyTest"
   * - "MyTest.kt > test function" -> "MyTest.kt"
   */
  private fun extractClassName(qualifiedName: String): String {
    // Handle different qualified name formats
    return when {
      // File path format: "/path/to/file.ext > ..."
      qualifiedName.contains(" > ") -> {
        val parts = qualifiedName.split(" > ")
        val filePath = parts.firstOrNull() ?: "unknown"
        // Extract just the filename from the path
        filePath.substringAfterLast('/')
      }
      // Simple class name format
      qualifiedName.contains('.') && !qualifiedName.startsWith('/') -> qualifiedName
      // Fallback
      else -> qualifiedName.takeIf { it.isNotEmpty() } ?: "UnknownTest"
    }
  }

  /**
   * Format a Kotlin Duration as seconds with decimal precision.
   * 
   * @param duration Duration to format
   * @return Formatted duration string (e.g., "1.234")
   */
  private fun formatDuration(duration: Duration): String {
    return "%.3f".format(duration.inWholeMilliseconds / MILLISECONDS_TO_SECONDS)
  }

  /**
   * Format an Instant as ISO 8601 timestamp.
   * 
   * @param instant Instant to format
   * @return ISO 8601 formatted timestamp
   */
  private fun formatTimestamp(instant: Instant): String {
    return instant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
  }
}
