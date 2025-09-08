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

import elide.tooling.testing.TestCase
import elide.tooling.testing.TestNodeKey
import elide.tooling.testing.TestTypeKey

/**
 * Represents a JVM test, characterized by its qualified [className] and a [methodName] within it. Drivers are expected
 * to handle the specifics of loading the class and creating test instances for execution.
 */
public data class JvmTestCase(
    override val id: TestNodeKey,
    override val parent: TestNodeKey?,
    override val displayName: String,
    val className: String,
    val methodName: String,
) : TestCase {
  override val type: TestTypeKey<JvmTestCase> get() = JvmTestCase

  public companion object : TestTypeKey<JvmTestCase>
}
