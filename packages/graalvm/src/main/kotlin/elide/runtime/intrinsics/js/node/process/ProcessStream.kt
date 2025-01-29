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
package elide.runtime.intrinsics.js.node.process

import java.io.InputStream
import java.io.OutputStream
import elide.annotations.API
import elide.runtime.intrinsics.js.node.stream.Readable
import elide.runtime.intrinsics.js.node.stream.Stream
import elide.runtime.intrinsics.js.node.stream.Writable

/**
 * TBD.
 */
@API public sealed interface ProcessStandardStream : Stream {
  /**
   * TBD.
   */
  public val fd: Int
}

/**
 * TBD.
 */
@API public class ProcessStandardOutputStream private constructor (
  override val fd: Int,
  private val backing: Writable,
) : Writable by backing, ProcessStandardStream {
  public companion object {
    @JvmStatic public fun wrap(id: Int, stream: OutputStream): ProcessStandardOutputStream =
      wrap(id, Writable.wrap(stream))

    @JvmStatic public fun wrap(id: Int, stream: Writable): ProcessStandardOutputStream =
      ProcessStandardOutputStream(id, stream)
  }
}

/**
 * TBD.
 */
@API public class ProcessStandardInputStream private constructor (
  override val fd: Int,
  private val backing: Readable,
) : Readable by backing, ProcessStandardStream {
  public companion object {
    @JvmStatic public fun wrap(id: Int, stream: InputStream): ProcessStandardInputStream =
      wrap(id, Readable.wrap(stream))

    @JvmStatic public fun wrap(id: Int, stream: Readable): ProcessStandardInputStream =
      ProcessStandardInputStream(id, stream)
  }
}
