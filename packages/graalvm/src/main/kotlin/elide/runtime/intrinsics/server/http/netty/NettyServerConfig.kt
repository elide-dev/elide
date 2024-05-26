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
package elide.runtime.intrinsics.server.http.netty

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.intrinsics.server.http.HTTP_SERVER_CONFIG_PROPS_AND_METHODS
import elide.runtime.intrinsics.server.http.HttpServerConfig
import elide.vm.annotations.Polyglot

// Properties and methods available for guest access on `HttpServerConfig`.
internal val NETTY_HTTP_SERVER_CONFIG_PROPS_AND_METHODS = HTTP_SERVER_CONFIG_PROPS_AND_METHODS.plus(arrayOf(
  "transport",
))

/** Configuration options specific to the [NettyServerEngine]. */
@DelicateElideApi internal class NettyServerConfig : HttpServerConfig(), ProxyObject {
  /**
   * The Netty transport to be used by the server, defaults to "auto", which detects the preferred transport for the
   * current platform.
   *
   * Explicitly setting this value will attempt to force loading the specified transport, failing if it's not
   * available.
   */
  @Polyglot var transport: String? = TRANSPORT_DETECT

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

    // All transport options.
    private val transportOptions get() = arrayOf(
      TRANSPORT_DETECT,
      TRANSPORT_IO_URING,
      TRANSPORT_EPOLL,
      TRANSPORT_KQUEUE,
      TRANSPORT_NIO,
    )
  }

  override fun getMemberKeys(): Array<String> = NETTY_HTTP_SERVER_CONFIG_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in NETTY_HTTP_SERVER_CONFIG_PROPS_AND_METHODS

  override fun putMember(key: String?, value: Value?) = when (key) {
    "transport" -> when {
      // switch to auto on `null`
      value == null || value.isNull -> {
        // auto-detect transport
        transport = TRANSPORT_DETECT
      }

      // accept string value
      value.isString -> transport = value.asString()

      else -> throw JsError.valueError(
        "Expected one of ${transportOptions.map { "'$it'" }.joinToString(", ")} for `transport`"
      )
    }

    // otherwise, defer to b ase settings
    else -> super.putMember(key, value)
  }

  override fun getMember(key: String?): Any? = when (key) {
    "transport" -> transport
    else -> super.getMember(key)
  }
}
