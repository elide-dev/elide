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
import elide.tooling.testing.TestPostProcessingOptions
import elide.tooling.testing.TestPostProcessor
import elide.tooling.testing.TestPostProcessorFactory
import elide.tooling.testing.TestRunResult

// Test post-processor which produces test result reports.
internal class TestReportProcessor : TestPostProcessor {
  override suspend fun invoke(options: TestPostProcessingOptions, results: TestRunResult): Tool.Result {
    // @TODO not yet implemented
    return Tool.Result.Success
  }

  // Create a coverage report processor if coverage is enabled.
  class Factory : TestPostProcessorFactory<TestReportProcessor> {
    override fun create(options: TestPostProcessingOptions): TestPostProcessor? = when (options.reportingEnabled) {
      false -> null
      else -> TestReportProcessor()
    }
  }
}
