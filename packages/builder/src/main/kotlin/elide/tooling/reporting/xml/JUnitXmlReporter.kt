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
import elide.tooling.testing.TestResult
import elide.tooling.testing.TestRunResult
import elide.tooling.testing.TestOutcome
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
   * Count the number of error outcomes in the individual results.
   */
  private fun countErrors(individualResults: List<TestResult>): Int {
    return individualResults.count { it.outcome is TestOutcome.Error }
  }

  /**
   * Generate JUnit XML report content from test run results.
   *
   * @param results The test run summary with aggregate statistics and individual test results
   * @param suiteName Optional name for the test suite (defaults to "Elide Tests")
   * @return XML content as a string
   */
  internal fun generateReport(
    results: TestRunResult,
    suiteName: String = "Elide Tests"
  ): String {
    val testSuite = convertToJUnitTestSuite(results, suiteName)
    val testSuites = JUnitTestSuites(
      tests = results.stats.tests.toInt(),
      failures = results.stats.fails.toInt(),
      errors = countErrors(results.testResults),
      skipped = results.stats.skips.toInt(),
      time = formatDuration(results.stats.duration),
      testSuites = listOf(testSuite)
    )
    return xmlMapper.writeValueAsString(testSuites)
  }

  /**
   * Convert Elide TestRunResult to JUnit XML test suite.
   */
  private fun convertToJUnitTestSuite(
    results: TestRunResult,
    suiteName: String
  ): JUnitTestSuite {
    val testCases = results.testResults.map { convertToJUnitTestCase(it) }

    return JUnitTestSuite(
      name = suiteName,
      tests = results.stats.tests.toInt(),
      failures = results.stats.fails.toInt(),
      errors = countErrors(results.testResults),
      skipped = results.stats.skips.toInt(),
      time = formatDuration(results.stats.duration),
      timestamp = formatTimestamp(Instant.now()),
      testCases = testCases,
      systemOut = "", // System output capture not yet implemented
      systemErr = ""  // System error capture not yet implemented
    )
  }

  /**
   * Convert Elide TestResult to JUnit XML test case.
   */
  private fun convertToJUnitTestCase(testResult: TestResult): JUnitTestCase {
    // Extract test name and classname from the test case
    val testName = extractTestName(testResult.test.displayName)
    val className = extractClassName(testResult.test.id)

    return JUnitTestCase(
      name = testName,
      classname = className,
      time = formatDuration(testResult.duration),
      failure = when (val outcome = testResult.outcome) {
        is TestOutcome.Failure -> convertToJUnitFailure(outcome)
        else -> null
      },
      error = when (val outcome = testResult.outcome) {
        is TestOutcome.Error -> convertToJUnitError(outcome)
        else -> null
      },
      skipped = when (val outcome = testResult.outcome) {
        is TestOutcome.Skipped -> convertToJUnitSkipped()
        else -> null
      }
    )
  }

  /**
   * Convert Elide TestOutcome.Failure to JUnit failure element.
   */
  private fun convertToJUnitFailure(failure: TestOutcome.Failure): JUnitFailure {
    val reason = failure.reason
    return JUnitFailure(
      message = when (reason) {
        is Throwable -> reason.message ?: "Test failed"
        else -> reason?.toString() ?: "Test failed"
      },
      type = when (reason) {
        is Throwable -> reason.javaClass.simpleName
        else -> "TestFailure"
      },
      content = when (reason) {
        is Throwable -> reason.stackTraceToString()
        else -> reason?.toString() ?: "No details available"
      }
    )
  }

  /**
   * Convert Elide TestOutcome.Error to JUnit error element.
   */
  private fun convertToJUnitError(error: TestOutcome.Error): JUnitError {
    val reason = error.reason
    return JUnitError(
      message = when (reason) {
        is Throwable -> reason.message ?: "Test error"
        else -> reason?.toString() ?: "Test error"
      },
      type = when (reason) {
        is Throwable -> reason.javaClass.simpleName
        else -> "TestError"
      },
      content = when (reason) {
        is Throwable -> reason.stackTraceToString()
        else -> reason?.toString() ?: "No details available"
      }
    )
  }

  /**
   * Convert Elide TestOutcome.Skipped to JUnit skipped element.
   */
  private fun convertToJUnitSkipped(): JUnitSkipped {
    return JUnitSkipped(
      message = "Test was skipped"
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
   * Extract a meaningful class name from the test case ID.
   *
   * For example:
   * - "/path/to/test.js:testName" -> "test.js"
   * - "com.example.MyTest.testMethod" -> "com.example.MyTest"
   * - "MyTest.kt:test_function" -> "MyTest.kt"
   */
  private fun extractClassName(testId: String): String {
    // Handle different test ID formats
    return when {
      // File path format with colon: "/path/to/file.ext:testName"
      testId.contains(':') && testId.contains('/') -> {
        val filePath = testId.substringBeforeLast(':')
        filePath.substringAfterLast('/')
      }
      // File path format: "/path/to/file.ext"
      testId.contains('/') -> {
        testId.substringAfterLast('/')
      }
      // Package.Class.method format
      testId.contains('.') -> {
        val parts = testId.split('.')
        if (parts.size > 1) {
          parts.dropLast(1).joinToString(".")
        } else {
          testId
        }
      }
      // Fallback
      else -> testId.takeIf { it.isNotEmpty() } ?: "UnknownTest"
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
