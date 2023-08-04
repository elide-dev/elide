/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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
