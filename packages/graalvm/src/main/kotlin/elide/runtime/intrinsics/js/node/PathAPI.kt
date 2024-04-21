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

package elide.runtime.intrinsics.js.node

import elide.runtime.intrinsics.js.node.path.Path as NodePath
import kotlinx.io.files.Path
import elide.annotations.API
import elide.runtime.gvm.internals.node.path.PathStyle
import elide.vm.annotations.Polyglot

/**
 * # Node API: `path`
 *
 * Describes the API provided by the Node API built-in `path` module, which supplies utilities for dealing with file and
 * directory paths. Routines are provided for joining, parsing, and extracting bits of information from paths.
 *
 * &nbsp;
 *
 * ## Summary
 *
 * The default implementation of the `path` module supplies path logic for the operating system on which the program is
 * currently running; specific implementations for POSIX and Windows are also provided. The module provides utilities
 * for common path operations, including:
 *
 * - `basename()`: Extracts the last portion of a path.
 * - `dirname()`: Extracts the directory portion of a path.
 * - `extname()`: Extracts the file extension from a path.
 * - `isAbsolute()`: Determines if a path is absolute.
 * - `join()`: Joins multiple path segments together.
 * - `normalize()`: Normalizes a path.
 * - `parse()`: Parses a path into an object.
 * - `relative()`: Determines the relative path between two paths.
 * - `resolve()`: Resolves a sequence of paths or path segments into an absolute path.
 * - `toNamespacedPath()`: Converts a path to a namespace-prefixed path.
 * - `format()`: Converts a path object to a path string.
 *
 * Several configuration properties are also provided, including:
 *
 * - `sep`: The path segment separator.
 * - `delimiter`: The path delimiter.
 *
 * &nbsp;
 *
 * ## Specification compliance
 *
 * This formulation of the Node Path API follows the official definition of the `path` module for Node version `21.7.3`,
 * which is current at the time of this writing. The specification for this API can be found
 * [here](https://nodejs.org/api/path.html).
 *
 * Differences in behavior between Node's implementation of this API are usually considered bugs; where this behavior is
 * intentional, it will be noted in this section.
 *
 * &nbsp;
 *
 * ## Behavior in Elide
 *
 * Elide doesn't provide host I/O access by default, and in the case of embedded VFS use, paths are considered under
 * Unix-style rules unconditionally. Most path function implementations use the underlying JVM NIO path utilities.
 *
 * &nbsp;
 *
 * ## Further reading
 *
 * - [Node.js `path` module](https://nodejs.org/api/path.html)
 * - [Java NIO `Path`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Path.html)
 */
@API public interface PathAPI : NodeAPI {
  /**
   * ## Separator
   *
   * Provides the platform-specific path segment separator:
   * - On POSIX systems, this is `/`.
   * - On Windows systems, this is `\`.
   *
   * @see [Node.js `path.sep`](https://nodejs.org/api/path.html#path_path_sep)
   */
  @get:Polyglot public val sep: String

  /**
   * ## Delimiter
   *
   * Provides the platform-specific path delimiter:
   * - On POSIX systems, this is `:`.
   * - On Windows systems, this is `;`.
   *
   * @see [Node.js `path.delimiter`](https://nodejs.org/api/path.html#path_path_delimiter)
   */
  @get:Polyglot public val delimiter: String

  /**
   * ## POSIX Path Utilities
   *
   * Provides utilities implementing the [NodeAPI] for POSIX-style paths, regardless of the current operating system.
   * Paths passed to this instance will forcibly be considered as [PathStyle.POSIX]-style paths.
   *
   * @see [Node.js `path.posix`](https://nodejs.org/api/path.html#path_path_posix)
   */
  @get:Polyglot public val posix: PathAPI

  /**
   * ## Windows Path Utilities
   *
   * Provides utilities implementing the [NodeAPI] for Windows-style paths, regardless of the current operating system.
   * Paths passed to this instance will forcibly be considered as [PathStyle.WIN32]-style paths.
   *
   * @see [Node.js `path.win32`](https://nodejs.org/api/path.html#path_path_win32)
   */
  @get:Polyglot public val win32: PathAPI

  /**
   * ## `basename()`
   *
   * Extracts the last portion of a path.
   *
   * This method variant accepts a string path and returns a string path.
   *
   * @param path The path to extract the basename from.
   * @return The extracted basename.
   * @see [Node.js `path.basename()`](https://nodejs.org/api/path.html#path_path_basename_path_ext)
   */
  @Polyglot public fun basename(path: String, ext: String? = null): String = basename(NodePath.from(path), ext)

  /**
   * ## `basename()`
   *
   * Extracts the last portion of a path.
   *
   * This method variant accepts a [Path] object and returns a [String].
   *
   * @param path The path to extract the basename from.
   * @return The extracted basename as a Kotlin path.
   * @see [Node.js `path.basename()`](https://nodejs.org/api/path.html#path_path_basename_path_ext)
   */
  @Polyglot public fun basename(path: Path, ext: String? = null): String = basename(NodePath.from(path), ext)

  /**
   * ## `basename()`
   *
   * Extracts the last portion of a path.
   *
   * This method variant accepts a [NodePath] object and returns a [String].
   *
   * @param path The path to extract the basename from.
   * @return The extracted basename as a Node path.
   * @see [Node.js `path.basename()`](https://nodejs.org/api/path.html#path_path_basename_path_ext)
   */
  @Polyglot public fun basename(path: NodePath, ext: String? = null): String

  /**
   * ## `dirname()`
   *
   * Extracts the directory portion of a path.
   *
   * This method variant accepts a string path and returns a string path.
   *
   * @param path The path to extract the directory from.
   * @return The extracted directory.
   * @see [Node.js `path.dirname()`](https://nodejs.org/api/path.html#path_path_dirname_path)
   */
  @Polyglot public fun dirname(path: String): String = dirname(NodePath.from(path)) ?: ""

  /**
   * ## `dirname()`
   *
   * Extracts the directory portion of a path.
   *
   * This method variant accepts a [Path] object and returns a [String] object.
   *
   * @param path The path to extract the directory from.
   * @return The extracted directory as a Kotlin path.
   * @see [Node.js `path.dirname()`](https://nodejs.org/api/path.html#path_path_dirname_path)
   */
  @Polyglot public fun dirname(path: Path): String? = dirname(NodePath.from(path))

  /**
   * ## `dirname()`
   *
   * Extracts the directory portion of a path.
   *
   * This method variant accepts a [NodePath] object and returns a [String] object.
   *
   * @param path The path to extract the directory from.
   * @return The extracted directory as a Node path.
   * @see [Node.js `path.dirname()`](https://nodejs.org/api/path.html#path_path_dirname_path)
   */
  @Polyglot public fun dirname(path: NodePath): String?

  /**
   * ## `extname()`
   *
   * Extracts the file extension from a path.
   *
   * This method variant accepts a string path and returns a string path.
   *
   * @param path The path to extract the extension from.
   * @return The extracted extension.
   * @see [Node.js `path.extname()`](https://nodejs.org/api/path.html#path_path_extname_path)
   */
  @Polyglot public fun extname(path: String): String = extname(NodePath.from(path))

  /**
   * ## `extname()`
   *
   * Extracts the file extension from a path.
   *
   * This method variant accepts a [Path] object and returns a [String] object.
   *
   * @param path The path to extract the extension from.
   * @return The extracted extension as a Kotlin path.
   * @see [Node.js `path.extname()`](https://nodejs.org/api/path.html#path_path_extname_path)
   */
  @Polyglot public fun extname(path: Path): String = extname(NodePath.from(path))

  /**
   * ## `extname()`
   *
   * Extracts the file extension from a path.
   *
   * This method variant accepts a [NodePath] object and returns a [String] object.
   *
   * @param path The path to extract the extension from.
   * @return The extracted extension as a Node path.
   * @see [Node.js `path.extname()`](https://nodejs.org/api/path.html#path_path_extname_path)
   */
  @Polyglot public fun extname(path: NodePath): String

  /**
   * ## `isAbsolute()`
   *
   * Determines if a path is absolute.
   *
   * This method variant accepts a string path and returns a boolean.
   *
   * @param path The path to check.
   * @return `true` if the path is absolute; `false` otherwise.
   * @see [Node.js `path.isAbsolute()`](https://nodejs.org/api/path.html#path_path_isabsolute_path)
   */
  @Polyglot public fun isAbsolute(path: String): Boolean = isAbsolute(NodePath.from(path))

  /**
   * ## `isAbsolute()`
   *
   * Determines if a path is absolute.
   *
   * This method variant accepts a [Path] object and returns a boolean.
   *
   * @param path The path to check.
   * @return `true` if the path is absolute; `false` otherwise.
   * @see [Node.js `path.isAbsolute()`](https://nodejs.org/api/path.html#path_path_isabsolute_path)
   */
  @Polyglot public fun isAbsolute(path: Path): Boolean = isAbsolute(NodePath.from(path))

  /**
   * ## `isAbsolute()`
   *
   * Determines if a path is absolute.
   *
   * This method variant accepts a [NodePath] object and returns a boolean.
   *
   * @param path The path to check.
   * @return `true` if the path is absolute; `false` otherwise.
   * @see [Node.js `path.isAbsolute()`](https://nodejs.org/api/path.html#path_path_isabsolute_path)
   */
  @Polyglot public fun isAbsolute(path: NodePath): Boolean

  /**
   * ## `join()`
   *
   * Joins multiple path segments together.
   *
   * This method variant accepts a string path and returns a string path.
   *
   * @param first The first path segment.
   * @param rest The remaining path segments.
   * @return The joined path.
   * @see [Node.js `path.join()`](https://nodejs.org/api/path.html#path_path_join_paths)
   */
  @Polyglot public fun join(first: String, vararg rest: String): String =
    join(NodePath.from(first), *rest.map { NodePath.from(it) }.toTypedArray())

  /**
   * ## `join()`
   *
   * Joins multiple path segments together.
   *
   * This method variant accepts a [Path] object and returns a [String].
   *
   * @param first The first path segment.
   * @param rest The remaining path segments.
   * @return The joined path as a Kotlin path.
   * @see [Node.js `path.join()`](https://nodejs.org/api/path.html#path_path_join_paths)
   */
  @Polyglot public fun join(first: Path, vararg rest: Path): String =
    join(NodePath.from(first), *rest.map { NodePath.from(it) }.toTypedArray())

  /**
   * ## `join()`
   *
   * Joins multiple path segments together.
   *
   * This method variant accepts a [NodePath] object and returns a [String].
   *
   * @param first The first path segment.
   * @param rest The remaining path segments.
   * @return The joined path as a Node path.
   * @see [Node.js `path.join()`](https://nodejs.org/api/path.html#path_path_join_paths)
   */
  @Polyglot public fun join(first: NodePath, vararg rest: NodePath): String

  /**
   * ## `normalize()`
   *
   * Normalizes a path.
   *
   * This method variant accepts a string path and returns a string path.
   *
   * @param path The path to normalize.
   * @return The normalized path.
   * @see [Node.js `path.normalize()`](https://nodejs.org/api/path.html#path_path_normalize_path)
   */
  @Polyglot public fun normalize(path: String): String = normalize(NodePath.from(path))

  /**
   * ## `normalize()`
   *
   * Normalizes a path.
   *
   * This method variant accepts a [Path] object and returns a [String].
   *
   * @param path The path to normalize.
   * @return The normalized path as string.
   * @see [Node.js `path.normalize()`](https://nodejs.org/api/path.html#path_path_normalize_path)
   */
  @Polyglot public fun normalize(path: Path): String = normalize(NodePath.from(path))

  /**
   * ## `normalize()`
   *
   * Normalizes a path.
   *
   * This method variant accepts a [NodePath] object and returns a [String].
   *
   * @param path The path to normalize.
   * @return The normalized path as a Node path.
   * @see [Node.js `path.normalize()`](https://nodejs.org/api/path.html#path_path_normalize_path)
   */
  @Polyglot public fun normalize(path: NodePath): String

  /**
   * ## `parse()`
   *
   * Parses a path into an object.
   *
   * This method variant accepts a string path and returns a [NodePath] object.
   *
   * @param path The path to parse.
   * @return The parsed path.
   * @see [Node.js `path.parse()`](https://nodejs.org/api/path.html#path_path_parse_path)
   */
  @Polyglot public fun parse(path: String): NodePath = parse(path, null)

  /**
   * ## `parse()`
   *
   * Parses a path into an object.
   *
   * This method variant accepts a string path and a path style and returns a [NodePath] object. This is available for
   * host-side use only.
   *
   * @param path The path to parse.
   * @param pathStyle The path style to use.
   * @return The parsed path.
   */
  public fun parse(path: String, pathStyle: PathStyle?): NodePath

  /**
   * ## `relative()`
   *
   * Determines the relative path between two paths.
   *
   * This method variant accepts two string paths and returns a string path.
   *
   * @param from The source path.
   * @param to The target path.
   * @return The relative path as a string.
   * @see [Node.js `path.relative()`](https://nodejs.org/api/path.html#path_path_relative_from_to)
   */
  @Polyglot public fun relative(from: String, to: String): String = relative(NodePath.from(from), NodePath.from(to))

  /**
   * ## `relative()`
   *
   * Determines the relative path between two paths.
   *
   * This method variant accepts two [Path] objects and returns a [String].
   *
   * @param from The source path.
   * @param to The target path.
   * @return The relative path as a string.
   * @see [Node.js `path.relative()`](https://nodejs.org/api/path.html#path_path_relative_from_to)
   */
  @Polyglot public fun relative(from: Path, to: Path): String = relative(NodePath.from(from), NodePath.from(to))

  /**
   * ## `relative()`
   *
   * Determines the relative path between two paths.
   *
   * This method variant accepts two [NodePath] objects and returns a [String].
   *
   * @param from The source path.
   * @param to The target path.
   * @return The relative path as a string.
   * @see [Node.js `path.relative()`](https://nodejs.org/api/path.html#path_path_relative_from_to)
   */
  @Polyglot public fun relative(from: NodePath, to: NodePath): String

  /**
   * ## `resolve()`
   *
   * Resolves a sequence of paths or path segments into an absolute path.
   *
   * This method variant accepts a string path and returns a string path.
   *
   * @param first The first path to resolve.
   * @param rest The remaining paths to resolve.
   * @return The resolved path as a string.
   * @see [Node.js `path.resolve()`](https://nodejs.org/api/path.html#path_path_resolve_paths)
   */
  @Polyglot public fun resolve(first: String, vararg rest: String): String =
    resolve(NodePath.from(first), *rest.map { NodePath.from(it) }.toTypedArray())

  /**
   * ## `resolve()`
   *
   * Resolves a sequence of paths or path segments into an absolute path.
   *
   * This method variant accepts a [Path] object and returns a [String].
   *
   * @param first The first path to resolve.
   * @param rest The remaining paths to resolve.
   * @return The resolved path as a string.
   * @see [Node.js `path.resolve()`](https://nodejs.org/api/path.html#path_path_resolve_paths)
   */
  @Polyglot public fun resolve(first: Path, vararg rest: Path): String =
    resolve(NodePath.from(first), *rest.map { NodePath.from(it) }.toTypedArray())

  /**
   * ## `resolve()`
   *
   * Resolves a sequence of paths or path segments into an absolute path.
   *
   * This method variant accepts a [NodePath] object and returns a [String].
   *
   * @param first The first path to resolve.
   * @param rest The remaining paths to resolve.
   * @return The resolved path as a string.
   * @see [Node.js `path.resolve()`](https://nodejs.org/api/path.html#path_path_resolve_paths)
   */
  @Polyglot public fun resolve(first: NodePath, vararg rest: NodePath): String

  /**
   * ## `toNamespacedPath()`
   *
   * Converts a path to a namespace-prefixed path.
   *
   * This method variant accepts a string path and returns a string path.
   *
   * @param path The path to convert.
   * @return The converted path.
   */
  @Polyglot public fun toNamespacedPath(path: String): String = toNamespacedPath(NodePath.from(path))

  /**
   * ## `toNamespacedPath()`
   *
   * Converts a path to a namespace-prefixed path.
   *
   * This method variant accepts a [Path] object and returns a [String].
   *
   * @param path The path to convert.
   * @return The converted path as a string.
   */
  @Polyglot public fun toNamespacedPath(path: Path): String = toNamespacedPath(NodePath.from(path))

  /**
   * ## `toNamespacedPath()`
   *
   * Converts a path to a namespace-prefixed path.
   *
   * This method variant accepts a [NodePath] object and returns a [String].
   *
   * @param path The path to convert.
   * @return The converted path as a Node path.
   */
  @Polyglot public fun toNamespacedPath(path: NodePath): String

  /**
   * ## `format()`
   *
   * Formats the provided [pathObject] as a string path; the path object may carry a complete set, or valid subset of,
   * the suite of properties defined on a [NodePath] object; these include:
   *
   * - `dir`: The directory portion of the path.
   * - `root`: The root directory of the path.
   * - `base`: The base name of the path.
   * - `name`: The file name of the path.
   * - `ext`: The file extension of the path.
   *
   * &nbsp;
   *
   * ### Rendering precedence
   *
   * The `base` property takes precedence over the `name` and `ext` properties, if both are specified; the `root`
   * property is prepended to the rendered path.
   *
   * &nbsp;
   *
   * ### File extensions
   *
   * In conformance with Node's behavior, file extensions are expected to carry their initial `.` character. No
   * normalization or transformation of the properties occurs.
   *
   * &nbsp;
   *
   * ### Accepted types
   *
   * In conformance with Node's behavior, the `pathObject` may be any object that carries the properties described
   * above, or it may be an instance of a parsed path, such as a [NodePath] object (or its derivatives), as produced by
   * the [parse] method.
   *
   * @param pathObject The path object to format.
   * @return The formatted path.
   * @see [Node.js `path.format()`](https://nodejs.org/api/path.html#path_path_format_pathobject)
   */
  @Polyglot public fun format(pathObject: Any): String
}
