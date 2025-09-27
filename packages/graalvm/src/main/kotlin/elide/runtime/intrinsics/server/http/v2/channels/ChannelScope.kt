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

package elide.runtime.intrinsics.server.http.v2.channels

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelProgressivePromise
import io.netty.channel.ChannelPromise

/**
 * Lightweight handle linked to a Netty [ChannelHandlerContext], allowing request handlers to create futures and
 * promises to indicate execution progress, without exposing unnecessary channel APIs.
 */
@JvmInline public value class ChannelScope(private val context: ChannelHandlerContext) {
  /** Allocator used by the channel. */
  public val alloc: ByteBufAllocator get() = context.alloc()

  /** Returns a new completable [ChannelPromise]. */
  public fun newPromise(): ChannelPromise = context.newPromise()

  /** Returns a new completable promise with an associated progress. */
  public fun newProgressivePromise(): ChannelProgressivePromise = context.newProgressivePromise()

  /** Returns a new completed future that has succeeded. */
  public fun newSucceededFuture(): ChannelFuture = context.newSucceededFuture()

  /** Returns a new completed future that has failed. */
  public fun newFailedFuture(cause: Throwable? = null): ChannelFuture = context.newFailedFuture(cause)
}
