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
package elide.exec

import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import elide.exec.TaskGraphEvent.*

/**
 * # Coordinator
 */
public interface Coordinator {
  public suspend fun satisfy(
    scope: ActionScope,
    graph: TaskGraph,
    binder: (TaskGraphExecution.Listener.() -> Unit)? = null,
  ): TaskGraphExecution

  public suspend fun satisfyFor(
    scope: ActionScope,
    graph: TaskGraph,
    taskId: TaskId,
    binder: (TaskGraphExecution.Listener.() -> Unit)?,
  ): TaskGraphExecution

  public sealed interface Options

  public data object Default : Coordinator {
    override suspend fun satisfy(
      scope: ActionScope,
      graph: TaskGraph,
      binder: ExecutionBinder?,
    ): TaskGraphExecution.Listener {
      val latch = CountDownLatch(1)
      return TaskGraphExecution.Listener(graph, scope, scope.taskScope, latch).also { execution ->
        scope.bind(execution)
        binder?.invoke(execution)
        execution.dispatch(Configured, graph)

        execution.rootJob = scope.async {
          execution.dispatch(ExecutionStart, graph)

          do {
            graph.poll().toList().let { seq ->
              when (seq.isEmpty()) {
                true -> { /* nothing to do */ }
                else -> seq.forEach { task ->
                  execution.dispatch(TaskReady, task)
                  task.executeTask(scope)
                }
              }
            }
          } while (!graph.isComplete())

          if (!graph.isOk()) {
            execution.dispatch(ExecutionFailed, graph)
          } else {
            execution.dispatch(ExecutionCompleted, graph)
          }
          execution.dispatch(ExecutionFinished, graph)
          latch.countDown()
        }
      }
    }

    override suspend fun satisfyFor(
      scope: ActionScope,
      graph: TaskGraph,
      taskId: TaskId,
      binder: ExecutionBinder?,
    ): TaskGraphExecution {
      val latch = CountDownLatch(1)
      return TaskGraphExecution.Listener(graph, scope, scope.taskScope, latch).also { execution ->
        scope.bind(execution)
        binder?.invoke(execution)
        execution.dispatch(Configured, graph)

        execution.rootJob = coroutineScope {
          async {
            graph.flowFor(taskId).let { flow ->
              flow.collect { task ->
                if (task == null) {
                  latch.countDown()
                } else {
                  val job = scope.async {
                    task.executeTask(scope)
                  }
                  scope.taskScope.jobs.add(job)
                  scope.register(job)
                }
              }
            }
          }
        }
      }
    }
  }
}
