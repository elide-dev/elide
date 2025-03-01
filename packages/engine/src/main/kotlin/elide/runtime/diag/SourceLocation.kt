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
package elide.runtime.diag

import kotlinx.serialization.Serializable

/**
 * ## Source Location
 *
 * Data class representing a source location; a source location uses the combination of a [line] and [column] to
 * identify where a [Diagnostic] was triggered in source code.
 *
 * See also: a [SourceSpan], which expresses this information as byte offsets.
 *
 * @param line Line number in the source file where the diagnostic was triggered.
 * @param column Column number in the source file where the diagnostic was triggered.
 */
@Serializable
@JvmRecord public data class SourceLocation (
  public val line: UInt,
  public val column: UInt,
)
