/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.plugins.python.flask

import org.graalvm.polyglot.HostAccess.Export
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * Host bridge object injected into Python as `__host__`.
 * Provides minimal APIs required by the Flask shim.
 */
@DelicateElideApi public class HostBridge internal constructor() {
  /** In-memory route registry: (method,path) -> handler id. */
  private val routes: ConcurrentMap<Pair<String, String>, String> = ConcurrentHashMap()

  /** Router handle from Elide intrinsics (Elide.http.router), set during context init. */
  private var router: PolyglotValue? = null

  /** Thread-local snapshot of the current request for this handler thread. */
  private val currentReq: ThreadLocal<Map<String, Any?>> = ThreadLocal.withInitial { emptyMap() }

  /** Register a route mapping for a Flask-like rule and list of methods (diagnostic). */
  @Export public fun register_route(rule: String, methods: List<String>, handler_id: String) {
    methods.forEach { m -> routes[m.uppercase() to rule] = handler_id }
  }

  /** Host-side: set the router value so we can bind routes from host without Python referencing Elide. */
  @Export public fun _set_router(router: PolyglotValue) {
    this.router = router
  }

  /** Host-side: bind a wrapped Python handler directly to the Elide router. */
  @Export public fun bind_route(method: String, rule: String, handler: PolyglotValue) {
    val r = requireNotNull(router) { "Router not set on HostBridge" }
    r.invokeMember("handle", method.uppercase(), rule, handler)
  }

  /** Return the current request snapshot as a dictionary. */
  @Export public fun current_request(): Map<String, Any?> = currentReq.get()

  /** Internal: set the current request snapshot (used by the Python shim). */
  @Export public fun _set_current_request(snapshot: Map<String, Any?>) {
    currentReq.set(snapshot)
  }

  /** Call a registered handler by id with the given request dict; returns a response dict. */
  @Export public fun call_handler(handler_id: String, req: Map<String, Any?>): Map<String, Any?> {
    // Minimal stub for compatibility; Python shim handles direct handler invocation via router.
    // This can be expanded to drive host-side dispatch if desired.
    return mapOf("status" to 200, "headers" to emptyMap<String, String>(), "body" to "")
  }

  /** Expose registered routes (primarily for diagnostics). */
  @Export public fun _routes(): Map<String, String> = routes.mapKeys { (k, _) -> "${'$'}{k.first} ${'$'}{k.second}" }
}

