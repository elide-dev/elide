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

import kotlinx.serialization.Serializable

/**
 * Serializable copy of [TrackedTask] for testing.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
data class SerializableTask(
  val name: String,
  val target: Int,
  val status: String = "",
  val position: Int = -1,
  val output: Map<Long, String> = mapOf(),
  val failed: Boolean = false,
) {
  fun convert(): TrackedTask = TrackedTask(name, target, status, position, output, failed)
}
