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
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.URLAPI
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Value
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
    val moduleInfo = ModuleInfo.of(NodeModuleName.URL)
    ModuleRegistry.deferred(moduleInfo) { NodeURL.obtain() }
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
    DOMAIN_TO_ASCII_FN -> ProxyExecutable { args ->
      if (args.isEmpty()) return@ProxyExecutable ""
      val input = args[0].asStringSafe()
      if (input.isEmpty()) return@ProxyExecutable ""
      try {
        IDN.toASCII(input)
      } catch (_: Throwable) {
        ""
      }
    }
    DOMAIN_TO_UNICODE_FN -> ProxyExecutable { args ->
      if (args.isEmpty()) return@ProxyExecutable ""
      val input = args[0].asStringSafe()
      if (input.isEmpty()) return@ProxyExecutable ""
      try {
        IDN.toUnicode(input)
      } catch (_: Throwable) {
        ""
      }
    }
    FILE_URL_TO_PATH_FN -> ProxyExecutable { args ->
      if (args.isEmpty()) return@ProxyExecutable ""
      val raw = args[0]
      val href = when {
        raw.isString -> raw.asString()
        raw.hasMembers() -> raw.getMember("href")?.takeIf { it.isString }?.asString() ?: raw.toString()
        else -> raw.toString()
      }
      if (href.isEmpty()) return@ProxyExecutable ""
      try {
        val uri = URI(href)
        // Only handle file scheme
        if (uri.scheme?.lowercase() != "file") return@ProxyExecutable ""
        // Handle Windows drive letters and UNC
        if (uri.authority != null && uri.path != null && uri.path!!.startsWith("/")) {
          // file://server/share -> \\server\share
          return@ProxyExecutable "\\\\" + uri.authority + uri.path!!.replace('/', '\\')
        }
        Paths.get(uri).toString()
      } catch (_: Throwable) {
        ""
      }
    }
    PATH_TO_FILE_URL_FN -> ProxyExecutable { args ->
      if (args.isEmpty()) return@ProxyExecutable null
      val input = args[0].asStringSafe()
      if (input.isEmpty()) return@ProxyExecutable null
      try {
        var href = Paths.get(input).toUri().toString()
        // Normalize to `file:///` for Windows and platforms that emit `file:/` from toUri()
        if (href.startsWith("file:/") && !href.startsWith("file:///")) {
          href = href.replaceFirst("file:/", "file:///")
        }
        // Ensure UNC shares get the correct authority form
        if (input.startsWith("\\\\")) {
          // file:////server/share style
          val without = input.removePrefix("\\\\").replace('\\', '/')
          href = "file:////" + without
        }
        // Return a URL object per Node API
        (URLIntrinsic.constructor as ProxyInstantiable).newInstance(Value.asValue(href))
      } catch (_: Throwable) {
        null
      }
    }
    URL_TO_HTTPOPTIONS_FN -> ProxyExecutable { args ->
      if (args.isEmpty()) return@ProxyExecutable ProxyObject.fromMap(mutableMapOf<String, Any>())
      val input = args[0]
      val href = when {
        input.isString -> input.asString()
        input.hasMembers() ->
          input.getMember("href")?.takeIf { it.isString }?.asString()
            ?: runCatching { input.toString() }.getOrDefault("")
        else -> runCatching { input.toString() }.getOrDefault("")
      }
      if (href.isBlank()) return@ProxyExecutable ProxyObject.fromMap(mutableMapOf<String, Any>())
      val map = linkedMapOf<String, Any>()
      try {
        val uri = URI(href)
        val scheme = uri.scheme ?: "http"
        val hostname = uri.host ?: ""
        val port = if (uri.port > 0) uri.port.toString() else ""
        val host = if (port.isNotEmpty() && hostname.isNotEmpty()) "$hostname:$port" else hostname
        val path = buildString {
          append(uri.path ?: "")
          val q = uri.rawQuery
          if (!q.isNullOrEmpty()) {
            append("?")
            append(q)
          }
        }
        val auth = uri.userInfo
        map["protocol"] = "$scheme:"
        if (host.isNotEmpty()) map["host"] = host
        if (hostname.isNotEmpty()) map["hostname"] = hostname
        if (port.isNotEmpty()) map["port"] = port
        if (path.isNotEmpty()) map["path"] = path
        if (!auth.isNullOrEmpty()) map["auth"] = auth
      } catch (_: Throwable) {
        // return empty options on parse failure (minimal behavior)
      }
      ProxyObject.fromMap(map)
    }
    else -> null
  }

  internal companion object {
    private val SINGLETON = NodeURL()
    fun obtain(): NodeURL = SINGLETON
  }
}

// Helper to safely coerce a Polyglot Value to String
private fun Value.asStringSafe(): String = when {
  this.isNull -> ""
  this.isString -> this.asString()
  else -> this.toString()
}
