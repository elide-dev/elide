/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded

/**
 * Enumerates the versions of the invocation protocol and the serialized data structures used for its operations.
 *
 * Note that while the binary exchange [formats][EmbeddedProtocolFormat] used by the runtime typically support
 * backwards-compatibility, using a matching version is recommended to enable the latest runtime features.
 */
public enum class EmbeddedProtocolVersion {
  /** Selects version 1.0 of the dispatch protocol. */
  V1_0,
}
