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
package elide.progress

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/**
 * Tests for [ProgressManager].
 *
 * @author Lauri Heino <datafox>
 */
@TestCase
class ProgressManagerTest : ProgressTestBase() {
  @Test
  fun `progress manager test`() {
    val manager = Progress.managed("managed test progress", mockTerminal)
    runBlocking {
      val task1 = manager.addTask("task 1", 30)
      val task2 = manager.addTask("task 2", 20)
      manager.start()
      assertEquals("", task1.task.status)
      task1.setStatus("status message")
      assertEquals("status message", task1.task.status)
      assertEquals(-1, task1.task.position)
      task1.setPosition(0)
      assertEquals(0, task1.task.position)
      task1.setPosition(15)
      assertEquals(15, task1.task.position)
      task1.setPosition(50)
      assertEquals(30, task1.task.position)
      task1.setPosition(30)
      assertEquals(30, task1.task.position)
      assertEquals(0, task1.task.output.size)
      task1.appendOutput("console output")
      assertEquals(1, task1.task.output.size)
      assertEquals("console output", task1.task.output.values.first())
      task1.appendOutput("more output")
      assertEquals(2, task1.task.output.size)
      // force task position back
      manager.progress.updateTask(0) { copy(position = 5) }
      assertFalse(task1.task.failed)
      task1.fail(false)
      assertTrue(task1.task.failed)
      assertEquals(5, task1.task.position)
      task1.fail(true)
      assertTrue(task1.task.failed)
      assertEquals(30, task1.task.position)
      val flow: MutableSharedFlow<TaskEvent> = MutableSharedFlow()
      coroutineScope {
        task2.subscribe(flow, this)
        assertEquals("", task2.task.status)
        assertEquals(-1, task2.task.position)
        assertEquals(0, task2.task.output.size)
        flow.emit(TaskEvent("status message", 1, "console output"))
        assertEquals("status message", task2.task.status)
        assertEquals(1, task2.task.position)
        assertEquals(1, task2.task.output.size)
        assertEquals("console output", task1.task.output.values.first())
        flow.emit(TaskEvent("new status message"))
        assertEquals("new status message", task2.task.status)
        assertEquals(1, task2.task.position)
        assertEquals(1, task2.task.output.size)
        assertEquals("console output", task1.task.output.values.first())
        assertFalse(task2.task.failed)
        flow.emit(TaskEvent(failed = true))
        assertTrue(task2.task.failed)
        flow.emit(TaskEvent(failed = false))
        assertTrue(task2.task.failed)
        manager.stop()
      }
    }
  }
}
