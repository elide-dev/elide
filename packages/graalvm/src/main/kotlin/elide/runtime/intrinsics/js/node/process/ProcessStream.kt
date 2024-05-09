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

}

/**
 * TBD.
 */
@API public sealed interface ProcessStandardOutputStream : Readable, ProcessStandardStream {
  public companion object {
    @JvmStatic public fun wrap(stream: OutputStream): ProcessStandardOutputStream {
      TODO("Not yet implemented")
    }
  }
}

/**
 * TBD.
 */
@API public sealed interface ProcessStandardInputStream : Writable, ProcessStandardStream {
  public companion object {
    @JvmStatic public fun wrap(stream: InputStream): ProcessStandardInputStream {
      TODO("Not yet implemented")
    }
  }
}
