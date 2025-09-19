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
    // Create mock test cases using new TestCase interface
    val passedTestCase = MockTestCase("RuntimeInteropTest.kt:vectorize_polyglot_bytecode", "should vectorize polyglot bytecode transformations")
    val failedTestCase = MockTestCase("ParserEngineTest.kt:demultiplex_concurrent_tokens", "should demultiplex concurrent lexer tokens")
    val skippedTestCase = MockTestCase("CodegenBackendTest.kt:serialize_ast_binary", "should serialize abstract syntax trees to binary format")

    // Create individual test results using new TestResult structure
    val individualResults = listOf(
      TestResult(
        test = passedTestCase,
        outcome = TestOutcome.Success,
        duration = 100.milliseconds
      ),
      TestResult(
        test = failedTestCase,
        outcome = TestOutcome.Failure(AssertionError("Expected token stream to converge but got divergent branching factor")),
        duration = 150.milliseconds
      ),
      TestResult(
        test = skippedTestCase,
        outcome = TestOutcome.Skipped,
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

    // Create aggregate TestRunResult with individual results
    val runResult = TestRunResult(
      outcome = TestOutcome.Failure(), // Overall result is fail because one test failed
      stats = stats,
      earlyExit = false,
      testResults = individualResults
    )

    return runResult
  }

  /**
   * Mock implementation of TestCase for testing.
   */
  private class MockTestCase(
    override val id: String,
    override val displayName: String
  ) : TestCase {
    override val parent: String? = null
    override val type: TestTypeKey<TestCase> = object : TestTypeKey<TestCase> {}
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
    assertTrue(xmlContent.contains("should vectorize polyglot bytecode transformations"), "XML should contain passed test")
    assertTrue(xmlContent.contains("should demultiplex concurrent lexer tokens"), "XML should contain failed test")
    assertTrue(xmlContent.contains("should serialize abstract syntax trees to binary format"), "XML should contain skipped test")
    assertTrue(xmlContent.contains("<system-out"), "XML should contain empty system-out")
    assertTrue(xmlContent.contains("<system-err"), "XML should contain empty system-err")
  }

  @Test
  fun `should generate HTML report successfully`() = runTest {
    val processor = TestReportProcessor()
    val options = TestPostProcessingOptions(
      reportingEnabled = true,
      reportFormat = TestReportFormat.HTML
    )
    val mockResults = createMockTestResults()

    val result = processor.invoke(options, mockResults)

    assertEquals(Tool.Result.Success, result)

    // Verify the HTML file was created
    val outputFile = java.nio.file.Paths.get("build", "test-results", "test-report.html")
    assertTrue(outputFile.toFile().exists(), "HTML report file should be created")

    // Verify the HTML content contains expected elements
    val htmlContent = outputFile.toFile().readText()
    assertTrue(htmlContent.contains("<!DOCTYPE html>") || htmlContent.contains("<html"), "HTML should contain html tag")
    assertTrue(htmlContent.contains("Test Report"), "HTML should contain title")
    assertTrue(htmlContent.contains("should vectorize polyglot bytecode transformations"), "HTML should contain passed test")
    assertTrue(htmlContent.contains("should demultiplex concurrent lexer tokens"), "HTML should contain failed test") 
    assertTrue(htmlContent.contains("should serialize abstract syntax trees to binary format"), "HTML should contain skipped test")
    assertTrue(htmlContent.contains("Expected token stream to converge but got divergent branching factor"), "HTML should contain failure message")
    assertTrue(htmlContent.contains("3"), "HTML should show total test count")
    assertTrue(htmlContent.contains("test-passed"), "HTML should contain passed test CSS class")
    assertTrue(htmlContent.contains("test-failed"), "HTML should contain failed test CSS class")
    assertTrue(htmlContent.contains("test-skipped"), "HTML should contain skipped test CSS class")
    
    // Print file location for manual inspection
    println("HTML report generated at: ${outputFile.toAbsolutePath()}")
  }
}

