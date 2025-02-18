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

import com.oracle.js.parser.ir.Module
import com.oracle.js.parser.ir.Module.ExportEntry
import com.oracle.js.parser.ir.Module.ModuleRequest
import com.oracle.truffle.api.CallTarget
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.source.Source
import com.oracle.truffle.js.nodes.JSFrameDescriptor
import com.oracle.truffle.js.nodes.JSFrameSlot
import com.oracle.truffle.js.runtime.JSArguments
import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.JavaScriptRootNode
import com.oracle.truffle.js.runtime.Strings
import com.oracle.truffle.js.runtime.builtins.JSFunctionData
import com.oracle.truffle.js.runtime.objects.*
import com.oracle.truffle.js.runtime.objects.JSModuleRecord.Status
import elide.core.api.Symbolic
import elide.runtime.gvm.internals.js.ELIDE_TS_LANGUAGE_ID
import elide.runtime.gvm.loader.JSRealmPatcher
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import kotlinx.atomicfu.atomic
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.LinkedList
import java.util.concurrent.ConcurrentSkipListMap
import elide.runtime.Logging
import elide.runtime.gvm.internals.js.SyntheticJsModule

// Whether assertions are enabled for comparison with regular module imports.
private const val COMPARE_WITH_BASE = true

// Whether to disable this loader and apply default behavior.
private const val ALWAYS_FALLBACK = true

// Known module prefixes.
private const val DENO_MODULE_PREFIX = "deno"
private const val BUN_MODULE_PREFIX = "bun"
private const val NODE_MODULE_PREFIX = "node"
private const val ELIDE_MODULE_PREFIX = "elide"

// All built-in Elide modules.
private val allElideModules = sortedSetOf(
  "sqlite",
)

// All built-in Node modules.
private val allNodeModules = sortedSetOf(
    "assert",
    "async_hooks",
    "buffer",
    "child_process",
    "cluster",
    "console",
    "constants",
    "crypto",
    "dgram",
    "dns",
    "domain",
    "events",
    "fs",
    "http",
    "http2",
    "https",
    "inspector",
    "module",
    "net",
    "os",
    "path",
    "perf_hooks",
    "process",
    "punycode",
    "querystring",
    "readline",
    "repl",
    "stream",
    "string_decoder",
    "timers",
    "tls",
    "trace_events",
    "tty",
    "url",
    "util",
    "v8",
    "vm",
    "worker_threads",
    "zlib",
)

// Module prefixes which trigger some kind of behavior.
private val specialModulePrefixes = arrayOf(
  DENO_MODULE_PREFIX,
  BUN_MODULE_PREFIX,
  NODE_MODULE_PREFIX,
  ELIDE_MODULE_PREFIX,
)

// Implements Elide's internal ECMA-compliant module loader.
internal class ElideEsModuleLoader private constructor (
  @Suppress("unused") private val realm: JSRealm,
) : AbstractJsModuleLoader(realm) {
  // Strategies for loading modules.
  enum class ModuleStrategy {
    // Fallback to built-in behaviors.
    FALLBACK,

    // Use a synthetic injected module.
    SYNTHETIC,

    // Delegate to other module loaders (for instance, for TypeScript or other meta-lang support).
    DELEGATED,
  }

  // Qualifies the type of synthesized module, as applicable.
  enum class ModuleQualifier (override val symbol: String): Symbolic<String> {
    DENO(DENO_MODULE_PREFIX),
    BUN(BUN_MODULE_PREFIX),
    NODE(NODE_MODULE_PREFIX),
    ELIDE(ELIDE_MODULE_PREFIX);

    companion object: Symbolic.SealedResolver<String, ModuleQualifier> {
      override fun resolve(symbol: String): ModuleQualifier = when (symbol) {
        DENO.symbol -> DENO
        BUN.symbol -> BUN
        NODE.symbol -> NODE
        ELIDE.symbol -> ELIDE
        else -> throw unresolved("Unknown module qualifier: $symbol")
      }
    }
  }

  // Cache of injected modules.
  private val injectedModuleCache: MutableMap<String, JSModuleRecord> = ConcurrentSkipListMap()

  // Module record which holds a synthesized module object.
  inner class ElideSyntheticModuleRecord (qualifier: ModuleQualifier, data: JSModuleData, module: Any)
    : JSModuleRecord(data, this, module) {

    }

  // Check an un-prefixed module name against the built-in modules.
  private fun resolveUnprefixed(name: String): ModuleStrategy = when (name) {
    in allElideModules, in allNodeModules -> ModuleStrategy.SYNTHETIC
    else -> ModuleStrategy.DELEGATED
  }

  // Determine the loading strategy to use for a given module request.
  private fun determineModuleStrategy(source: Source): ModuleStrategy {
    if (ALWAYS_FALLBACK) return ModuleStrategy.FALLBACK
    return when (source.language) {
      // typescript/tsx/etc. is delegated, always
      ELIDE_TS_LANGUAGE_ID -> ModuleStrategy.DELEGATED

      // otherwise, fall back to regular module loader behavior
      else -> ModuleStrategy.FALLBACK
    }
  }

  // Determine the loading strategy to use for a given module request.
  private fun determineModuleStrategy(requested: String): ModuleStrategy {
    if (ALWAYS_FALLBACK) return ModuleStrategy.FALLBACK
    val indexOfSplit = requested.indexOf(':')
    val prefix = if (indexOfSplit != -1) requested.substring(0, indexOfSplit) else null
    val unprefixed = if (prefix != null) requested.substring(indexOfSplit, requested.length) else requested

    return when (prefix) {
      // un-prefixed modules may still be built-in (for example, `fs`).
      null -> resolveUnprefixed(unprefixed)

      // special prefixes are always synthetic
      in specialModulePrefixes -> ModuleStrategy.SYNTHETIC

      // otherwise we try delegation
      else -> ModuleStrategy.FALLBACK
    }
  }

  // Synthesize an injected module.
  private fun synthesizeInjected(
    referencingModule: ScriptOrModule,
    moduleRequest: ModuleRequest,
    prefix: String?,
    name: String,
  ): JSModuleRecord {
    if (name in moduleMap) {
      return requireNotNull(moduleMap[name])
    }
    return when (val info = ModuleInfo.of(name)) {
        in ModuleRegistry -> {
            val surface = requireNotNull(ModuleRegistry.load(info)) { "No such synthesized module: $info" }
            if (surface is SyntheticJsModule) {
              val facadeSrc = surface.facade()
              val facade = Source.newBuilder("js", facadeSrc, "$name-facade.mjs")
                .internal(true)
                .cached(true)
                .build()

              val parsedModule = realm.context.evaluator.envParseModule(realm, facade)
              val record = JSModuleRecord(parsedModule, this)
              moduleMap[name] = record
              record.rememberImportedModuleSource(moduleRequest.specifier, facade)
              return record
            }
            // @TODO: code does not work from here to return
            val defaultExportName = Strings.DEFAULT
            val defaultExport = ExportEntry.exportSpecifier(defaultExportName)

            @Suppress("UNCHECKED_CAST")
            val propNames = when (surface) {
                is ProxyObject -> surface.memberKeys as Array<String>
                else -> emptyArray<String>()
            }.map {
                it to Strings.constant(it)
            }
            val propExports = propNames.map { (_, const) ->
                ExportEntry.exportSpecifier(const)
            }

            // @TODO build slots for all exports
            val frameDescBuilder = JSFrameDescriptor(Undefined.instance)
            val defaultExportSlot = frameDescBuilder.addFrameSlot(defaultExportName)
            val mappedSlots = sortedMapOf<String, JSFrameSlot>()
            val exportEntries = LinkedList<ExportEntry>()
            for ((slotName, const) in propNames) {
                val export = ExportEntry.exportSpecifier(const)
                exportEntries.add(export)
                val slot = frameDescBuilder.addFrameSlot(const)
                mappedSlots[slotName] = slot
            }

            val frameDescriptor = frameDescBuilder.toFrameDescriptor()

            val localExportEntries = buildList {
                add(defaultExport)
                addAll(propExports)
            }

            val modRecord = Module(
                /* requestedModules = */ emptyList(),
                /* importEntries = */ emptyList(),
                /* localExportEntries = */ localExportEntries,
                /* indirectExportEntries = */ emptyList(),
                /* starExportEntries = */ emptyList(),
                /* imports = */ null,
                /* exports = */ null,
            )
            val source = Source.newBuilder("js", "", "$name.mjs")
                .internal(true)
                .cached(false)
                .build()

            val rootNode: JavaScriptRootNode = object :
                JavaScriptRootNode(realm.context.language, source.createUnavailableSection(), frameDescriptor) {

                override fun execute(frame: VirtualFrame): Any {
                    val module = JSArguments.getUserArgument(frame.arguments, 0) as JSModuleRecord

                    if (module.environment == null) {
                        assert(module.status == Status.Linking)
                        module.environment = frame.materialize()
                    } else {
                        assert(module.status == Status.Evaluating)
                        setSyntheticModuleExport(module)
                    }
                    return Undefined.instance
                }

                private fun setSyntheticModuleExport(module: JSModuleRecord) {
                    module.environment.setObject(defaultExportSlot.index, surface)
                    if (surface is ProxyObject) {
                        for ((slotName, slot) in mappedSlots) {
                          // module.namespace.
                          module.environment.setObject(slot.index, surface.getMember(slotName))
                        }
                    }
                }
            }
            val callTarget: CallTarget = rootNode.callTarget
            val functionData = JSFunctionData.create(
                realm.context,
                callTarget,
                callTarget,
                0,
                Strings.EMPTY_STRING,
                false,
                false,
                true,
                true
            )
            val data = JSModuleData(
                modRecord,
                source,
                functionData,
                frameDescriptor,
            )
            if (COMPARE_WITH_BASE) {
              val base = super.resolveImportedModule(referencingModule, moduleRequest)
              when {
                base.module.localExportEntries.size != modRecord.localExportEntries.size -> error(
                  "Mismatch between expected export count ('${base.module.localExportEntries.size}') and " +
                  "actual export count ('${modRecord.localExportEntries.size}') for module '$name'"
                )
                base.module.imports.size != modRecord.imports.size -> error(
                  "Mismatch between expected import count ('${base.module.imports.size}') and " +
                  "actual import count ('${modRecord.imports.size}') for module '$name'"
                )
                base.module.indirectExportEntries.size != modRecord.indirectExportEntries.size -> error(
                  "Mismatch between expected indirect export count ('${base.module.indirectExportEntries.size}') and " +
                  "actual indirect export count ('${modRecord.indirectExportEntries.size}') for module '$name'"
                )
                base.module.starExportEntries.size != modRecord.starExportEntries.size -> error(
                  "Mismatch between expected star export count ('${base.module.starExportEntries.size}') and " +
                  "actual star export count ('${modRecord.starExportEntries.size}') for module '$name'"
                )
                base.moduleData.frameDescriptor.numberOfSlots != data.frameDescriptor.numberOfSlots -> error(
                  "Mismatch between expected slot count ('${base.moduleData.frameDescriptor.numberOfSlots}') and " +
                  "actual slot count ('${data.frameDescriptor.numberOfSlots}') for module '$name'"
                )
                base.moduleData.frameDescriptor.numberOfAuxiliarySlots
                        != data.frameDescriptor.numberOfAuxiliarySlots -> error(
                  "Mismatch between expected auxiliary slot count " +
                  "('${base.moduleData.frameDescriptor.numberOfAuxiliarySlots}') and " +
                  "actual auxiliary slot count ('${data.frameDescriptor.numberOfAuxiliarySlots}') for module '$name'"
                )
              }
            }
            ElideSyntheticModuleRecord(
                prefix?.let { ModuleQualifier.resolve(it) } ?: ModuleQualifier.ELIDE,
                data,
                surface,
            )
        }
        else -> {
          Logging.root().warn("No such synthesized module: '$name'. Falling back to VFS.")
          super.resolveImportedModule(referencingModule, moduleRequest)
        }
    }
  }

  // Load a module which needs pre-compilation (or some other special load step).
  private fun loadDelegatedSourceModule(moduleSource: Source, moduleData: JSModuleData): JSModuleRecord {
    TODO("not yet implemented: `loadDelegatedSourceModule`")
  }

  // Load a module which needs pre-compilation (or some other special load step).
  private fun resolveDelegatedImportedModule(ref: ScriptOrModule, req: ModuleRequest): JSModuleRecord {
    TODO("not yet implementedf: `resolveDelegatedImportedModule`")
  }

  override fun resolveImportedModule(
    referencingModule: ScriptOrModule,
    moduleRequest: ModuleRequest
  ): JSModuleRecord {
    val requested = moduleRequest.specifier.toString()
    val prefixPosition = requested.indexOf(':')
    val prefix = if (prefixPosition != -1) requested.substring(0, prefixPosition) else null
    val unprefixed = if (prefix != null) requested.substring(prefixPosition + 1, requested.length) else requested

    return when (determineModuleStrategy(requested)) {
      ModuleStrategy.FALLBACK -> super.resolveImportedModule(referencingModule, moduleRequest)
      ModuleStrategy.DELEGATED -> resolveDelegatedImportedModule(referencingModule, moduleRequest)
      ModuleStrategy.SYNTHETIC -> injectedModuleCache.computeIfAbsent(unprefixed) {
        synthesizeInjected(referencingModule, moduleRequest, prefix, unprefixed)
      }
    }
  }

  override fun loadModule(moduleSource: Source, moduleData: JSModuleData): JSModuleRecord {
    return when (determineModuleStrategy(moduleSource)) {
      // source modules (aside from meta-languages) are always handled by the base loader
      ModuleStrategy.FALLBACK -> super.loadModule(moduleSource, moduleData)
      ModuleStrategy.DELEGATED -> loadDelegatedSourceModule(moduleSource, moduleData)
      else -> error("Mode is not supported for source loading")
    }
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
          create(realm).also {
            singleton = it
            JSRealmPatcher.installModuleLoader<ElideEsModuleLoader>(realm, it)
          }
        }
      }

    /**
     * Create a new ESM loader; this method should be used with care (mostly for testing).
     *
     * @param realm The realm to install the loader to.
     * @return The ES module loader singleton.
     */
    @JvmStatic fun create(realm: JSRealm): ElideEsModuleLoader = ElideEsModuleLoader(realm)

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
