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

import elide.tooling.testing.TestCase
import elide.tooling.testing.TestResult
import elide.tooling.testing.TestRunResult
import elide.tooling.testing.TestOutcome
import java.time.Instant
import kotlin.time.Duration

/**
 * Data models for HTML test report rendering.
 */

/**
 * HTML report summary data.
 *
 * @property suiteName Name of the test suite
 * @property timestamp When the test run started
 * @property totalTests Total number of test cases
 * @property passed Number of passed tests
 * @property failed Number of failed tests
 * @property skipped Number of skipped tests
 * @property duration Total execution time
 * @property passRate Success rate as a percentage (0.0 to 1.0)
 */
internal data class HtmlTestSummary(
  val suiteName: String,
  val timestamp: Instant,
  val totalTests: Int,
  val passed: Int,
  val failed: Int,
  val skipped: Int,
  val duration: Duration,
  val passRate: Double
)

/**
 * HTML test case data with hierarchical structure support.
 *
 * @property name Test case name
 * @property className Class or file name containing the test
 * @property fullName Full qualified name for unique identification
 * @property status Test result status
 * @property duration Test execution time
 * @property errorMessage Error message if failed
 * @property stackTrace Full stack trace if failed
 * @property skipReason Reason for skipping if skipped
 * @property parentPath Hierarchical path to this test (e.g., ["describe block", "nested describe"])
 */
internal data class HtmlTestCase(
  val name: String,
  val className: String,
  val fullName: String,
  val status: HtmlTestStatus,
  val duration: Duration,
  val errorMessage: String? = null,
  val stackTrace: String? = null,
  val skipReason: String? = null,
  val parentPath: List<String> = emptyList()
)

/**
 * Test status enum for HTML rendering.
 */
internal enum class HtmlTestStatus(val cssClass: String, val displayName: String) {
  PASSED("test-passed", "Passed"),
  FAILED("test-failed", "Failed"),
  SKIPPED("test-skipped", "Skipped")
}

/**
 * Hierarchical test group for nested display.
 *
 * @property name Group name (e.g., describe block)
 * @property path Full path to this group
 * @property children Child test groups
 * @property tests Test cases directly in this group
 * @property summary Aggregated statistics for this group
 */
internal data class HtmlTestGroup(
  val name: String,
  val path: List<String>,
  val children: MutableList<HtmlTestGroup> = mutableListOf(),
  val tests: MutableList<HtmlTestCase> = mutableListOf(),
  var summary: HtmlGroupSummary
)

/**
 * Summary statistics for a test group.
 *
 * @property totalTests Total tests in this group and children
 * @property passed Number of passed tests
 * @property failed Number of failed tests
 * @property skipped Number of skipped tests
 * @property duration Total execution time
 */
internal data class HtmlGroupSummary(
  val totalTests: Int,
  val passed: Int,
  val failed: Int,
  val skipped: Int,
  val duration: Duration
)

/**
 * Convert Elide test results to HTML report data structures.
 */
internal object HtmlTestModelConverter {
  
  /**
   * Convert TestRunResult to HtmlTestSummary.
   */
  fun convertSummary(results: TestRunResult, suiteName: String): HtmlTestSummary {
    return HtmlTestSummary(
      suiteName = suiteName,
      timestamp = Instant.now(),
      totalTests = results.stats.tests.toInt(),
      passed = results.stats.passes.toInt(),
      failed = results.stats.fails.toInt(),
      skipped = results.stats.skips.toInt(),
      duration = results.stats.duration,
      passRate = if (results.stats.tests > 0u) {
        results.stats.passes.toDouble() / results.stats.tests.toDouble()
      } else 0.0
    )
  }
  
  /**
   * Convert TestResult to HtmlTestCase.
   */
  fun convertTestCase(testResult: TestResult): HtmlTestCase {
    val status = when (val outcome = testResult.outcome) {
      is TestOutcome.Success -> HtmlTestStatus.PASSED
      is TestOutcome.Failure -> HtmlTestStatus.FAILED
      is TestOutcome.Error -> HtmlTestStatus.FAILED
      is TestOutcome.Skipped -> HtmlTestStatus.SKIPPED
    }

    val (errorMessage, stackTrace) = when (val outcome = testResult.outcome) {
      is TestOutcome.Failure -> {
        val reason = outcome.reason
        Pair(
          when (reason) {
            is Throwable -> reason.message ?: "Test failed"
            else -> reason?.toString() ?: "Test failed"
          },
          when (reason) {
            is Throwable -> reason.stackTraceToString()
            else -> null
          }
        )
      }
      is TestOutcome.Error -> {
        val reason = outcome.reason
        Pair(
          when (reason) {
            is Throwable -> reason.message ?: "Test error"
            else -> reason?.toString() ?: "Test error"
          },
          when (reason) {
            is Throwable -> reason.stackTraceToString()
            else -> null
          }
        )
      }
      else -> Pair(null, null)
    }

    val skipReason = when (testResult.outcome) {
      is TestOutcome.Skipped -> "Test was skipped"
      else -> null
    }

    // Parse hierarchical path from test case ID
    val parentPath = parseParentPath(testResult.test.id)

    return HtmlTestCase(
      name = testResult.test.displayName.trim().takeIf { it.isNotEmpty() } ?: "unnamed_test",
      className = extractClassName(testResult.test.id),
      fullName = testResult.test.id,
      status = status,
      duration = testResult.duration,
      errorMessage = errorMessage,
      stackTrace = stackTrace,
      skipReason = skipReason,
      parentPath = parentPath
    )
  }
  
  /**
   * Build hierarchical test structure from flat test cases.
   */
  fun buildTestHierarchy(testCases: List<HtmlTestCase>): HtmlTestGroup {
    val rootGroup = HtmlTestGroup(
      name = "Root",
      path = emptyList(),
      summary = HtmlGroupSummary(0, 0, 0, 0, Duration.ZERO)
    )
    
    testCases.forEach { testCase ->
      insertTestCase(rootGroup, testCase)
    }
    
    // Calculate summaries bottom-up
    calculateGroupSummary(rootGroup)
    
    return rootGroup
  }
  
  private fun insertTestCase(group: HtmlTestGroup, testCase: HtmlTestCase) {
    if (testCase.parentPath.isEmpty()) {
      // Test belongs directly to this group
      group.tests.add(testCase)
    } else {
      // Find or create child group
      val childName = testCase.parentPath.first()
      val childPath = group.path + childName
      
      val childGroup = group.children.find { it.name == childName }
        ?: HtmlTestGroup(
          name = childName,
          path = childPath,
          summary = HtmlGroupSummary(0, 0, 0, 0, Duration.ZERO)
        ).also { group.children.add(it) }
      
      // Continue with remaining path
      val remainingTest = testCase.copy(parentPath = testCase.parentPath.drop(1))
      insertTestCase(childGroup, remainingTest)
    }
  }
  
  private fun calculateGroupSummary(group: HtmlTestGroup): HtmlGroupSummary {
    val directStats = group.tests.fold(
      Triple(0, 0, 0)
    ) { (passed, failed, skipped), test ->
      when (test.status) {
        HtmlTestStatus.PASSED -> Triple(passed + 1, failed, skipped)
        HtmlTestStatus.FAILED -> Triple(passed, failed + 1, skipped)
        HtmlTestStatus.SKIPPED -> Triple(passed, failed, skipped + 1)
      }
    }
    
    val childStats = group.children.map { calculateGroupSummary(it) }
      .fold(Triple(0, 0, 0)) { (passed, failed, skipped), summary ->
        Triple(
          passed + summary.passed,
          failed + summary.failed,
          skipped + summary.skipped
        )
      }
    
    val totalPassed = directStats.first + childStats.first
    val totalFailed = directStats.second + childStats.second
    val totalSkipped = directStats.third + childStats.third
    
    val totalDuration = group.tests.fold(Duration.ZERO) { acc, test -> acc + test.duration } +
                       group.children.fold(Duration.ZERO) { acc, child -> acc + child.summary.duration }
    
    val summary = HtmlGroupSummary(
      totalTests = totalPassed + totalFailed + totalSkipped,
      passed = totalPassed,
      failed = totalFailed,
      skipped = totalSkipped,
      duration = totalDuration
    )
    
    // Update the group's summary
    group.summary = summary
    return summary
  }
  
  /**
   * Parse parent path from test case ID.
   *
   * Examples:
   * - "/path/to/test.js:describe.nested.test" -> ["describe", "nested"]
   * - "com.example.MyTest:testMethod" -> []
   * - "/path/test.js:test" -> []
   */
  private fun parseParentPath(testId: String): List<String> {
    if (!testId.contains(':')) return emptyList()

    val testPath = testId.substringAfterLast(':')
    val pathParts = testPath.split('.')

    // Return all parts except the last one (which is the test name)
    return if (pathParts.size > 1) {
      pathParts.dropLast(1)
    } else {
      emptyList()
    }
  }

  /**
   * Extract class name from test case ID (same logic as XML reporter).
   */
  private fun extractClassName(testId: String): String {
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
}
