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

package elide.http.response

import io.netty.handler.codec.http.HttpResponseStatus
import elide.http.StatusCode

// Implements a platform-specific HTTP status for Netty.
@JvmInline internal value class NettyHttpStatus (override val status: HttpResponseStatus):
  PlatformHttpStatus<HttpResponseStatus> {
  override val code: StatusCode get() = StatusCode.resolve(status.code().toUShort())
  override val message: String? get() = status.reasonPhrase()
}
