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
package elide.tooling.testing.jvm

import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.jvm.Jvm
import elide.tooling.testing.TestDriver
import elide.tooling.testing.TestOutcome
import elide.tooling.testing.TestTypeKey

/**
 * Test driver for [JVM test cases][JvmTestCase], using a [PolyglotContext] to obtain an instance of the test class and
 * invoke the test method.
 */
public class GuestJvmTestDriver(private val contextProvider: () -> PolyglotContext) : TestDriver<JvmTestCase> {
  override val type: TestTypeKey<JvmTestCase> get() = JvmTestCase

  override suspend fun run(testCase: JvmTestCase): TestOutcome {
    val testClass = contextProvider().bindings(Jvm.Plugin).getMember(testCase.className)
      ?: return TestOutcome.Error("Failed to resolve test class: ${testCase.className}")

    if (!testClass.canInstantiate()) return TestOutcome.Error("Failed to instantiate test class ${testCase.className}")
    val testInstance = testClass.newInstance()

    if (!testInstance.canInvokeMember(testCase.methodName))
      return TestOutcome.Error("Cannot invoke member ${testCase.methodName}")

    return runCatching { testInstance.invokeMember(testCase.methodName) }.fold(
      onSuccess = { TestOutcome.Success },
      onFailure = { TestOutcome.Failure(it) },
    )
  }
}
