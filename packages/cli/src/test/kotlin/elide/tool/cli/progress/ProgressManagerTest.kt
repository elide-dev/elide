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

import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.assertThrows

/**
 * Tests for [ProgressManager].
 *
 * @author Lauri Heino <datafox>
 */
@TestCase
class ProgressManagerTest : ProgressTestBase() {
  @Test
  fun `progress manager test`() = runTest {
    val manager = Progress.managed("managed test progress", mockTerminal, coroutineContext)
    val flow: MutableSharedFlow<TaskEvent> = MutableSharedFlow()
    val task: StateFlow<TrackedTask> = manager.register("task1", "task 1", 30, "", flow)
    yield()
    assertThrows<IllegalArgumentException> { manager.register("task1", "task 1", 30, "", flow) }
    assertThrows<IllegalArgumentException> { manager.register("task", "task", -1, "", flow) }
    assertEquals(-1, task.value.position)
    flow.emit(TaskStarted(false))
    assertEquals(-1, task.value.position)
    flow.emitStarted()
    assertEquals(0, task.value.position)
    flow.emitStarted()
    assertEquals(0, task.value.position)
    flow.emitProgress(1)
    assertEquals(1, task.value.position)
    flow.emitProgress(15)
    assertEquals(15, task.value.position)
    flow.emitProgress(1)
    assertEquals(15, task.value.position)
    flow.emitStatus("status message")
    assertEquals("status message", task.value.status)
    assertEquals(0, task.value.output.size)
    flow.emitOutput("console output")
    assertEquals(1, task.value.output.size)
    assertEquals("console output", task.value.output.values.first())
    flow.emitOutput("more console output")
    assertEquals(2, task.value.output.size)
    assertFalse(task.value.failed)
    flow.emit(TaskFailed(false))
    assertFalse(task.value.failed)
    flow.emitFailed()
    assertTrue(task.value.failed)
    assertEquals(15, task.value.position)
    flow.emit(TaskCompleted(false))
    assertEquals(15, task.value.position)
    flow.emitCompleted()
    assertEquals(30, task.value.position)
    manager.register("task2", "task 2", 30, "") { flow.emitCompleted() }
    yield()
    assertTrue(manager.progress.running)
    manager.stop("task1")
    yield()
    yield()
    assertFalse(manager.progress.running)
    manager.register("task3", "task 3", 30, "", MutableSharedFlow())
    manager.register("task4", "task 4", 30, "", MutableSharedFlow())
    assertTrue(manager.progress.running)
    manager.stopAll()
    assertFalse(manager.progress.running)
    val task5 = manager.register("task5", "task 5", 30, "") { throw IllegalArgumentException("test") }
    yield()
    assertEquals(30, task5.value.position)
    assertTrue(task5.value.failed)
    val task6 = manager.register("task6", "task 6", 30, "") { throw CancellationException("test") }
    yield()
    assertEquals(-1, task6.value.position)
    assertFalse(task6.value.failed)
  }
}
