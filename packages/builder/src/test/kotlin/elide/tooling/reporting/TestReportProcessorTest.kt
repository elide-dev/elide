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
import elide.tooling.testing.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for TestReportProcessor XML report generation.
 */
class TestReportProcessorTest {

  /**
   * Create mock test results for testing.
   */
  private fun createMockTestResults(): TestRunResult {
    // Create mock test scopes
    val passedTestScope = MockTestScope("testPassed", "MockTest.kt > testPassed")
    val failedTestScope = MockTestScope("testFailed", "MockTest.kt > testFailed")
    val skippedTestScope = MockTestScope("testSkipped", "MockTest.kt > testSkipped")

    // Create test case results
    val testCases = listOf(
      TestCaseResult(
        scope = passedTestScope,
        result = TestResult.Pass,
        duration = 100.milliseconds
      ),
      TestCaseResult(
        scope = failedTestScope,
        result = TestResult.Fail(AssertionError("Test assertion failed")),
        duration = 150.milliseconds
      ),
      TestCaseResult(
        scope = skippedTestScope,
        result = TestResult.Skip(Reason.ReasonMessage("Test was disabled")),
        duration = 0.milliseconds
      )
    )

    // Create test stats
    val stats = TestStats(
      tests = 3u,
      executions = 2u, // skipped test doesn't count as execution
      passes = 1u,
      fails = 1u,
      skips = 1u,
      duration = 2.seconds
    )

    return TestRunResult(
      result = TestResult.Fail(), // Overall result is fail because one test failed
      exitCode = 1u,
      stats = stats,
      results = testCases,
      earlyExit = false
    )
  }

  /**
   * Mock implementation of TestScope for testing.
   */
  private class MockTestScope(
    override val simpleName: String,
    override val qualifiedName: String
  ) : TestScope<MockTestScope> {
    override fun compareTo(other: MockTestScope): Int {
      return qualifiedName.compareTo(other.qualifiedName)
    }
  }

  @Test
  fun `should skip report generation when reporting disabled`() = runTest {
    // This test verifies the factory behavior
    val factory = TestReportProcessor.Factory()
    val options = TestPostProcessingOptions(reportingEnabled = false)

    val processor = factory.create(options)

    assertEquals(null, processor, "Should not create processor when reporting is disabled")
  }

  @Test
  fun `should create processor when reporting enabled`() = runTest {
    // This test verifies the factory behavior
    val factory = TestReportProcessor.Factory()
    val options = TestPostProcessingOptions(reportingEnabled = true)

    val processor = factory.create(options)

    assertTrue(processor is TestReportProcessor, "Should create processor when reporting is enabled")
  }

  @Test
  fun `should generate XML report successfully`() = runTest {
    val processor = TestReportProcessor()
    val options = TestPostProcessingOptions(reportingEnabled = true)
    val mockResults = createMockTestResults()

    val result = processor.invoke(options, mockResults)

    assertEquals(Tool.Result.Success, result)

    // Verify the XML file was created
    val outputFile = java.nio.file.Paths.get("build", "test-results", "TEST-elide-results.xml")
    assertTrue(outputFile.toFile().exists(), "XML report file should be created")

    // Verify the XML content contains expected elements
    val xmlContent = outputFile.toFile().readText()
    assertTrue(xmlContent.contains("<testsuite"), "XML should contain testsuite element")
    assertTrue(xmlContent.contains("tests=\"3\""), "XML should show correct test count")
    assertTrue(xmlContent.contains("failures=\"1\""), "XML should show correct failure count")
    assertTrue(xmlContent.contains("testPassed"), "XML should contain passed test")
    assertTrue(xmlContent.contains("testFailed"), "XML should contain failed test")
    assertTrue(xmlContent.contains("testSkipped"), "XML should contain skipped test")
    assertTrue(xmlContent.contains("<system-out"), "XML should contain empty system-out")
    assertTrue(xmlContent.contains("<system-err"), "XML should contain empty system-err")
  }
}

