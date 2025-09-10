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

/**
 * Handles execution logic for a specific type of test case. Test drivers use the data from the test instance to
 * resolve an entrypoint and invoke it.
 */
public interface TestDriver<T : TestCase> {
  /** The type of test cases supported by this driver. */
  public val type: TestTypeKey<T>

  /**
   * Execute a single [testCase] and return its outcome. Exceptions thrown within this method should always be captured
   * and encapsulated by the return value instead.
   */
  public suspend fun run(testCase: T): TestOutcome
}
