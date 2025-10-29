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
package elide.runtime.intrinsics.server.http.v2

import io.netty.channel.ChannelFuture
import elide.runtime.intrinsics.server.http.v2.channels.ChannelScope

/**
 * Functional handler for incoming HTTP requests.
 *
 * During handling, implementations have access to an [HttpContext] that allows modifying the response to be sent to
 * the client, and must return a [ChannelFuture] to notify the engine that the response should be sent.
 */
public fun interface HttpContextHandler {
  /**
   * Handle an incoming HTTP request [context] asynchronously in the given [scope] and return a [ChannelFuture] to
   * signal completion.
   *
   * @param context The context of the HTTP request being handled.
   * @param scope A channel scope that can be used to create promises and futures to signal completion.
   * @return A channel future that signals completion of the handling, triggering the server to send the response.
   */
  public fun handle(context: HttpContext, scope: ChannelScope): ChannelFuture
}
