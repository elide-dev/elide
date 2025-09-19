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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.server.http.internal.GuestCallback
import elide.vm.annotations.Polyglot

// Properties and methods available for guest access on `HttpServerConfig`.
internal val HTTP_SERVER_CONFIG_PROPS_AND_METHODS = arrayOf(
  "port",
  "autoStart",
  "onBind",
)

// Default host to use for HTTP services.
private const val DEFAULT_ELIDE_HTTP_SERVER_HOST = "localhost"

// Default port to use for HTTP services.
private const val DEFAULT_ELIDE_HTTP_SERVER_PORT = 8080

/**
 * An extensible container for HTTP server configuration values. Backend-specific implementations may be added to allow
 * low-level customization of the server settings.
 */
@DelicateElideApi public open class HttpServerConfig internal constructor() : ProxyObject {
  /**
   * Optional callback to be invoked once the server starts listening for connections. This value can be set using
   * [onBind] and is verified to be executable, though no guarantees are made regarding its signature.
   */
  internal var onBindCallback: GuestCallback? = null
    private set

  /** The host to which the server will bind when listening for connections, defaults to `localhost`. */
  @Polyglot public open var host: String = DEFAULT_ELIDE_HTTP_SERVER_HOST

  /** The port to which the server will bind when listening for connections.
   *
   * Default precedence:
   * 1) System property 'elide.server.port'
   * 2) Environment variable 'PORT'
   * 3) Fallback to 8080
   */
  @Polyglot public open var port: Int = run {
    val sysProp = System.getProperty("elide.server.port")?.toIntOrNull()
    val envPort = System.getenv("PORT")?.toIntOrNull()
    val candidate = sysProp ?: envPort ?: DEFAULT_ELIDE_HTTP_SERVER_PORT
    when {
      candidate in 1..65535 -> candidate
      else -> DEFAULT_ELIDE_HTTP_SERVER_PORT
    }
  }

  /**
   * Whether to automatically start the server after evaluating the configuration code. If `true`, calling
   * [HttpServerEngine.start] explicitly from guest code is not necessary. Defaults to `false`.
   */
  @Polyglot public open var autoStart: Boolean = false

  /**
   * Register a [callback] to be invoked when the server starts listening for connections. The [callback] value must
   * be executable, otherwise an exception will be thrown.
   *
   * If a callback is already registered, it will be replaced.
   *
   * @throws IllegalArgumentException If the provided [callback] value is not executable.
   */
  @Polyglot public open fun onBind(callback: PolyglotValue) {
    // verify and register the callback
    onBindCallback = GuestCallback.of(callback)
  }

  override fun getMemberKeys(): Array<String> = HTTP_SERVER_CONFIG_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in HTTP_SERVER_CONFIG_PROPS_AND_METHODS

  override fun putMember(key: String?, value: Value?): Unit = when (key) {
    "port" -> {
      val isNull = value == null || value.isNull
      if (!isNull) {
        when {
          !value!!.isNumber -> throw JsError.typeError("Please set `port` to a number")
          !value.fitsInShort() -> throw JsError.valueError("Port is out of range (not a short)")
          else -> value.asInt().let { portInt ->
            when {
              portInt > 65535 -> throw JsError.valueError("Port is out of range (bigger than 65535)")
              portInt < 1 -> throw JsError.valueError("Port is out of range (0 or negative)")
              else -> port = portInt
            }
          }
        }
      } else {
        // reset the port to the default value if given host or guest `null`
        port = DEFAULT_ELIDE_HTTP_SERVER_PORT
      }
    }
    else -> { /* no-op */ }
  }

  override fun getMember(key: String?): Any? = when (key) {
    "port" -> port
    "autoStart" -> autoStart
    "onBind" -> ProxyExecutable { args ->
      onBind(requireNotNull(args.getOrNull(0)).also {
        if (!it.canExecute()) throw JsError.typeError("Expected function for `onBind`")
      })
    }

    // unfamiliar key
    else -> null
  }
}
