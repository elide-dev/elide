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
package elide.runtime.intrinsics.js.node.zlib

import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.nio.ByteBuffer
import elide.runtime.gvm.js.JsError
import elide.vm.annotations.Polyglot

/**
 * ## Compress Callback
 *
 * Describes the type interface for a callback function passed to methods which compress data, like `zlib.deflate`.
 */
@HostAccess.Implementable
@FunctionalInterface
public fun interface CompressCallback {
  public companion object {
    /**
     * Construct a [CompressCallback] from a [Value] instance.
     *
     * @param value Value instance to convert.
     * @return [CompressCallback] instance.
     */
    @JvmStatic public fun from(value: Value): CompressCallback {
      if (!value.canExecute()) throw JsError.typeError("Compression callback must be executable")
      return CompressCallback { error, result ->
        when (error) {
          null -> value.executeVoid(result)
          else -> value.executeVoid(error)
        }
      }
    }
  }

  /**
   * Invoke the callback with a failure result.
   *
   * @param error Error which occurred
   */
  public fun failed(error: RuntimeException): Unit = invoke(error, null)

  /**
   * Invoke the callback with a successful result.
   *
   * @param result Result buffer
   */
  public fun done(result: ByteBuffer): Unit = invoke(null, result)

  /**
   * Invoke the compression operation callback, with an error or result instance.
   *
   * @param error Error, if any; when no error occurred, this will be `null`.
   * @param result Result buffer, if any; when an error occurred, this will be `null`.
   */
  @Polyglot public operator fun invoke(error: RuntimeException?, result: ByteBuffer?)
}
