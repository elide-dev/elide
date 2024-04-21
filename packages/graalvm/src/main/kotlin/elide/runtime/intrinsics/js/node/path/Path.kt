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

package elide.runtime.intrinsics.js.node.path

import java.io.File
import elide.runtime.gvm.internals.node.path.PathBuf
import elide.runtime.gvm.internals.node.path.PathStyle
import elide.runtime.intrinsics.js.StringLike
import elide.vm.annotations.Polyglot

/**
 * # Node API: Path
 *
 * Describes a "path object," as specified by the Node JS "Path" built-in module. Path objects carry information about a
 * parsed path, or allow the user to specify path information in a structured way. Certain methods in the Path API can
 * accept an object of this type.
 */
public interface Path : Comparable<Path>, StringLike {
  /**
   * ## Node API: Path Factory
   *
   * Defines methods for creating paths from various primitive values, including [String] instances, and Kotlin and Java
   * standard path types.
   */
  public companion object : PathFactory {
    override fun from(path: String, style: PathStyle?): Path = PathBuf.from(path, style)
    override fun from(first: String, vararg rest: String): Path = PathBuf.from(first, *rest)
    override fun from(path: kotlinx.io.files.Path): Path = PathBuf.from(path)
    override fun from(path: java.nio.file.Path): Path = PathBuf.from(path)
    override fun from(path: File): Path = PathBuf.from(path.path)
    override fun from(path: Path): Path = PathBuf.from(path)
  }

  /**
   * ## Kotlin Path
   *
   * Utility to convert this Node-style path to a Kotlin path.
   */
  public fun toKotlinPath(): kotlinx.io.files.Path

  /**
   * ## Java Path
   *
   * Utility to convert this Node-style path to a Java path.
   */
  public fun toJavaPath(): java.nio.file.Path

  /**
   * ## Copy Path
   *
   * Creates a new path object with the same properties as this one.
   */
  public fun copy(): Path

  /**
   * ## Split Path
   *
   * Split this path by the delimiter for this path type, yielding each string section as a sequence entry.
   */
  public fun split(): Sequence<String>

  /**
   * ## Path Style
   *
   * Describes the style of the path, which is used to determine how the path is rendered.
   */
  public val style: PathStyle

  /**
   * ## Join Path
   *
   * Join this path by the delimiter for this path type with another path, yielding a path composed of both.
   */
  @Polyglot public fun join(vararg paths: Path): String

  /**
   * ## Join Path by String
   *
   * Join this path by the delimiter for this path type with another path, yielding a path composed of both.
   */
  @Polyglot public fun join(first: String, vararg rest: String): String

  /**
   * ## Join Path by Segments
   *
   * Join this path by the delimiter for this path type with another path, yielding a path composed of both.
   */
  @Polyglot public fun join(other: Iterable<String>): String

  /**
   * ## Path Properties: Directory
   *
   * Holds the base directory, or directory prefix, which is applied to the rendered path; if `dir` is specified, any
   * present value for [root] is ignored.
   */
  @get:Polyglot public val dir: String

  /**
   * ## Path Properties: Root
   *
   * Holds the root directory, if any, of the path. Only one of `root` or [dir] may be specified; if both are provided,
   * [dir] wins.
   */
  @get:Polyglot public val root: String

  /**
   * ## Path Properties: Base
   *
   * Describes the "basename" of the desired path, which is the combined portion of the file name and file extension. If
   * this property (`base`) is specified, [name] and [ext] are used instead.
   */
  @get:Polyglot public val base: String

  /**
   * ## Path Properties: Name
   *
   * Describes the naked "name" of the file, without any file extension; if specified, [ext] is also consulted, and any
   * value for [base] is ignored.
   */
  @get:Polyglot public val name: String

  /**
   * ## Path Properties: Extension
   *
   * Describes the file extension to be used or which is present for the path; if specified, [name] is also consulted,
   * and any present value for [base] is ignored.
   */
  @get:Polyglot public val ext: String

  /**
   * ## Generic Paths: Is Absolute
   *
   * Determine if the underlying path is an absolute expression of the path segments.
   */
  @get:Polyglot public val isAbsolute: Boolean
}
