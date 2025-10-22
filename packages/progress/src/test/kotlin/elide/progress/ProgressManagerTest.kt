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

import org.junit.jupiter.api.assertThrows
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    val manager: ProgressManager
    runBlocking {
      manager = Progress.managed("managed test progress", mockTerminal)
      val flow: MutableSharedFlow<TaskEvent> = MutableSharedFlow()
      val task: StateFlow<TrackedTask> = manager.register("task1", "task 1", 30, "", flow)
      assertThrows<IllegalArgumentException> { manager.register("task1", "task 1", 30, "", flow) }
      assertThrows<IllegalArgumentException> { manager.register("task", "task", -1, "", flow) }
      assertEquals(-1, task.value.position)
      val taskAssertions =
        ConcurrentLinkedQueue(
          assertions {
            +Assertion(-1) { position }
            +Assertion(0) { position }
            +Assertion(1) { position }
            +Assertion(15) { position }
            +listOf(Assertion("status message") { status }, Assertion(0) { output.size })
            +listOf(Assertion(1) { output.size }, Assertion("console output") { output.values.first() })
            +listOf(Assertion(2) { output.size }, Assertion(false) { failed })
            +listOf(Assertion(true) { failed }, Assertion(15) { position })
            +Assertion(30) { position }
          },
        )
      launch {
        task.collect { task ->
          taskAssertions.remove().forEach { assertEquals(it.expected, it.actual(task)) }
          if (taskAssertions.isEmpty()) cancel()
        }
      }
      flow.emit(TaskStarted(false))
      flow.emitStarted()
      flow.emitStarted()
      flow.emitProgress(1)
      flow.emitProgress(15)
      flow.emitProgress(1)
      flow.emitStatus("status message")
      flow.emitOutput("console output")
      flow.emitOutput("more console output")
      flow.emit(TaskFailed(false))
      flow.emitFailed()
      flow.emit(TaskCompleted(false))
      flow.emitCompleted()
      manager.register("task2", "task 2", 30, "") { flow.emitCompleted() }
      manager.stop("task1")
      manager.register("task3", "task 3", 30, "", MutableSharedFlow())
      manager.register("task4", "task 4", 30, "", MutableSharedFlow())
      manager.stopAll()
      val task5 = manager.register("task5", "task 5", 30, "") { throw IllegalArgumentException("test") }
      launch {
        task5.collect { task ->
          assertEquals(30, task.position)
          assertTrue(task.failed)
          cancel()
        }
      }
      val task6 = manager.register("task6", "task 6", 30, "") { throw CancellationException("test") }
      launch {
        task6.collect { task ->
          assertEquals(-1, task.position)
          assertFalse(task.failed)
          cancel()
        }
      }
    }
    assertFalse(manager.progress.running)
  }

  private data class Assertion<R>(val expected: R, val actual: TrackedTask.() -> R)

  private class AssertionBuilder() {
    val list: MutableList<List<Assertion<*>>> = mutableListOf()

    operator fun <T> Assertion<T>.unaryPlus() {
      list.add(listOf(this))
    }

    operator fun List<Assertion<*>>.unaryPlus() {
      list.add(this)
    }
  }

  private fun assertions(block: AssertionBuilder.() -> Unit): List<List<Assertion<*>>> {
    return AssertionBuilder().apply(block).list.toList()
  }
}
