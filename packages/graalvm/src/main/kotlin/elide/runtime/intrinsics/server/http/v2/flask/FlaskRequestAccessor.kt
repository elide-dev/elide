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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.intrinsics.server.http.v2.HttpContext

internal class FlaskRequestAccessor : ProxyObject {
  private val localContext = ThreadLocal<HttpContext>()

  internal fun push(context: HttpContext) {
    localContext.set(context)
  }

  internal fun pop() {
    localContext.remove()
  }

  private inline fun <R> withContext(block: FlaskHttpContext.() -> R): R {
    val context = checkNotNull(localContext.get()) { "No active request context" }
    return block(context as FlaskHttpContext)
  }

  override fun getMember(key: String?): Any? = when (key) {
    "method" -> withContext { request.method().name() }
    "args" -> withContext {
      queryParams
    }

    "path" -> withContext { request.uri() }
    else -> null
  }

  override fun getMemberKeys(): Any = MEMBER_KEYS
  override fun hasMember(key: String?): Boolean = key in MEMBER_KEYS
  override fun putMember(key: String?, value: Value?) {
    // noop
  }

  private companion object {
    private val MEMBER_KEYS = arrayOf("method", "path", "args")
  }
}
