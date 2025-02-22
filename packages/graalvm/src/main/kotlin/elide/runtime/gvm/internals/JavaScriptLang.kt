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
package elide.runtime.gvm.internals

import com.oracle.truffle.api.nodes.Node
import com.oracle.truffle.js.runtime.JSRealm
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import kotlinx.atomicfu.atomic
import elide.runtime.Logging
import elide.runtime.gvm.loader.JSRealmPatcher

// Access to GraalJs internals and other types.
internal object JavaScriptLang {
  private val logging by lazy { Logging.of(JavaScriptLang::class) }

  // Whether JavaScript language extensions have initialized.
  private val initialized = atomic(false)

  // Initialize JavaScript language tooling; called early in JavaScript's init lifecycle.
  fun initialize() {
    if (initialized.compareAndSet(false, true)) {
      //initializeJsIntegration()
    }
  }

  // Register overrides for the JavaScript language engine.
  @JvmStatic private fun initializeJsIntegration() {
    ByteBuddyAgent.install()

    ByteBuddy()
      // extends standard `JSRealm` class
      //.redefine(JSRealm::class.java)
      // replace/patch `JSRealm`
      .rebase(JSRealm::class.java)
      // define within the same package
      //.name("${JSRealm::class.java.packageName}.ElideJSRealmProxy")
      // override `JSRealm.getModuleLoader` and `JSRealm.createModuleLoader`
      .method(ElementMatchers.named(ElideEsModuleLoader.Proxy.GET_MODULE_LOADER))
      // intercept and delegate methods to `ElideEsModuleLoader.Proxy`
      .intercept(MethodDelegation.to(ElideEsModuleLoader.Proxy::class.java))
      // generate the dynamic patch class
      .make()
      // load it into the vm
      .load(JSRealm::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())
  }

  /**
   * Obtain a [JSRealm] according to the provided [node]; if [node] is `null`, the root [JSRealm] is returned.
   *
   * @param node the node to obtain the [JSRealm] for; if `null`, the root [JSRealm] is returned
   * @return the [JSRealm] for the provided [node], or the root [JSRealm] if [node] is `null`
   */
  @JvmStatic @Suppress("unused") fun obtainRealm(node: Node? = null): JSRealm = JSRealm.get(node)

  // Bootstrap a JavaScript realm; if the realm is the root realm, additional setup is performed.
  fun JSRealm.bootstrap(root: Boolean) {
    logging.info("Acquired realm (root: $root) / $this")
    ElideEsModuleLoader.obtain(this).also {
      JSRealmPatcher.installModuleLoader(this, it)
    }
  }
}
