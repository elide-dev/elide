package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi

/**
 * A base contract for HTTP server implementations that must be configurable by guest code.
 *
 * The [config] property allows guest code to change the binding port, set a start callback, and adjust other
 * backend-specific options as applicable.
 *
 * The [router] property can be used to register route handlers from guest code.
 */
@DelicateElideApi public interface HttpServerEngine {
  /** Whether the server is running and listening for connections. */
  @get:Export public val running: Boolean

  /** Router for this server engine, accessible by guest code and capable of registering route handlers. */
  @get:Export public val router: HttpRouter

  /** Configuration for this server engine, accessible by guest code. */
  @get:Export public val config: HttpServerConfig

  /**
   * Starts listening for connections.
   *
   * Only the first call to [start] will take effect, subsequent invocations will be ignored. Once called, [running]
   * will return `true`.
   *
   * If [config.autoStart][HttpServerConfig.autoStart] is set to `true`, this method will be called automatically by
   * the [HttpServerAgent] after configuration, if it was not explicitly called by guest code.
   */
  @Export public fun start()
}