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

/**
 * Possible states of a [TrackedTask].
 *
 * @property displayName Text that should be displayed for the given state.
 * @author Lauri Heino <datafox>
 */
enum class TaskState(val displayName: String) {
  NOT_STARTED("not started"),
  RUNNING("running"),
  COMPLETED("completed"),
  FAILED("failed"),
}
