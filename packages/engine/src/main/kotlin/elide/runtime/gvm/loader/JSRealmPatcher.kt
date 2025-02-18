/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.gvm.loader

import com.oracle.truffle.js.builtins.commonjs.NpmCompatibleESModuleLoader
import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.objects.JSModuleLoader
import java.lang.reflect.Field

/**
 * # JS Realm Patcher
 *
 * Internal utilities for patching the root JavaScript realm.
 */
public object JSRealmPatcher {
  // Resolved field for the root JS realm's module loader.
  @PublishedApi internal val moduleLoaderField: Field by lazy {
    try {
      JSRealm::class.java.getDeclaredField("moduleLoader").also {
        it.isAccessible = true
      }
    } catch (e: NoSuchFieldException) {
      throw IllegalStateException("Failed to resolve `moduleLoader` field from `JSRealm`", e)
    } catch (e: IllegalAccessException) {
      throw IllegalStateException("Failed to forcibly allow access to `moduleLoader` field from `JSRealm`", e)
    }
  }

  /**
   * Override the current [JSModuleLoader] with a wrapped instance, based on the [Base] loader type; the returned
   * [Loader] type is the overridden loader, which is expected to wrap the base loader.
   *
   * Under the hood, this method simply calls [getModuleLoader], then the [prepare] factory, then [installModuleLoader].
   *
   * @param jsRealm The JS realm to override the module loader in.
   * @param prepare The factory to prepare the new loader instance.
   * @return The overridden loader instance.
   */
  public inline fun <reified Base : JSModuleLoader, reified Loader : JSModuleLoader> overrideLoader(
    jsRealm: JSRealm,
    crossinline prepare: (Base) -> Loader,
  ): Loader = getModuleLoader<Base>(jsRealm).let { baseLoader ->
    prepare(baseLoader).also { loader ->
      installModuleLoader(jsRealm, loader)
    }
  }

  /**
   * Retrieve the current [JSModuleLoader] from the provided [jsRealm].
   *
   * This method will throw if:
   * - The loader is not available because its field is set to `null`.
   * - The loader is not an instance of the expected type.
   *
   * @param jsRealm The JS realm to retrieve the module loader from.
   * @return The module loader instance.
   * @throws IllegalStateException If the module loader is not available or is not an instance of the expected type.
   */
  public inline fun <reified Loader : JSModuleLoader> getModuleLoader(jsRealm: JSRealm): Loader {
    return (moduleLoaderField[jsRealm] ?: NpmCompatibleESModuleLoader.create(jsRealm)).also {
      require(it is Loader) {
        "Failed to cast `moduleLoader` field from `JSRealm` to `${Loader::class.java.simpleName}`"
      }
    } as Loader
  }

  /**
   * Forcibly install the specified [moduleLoader] from the provided [jsRealm].
   *
   * This method will throw if:
   * - The field cannot be resolved (on first access).
   * - The loader cannot be set because its field is not accessible.
   *
   * @param jsRealm The JS realm to install the module loader into.
   * @throws IllegalStateException If the module loader install step fails.
   */
  public inline fun <reified Loader : JSModuleLoader> installModuleLoader(jsRealm: JSRealm, moduleLoader: Loader) {
    val current = moduleLoaderField[jsRealm]
    if (current !== moduleLoader) {
      moduleLoaderField[jsRealm] = moduleLoader
    }
  }
}
