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

package elide.runtime.lang.javascript

import com.oracle.truffle.api.TruffleFile
import com.oracle.truffle.js.runtime.CommonJSResolverHook
import com.oracle.truffle.js.runtime.JSEngine
import com.oracle.truffle.js.runtime.JSModuleLoaderFactory
import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.objects.JSModuleLoader
import java.util.Optional
import kotlinx.atomicfu.atomic
import elide.runtime.Logging

// Whether to install a custom JS module loader; should normally be `true`.
private const val ELIDE_LOADER_ENABLED = true

/**
 * ## Elide JavaScript Module Router
 *
 * This is the central installation point for JavaScript module injection; ESM and CJS are supported transparently. The
 * module router must be installed before any JS code is executed (specifically, before evaluation calls), through the
 * [install] method.
 *
 * Once installed, the JavaScript module router accepts requests from GraalJs to:
 *
 * - Create ESM module loader implementation instances on-demand, and
 * - Intercept CommonJS module resolution requests.
 *
 * Based on registered built-ins, registered resolvers/module loaders, and so on, the router will then delegate to Elide
 * to attempt to load the module. If the module:
 *
 * - Is not a built-in module, or
 * - Cannot be located
 *
 * Then the module injection system will fall-back to default GraalJs behavior, through an NPM-compatible ESM loader.
 *
 * @see ElideUniversalJsModuleLoader Elide's universal module loader
 * @see SyntheticJSModule Interface that modules are expected to implement to be eligible for injection
 * @see JSModuleProvider Interface for ESM compatibility
 * @see CommonJSResolverHook Interface for CJS compatibility
 */
internal object ElideJsModuleRouter : JSModuleLoaderFactory, CommonJSResolverHook {
  private val initialized = atomic(false)

  /**
   * Install the Elide JS module router hooks into the GraalJs runtime.
   *
   * This method should be called once, early in the application lifecycle, to ensure that Elide's module loader is
   * available before any JavaScript code is executed. Installation only needs to take place once because these factory
   * methods are installed statically.
   *
   * As a result, after calling this method, signals will be sent to Elide to load modules for the remainder of the
   * application's lifecycle (unless cleared at a future time).
   *
   * This method is idempotent; repeated calls to this method are ignored.
   */
  @JvmStatic fun install() {
    if (!initialized.getAndSet(true)) {
      JSEngine.setModuleLoaderFactory(this)
      JSEngine.setCjsResolverHook(this)
    }
  }

  // Create an ESM-compatible loader for a new realm which is loading modules. Called on-demand.
  override fun createLoader(realm: JSRealm): JSModuleLoader? = if (!ELIDE_LOADER_ENABLED) {
    null
  } else {
    ElideUniversalJsModuleLoader.create(
      realm,
    )
  }

  // Resolve a CommonJS module require encountered in JavaScript. Called on-demand.
  override fun resolveModule(realm: JSRealm, moduleIdentifier: String, entryPath: TruffleFile): Any? {
    Logging.root().warn("CJS: require('$moduleIdentifier')")
    return ElideUniversalJsModuleLoader.resolve(realm, moduleIdentifier)
      ?.provide()
      ?.let { realm.env.asGuestValue(it) }
  }
}
