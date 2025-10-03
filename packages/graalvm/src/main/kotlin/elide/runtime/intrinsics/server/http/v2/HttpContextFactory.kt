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

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpRequest

/**
 * A provider for [HttpContext] instances; implementations are expected to condense an incoming Netty request and
 * associated state into a single context that can be used to dispatch handlers.
 */
public fun interface HttpContextFactory<out C : HttpContext> {
  public fun newContext(
    incomingRequest: HttpRequest,
    channelContext: ChannelHandlerContext,
    requestSource: HttpContentSource,
    responseSink: HttpContentSink,
  ): C
}
