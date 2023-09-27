package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.internal.GuestCallback

/**
 * An extensible container for HTTP server configuration values. Backend-specific implementations may be added to allow
 * low-level customization of the server settings.
 */
@DelicateElideApi public open class HttpServerConfig internal constructor() {
  /**
   * Optional callback to be invoked once the server starts listening for connections. This value can be set using
   * [onBind] and is verified to be executable, though no guarantees are made regarding its signature.
   */
  internal var onBindCallback: GuestCallback? = null
    private set

  /** The port to which the server will bind when listening for connections, defaults to `8080`. */
  @Export public open var port: Int = 8080

  /**
   * Whether to automatically start the server after evaluating the configuration code. If `true`, calling
   * [HttpServerEngine.start] explicitly from guest code is not necessary. Defaults to `false`.
   */
  @Export public open var autoStart: Boolean = false

  /**
   * Register a [callback] to be invoked when the server starts listening for connections. The [callback] value must
   * be executable, otherwise an exception will be thrown.
   *
   * If a callback is already registered, it will be replaced.
   *
   * @throws IllegalArgumentException If the provided [callback] value is not executable.
   */
  @Export public open fun onBind(callback: PolyglotValue) {
    // verify and register the callback
    onBindCallback = GuestCallback.of(callback)
  }
}