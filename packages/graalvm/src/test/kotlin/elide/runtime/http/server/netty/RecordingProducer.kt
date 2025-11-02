/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server.netty

import elide.runtime.http.server.ContentStreamSource
import elide.runtime.http.server.WritableContentStream
import io.netty.buffer.ByteBuf

internal class RecordingProducer : ContentStreamSource {
  var writer: WritableContentStream.Writer? = null
    private set
  var attachCount = 0
    private set
  var closeCount = 0
    private set
  var pullCount = 0
    private set
  var closeCause: Throwable? = null
    private set
  var onAttach: (WritableContentStream.Writer) -> Unit = {}
  var onPullAction: () -> Unit = {}
  var onCloseAction: (Throwable?) -> Unit = {}

  fun write(content: ByteBuf) {
    writer?.write(content) ?: error("writer not available")
  }

  fun end(error: Throwable? = null) {
    writer?.end(error) ?: error("writer not available")
  }

  override fun onAttached(writer: WritableContentStream.Writer) {
    attachCount++
    this.writer = writer
    onAttach(writer)
  }

  override fun onPull() {
    pullCount++
    onPullAction()
  }

  override fun onClose(failure: Throwable?) {
    closeCount++
    closeCause = failure
    onCloseAction(failure)
  }
}
