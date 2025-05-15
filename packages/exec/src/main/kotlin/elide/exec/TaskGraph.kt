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
@file:Suppress("UnstableApiUsage", "CONTEXT_RECEIVERS_DEPRECATED")

package elide.exec

import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import com.google.errorprone.annotations.Immutable
import com.google.errorprone.annotations.ThreadSafe
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import elide.exec.Action.ActionContext
import elide.exec.Task.Companion.fn
import com.google.common.graph.Graph as GuavaGraph
import com.google.common.graph.MutableGraph as GuavaMutableGraph
import kotlinx.coroutines.flow.flow as createFlow

/**
 * Structure to use for a task graph.
 */
public typealias TaskGraphStruct = GuavaGraph<Task>

/**
 * Structure to use for a mutable task graph.
 */
public typealias MutableTaskGraphStruct = GuavaMutableGraph<Task>

/**
 * Structure to use for building a new task graph.
 */
public typealias TaskGraphBuilder = ImmutableGraph.Builder<Task>

/**
 * Structure to use for building a new mutable task graph.
 */
public typealias MutableTaskGraphBuilder = MutableTaskGraphStruct

@DslMarker
public annotation class TaskDsl

/**
 * Creates a new task within an immutable task graph.
 *
 * This is a DSL function which is designed to be used while building a task graph; the utility spawns an arbitrary
 * [Action] which executes the provided [block].
 *
 * By default, these simple tasks have no inputs, outputs, or dependencies.
 *
 * @param name Name to use for the task, if any; defaults to `null`, which creates an anonymous task.
 * @param id Optional explicit task ID to use for this task; defaults to `null`, which generates one.
 * @param block Code to run when this task is invoked; this block will be executed in the context of an [ActionContext].
 */
context(TaskGraphBuilder, ActionScope)
@TaskDsl public inline fun <reified T: Any> task(
  name: String? = null,
  id: TaskId? = null,
  type: KClass<T> = T::class,
  crossinline block: suspend ActionContext.() -> T,
): Task {
  val task = fn(name = name, id = id) {
    runCatching {
      val result = actionContext.block()
      when (type) {
        Result::class -> result
        else -> Result.Something(result)
      }
    }.exceptionOrNull().let {
      when (it) {
        null -> Result.Nothing
        else -> Result.ThrowableFailure(it)
      }
    }
  }
  addNode(task)
  return task
}

/**
 * Add a dependency between tasks while building an immutable task graph.
 */
context(TaskGraphBuilder, ActionScope)
public fun Task.dependsOn(other: Task) {
  putEdge(this, other)
}

/**
 * Add a dependency between tasks while filling a mutable task graph.
 */
context(TaskGraph.Mutable)
public fun Task.dependsOn(other: Task) {
  edge(this, other)
}

/**
 * # Task Graph
 */
@Serializable
public sealed interface TaskGraph : Graph<TaskId, RootTask, Task> {
  /**
   * Root task for this graph; always present.
   */
  override val root: RootTask

  /**
   * Poll for tasks which are eligible for execution.
   */
  public fun poll(): Sequence<Task>

  /**
   * Returns `true` if no task triggered build failure.
   */
  public fun isOk(): Boolean

  /**
   * Returns `true` if all tasks have terminally completed.
   */
  public fun isComplete(): Boolean

  /**
   * Poll for tasks which are eligible for execution.
   */
  public fun flow(): Flow<Task>

  /**
   * Poll for tasks which are eligible for execution.
   */
  public fun flowFor(taskId: TaskId): Flow<Task?>

  /**
   * Copy this immutable task graph into a mutable form; if this graph is already mutable, this will return the same
   * object.
   *
   * @return Mutable task graph.
   */
  public fun toMutable(): Mutable

  /**
   * ## Task Graph (Mutable)
   */
  public sealed interface Mutable : TaskGraph, Graph.MutableGraph<TaskId, RootTask, Task> {
    /**
     * Build this mutable task graph into an immutable form.
     *
     * @return Immutable task graph.
     */
    public fun build(): TaskGraph
  }

  /**
   * ## Default Task Graph
   *
   * Holds a triple of a [RootTask], a [TaskGraphStruct], and a [SortedSet] of held [TaskId] instances. This instance is
   * designed to be immutable.
   */
  @JvmInline @Immutable @ThreadSafe public value class DefaultTaskGraph internal constructor (
    private val graph: Triple<RootTask, TaskGraphStruct, Map<TaskId, Task>>,
  ) : TaskGraph {
    internal constructor(graph: TaskGraphStruct, taskMap: Map<TaskId, Task>) :
      this(Triple(RootTask.Default, graph, taskMap))

    override val root: RootTask get() = graph.first
    override val nodeCount: UInt get() = graph.third.size.toUInt()
    override val edgeCount: UInt get() = graph.second.edges().size.toUInt()
    override fun contains(node: Task): Boolean = graph.second.nodes().contains(node)
    override fun contains(id: TaskId): Boolean = graph.third.contains(id)
    override fun get(id: TaskId): Task = graph.third[id] ?: throw NoSuchElementException(id.toString())
    override fun findByName(id: TaskId): Task? = graph.third[id]
    override fun isComplete(): Boolean = graph.second.nodes().all { it.isSatisfied }
    override fun isOk(): Boolean = graph.second.nodes().all { it.status != Status.FAIL }

    private fun taskIsReady(task: Task): Boolean {
      return when (task.status) {
        Status.RUNNING,
        Status.SUCCESS,
        Status.FAIL,
        Status.NONE -> false // already running or completed

        else -> {
          val inputs = task.inputs
          val deps = task.dependencies
          val precursorsReady = inputs.isSatisfied && deps.isSatisfied
          if (!precursorsReady) {
            return false // waiting for inputs or dependencies to resolve
          }
          val anyPrecursorsFailed = deps.status == Status.FAIL
          if (anyPrecursorsFailed) {
            task.transition(Status.FAIL)
            return false
          }
          return when (graph.second.degree(task)) {
            0 -> true // precursors are ready and there are no task dependencies
            else -> graph.second.successors(task).all { it.isSatisfied }
          }
        }
      }
    }

    override fun poll(): Sequence<Task> = sequence {
      for (node in graph.second.nodes()) {
        if (node !is RootTask) {
          if (taskIsReady(node)) {
            node.transition(Status.RUNNING)
            yield(node)
          }
        }
      }
    }

    override fun flow(): Flow<Task> = createFlow {
      do {
        poll().let { seq ->
          when (seq.count() == 0) {
            true -> { /* nothing to do right now */ }
            else -> emitAll(seq.asFlow())
          }
        }
      } while (!isComplete() && isOk())
    }

    override fun flowFor(taskId: TaskId): Flow<Task?> = createFlow {
      var ready = false
      val target = get(taskId)

      do {
        // start at the provided task, and emit tasks until we are complete
        val inputs = target.inputs
        val deps = target.dependencies
        val precursorsReady = inputs.isSatisfied && deps.isSatisfied
        val anyPrecursorsFailed = deps.status == Status.FAIL
        val depTasks = graph.second.successors(target)
        val depTasksReady = depTasks.all { it.isSatisfied }

        when {
          precursorsReady && depTasksReady -> ready = true
          anyPrecursorsFailed -> {
            // propagate failures
            target.transition(Status.FAIL)
            ready = false
          }

          else -> {
            // emit all dependency tasks
            for (depTask in depTasks) {
              if (taskIsReady(depTask)) {
                depTask.transition(Status.RUNNING)
                emit(depTask)
              }
            }
          }
        }
      } while (!ready)

      var isRunning = false
      do {
        when (isRunning) {
          false -> {
            // finally, execute the task itself
            isRunning = true
            target.transition(Status.RUNNING)
            emit(target)
          }
          true -> {
            Thread.sleep(1)
          }
        }
      } while (!target.isSatisfied)

      emit(null) // we are done
    }

    override fun toMutable(): Mutable = mutable().apply {
      graph.second.nodes().forEach { add(it) }
      graph.second.edges().forEach { it.source().dependsOn(it.target()) }
    }

    override fun asSequence(): Sequence<Task> = sequence {
      yield(root)
      yieldAll(graph.second.nodes())
    }
  }

  /**
   * ## Mutable Task Graph
   */
  public class MutableTaskGraph(
    private val graph: MutableTaskGraphStruct,
    private val idMap: MutableMap<TaskId, Task> = mutableMapOf(),
    private val ids: MutableSet<TaskId> = TreeSet(),
    override val root: RootTask = RootTask.Default,
  ) : Mutable, MutableTaskGraphBuilder by graph {
    override fun contains(id: TaskId): Boolean = ids.contains(id)
    override fun contains(node: Task): Boolean = graph.nodes().contains(node)
    override val nodeCount: UInt get() = ids.size.toUInt()
    override val edgeCount: UInt get() = graph.edges().size.toUInt()
    override fun get(id: TaskId): Task = idMap[id] ?: throw NoSuchElementException(id.toString())
    override fun findByName(id: TaskId): Task? = idMap[id]
    override fun toMutable(): Mutable = this
    override fun isComplete(): Boolean = graph.nodes().all { it.isSatisfied }
    override fun isOk(): Boolean = graph.nodes().all { it.isSatisfied && it.status != Status.FAIL }

    override fun poll(): Sequence<Task> {
      TODO("Not yet implemented: mutable task graph poll")
    }

    override fun flow(): Flow<Task> {
      TODO("Not yet implemented: mutable task graph flow")
    }

    override fun flowFor(taskId: TaskId): Flow<Task> {
      TODO("Not yet implemented")
    }

    override fun edge(from: Task, to: Task) {
      graph.putEdge(from, to)
    }

    override fun Task.dependsOn(other: Task) {
      if (other.id !in ids) {
        graph.addNode(other)
        ids.add(other.id)
      }
      graph.putEdge(this, other)
    }

    override fun asSequence(): Sequence<Task> = sequence {
      yield(root)
      yieldAll(graph.nodes())
    }

    override fun add(node: Task) {
      graph.addNode(node)
      ids.add(node.id)
    }

    override fun build(): TaskGraph = build {
      val that = this
      graph.nodes().forEach {
        that.addNode(it)
      }
      graph.edges().forEach {
        val src = it.source()
        val tgt = it.target()
        that.putEdge(src, tgt)
      }
    }
  }

  /** Factories for creating or obtaining instances of [TaskGraph]. */
  public companion object {
    /** @return Immutable task graph with the provided [root] node, [graph] data, and [ids]. */
    @JvmStatic public fun of(graph: TaskGraphStruct, ids: Map<TaskId, Task>): TaskGraph {
      return DefaultTaskGraph(graph, ids)
    }

    /** @return Mutable task graph. */
    @JvmStatic public fun mutable(): Mutable = MutableTaskGraph(
      GraphBuilder
        .directed()
        .allowsSelfLoops(false)
        .build<Task>()
    )

    /** @return Builder for a task graph. */
    @JvmStatic public fun builder(): TaskGraphBuilder = GraphBuilder
      .directed()
      .allowsSelfLoops(false)
      .immutable<Task>()

    /** @return Built task graph prepared by the provided [block]. */
    @JvmStatic public fun build(block: TaskGraphBuilder.() -> Unit): TaskGraph {
      val builder = builder()
      builder.block()
      return build(builder)
    }

    /** @return Built task graph prepared by the provided [builder]. */
    @JvmStatic public fun build(builder: TaskGraphBuilder): TaskGraph {
      val graph = builder.build()
      val ids = graph.nodes().associateByTo(TreeMap()) { it.id }
      return of(graph, ids)
    }
  }
}
