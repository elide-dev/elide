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
@file:Suppress("MatchingDeclarationName")

package elide.runtime.intrinsics.js.node.fs

import elide.runtime.intrinsics.js.err.AbstractJsException
import elide.runtime.intrinsics.js.node.AbortSignal

/**
 * ## Options: `fs.writeFile`
 *
 * Describes the options which can be provided to a `writeFile` operation.
 */
public data class WriteFileOptions(
  /**
   * The encoding to use for the file write operation; optional. If not specified, files are written as raw `Buffer`
   * values; otherwise, the file is written as a string.
   */
  val encoding: StringOrBuffer? = null,

  /** The flag to use for the file write operation; defaults to `w`. */
  val flag: String? = "w",

  /** The mode to use for the file write operation. */
  val mode: Int? = null,

  /** The signal to use for the operation; optional. */
  val signal: AbortSignal? = null,
) {
  public companion object {
    /** Default write options. */
    public val DEFAULTS: WriteFileOptions = WriteFileOptions()
  }
}

/**
 * ## Callback: `fs.writeFile`
 *
 * Describes the callback function shape which is provided to the `writeFile` operation.
 */
public typealias WriteFileCallback = (err: AbstractJsException?) -> Unit
