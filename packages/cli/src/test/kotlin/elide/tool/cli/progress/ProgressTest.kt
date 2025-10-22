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

import org.junit.jupiter.api.assertThrows
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/**
 * Tests for [Progress].
 *
 * @author Lauri Heino <datafox>
 */
@TestCase
class ProgressTest : ProgressTestBase() {
  @Test
  fun `progress test`() {
    val progress =
      Progress.create("test progress", mockTerminal) {
        add(TrackedTask("task 1", 30, "status message"))
        add(TrackedTask("task 2", 20, position = 10, output = mapOf(0L to "console output")))
        add(TrackedTask("task 3", 40, position = 40, failed = true))
      }
    runBlocking {
      assertEquals(3, progress.tasks.size)
      progress.addTask("task 4", 15)
      assertEquals(4, progress.tasks.size)
      assertEquals("status message", progress.getTask(0).status)
      progress.updateTask(0) { copy(status = "other status") }
      assertEquals("other status", progress.getTask(0).status)
      assertThrows<IndexOutOfBoundsException> { progress.getTask(4) }
      assertThrows<IllegalStateException> { progress.stop() }
      progress.start()
      assertThrows<IllegalStateException> { progress.start() }
      progress.addTask("task 5", 25)
      assertEquals(5, progress.tasks.size)
      progress.updateTask(4) { copy(position = 25) }
      assertEquals(25, progress.getTask(4).position)
      progress.stop()
    }
  }
}
