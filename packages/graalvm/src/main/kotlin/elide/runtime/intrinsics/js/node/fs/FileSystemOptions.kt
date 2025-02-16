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
package elide.runtime.intrinsics.js.node.fs

import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.err.AbstractJsException
import elide.runtime.intrinsics.js.AbortSignal

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

    /**
     * Convert a guest data structure to a [ReadFileOptions] structure on a best-effort basis.
     *
     * @param options The guest data structure to convert.
     * @return The converted [ReadFileOptions] structure.
     */
    @JvmStatic public fun fromGuest(options: Value?): ReadFileOptions = when {
      options == null -> DEFAULTS
      options.hasMembers() -> ReadFileOptions(
        encoding = options.getMember("encoding")?.takeIf { it.isString }?.asString(),
        flag = options.getMember("flag")?.takeIf { it.isString }?.asString(),
        signal = null,  // @TODO abort signals are not supported yet
      )

      options.hasHashEntries() -> ReadFileOptions(
        encoding = options.getMember("encoding")?.takeIf { it.isString }?.asString(),
        flag = options.getMember("flag")?.takeIf { it.isString }?.asString(),
        signal = null,  // @TODO abort signals are not supported yet
      )

      else -> throw IllegalArgumentException("Invalid options for fs.readFile: $options")
    }
  }
}

/**
 * ## Callback: `fs.access`
 *
 * Describes the callback function shape which is provided to the `access` operation.
 */
public typealias AccessCallback = (err: AbstractJsException?) -> Unit

/**
 * ## Callback: `fs.readFile`
 *
 * Describes the callback function shape which is provided to the `readFile` operation.
 */
public typealias ReadFileCallback = (err: AbstractJsException?, data: Any?) -> Unit

/**
 * ## Options: `fs.mkdir` and `fs.mkdirSync`
 *
 * Describes options which can be passed to `mkdir` or `mkdirSync`.
 *
 * @param recursive Whether to create parent directories if they do not exist; defaults to `false`.
 * @param mode The mode to use for the directory creation; defaults to `0o777`.
 */
public data class MkdirOptions(
  val recursive: Boolean = false,
  val mode: Int = 0x777,
) {
  public companion object {
    /** Default directory creation options. */
    public val DEFAULTS: MkdirOptions = MkdirOptions()

    /**
     * Convert a guest data structure to a [MkdirOptions] structure on a best-effort basis.
     *
     * @param options The guest data structure to convert.
     * @return The converted [MkdirOptions] structure.
     */
    @JvmStatic public fun fromGuest(options: Value?): MkdirOptions = when {
      options == null || options.isNull -> DEFAULTS
      options.hasMembers() -> MkdirOptions(
        recursive = options.getMember("recursive")?.takeIf { it.isBoolean }?.asBoolean() ?: false,
        mode = options.getMember("mode")?.takeIf { it.isNumber }?.asInt() ?: 0x777,
      )

      options.hasHashEntries() -> MkdirOptions(
        recursive = options.getMember("recursive")?.takeIf { it.isBoolean }?.asBoolean() ?: false,
        mode = options.getMember("mode")?.takeIf { it.isNumber }?.asInt() ?: 0x777,
      )
      else -> throw IllegalArgumentException("Invalid options for fs.mkdir: $options")
    }
  }
}

/**
 * ## Callback: `fs.mkdir`
 *
 * Describes the callback function shape which is provided to the `mkdir` operation.
 */
public typealias MkdirCallback = (err: AbstractJsException?) -> Unit
