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

import kotlinx.serialization.Serializable
import elide.exec.TaskId.BoundTaskId

/**
 * # Task ID
 */
@Serializable
public sealed interface TaskId : NodeId, Comparable<TaskId> {
  /**
   * Task ID associated with a name.
   */
  public interface Named : TaskId {
    /**
     * Name of this task.
     */
    public val name: String
  }

  /**
   * Built-in task ID, defined by a type and singleton.
   */
  public sealed interface Builtin : TaskId

  /**
   * ## Named Task ID
   *
   * Simple task ID which is defined by a string name.
   */
  @JvmInline public value class NamedTaskId internal constructor (private val id: String) : TaskId, Named {
    override val name: String get() = id
    override fun toString(): String = id
    override fun compareTo(other: TaskId): Int {
      return if (other is NamedTaskId) {
        id.compareTo(other.id)
      } else {
        -1
      }
    }
  }

  /**
   * ## Hash Code Task ID
   *
   * Simple task ID which is defined by a hash code value.
   */
  @JvmInline public value class HashCodeTaskId internal constructor (private val id: Int) : TaskId {
    override fun toString(): String = id.toString()
    override fun compareTo(other: TaskId): Int {
      return if (other is HashCodeTaskId) {
        id.compareTo(other.id)
      } else {
        -1
      }
    }
  }

  @JvmInline public value class BoundTaskId(private val graph: TaskGraph) {
    public operator fun getValue(thisRef: Nothing?, property: kotlin.reflect.KProperty<*>): Task {
      return graph[fromName(property.name)]
    }
  }

  /** Factories for creating or obtaining [TaskId] instances. */
  public companion object {
    /**
     * Create a stable [TaskId] from the provided simple [name].
     *
     * @param name Name of the task.
     * @return Task ID for the provided name.
     */
    @JvmStatic public fun fromName(name: String): TaskId = NamedTaskId(name)

    /**
     * Create a stable [TaskId] from the hash code of the provided [value].
     *
     * @param value Value to hash.
     * @return Task ID for the provided value.
     */
    @JvmStatic public fun defaultFrom(value: Any): TaskId = HashCodeTaskId(value.hashCode())
  }
}

/**
 * Bind a stable [Task] from the provided [TaskGraph] context.
 *
 * @return Bound task for the provided name.
 */
public fun TaskGraph.tasks(): BoundTaskId {
  return BoundTaskId(this)
}
