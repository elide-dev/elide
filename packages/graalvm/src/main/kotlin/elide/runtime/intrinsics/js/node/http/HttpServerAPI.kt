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
package elide.runtime.intrinsics.js.node.http

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.runtime.intrinsics.js.node.events.EventEmitter
import elide.vm.annotations.Polyglot

@API public interface HttpServerAPI : EventEmitter {
  @get:Polyglot public var headersTimeout: Long
  @get:Polyglot public var requestTimeout: Long
  @get:Polyglot public var maxHeadersCount: Long
  @get:Polyglot public var maxRequestsPerSocket: Long
  @get:Polyglot public var timeout: Long
  @get:Polyglot public var keepAliveTimeout: Long
  @get:Polyglot public var keepAliveTimeoutBuffer: Long

  @get:Polyglot public val listening: Boolean

  @Polyglot public fun listen(port: Int, callback: Value? = null)
  @Polyglot public fun listen(port: Int, host: String, callback: Value? = null)

  @Polyglot public fun close(callback: Value? = null)
  @Polyglot public fun closeAllConnections()
  @Polyglot public fun closeIdleConnections()

  @Polyglot public fun setTimeout(millis: Long?, callback: Value? = null)

  public companion object {
    public const val EVENT_CHECK_CONTINUE: String = "checkContinue"
    public const val EVENT_CHECK_EXPECTATION: String = "checkExpectation"
    public const val EVENT_CLIENT_ERROR: String = "clientError"
    public const val EVENT_CLOSE: String = "close"
    public const val EVENT_CONNECT: String = "connect"
    public const val EVENT_CONNECTION: String = "connection"
    public const val EVENT_DROP_REQUEST: String = "dropRequest"
    public const val EVENT_REQUEST: String = "request"
    public const val EVENT_UPGRADE: String = "upgrade"
    public const val EVENT_LISTENING: String = "listening"
  }
}
