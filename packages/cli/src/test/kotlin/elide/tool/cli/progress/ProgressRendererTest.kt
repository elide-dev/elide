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
package elide.tool.cli.progress

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlinx.serialization.Serializable
import kotlin.test.assertEquals
import elide.testing.annotations.TestCase

/**
 * Tests for [ProgressRenderer].
 *
 * @author Lauri Heino <datafox>
 */
@TestCase
class ProgressRendererTest : ProgressTestBase() {
  private val cases: List<Case> = readJson("progress-renderer-test.json")

  @TestFactory
  fun progressRendererTestFactory(): Sequence<DynamicTest> {
    fun test(it: Case) {
      val state = ProgressState(it.progressName, it.tasks.map { t -> t.convert() })
      assertEquals(it.expected(), mockTerminal.render(ProgressRenderer.render(state)))
    }
    return cases.asSequence().map { dynamicTest(it.name) { test(it) } }
  }

  @Serializable
  private data class Case(
    val name: String,
    val progressName: String,
    val expected: List<String>,
    val tasks: List<SerializableTask>
  ) {
    fun expected(): String = expected.joinToString("\n")
  }
}
