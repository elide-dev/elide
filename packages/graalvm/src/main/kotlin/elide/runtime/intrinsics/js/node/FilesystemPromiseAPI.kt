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
package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import java.nio.file.AccessMode
import elide.annotations.API
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.err.JsError
import elide.runtime.intrinsics.js.node.fs.*
import elide.runtime.intrinsics.js.node.path.Path
import elide.vm.annotations.Polyglot

/**
 * # Node API: Filesystem (Promises)
 *
 * The `fs/promises` API provides asynchronous file system methods that return promises.
 *
 * The promise APIs use the underlying Node.js thread pool to perform file system operations off the event loop thread.
 * These operations are not synchronized or threadsafe. Care must be taken when performing multiple concurrent
 * modifications on the same file or data corruption may occur.
 *
 * @see FilesystemAPI for non-promise filesystem methods.
 */
@API public interface FilesystemPromiseAPI {
  /**
   * ## Method: `fs.access`
   *
   * Tests a user's permissions for the file or directory specified by [path]. The `mode` argument is an optional
   * integer that specifies the accessibility checks to be performed. `mode` should be either the value
   * `fs.constants.F_OK` or a mask consisting of the bitwise OR of any of `fs.constants.R_OK`,` fs.constants.W_OK`, and
   * `fs.constants.X_OK `(e.g. `fs.constants.W_OK | fs.constants.R_OK`).
   *
   * Check File access constants for possible values of ``ode].
   *
   * If the accessibility check is successful, the promise is fulfilled with no value. If any of the accessibility
   * checks fail, the promise is rejected with a [JsError] object. The following example checks if the file
   * `/etc/passwd` can be read and written by the current process:
   *
   * ```javascript
   * import { access, constants } from 'node:fs/promises';
   *
   * try {
   *   await access('/etc/passwd', constants.R_OK | constants.W_OK);
   *   console.log('can access');
   * } catch {
   *   console.error('cannot access');
   * }
   * ```
   *
   * Using [access] to check for the accessibility of a file before calling [open] is not recommended. Doing so
   * introduces a race condition, since other processes may change the file's state between the two calls.
   *
   * Instead, user code should open/read/write the file directly and handle the error raised if the file is not
   * accessible.
   *
   * @param path The path to the file or directory.
   * @return A promise which resolves with no value if the file or directory exists, and rejects with an error if it
   *  does not.
   */
  @Polyglot public fun access(path: Value): JsPromise<Unit>

  /**
   * ## Method: `fs.access`
   *
   * Tests a user's permissions for the file or directory specified by [path]. The [mode] argument is an optional
   * integer that specifies the accessibility checks to be performed. [mode] should be either the value
   * `fs.constants.F_OK` or a mask consisting of the bitwise OR of any of `fs.constants.R_OK`,` fs.constants.W_OK`, and
   * `fs.constants.X_OK `(e.g. `fs.constants.W_OK | fs.constants.R_OK`).
   *
   * Check File access constants for possible values of [mode].
   *
   * If the accessibility check is successful, the promise is fulfilled with no value. If any of the accessibility
   * checks fail, the promise is rejected with a [JsError] object. The following example checks if the file
   * `/etc/passwd` can be read and written by the current process:
   *
   * ```javascript
   * import { access, constants } from 'node:fs/promises';
   *
   * try {
   *   await access('/etc/passwd', constants.R_OK | constants.W_OK);
   *   console.log('can access');
   * } catch {
   *   console.error('cannot access');
   * }
   * ```
   *
   * Using [access] to check for the accessibility of a file before calling [open] is not recommended. Doing so
   * introduces a race condition, since other processes may change the file's state between the two calls.
   *
   * Instead, user code should open/read/write the file directly and handle the error raised if the file is not
   * accessible.
   *
   * @param path The path to the file or directory.
   * @param mode The mode to use for the access check.
   * @return A promise which resolves with no value if the file or directory exists, and rejects with an error if it
   *  does not.
   */
  @Polyglot public fun access(path: Value, mode: Value?): JsPromise<Unit>

  /**
   * ## Method: `fs.access`
   *
   * Tests a user's permissions for the file or directory specified by [path]. The [mode] argument is an optional
   * integer that specifies the accessibility checks to be performed. [mode] should be either the value
   * `fs.constants.F_OK` or a mask consisting of the bitwise OR of any of `fs.constants.R_OK`,` fs.constants.W_OK`, and
   * `fs.constants.X_OK `(e.g. `fs.constants.W_OK | fs.constants.R_OK`).
   *
   * Check File access constants for possible values of [mode].
   *
   * If the accessibility check is successful, the promise is fulfilled with no value. If any of the accessibility
   * checks fail, the promise is rejected with a [JsError] object. The following example checks if the file
   * `/etc/passwd` can be read and written by the current process:
   *
   * ```javascript
   * import { access, constants } from 'node:fs/promises';
   *
   * try {
   *   await access('/etc/passwd', constants.R_OK | constants.W_OK);
   *   console.log('can access');
   * } catch {
   *   console.error('cannot access');
   * }
   * ```
   *
   * Using [access] to check for the accessibility of a file before calling [open] is not recommended. Doing so
   * introduces a race condition, since other processes may change the file's state between the two calls.
   *
   * Instead, user code should open/read/write the file directly and handle the error raised if the file is not
   * accessible.
   *
   * @param path The path to the file or directory.
   * @param mode The mode to use for the access check.
   * @return A promise which resolves with no value if the file or directory exists, and rejects with an error if it
   *  does not.
   */
  @Polyglot public fun access(path: Path, mode: AccessMode? = null): JsPromise<Unit>

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a polyglot [Value].
   *
   * @param path The path to the file to read.
   * @return A promise which resolves with the file contents or rejects with an error.
   */
  @Polyglot public fun readFile(path: Value): JsPromise<StringOrBuffer>

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a polyglot [Value].
   *
   * @param path The path to the file to read.
   * @param options The options to use for the file read operation.
   * @return A promise which resolves with the file contents or rejects with an error.
   */
  @Polyglot public fun readFile(path: Value, options: Value?): JsPromise<StringOrBuffer>

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a polyglot [Value].
   *
   * @param path The path to the file to read.
   * @param options The options to use for the file read operation.
   * @return A promise which resolves with the file contents or rejects with an error.
   */
  @Polyglot public fun readFile(path: Path, options: ReadFileOptions? = null): JsPromise<StringOrBuffer>
}

/**
 * # Node API: `fs/promises` (Writable)
 */
@API public interface WritableFilesystemPromiseAPI {
  /**
   * ## Method: `fs.writeFile`
   *
   * Asynchronously writes data to a file, replacing the file if it already exists. [data] can be a string, a buffer, an
   * `AsyncIterable`, or an `Iterable` object. The promise is fulfilled with no arguments upon success.
   *
   * If `options` is a string, then it specifies the encoding.
   * The `FileHandle` has to support writing.
   *
   * It is unsafe to use [writeFile] multiple times on the same file without waiting for the promise to be fulfilled
   * (or rejected).
   *
   * If one or more [write] calls are made on a file handle and then a [writeFile] call is made, the data will be
   * written from the current position till the end of the file. It doesn't always write from the beginning of the file.
   *
   * @param path The path to the file to write.
   * @param data The data to write to the file.
   * @return A promise which resolves with no value upon success.
   */
  @Polyglot public fun writeFile(path: Value, data: Value): JsPromise<Unit>

  /**
   * ## Method: `fs.writeFile`
   *
   * Asynchronously writes data to a file, replacing the file if it already exists. [data] can be a string, a buffer, an
   * `AsyncIterable`, or an `Iterable` object. The promise is fulfilled with no arguments upon success.
   *
   * If [options] is a string, then it specifies the encoding.
   * The `FileHandle` has to support writing.
   *
   * It is unsafe to use [writeFile] multiple times on the same file without waiting for the promise to be fulfilled
   * (or rejected).
   *
   * If one or more [write] calls are made on a file handle and then a [writeFile] call is made, the data will be
   * written from the current position till the end of the file. It doesn't always write from the beginning of the file.
   *
   * @param path The path to the file to write.
   * @param data The data to write to the file.
   * @param options The options to use for the file write operation.
   * @return A promise which resolves with no value upon success.
   */
  @Polyglot public fun writeFile(path: Value, data: Value, options: Value?): JsPromise<Unit>

  /**
   * ## Method: `fs.writeFile`
   *
   * Asynchronously writes data to a file, replacing the file if it already exists. [data] can be a string, a buffer, an
   * `AsyncIterable`, or an `Iterable` object. The promise is fulfilled with no arguments upon success.
   *
   * If [options] is a string, then it specifies the encoding.
   * The `FileHandle` has to support writing.
   *
   * It is unsafe to use [writeFile] multiple times on the same file without waiting for the promise to be fulfilled
   * (or rejected).
   *
   * If one or more [write] calls are made on a file handle and then a [writeFile] call is made, the data will be
   * written from the current position till the end of the file. It doesn't always write from the beginning of the file.
   *
   * @param path The path to the file to write.
   * @param data The data to write to the file.
   * @param options The options to use for the file write operation.
   * @return A promise which resolves with no value upon success.
   */
  @Polyglot public fun writeFile(path: Path, data: StringOrBuffer, options: WriteFileOptions? = null): JsPromise<Unit>

  /**
   * ## Method: `fs.mkdir`
   *
   * Asynchronously creates a directory.
   *
   * The optional `options` argument can be an integer specifying mode (permission and sticky bits), or an object with a
   * `mode` property and a `recursive` property indicating whether parent directories should be created. Calling [mkdir]
   * when [path] is a directory that exists results in a rejection only when `recursive` is `false`.
   *
   * @param path The path to the directory to create.
   * @return Upon success, fulfills with `undefined` if `recursive` is `false`, or the first directory path created if
   *   `recursive` is `true`.
   */
  @Polyglot public fun mkdir(path: Value): JsPromise<StringOrBuffer>

  /**
   * ## Method: `fs.mkdir`
   *
   * Asynchronously creates a directory.
   *
   * The optional [options] argument can be an integer specifying mode (permission and sticky bits), or an object with a
   * `mode` property and a `recursive` property indicating whether parent directories should be created. Calling [mkdir]
   * when [path] is a directory that exists results in a rejection only when `recursive` is `false`.
   *
   * @param path The path to the directory to create.
   * @Param options The options to use for the directory creation operation.
   * @return Upon success, fulfills with `undefined` if `recursive` is `false`, or the first directory path created if
   *   `recursive` is `true`.
   */
  @Polyglot public fun mkdir(path: Value, options: Value?): JsPromise<StringOrBuffer>

  /**
   * ## Method: `fs.mkdir`
   *
   * Asynchronously creates a directory.
   *
   * The optional [options] argument can be an integer specifying mode (permission and sticky bits), or an object with a
   * `mode` property and a `recursive` property indicating whether parent directories should be created. Calling [mkdir]
   * when [path] is a directory that exists results in a rejection only when `recursive` is `false`.
   *
   * @param path The path to the directory to create.
   * @param options The options to use for the directory creation operation.
   * @return Upon success, fulfills with `undefined` if `recursive` is `false`, or the first directory path created if
   *   `recursive` is `true`.
   */
  @Polyglot public fun mkdir(path: Path, options: MkdirOptions? = null): JsPromise<StringOrBuffer>

  /**
   * ## Method: `fs.copyFile`
   *
   * Asynchronously copies a file using host-side types.
   *
   * Copies the contents at the provided [src] path to the provided [dest] path; the returned promise is resolved once
   * the copy operation completes.
   *
   * @param src The source path to copy from.
   * @param dest The destination path to copy to.
   * @param mode Copy mode constant to apply.
   * @return Upon success, fulfills with `undefined`; otherwise, rejects with an error.
   */
  public fun copyFile(src: Path, dest: Path, mode: Int? = null): JsPromise<Value>

  /**
   * ## Method: `fs.copyFile`
   *
   * Asynchronously copies a file.
   *
   * Copies the contents at the provided [src] path to the provided [dest] path; the returned promise is resolved once
   * the copy operation completes.
   *
   * @param src The source path to copy from.
   * @param dest The destination path to copy to.
   * @return Upon success, fulfills with `undefined`; otherwise, rejects with an error.
   */
  @Polyglot public fun copyFile(src: Value, dest: Value): JsPromise<Value>

  /**
   * ## Method: `fs.copyFile`
   *
   * Asynchronously copies a file.
   *
   * Copies the contents at the provided [src] path to the provided [dest] path; the returned promise is resolved once
   * the copy operation completes.
   *
   * @param src The source path to copy from.
   * @param dest The destination path to copy to.
   * @param mode The mode to use for the copy operation.
   * @return Upon success, fulfills with `undefined`; otherwise, rejects with an error.
   */
  @Polyglot public fun copyFile(src: Value, dest: Value, mode: Int): JsPromise<Value>
}

/**
 * # Node API: Filesystem Promises (Module)
 *
 * Defines the module-level API for the Node `fs/promises` built-in module, which is the combination of
 * [FilesystemPromiseAPI] and [WritableFilesystemPromiseAPI].
 *
 * @see FilesystemPromiseAPI for read methods which produce promises.
 * @see WritableFilesystemPromiseAPI for write methods which produce promises.
 */
@API public interface NodeFilesystemPromiseAPI: NodeAPI, FilesystemPromiseAPI, WritableFilesystemPromiseAPI
