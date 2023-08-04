/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.intrinsics.js.webstreams

import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.ReadableStream

/** Implementation of readable streams (via the Web Streams standard). */
@Intrinsic(global = "ReadableStream") internal class ReadableStreamIntrinsic : AbstractJsIntrinsic() {
  /**
   * TBD.
   */
  internal class ReadableStreamImpl : ReadableStream {

  }

  /**
   * TBD.
   */
  internal companion object Factory : ReadableStream.Factory<ReadableStreamImpl> {
    /**
     * TBD.
     */
    override fun empty(): ReadableStreamImpl {
      TODO("Not yet implemented")
    }

    /**
     * TBD.
     */
    override fun wrap(input: InputStream): ReadableStreamImpl {
      TODO("Not yet implemented")
    }

    /**
     * TBD.
     */
    override fun wrap(reader: Reader): ReadableStreamImpl {
      TODO("Not yet implemented")
    }

    /**
     * TBD.
     */
    override fun wrap(bytes: ByteArray): ReadableStreamImpl {
      TODO("Not yet implemented")
    }

    /**
     * TBD.
     */
    override fun wrap(buffer: ByteBuffer): ReadableStreamImpl {
      TODO("Not yet implemented")
    }
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // not yet implemented
  }
}
