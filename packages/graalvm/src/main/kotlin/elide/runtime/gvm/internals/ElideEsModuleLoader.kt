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

import com.oracle.js.parser.ir.Module.ModuleRequest
import com.oracle.truffle.api.source.Source
import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.objects.*
import org.graalvm.polyglot.proxy.ProxyObject
import kotlinx.atomicfu.atomic
import elide.runtime.gvm.loader.JSRealmPatcher

private const val DENO_MODULE_PREFIX = "deno"
private const val BUN_MODULE_PREFIX = "bun"
private const val NODE_MODULE_PREFIX = "node"
private const val ELIDE_MODULE_PREFIX = "elide"

// Implements Elide's internal ECMA-compliant module loader.
internal class ElideEsModuleLoader private constructor (
  @Suppress("unused") private val realm: JSRealm,
  private val base: DefaultESModuleLoader,
) : JSModuleLoader {
  // Qualifies the type of synthesized module, as applicable.
  enum class ModuleQualifier {
    DENO,
    BUN,
    NODE,
    ELIDE,
  }

  // Module record which holds a synthesized module object.
  inner class ElideSynthesizedModuleRecord (qualifier: ModuleQualifier, data: JSModuleData, module: ProxyObject)
    : JSModuleRecord(data, this, module)

  override fun loadModule(moduleSource: Source?, moduleData: JSModuleData): JSModuleRecord {
//    moduleData.
    val ret = base.loadModule(moduleSource, moduleData)
//    JSModuleRecord(moduleData, this, null)
    return ret
  }

  override fun resolveImportedModule(
    referencingModule: ScriptOrModule?,
    moduleRequest: ModuleRequest,
  ): JSModuleRecord {
//    val moduleName = moduleRequest.specifier.toString()
//    when (if (':' in moduleName) moduleName.substringBefore(':') else null) {
//      DENO_MODULE_PREFIX -> ModuleQualifier.DENO
//      BUN_MODULE_PREFIX -> ModuleQualifier.BUN
//      NODE_MODULE_PREFIX -> ModuleQualifier.NODE
//      ELIDE_MODULE_PREFIX -> ModuleQualifier.ELIDE
//      else -> null
//    }.let { qualifier ->
//      when (qualifier) {
//        null -> null
//        else -> ElideSynthesizedModuleRecord(qualifier,)
//      }
//    }

    val ret = base.resolveImportedModule(referencingModule, moduleRequest)
    return ret
  }

  companion object {
    // Whether the loader has been installed in the root realm yet.
    private val installed = atomic(false)

    // Registered singleton holder.
    private lateinit var singleton: ElideEsModuleLoader

    // Install the module loader and return it.
    @JvmStatic private fun install(realm: JSRealm): ElideEsModuleLoader =
      when (installed.compareAndSet(false, true)) {
        false -> singleton
        true -> synchronized(this) {
          JSRealmPatcher.overrideLoader<DefaultESModuleLoader, ElideEsModuleLoader>(realm) { base ->
            ElideEsModuleLoader(realm, base).also {
              singleton = it
            }
          }
        }
      }

    /**
     * Lazily initialize Elide's ES module loader singleton; install it to the provided [realm] if needed.
     *
     * If the loader has already been installed, this method will return the singleton instance.
     *
     * @param realm The realm to install the loader to.
     * @return The ES module loader singleton.
     */
    @JvmStatic fun obtain(realm: JSRealm): ElideEsModuleLoader = install(realm)
  }
}
