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
 * Source position: describes a zero-based absolute position within a source file.
 */
public typealias SourcePosition = UInt

/**
 * Relative source position: describes a zero-based offset-expressed position within a source file, relative to some
 * other position (typically the first type provided as a [SourcePosition]).
 */
public typealias SourceRelativePosition = UInt

/**
 * ## Source Span
 *
 * Describes a span of source code, expressed as a [start] and (relative) [end] position within some source file or
 * snippet of code.
 *
 * @property start Start position of the span, expressed as an absolute [SourcePosition].
 * @property end End position of the span, expressed as a relative [SourceRelativePosition].
 */
@Serializable
@JvmRecord public data class SourceSpan internal constructor (
  public val start: SourcePosition,
  public val end: SourceRelativePosition,
)
