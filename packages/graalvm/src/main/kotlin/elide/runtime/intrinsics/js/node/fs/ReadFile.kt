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
package elide.runtime.intrinsics.js.node.fs

import elide.runtime.intrinsics.js.err.AbstractJsException
import elide.runtime.intrinsics.js.node.AbortSignal

/**
 * ## Type: `StringOrBuffer`
 *
 * Describes a `String` or `Buffer` type, depending on the encoding context for a file operation.
 */
public typealias StringOrBuffer = Any

/**
 * ## Options: `fs.readFile`
 *
 * Describes the options which can be provided to a `readFile` operation.
 *
 * @param encoding The encoding to use for the file read operation; optional. If not specified, files are read as raw
 *   `Buffer` values; otherwise, the file is read as a string.
 * @param flag The flag to use for the file read operation; defaults to `r`.
 * @param signal Abort signal to use for the operation; optional.
 */
public data class ReadFileOptions(
  val encoding: StringOrBuffer? = null,
  val flag: String? = "r",
  val signal: AbortSignal? = null,
) {
  public companion object {
    /** Default file read options. */
    public val DEFAULTS: ReadFileOptions = ReadFileOptions()
  }
}

/**
 * ## Callback: `fs.readFile`
 *
 * Describes the callback function shape which is provided to the `readFile` operation.
 */
public typealias ReadFileCallback = (err: AbstractJsException?, data: Any?) -> Unit
