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
package elide.runtime.http.server.netty

import elide.runtime.http.server.HttpRequestConsumer
import elide.runtime.http.server.HttpRequestBody
import io.netty.buffer.ByteBuf

internal class RecordingConsumer : HttpRequestConsumer {
  var reader: HttpRequestBody.Reader? = null
    private set
  var attachCount = 0
    private set
  var readCount = 0
    private set
  var closeCount = 0
    private set
  var closeCause: Throwable? = null
    private set
  val received: MutableList<String> = mutableListOf()
  var onAttach: (HttpRequestBody.Reader) -> Unit = {}
  var onReadAction: (ByteBuf) -> Unit = { content -> received += content.toString(Charsets.UTF_8) }
  var onCloseAction: (Throwable?) -> Unit = {}

  override fun onAttached(reader: HttpRequestBody.Reader) {
    attachCount++
    this.reader = reader
    onAttach(reader)
  }

  override fun onRead(content: ByteBuf) {
    readCount++
    onReadAction(content)
  }

  override fun onClose(failure: Throwable?) {
    closeCount++
    closeCause = failure
    onCloseAction(failure)
  }
}
