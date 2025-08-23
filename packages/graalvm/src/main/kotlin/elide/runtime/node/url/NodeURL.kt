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
package elide.runtime.node.url

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.url.URLSearchParamsIntrinsic
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.URLAPI
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.net.IDN
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

// Constructor for `URL`.
private const val URL_CONSTRUCTOR_FN = "URL"

// Other constants for `url` module.
private const val URLSEARCHPARAMS_CONSTRCUTOR_FN = "URLSearchParams"
private const val DOMAIN_TO_ASCII_FN = "domainToASCII"
private const val DOMAIN_TO_UNICODE_FN = "domainToUnicode"
private const val FILE_URL_TO_PATH_FN = "fileURLToPath"
private const val PATH_TO_FILE_URL_FN = "pathToFileURL"
private const val URL_TO_HTTPOPTIONS_FN = "urlToHttpOptions"

// Members of the URL module.
private val URL_MODULE_MEMBERS = arrayOf(
  URL_CONSTRUCTOR_FN,
  URLSEARCHPARAMS_CONSTRCUTOR_FN,
  DOMAIN_TO_ASCII_FN,
  DOMAIN_TO_UNICODE_FN,
  FILE_URL_TO_PATH_FN,
  PATH_TO_FILE_URL_FN,
  URL_TO_HTTPOPTIONS_FN,
)

// Installs the Node URL module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeURLModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): URLAPI = NodeURL.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.URL)) { NodeURL.obtain() }
  }
}

/**
 * # Node API: `url`
 */
internal class NodeURL : ReadOnlyProxyObject, URLAPI {
  override fun getMemberKeys(): Array<String> = URL_MODULE_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    URL_CONSTRUCTOR_FN -> URLIntrinsic.constructor
    URLSEARCHPARAMS_CONSTRCUTOR_FN -> URLSearchParamsIntrinsic.constructor
    DOMAIN_TO_ASCII_FN -> ProxyExecutable { a -> IDN.toASCII(a[0].asString()) }
    DOMAIN_TO_UNICODE_FN -> ProxyExecutable { a -> IDN.toUnicode(a[0].asString()) }
    FILE_URL_TO_PATH_FN -> ProxyExecutable { a -> Paths.get(URI(a[0].asString())).toAbsolutePath().toString() }
    PATH_TO_FILE_URL_FN -> ProxyExecutable { a -> URLIntrinsic.URLValue.fromURL(Path.of(a[0].asString()).toUri().toURL()) }
    URL_TO_HTTPOPTIONS_FN -> ProxyExecutable { a ->
      val u = URI(a[0].asString())
      val path = (u.rawPath ?: "") + (u.rawQuery?.let { "?${it}" } ?: "")
      ProxyObject.fromMap(mapOf(
        "protocol" to "${u.scheme}:",
        "hostname" to u.host,
        "port" to (u.port.takeIf { it >= 0 }),
        "path" to path,
      ))
    }
    else -> null
  }

  internal companion object {
    private val SINGLETON = NodeURL()
    fun obtain(): NodeURL = SINGLETON
  }
}
