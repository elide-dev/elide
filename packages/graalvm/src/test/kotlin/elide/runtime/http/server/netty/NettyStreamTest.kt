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

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.DefaultLastHttpContent
import io.netty.handler.codec.http.HttpContent

abstract class NettyStreamTest {
  protected abstract val channel: EmbeddedChannel

  protected inline fun withChannelEventLoop(wait: Boolean = true, crossinline block: () -> Unit) {
    if (wait) {
      val result = channel.eventLoop().submit { block() }
      channel.runPendingTasks()
      result.get()
    } else {
      channel.eventLoop().execute { block() }
    }
  }

  protected fun httpContent(content: String, isLast: Boolean = false): HttpContent {
    return if (isLast) DefaultLastHttpContent(Unpooled.copiedBuffer(content, Charsets.UTF_8))
    else DefaultHttpContent(Unpooled.copiedBuffer(content, Charsets.UTF_8))
  }
}
