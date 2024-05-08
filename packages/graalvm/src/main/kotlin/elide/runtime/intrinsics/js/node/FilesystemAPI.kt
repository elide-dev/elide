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

import org.graalvm.polyglot.Value
import java.nio.file.AccessMode
import elide.annotations.API
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.runtime.intrinsics.js.node.fs.*
import elide.runtime.intrinsics.js.node.path.Path
import elide.vm.annotations.Polyglot

/**
 * # Node API: `fs`
 *
 * Describes the API made available via Node's built-in `fs` module, which implements filesystem operations in both
 * synchronous and asynchronous (callback) forms.
 *
 * Facilities are provided for reading and writing files, as well as iterating over directories, handling links, and
 * managing file descriptors.
 *
 * In Elide, filesystem operations are implemented on top of VFS, and use GraalVM's embedded I/O isolation.
 * The "view" of a filesystem available to a guest application is the composed form of the VFS and the host filesystem,
 * modulo application permissions.
 *
 * VFS sources can be things like the host filesystem, tarballs, in-memory or embedded data, or even remote filesystems.
 * Other than Elide's implementation mechanics, the API behaves as identically as possible to Node's built-in module.
 *
 * Each method is generally specified with two implementations: one for host-side use (with host types), and one for
 * entirely guest-side use, which accepts either [Value] or [Any] objects.
 *
 * &nbsp;
 *
 * ## I/O Isolation
 *
 * Filesystem operations are subject to I/O isolation policy in Elide; this means that files may be visible on the host
 * system, or may not be.
 *
 * Additionally, filesystem operations may be restricted to read-only status, in which case the methods provided by the
 * [WritableFilesystemAPI] will either not be available or will throw as if denied by the operating system.
 *
 * &nbsp;
 *
 * ## Interaction with NIO
 *
 * Most I/O operations under the hood are implemented with JDK NIO, including path translation, file reading and writing
 * and directory traversal, and streams.
 *
 * NIO documentation can be consulted for more information on the underlying behavior of these operations:
 * [Java NIO Docs](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/package-summary.html).
 *
 * &nbsp;
 *
 * ## Filesystem Paths
 *
 * This module makes extensive use of the [PathAPI] to resolve filesystem paths; operating system-specific path behavior
 * is defined in that module and documented there as well.
 *
 * &nbsp;
 *
 * See [Node's `fs` documentation](https://nodejs.org/api/fs.html) for more information.
 *
 * @see FilesystemPromiseAPI for a promise-based alternative to this API, made available at `fs/promises`.
 * @see WritableFilesystemAPI for writable extensions to this base API
 */
@API public interface FilesystemAPI {
  /**
   * ## Method: `fs.access`
   *
   * Tests a user's permissions for the file at the specified path; provides the results or an error to the callback.
   * This variant specifies the default mode of `F_OK`.
   *
   * @param path The path to the file to test access for.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun access(path: Value, callback: Value)

  /**
   * ## Method: `fs.access`
   *
   * Tests a user's permissions for the file at the specified path; provides the results or an error to the callback.
   * This variant accepts a [mode].
   *
   * @param path The path to the file to test access for.
   * @param mode The mode to test for access.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun access(path: Value, mode: Value, callback: Value)

  /**
   * ## Method: `fs.access`
   *
   * Tests a user's permissions for the file at the specified path; provides the results or an error to the callback.
   * This is a host-side variant.
   *
   * @param path The path to the file to read.
   * @param mode The mode to test for access.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun access(
    path: Path,
    mode: AccessMode = AccessMode.READ,
    callback: AccessCallback,
  )

  /**
   * ## Method: `fs.accessSync`
   *
   * Tests a user's permissions for the file at the specified path; provides the results or an error to the callback.
   * This variant specifies the default mode of `F_OK` and operates synchronously, raising an exception for failures.
   *
   * @param path The path to the file to test access for.
   */
  @Polyglot public fun accessSync(path: Value)

  /**
   * ## Method: `fs.access`
   *
   * Tests a user's permissions for the file at the specified path; provides the results or an error to the callback.
   * This variant accepts a [mode] and operates synchronously, raising an exception for failures.
   *
   * @param path The path to the file to test access for.
   * @param mode The mode to test for access.
   */
  @Polyglot public fun accessSync(path: Value, mode: Value)

  /**
   * ## Method: `fs.accessSync`
   *
   * Tests a user's permissions for the file at the specified path; provides the results or an error to the callback.
   * This is a host-side variant that operates synchronously.
   *
   * @param path The path to the file to read.
   * @param mode The mode to test for access.
   */
  @Polyglot public fun accessSync(
    path: Path,
    mode: AccessMode = AccessMode.READ,
  )

  /**
   * ## Method: `fs.exists`
   *
   * Tests whether the file at the specified path exists; provides the results or an error to the callback.
   *
   * @param path The path to the file to test for existence.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun exists(path: Value, callback: Value)

  /**
   * ## Method: `fs.exists`
   *
   * Tests whether the file at the specified path exists; provides the results or an error to the callback.
   *
   * @param path The path to the file to test for existence.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun exists(path: Path, callback: (Boolean) -> Unit)

  /**
   * ## Method: `fs.existsSync`
   *
   * Tests whether the file at the specified path exists; provides the results or an error to the callback.
   *
   * @param path The path to the file to test for existence.
   * @return Whether the file exists.
   */
  @Polyglot public fun existsSync(path: Value): Boolean

  /**
   * ## Method: `fs.existsSync`
   *
   * Tests whether the file at the specified path exists; provides the results or an error to the callback.
   *
   * @param path The path to the file to test for existence.
   * @return Whether the file exists.
   */
  @Polyglot public fun existsSync(path: Path): Boolean

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a polyglot [Value].
   *
   * @param path The path to the file to read.
   * @param options The options to use for the file read operation.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun readFile(path: Value, options: Value, callback: ReadFileCallback)

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a polyglot [Value].
   *
   * @param path The path to the file to read.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun readFile(path: Value, callback: ReadFileCallback)

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a polyglot [Value].
   *
   * @param path The path to the file to read.
   * @param options The options to use for the file read operation.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun readFile(
    path: Path,
    options: ReadFileOptions = ReadFileOptions.DEFAULTS,
    callback: ReadFileCallback,
  )

  /**
   * ## Method: `fs.readFileSync`
   *
   * Reads the contents of a file at the specified path and returns the results synchronously. This variant accepts a
   * plain [Value].
   *
   * @param path The path to the file to read.
   * @param options The options to use for the file read operation.
   * @return The contents of the file as a [Buffer].
   */
  @Polyglot public fun readFileSync(path: Value, options: Value? = null): StringOrBuffer

  /**
   * ## Method: `fs.readFileSync`
   *
   * Reads the contents of a file at the specified path and returns the results synchronously. This variant accepts a
   * plain [Value].
   *
   * @param path The path to the file to read.
   * @param options The options to use for the file read operation.
   * @return The contents of the file as a [Buffer].
   */
  @Polyglot public fun readFileSync(path: Path, options: ReadFileOptions = ReadFileOptions.DEFAULTS): StringOrBuffer
}

/**
 * # Node API: `fs` (Writable)
 */
@API public interface WritableFilesystemAPI : NodeAPI {
  /**
   * ## Method: `fs.writeFile`
   *
   * Writes the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a polyglot [Value].
   *
   * @param path The path to the file to write.
   * @param data The data to write to the file.
   * @param options The options to use for the file write operation.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun writeFile(
    path: Value,
    data: StringOrBuffer,
    options: Value,
    callback: WriteFileCallback,
  )

  /**
   * ## Method: `fs.writeFileSync`
   *
   * Writes the contents of a file at the specified path; provides the results or an error to the callback.
   * This variant accepts a polyglot [Value].
   *
   * @param path The path to the file to write.
   * @param data The data to write to the file.
   */
  @Polyglot public fun writeFileSync(
    path: Value,
    data: StringOrBuffer,
    options: Value? = null,
  )
}

/**
 * # Node API: `fs` (Node)
 */
@API public interface NodeFilesystemAPI : NodeAPI, FilesystemAPI, WritableFilesystemAPI
