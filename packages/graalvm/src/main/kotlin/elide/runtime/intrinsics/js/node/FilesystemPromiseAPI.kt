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

import elide.annotations.API
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.URL
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.runtime.intrinsics.js.node.fs.FileHandle
import elide.runtime.intrinsics.js.node.fs.ReadFileOptions
import elide.runtime.intrinsics.js.node.fs.StringOrBuffer
import elide.vm.annotations.Polyglot

/**
 * # Node API: `fs/promises`
 */
@API public interface FilesystemPromiseAPI : NodeAPI {
  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results in a promise. This variant accepts a plain
   * [String].
   *
   * @param path The path to the file to read.
   * @param options The options to use for the file read operation.
   * @return A promise which resolves with the file contents or rejects with an error.
   */
  @Polyglot public fun readFile(
    path: String,
    options: ReadFileOptions = ReadFileOptions.DEFAULTS,
  ): JsPromise<StringOrBuffer>

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a [URL] intrinsic.
   *
   * @param path The path to the file to read.
   * @param options The options to use for the file read operation.
   * @return A promise which resolves with the file contents or rejects with an error.
   */
  @Polyglot public fun readFile(
    path: URL,
    options: ReadFileOptions = ReadFileOptions.DEFAULTS,
  ): JsPromise<StringOrBuffer>

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a live [FileHandle].
   *
   * @param handle The file handle to read.
   * @param options The options to use for the file read operation.
   * @return A promise which resolves with the file contents or rejects with an error.
   */
  @Polyglot public fun readFile(
    handle: FileHandle,
    options: ReadFileOptions = ReadFileOptions.DEFAULTS,
  ): JsPromise<StringOrBuffer>

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a live [FileHandle].
   *
   * @param buffer Buffer containing the file path to read.
   * @param options The options to use for the file read operation.
   * @return A promise which resolves with the file contents or rejects with an error.
   */
  @Polyglot public fun readFile(
    buffer: Buffer,
    options: ReadFileOptions = ReadFileOptions.DEFAULTS,
  ): JsPromise<StringOrBuffer>
}
