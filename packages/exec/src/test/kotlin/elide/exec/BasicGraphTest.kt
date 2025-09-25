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
@file:Suppress("UnstableApiUsage")

package elide.exec

import org.junit.jupiter.api.Disabled
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.*
import elide.exec.TaskGraphEvent.*

class BasicGraphTest {
  @Test fun testCreateTaskGraph() = testInScope {
    TaskGraph.build {
      task(name = "example") {
        // this is an example task
        error("I should not actually execute")
      }
    }.let {
      assertNotNull(it)
      assertEquals(1u, it.nodeCount)
      assertEquals(0u, it.edgeCount)
      val id = TaskId.fromName("example")
      val example by it.tasks()
      assertTrue(id in it)
      assertTrue(example in it)
    }
  }

  @Test fun testCreateTaskGraphWithDependencies() = testInScope {
    TaskGraph.build {
      val example = task(name = "example") {
        // this is an example task
        error("I should not actually execute")
      }
      task(name = "example2") {
        // example task with dependency
        error("I should also not run")
      }.dependsOn(
        example
      )
    }.let {
      assertNotNull(it)
      assertEquals(2u, it.nodeCount)
      assertEquals(1u, it.edgeCount)
      val id = TaskId.fromName("example")
      val id2 = TaskId.fromName("example")
      val example by it.tasks()
      val example2 by it.tasks()
      assertTrue(id in it)
      assertTrue(id2 in it)
      assertTrue(example in it)
      assertTrue(example2 in it)
    }
  }

  @Test fun testExecuteTaskGraph() = testInScope {
    var executed = false

    TaskGraph.build {
      task(name = "example") {
        // this is an example task
        executed = true
      }
    }.apply {
      assertNotNull(this)
      assertEquals(1u, this.nodeCount)
      assertEquals(0u, this.edgeCount)
      val id = TaskId.fromName("example")
      val example by tasks()
      assertTrue(id in this)
      assertTrue(example in this)
    }.execute(this).apply {
      assertNotNull(this)
    }.await()

    assertTrue(executed, "Task was not executed")
  }

  @Test fun testExecuteTaskGraphWithDependency() = testInScope {
    var executed = false
    var executedTwo = false

    TaskGraph.build {
      val example = task(name = "example") {
        // this is an example task
        executed = true
      }
      task(name = "example2") {
        assertTrue(executed, "example task should have executed")
        assertFalse(executedTwo, "example2 should only execute once")
        executedTwo = true
      }.dependsOn(
        example
      )
    }.apply {
      assertNotNull(this)
      assertEquals(2u, this.nodeCount)
      assertEquals(1u, this.edgeCount)
      val id = TaskId.fromName("example")
      val example by tasks()
      assertTrue(id in this)
      assertTrue(example in this)
      assertFalse(executed)
      assertFalse(executedTwo)
    }.execute(this).apply {
      assertNotNull(this)
    }.await()

    assertTrue(executed, "example task was not executed")
    assertTrue(executedTwo, "example2 task should have executed")
  }

  @Disabled
  @Test fun testExecuteTaskGraphResolved() = testInScope {
    val executed = AtomicBoolean(false)
    val executed2 = AtomicBoolean(false)

    TaskGraph.build {
      task(name = "example") {
        // this is an example task
        executed.compareAndSet(false, true)
      }
      task(name = "example2") {
        // this is an example task
        executed2.compareAndSet(false, true)
      }
    }.apply {
      assertNotNull(this)
      assertEquals(2u, this.nodeCount)
      assertEquals(0u, this.edgeCount)
      val id = TaskId.fromName("example")
      val example by tasks()
      assertTrue(id in this)
      assertTrue(example in this)
    }.execute(this, TaskId.fromName("example")).apply {
      assertNotNull(this)
    }.await()

    assertTrue(executed.get(), "Task was not executed")
    assertFalse(executed2.get(), "example2 task should not have executed")
  }

  @Disabled
  @Test fun testExecuteTaskGraphResolvedWithDependencies() = testInScope {
    val executed = AtomicBoolean(false)
    val executed2 = AtomicBoolean(false)

    TaskGraph.build {
      val example = task(name = "example") {
        // this is an example task
        executed.compareAndSet(false, true)
      }
      task(name = "example2") {
        // this is an example task
        executed2.compareAndSet(false, true)
      }.dependsOn(
        example
      )
    }.apply {
      assertNotNull(this)
      assertEquals(2u, this.nodeCount)
      assertEquals(1u, this.edgeCount)
      val id = TaskId.fromName("example")
      val example by tasks()
      assertTrue(id in this)
      assertTrue(example in this)
    }.execute(this, TaskId.fromName("example2")).apply {
      assertNotNull(this)
    }.await()

    assertTrue(executed.get(), "example1 task should have executed")
    assertTrue(executed2.get(), "example2 task should have executed")
  }

  @Disabled
  @Test fun testExecuteTaskGraphResolvedWithFork() = testInScope {
    val executed = AtomicBoolean(false)
    val executed2 = AtomicBoolean(false)
    val subExecuted = AtomicBoolean(false)

    TaskGraph.build {
      task<Unit>(name = "example") {
        // this is an example task
        executed.compareAndSet(false, true)
        taskScope.scope.fork<Unit> {
          subExecuted.compareAndSet(false, true)
        }
      }
      task(name = "example2") {
        // this is an example task
        executed2.compareAndSet(false, true)
      }
    }.apply {
      assertNotNull(this)
      assertEquals(2u, this.nodeCount)
      assertEquals(0u, this.edgeCount)
      val id = TaskId.fromName("example")
      val example by tasks()
      assertTrue(id in this)
      assertTrue(example in this)
    }.execute(this, TaskId.fromName("example")).apply {
      assertNotNull(this)
    }.await()

    assertTrue(executed.get(), "Task was not executed")
    assertTrue(subExecuted.get(), "Subtask was not executed")
    assertFalse(executed2.get(), "example2 task should not have executed")
  }

  @Test fun testExecuteTaskGraphEventListenerConfigured() = testInScope {
    var executed = false
    var configuredEventDelivered = false

    TaskGraph.build {
      task(name = "example") {
        // this is an example task
        executed = true
      }
    }.apply {
      assertNotNull(this)
      assertEquals(1u, this.nodeCount)
      assertEquals(0u, this.edgeCount)
      val id = TaskId.fromName("example")
      val example by tasks()
      assertTrue(id in this)
      assertTrue(example in this)
    }.execute(this) {
      on(Configured) {
        configuredEventDelivered = true
      }
    }.apply {
      assertNotNull(this)
    }.await()

    assertTrue(executed, "Task was not executed")
    assertTrue(configuredEventDelivered, "`configured` event was not delivered")
  }

  @Test fun testExecuteTaskGraphEventListenerAllEvents() = testInScope {
    var executed = false
    var configuredEventDelivered = false
    var executionStartDelivered = false
    var taskReadyDelivered = false
    var taskExecutionDelivered = false
    var taskCompletedDelivered = false
    var taskFinishedDelivered = false
    var execCompletedDelivered = false
    var execFinishedDelivered = false

    TaskGraph.build {
      task(name = "example") {
        // this is an example task
        executed = true
      }
    }.apply {
      assertNotNull(this)
      assertEquals(1u, this.nodeCount)
      assertEquals(0u, this.edgeCount)
      val id = TaskId.fromName("example")
      val example by tasks()
      assertTrue(id in this)
      assertTrue(example in this)
    }.execute(this) {
      on(Configured) { configuredEventDelivered = true }
      on(ExecutionStart) { executionStartDelivered = true }
      on(TaskReady) { taskReadyDelivered = true }
      on(TaskExecute) { taskExecutionDelivered = true }
      on(TaskCompleted) { taskCompletedDelivered = true }
      on(TaskFinished) { taskFinishedDelivered = true }
      on(ExecutionCompleted) { execCompletedDelivered = true }
      on(ExecutionFinished) { execFinishedDelivered = true }
    }.apply {
      assertNotNull(this)
    }.await()

    assertTrue(executed, "Task was not executed")
    assertTrue(configuredEventDelivered, "`configured` event was not delivered")
    assertTrue(executionStartDelivered, "`executionStart` event was not delivered")
    assertTrue(taskReadyDelivered, "`taskReady` event was not delivered")
    assertTrue(taskExecutionDelivered, "`taskExecution` event was not delivered")
    assertTrue(taskCompletedDelivered, "`taskCompleted` event was not delivered")
    assertTrue(taskFinishedDelivered, "`taskFinished` event was not delivered")
    assertTrue(execCompletedDelivered, "`execCompleted` event was not delivered")
    assertTrue(execFinishedDelivered, "`execFinished` event was not delivered")
  }
}
