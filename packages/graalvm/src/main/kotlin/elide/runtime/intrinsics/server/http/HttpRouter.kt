package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.internal.HandlerRegistry

/**
 * Base class providing route registration APIs to guest code, compiling routing keys that can be used to resolve
 * handler references from a [HandlerRegistry].
 */
@DelicateElideApi public interface HttpRouter {
  /** Guest-accessible method used to register a [handler] for the provided [method] and [path]. */
  @Export public fun handle(method: String?, path: String?, handler: PolyglotValue)
}