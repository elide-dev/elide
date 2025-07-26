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

import kotlin.time.Duration

/**
 * Stats describing a test run.
 *
 * @property tests Total number of tests seen.
 * @property executions Total number of tests ran.
 * @property passes Total number of tests that passed.
 * @property fails Total number of tests that failed.
 * @property skips Total number of tests that were skipped.
 * @property duration Total duration of the test run.
 */
@JvmRecord public data class TestStats(
  public val tests: UInt,
  public val executions: UInt,
  public val passes: UInt,
  public val fails: UInt,
  public val skips: UInt,
  public val duration: Duration,
)
