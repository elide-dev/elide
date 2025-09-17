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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText

/**
 * Data model for JUnit XML test reports.
 *
 */

/**
 * Root element wrapping all test suites in JUnit XML format.
 *
 * @property name Name of the test suites collection
 * @property tests Total number of test cases across all suites
 * @property failures Total number of failed tests across all suites
 * @property errors Total number of test errors across all suites
 * @property skipped Total number of skipped tests across all suites
 * @property time Total execution time across all suites
 * @property testSuites List of test suites
 */
@JacksonXmlRootElement(localName = "testsuites")
internal data class JUnitTestSuites(
  @JacksonXmlProperty(isAttribute = true)
  val name: String = "Elide Tests",
  
  @JacksonXmlProperty(isAttribute = true)
  val tests: Int,
  
  @JacksonXmlProperty(isAttribute = true)
  val failures: Int,
  
  @JacksonXmlProperty(isAttribute = true)
  val errors: Int,
  
  @JacksonXmlProperty(isAttribute = true)
  val skipped: Int,
  
  @JacksonXmlProperty(isAttribute = true)
  val time: String,
  
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "testsuite")
  val testSuites: List<JUnitTestSuite>
)

/**
 * Test suite element in JUnit XML format.
 *
 * @property name Name of the test suite
 * @property tests Total number of test cases
 * @property failures Number of failed tests
 * @property errors Number of tests with errors
 * @property skipped Number of skipped tests
 * @property time Total execution time in seconds
 * @property timestamp ISO 8601 timestamp when the test suite started
 * @property testCases List of individual test cases
 * @property systemOut System output captured during test execution
 * @property systemErr System error output captured during test execution
 */
internal data class JUnitTestSuite(
  @JacksonXmlProperty(isAttribute = true)
  val name: String,
  
  @JacksonXmlProperty(isAttribute = true)
  val tests: Int,
  
  @JacksonXmlProperty(isAttribute = true)
  val failures: Int,
  
  @JacksonXmlProperty(isAttribute = true)
  val errors: Int,
  
  @JacksonXmlProperty(isAttribute = true)
  val skipped: Int,
  
  @JacksonXmlProperty(isAttribute = true)
  val time: String,
  
  @JacksonXmlProperty(isAttribute = true)
  val timestamp: String,
  
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "testcase")
  val testCases: List<JUnitTestCase>,
  
  @JacksonXmlProperty(localName = "system-out")
  val systemOut: String = "",
  
  @JacksonXmlProperty(localName = "system-err")
  val systemErr: String = ""
)

/**
 * Individual test case element in JUnit XML format.
 *
 * @property name Name of the test method/function
 * @property classname Fully qualified class name (or file path for non-JVM tests)
 * @property time Execution time in seconds for this specific test
 * @property failure Optional failure information if the test failed
 * @property error Optional error information if the test had an error
 * @property skipped Optional skip information if the test was skipped
 */
internal data class JUnitTestCase(
  @JacksonXmlProperty(isAttribute = true)
  val name: String,
  
  @JacksonXmlProperty(isAttribute = true)
  val classname: String,
  
  @JacksonXmlProperty(isAttribute = true)
  val time: String,
  
  val failure: JUnitFailure? = null,
  val error: JUnitError? = null,
  val skipped: JUnitSkipped? = null
)

/**
 * Test failure element for failed test cases.
 *
 * @property message Short failure message
 * @property type Type of failure (e.g., "AssertionError", "TestFailure")
 * @property content Full failure details including stack trace (as text content)
 */
internal data class JUnitFailure(
  @JacksonXmlProperty(isAttribute = true)
  val message: String,
  
  @JacksonXmlProperty(isAttribute = true)
  val type: String,
  
  @JacksonXmlText
  val content: String
)

/**
 * Test error element for test cases that had errors.
 *
 * @property message Short error message
 * @property type Type of error (e.g., "RuntimeException", "IOException")
 * @property content Full error details including stack trace (as text content)
 */
internal data class JUnitError(
  @JacksonXmlProperty(isAttribute = true)
  val message: String,
  
  @JacksonXmlProperty(isAttribute = true)
  val type: String,
  
  @JacksonXmlText
  val content: String
)

/**
 * Test skip element for skipped test cases.
 *
 * @property message Reason why the test was skipped
 */
internal data class JUnitSkipped(
  @JacksonXmlProperty(isAttribute = true)
  val message: String
)
