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

package elide.runtime.intrinsics.server.http.v2.flask

import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

internal class FlaskResponseObject(
  internal val content: Value = Value.asValue(null),
  internal val headers: MutableMap<String, String> = mutableMapOf(),
  internal var status: HttpResponseStatus = HttpResponseStatus.OK,
) : ProxyObject {
  override fun getMember(key: String?): Any? = when (key) {
    "status" -> status
    "headers" -> headers
    "body" -> content
    else -> null
  }

  override fun getMemberKeys(): Any = MEMBER_KEYS
  override fun hasMember(key: String?): Boolean = key in MEMBER_KEYS
  override fun putMember(key: String?, value: Value?): Unit = when (key) {
    "status" -> {
      status = when {
        value == null -> error("Cannot set status to `null`")
        value.isNumber && value.fitsInInt() -> HttpResponseStatus.valueOf(value.asInt())
        value.isString -> HttpResponseStatus.valueOf(200, value.asString())
        else -> error("Unexpected status value type: '$value'")
      }
    }

    else -> error("Cannot modify response object member '$key'")
  }

  private companion object {
    private val MEMBER_KEYS = arrayOf("status", "headers", "body")
  }
}
