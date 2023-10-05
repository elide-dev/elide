/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

/**
 * # Protocol Buffers: Syntax Version
 *
 * Describes the supported/known syntax versions for protocol buffers schema syntax. At the time of this writing, there
 * are two syntax versions: `2` and `3`, referred to as [PROTO2] and [PROTO3].
 *
 * @param symbol Symbol to be emitted when selecting this syntax.
 */
public enum class ProtoBufSyntaxVersion constructor (internal val symbol: String) {
  /**
   * ## Syntax: `proto`.
   *
   * Describes protocol buffers syntax version 2, which is the default syntax version for older versions of the protocol
   * buffers library. This syntax version is not recommended for new projects.
   */
  PROTO2("proto2"),

  /**
   * ## Syntax: `proto3`.
   *
   * Describes protocol buffers syntax version 3, which is the current and modern version of the protocol buffers syntax
   * which is recommended for new projects.
   */
  PROTO3("proto3");
}
