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
package elide.runtime.intrinsics.server.http.v2.flask

import io.netty.handler.codec.http.HttpHeaders
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.intrinsics.server.http.v2.HttpContext

internal class FlaskRequestAccessor : ProxyObject {
  @JvmInline private value class HeaderMapProxy(val headers: HttpHeaders) : ProxyHashMap {
    override fun getHashSize(): Long = headers.size().toLong()
    override fun hasHashEntry(key: Value?): Boolean = headers.contains(key?.asString())
    override fun getHashValue(key: Value?): Any? = headers.get(key?.asString())

    override fun putHashEntry(key: Value?, value: Value?): Unit =
      error("Modifying the request's headers is not allowed")

    override fun getHashEntriesIterator(): Any? = headers.entries()
  }

  private val localContext = ThreadLocal<FlaskHttpContext>()

  internal fun push(context: HttpContext) {
    localContext.set(context as FlaskHttpContext)
  }

  internal fun pop() {
    localContext.remove()
  }

  private inline fun <R> withContext(block: FlaskHttpContext.() -> R): R {
    val context = checkNotNull(localContext.get()) { "No active request context" }
    return block(context)
  }

  override fun getMember(key: String?): Any? = when (key) {
    "method" -> withContext { request.method().name() }
    "args" -> withContext {
      queryParams
    }

    "headers" -> withContext { HeaderMapProxy(request.headers()) }

    "path" -> withContext { request.uri() }
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
