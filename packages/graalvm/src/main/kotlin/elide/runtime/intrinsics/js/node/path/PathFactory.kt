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
package elide.runtime.intrinsics.js.node.path

import java.io.File
import elide.runtime.node.path.PathStyle

/**
 * # Path Factory
 *
 * Describes the layout of static methods which can be used to create [Path] instances; paths of this type are compliant
 * with Node's `path` built-in module.
 */
public interface PathFactory {
  /**
   * ## Path from String
   *
   * Create a Node-style parsed [Path] object from a string [path]; this method uses the active path style if none is
   * provided.
   *
   * @param path Path to parse
   * @param style Path style to use; if not specified; the default path style is used
   */
  public fun from(path: String, style: PathStyle? = null): Path

  /**
   * ## Path from String Segments
   *
   * Create a Node-style parsed [Path] object from string segments; this method uses the active path style.
   *
   * @param first First portion of the path
   * @param rest Remaining portions of the path
   */
  public fun from(first: String, vararg rest: String): Path

  /**
   * ## Path from Kotlin Path
   *
   * Create a Node-style parsed [Path] object from a Kotlin path.
   *
   * @param path Path to parse
   */
  public fun from(path: kotlinx.io.files.Path): Path

  /**
   * ## Path from Java Path
   *
   * Create a Node-style parsed [Path] object from a Java NIO path.
   *
   * @param path Path to parse
   */
  public fun from(path: java.nio.file.Path): Path

  /**
   * ## Path from Java File
   *
   * Create a Node-style parsed [Path] object from a Java I/O file.
   *
   * @param path Path to parse
   */
  public fun from(path: File): Path

  /**
   * ## Path from Node Path
   *
   * Effectively copies another Node-compliant [Path] type.
   *
   * @param path Node-compliant path type to copy
   * @return A new [Path] instance with the same path information
   */
  public fun from(path: Path): Path
}
