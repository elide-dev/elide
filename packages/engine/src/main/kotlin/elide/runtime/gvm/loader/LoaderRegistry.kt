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

import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.objects.JSModuleLoader
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

/**
 * # Loader Registry
 *
 * Static utility for registering [JSModuleLoader]-compliant implementations, and similar objects for other languages;
 * these registered objects are consulted from delegated Elide types.
 */
public object LoaderRegistry {
  // Registered JavaScript module loaders.
  @JvmStatic private val rootEsLoaders = LinkedList<JSModuleLoader>()

  // Registered JavaScript Realm-specific module loaders.
  @JvmStatic private val realmBoundEsLoaders = ConcurrentHashMap<JSRealm, LinkedList<WeakReference<JSModuleLoader>>>()

  /**
   * Register a JavaScript module loader.
   *
   * @param loader The JavaScript module loader to register.
   */
  @JvmStatic public fun register(loader: JSModuleLoader) {
    rootEsLoaders.add(loader)
  }

  /**
   * Register a JavaScript module loader.
   *
   * @param realm JavaScript realm to bind this loader to.
   * @param loader The JavaScript module loader to register.
   */
  @JvmStatic public fun register(realm: JSRealm, loader: JSModuleLoader) {
    val bound = realmBoundEsLoaders.computeIfAbsent(realm) { LinkedList() }
    bound.add(WeakReference(loader))
  }

  /**
   * Mount the provided module [loader] forcibly as the main loader for a given JavaScript [realm].
   *
   * @param realm The JavaScript realm to mount the loader to.
   * @param loader The JavaScript module loader to mount.
   */
  @JvmStatic public fun mountPrimary(realm: JSRealm, loader: JSModuleLoader) {
    if (realm.parent == null) {
      register(loader)
    }
    register(realm, loader) // associate with realm, too
    JSRealmPatcher.overrideLoader<JSModuleLoader, JSModuleLoader>(realm) {
      loader
    }
  }
}
