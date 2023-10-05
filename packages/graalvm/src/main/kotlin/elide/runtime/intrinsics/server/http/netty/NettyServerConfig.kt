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

package elide.runtime.intrinsics.server.http.netty

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpServerConfig

/** Configuration options specific to the [NettyServerEngine]. */
@DelicateElideApi internal class NettyServerConfig : HttpServerConfig() {
  /**
   * The Netty transport to be used by the server, defaults to "auto", which detects the preferred transport for the
   * current platform.
   *
   * Explicitly setting this value will attempt to force loading the specified transport, failing if it's not
   * available.
   */
  @Export var transport: String? = TRANSPORT_DETECT

  /** Resolve a [NettyTransport], honoring the [transport] option. */
  internal fun resolveTransport(): NettyTransport<*> = when (transport) {
    TRANSPORT_IO_URING -> IOUringTransport
    TRANSPORT_EPOLL -> EpollTransport
    TRANSPORT_KQUEUE -> KQueueTransport
    TRANSPORT_NIO -> NioTransport

    // resolve by default
    else -> NettyTransport.resolve()
  }

  companion object {
    /** Use with [transport] to automatically detect the best transport for the current platform. */
    const val TRANSPORT_DETECT = "auto"

    /** Use with [transport] to force using the [IOUringTransport] or fail if it's not available. */
    const val TRANSPORT_IO_URING = "io_uring"

    /** Use with [transport] to force using the [EpollTransport] or fail if it's not available. */
    const val TRANSPORT_EPOLL = "epoll"

    /** Use with [transport] to force using the [KQueueTransport] or fail if it's not available. */
    const val TRANSPORT_KQUEUE = "kqueue"

    /** Use with [transport] to force using the [NioTransport] or fail if it's not available. */
    const val TRANSPORT_NIO = "nio"
  }
}
