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
package elide.runtime.intrinsics.server.http

import io.netty.channel.ChannelHandlerContext
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.DelicateElideApi
import elide.runtime.exec.GuestExecutor

/**
 * A lightweight container for values bound to a specific [HttpRequest]. The [HttpContext] is meant to hold
 * values such as those extracted from path variables.
 */
@DelicateElideApi public class HttpContext private constructor(
  private val map: MutableMap<String, Any?>,
  internal val exec: GuestExecutor,
  internal val channelContext: ChannelHandlerContext,
) : MutableMap<String, Any?> by map, ProxyObject {
  /** Constructs a new empty context. */
  internal constructor(exec: GuestExecutor, chan: ChannelHandlerContext) : this(mutableMapOf(), exec, chan)

  override fun getMember(key: String): Any {
    return map[key] ?: error("no member found with key $key")
  }

  override fun getMemberKeys(): Any {
    return map.keys.toList()
  }

  override fun hasMember(key: String): Boolean {
    return map.containsKey(key)
  }

  override fun putMember(key: String, value: Value?) {
    map[key] = value
  }
}
