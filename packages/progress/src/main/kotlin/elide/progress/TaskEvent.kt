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

/**
 * An event used by [TaskCallback] to update the state of a task in a progress animation.
 *
 * @property status New status message, or `null` if the status message should not be updated.
 * @property position New progress position, or `null` if the progress position should not be updated.
 * @property message New console output, or `null` if no console output should be appended.
 * @property failed `true` if the task should be marked as failed.
 * @author Lauri Heino <datafox>
 */
public data class TaskEvent(
  val status: String? = null,
  val position: Int? = null,
  val message: String? = null,
  val failed: Boolean = false,
)
