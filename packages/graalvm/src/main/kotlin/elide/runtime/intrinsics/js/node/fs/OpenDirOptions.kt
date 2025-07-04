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
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.err.AbstractJsException

/**
 * ## Options: `fs.opendir`
 *
 * Describes the options which can be provided to an open-directory operation.
 */
public data class OpenDirOptions(
  /**
   * The encoding to use for paths; defaults to `utf-8`.
   */
  val encoding: StringOrBuffer? = null,

  /**
   * Number of directory entries that are buffered internally when reading from the directory. Higher values lead to
   * better performance but higher memory usage. Default: `32`.
   */
  val bufferSize: Int? = null,

  /**
   * Whether to operate recursively when listing directory contents.
   */
  val recursive: Boolean = false,
) {
  public companion object {
    /** Default open-dir options. */
    public val DEFAULTS: OpenDirOptions = OpenDirOptions()

    /**
     * Create open-dir-options from a guest object or map.
     *
     * @param obj Guest object.
     * @return Open-dir options.
     */
    @JvmStatic public fun from(obj: Value): OpenDirOptions? = when {
      obj.isNull -> null
      obj.isString -> OpenDirOptions(obj.asString())

      obj.hasMembers() -> OpenDirOptions(
        encoding = obj.getMember("encoding")?.asString(),
        bufferSize = obj.getMember("bufferSize")?.asInt(),
        recursive = obj.getMember("recursive")?.asBoolean() == true,
      )

      else -> throw JsError.typeError("Cannot use '$obj' as open-dir-options")
    }
  }
}

/**
 * ## Callback: `fs.opendir`
 *
 * Describes the callback function shape which is provided to the `opendir` operation.
 */
public typealias OpenDirCallback = (err: AbstractJsException?, dir: Dir?) -> Unit
