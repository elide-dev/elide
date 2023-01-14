package elide.runtime.gvm

import elide.runtime.js.require

/**
 *
 */
internal val globalThis: dynamic = js("globalThis")

/**
 *
 */
internal fun assign(name: String, target: dynamic) {
  globalThis[name] = target
}

/**
 *
 */
internal fun mount(pkg: String, path: String, name: String) {
  var target: dynamic = require(pkg)
  if (path.isNotEmpty()) {
    target = target[path]
  }
  assign(name, target)
}

/**
 *
 */
internal fun mount(pkg: String, name: String) {
  mount(
    pkg,
    "",
    name,
  )
}
