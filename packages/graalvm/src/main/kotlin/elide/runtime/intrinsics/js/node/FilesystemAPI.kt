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
import elide.annotations.API
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.runtime.intrinsics.js.node.fs.*
import elide.runtime.intrinsics.js.node.path.Path
import elide.vm.annotations.Polyglot

/**
 * # Node API: `fs`
 */
@API public interface FilesystemAPI {
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
    path: Value,
    options: Value,
    callback: ReadFileCallback,
  )

  /**
   * ## Method: `fs.readFile`
   *
   * Reads the contents of a file at the specified path; provides the results or an error to the callback. This variant
   * accepts a polyglot [Value].
   *
   * @param path The path to the file to read.
   * @param callback The callback to provide the results or an error.
   */
  @Polyglot public fun readFile(
    path: Value,
    callback: ReadFileCallback,
  )

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
