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

/**
 *
 */
public sealed interface TaskGraphEvent : Event {
  /**
   *
   */
  public sealed interface GraphEvent : TaskGraphEvent

  /**
   *
   */
  public sealed interface TaskEvent : TaskGraphEvent

  /**
   *
   */
  public data object Configured : GraphEvent

  /**
   *
   */
  public data object ExecutionStart : GraphEvent

  /**
   *
   */
  public data object TaskReady : TaskEvent

  /**
   *
   */
  public data object TaskExecute : TaskEvent

  /**
   *
   */
  public data object TaskFork : TaskEvent

  /**
   *
   */
  public data object TaskExecuteFinished : TaskEvent

  /**
   *
   */
  public data object TaskProgress : TaskEvent

  /**
   *
   */
  public data object TaskFailed : TaskEvent

  /**
   *
   */
  public data object TaskCompleted : TaskEvent

  /**
   *
   */
  public data object TaskFinished : TaskEvent

  /**
   *
   */
  public data object ExecutionFailed : GraphEvent

  /**
   *
   */
  public data object ExecutionCompleted : GraphEvent

  /**
   *
   */
  public data object ExecutionFinished : GraphEvent
}
