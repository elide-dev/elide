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
package elide.runtime.intrinsics.python.flask

import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.http.server.HttpCall
import elide.runtime.http.server.python.flask.FlaskContext

internal class FlaskRequestAccessor : ProxyObject {
  @JvmInline private value class HeaderMapProxy(val headers: HttpHeaders) : ProxyHashMap {
    override fun getHashSize(): Long = headers.size().toLong()
    override fun hasHashEntry(key: Value?): Boolean = headers.contains(key?.asString())
    override fun getHashValue(key: Value?): Any? = headers.get(key?.asString())

    override fun putHashEntry(key: Value?, value: Value?): Unit =
      error("Modifying the request's headers is not allowed")

    override fun getHashEntriesIterator(): Any? = headers.entries()
  }

  private val localCall = ThreadLocal<HttpCall<FlaskContext>>()

  internal fun push(call: HttpCall<FlaskContext>) {
    localCall.set(call)
  }

  internal fun pop() {
    localCall.remove()
  }

  private inline fun <R> useCall(block: HttpCall<FlaskContext>.() -> R): R {
    val context = checkNotNull(localCall.get()) { "No active request context" }
    return block(context)
  }

  override fun getMember(key: String?): Any? = when (key) {
    "method" -> useCall { request.method().name() }
    "args" -> useCall { context.queryParams }

    "headers" -> useCall { HeaderMapProxy(request.headers()) }

    "path" -> useCall { request.uri() }
    else -> null
  }

  override fun getMemberKeys(): Any = MEMBER_KEYS
  override fun hasMember(key: String?): Boolean = key in MEMBER_KEYS
  override fun putMember(key: String?, value: Value?) {
    // noop
  }

  private companion object {
    private val MEMBER_KEYS = arrayOf("method", "path", "args", "headers")
  }
}

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
