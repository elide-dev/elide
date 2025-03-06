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

@file:Suppress(
  "TOO_LONG_FUNCTION",
  "ForbiddenComment",
  "UnusedParameter",
  "UnusedPrivateProperty",
)

package elide.runtime.lang.javascript

import com.oracle.js.parser.ir.Module
import com.oracle.js.parser.ir.Module.ExportEntry
import com.oracle.js.parser.ir.Module.ModuleRequest
import com.oracle.truffle.api.CallTarget
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.TruffleFile
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.source.Source
import com.oracle.truffle.js.builtins.commonjs.NpmCompatibleESModuleLoader
import com.oracle.truffle.js.nodes.JSFrameDescriptor
import com.oracle.truffle.js.nodes.JSFrameSlot
import com.oracle.truffle.js.runtime.JSArguments
import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.JavaScriptRootNode
import com.oracle.truffle.js.runtime.Strings
import com.oracle.truffle.js.runtime.builtins.JSFunctionData
import com.oracle.truffle.js.runtime.objects.*
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.File
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import elide.core.api.Symbolic
import elide.runtime.Logging
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.lang.javascript.DelegatedModuleLoaderRegistry.DelegatedModuleRequest
import elide.runtime.lang.javascript.ElideUniversalJsModuleLoader.ModuleStrategy.*

// Whether assertions are enabled for comparison with regular module imports.
private const val COMPARE_WITH_BASE = false

// Whether to disable this loader and apply default behavior.
private const val ALWAYS_FALLBACK = false

private const val DENO_MODULE_PREFIX = "deno"
private const val BUN_MODULE_PREFIX = "bun"
private const val NODE_MODULE_PREFIX = "node"
private const val ELIDE_MODULE_PREFIX = "elide"
private const val ELIDE_TS_LANGUAGE_ID = "ts"

// All built-in Elide modules.
private val allElideModules = sortedSetOf(
  "sqlite",
)

// Module prefixes which trigger some kind of behavior.
private val specialModulePrefixes = sortedSetOf(
  DENO_MODULE_PREFIX,
  BUN_MODULE_PREFIX,
  NODE_MODULE_PREFIX,
  ELIDE_MODULE_PREFIX,
)

// All built-in Node modules in a sorted set.
private val allNodeModules = sortedSetOf(
  NodeModuleName.ASSERT,
  NodeModuleName.ASYNC_HOOKS,
  NodeModuleName.BUFFER,
  NodeModuleName.CHILD_PROCESS,
  NodeModuleName.CLUSTER,
  NodeModuleName.CONSOLE,
  NodeModuleName.CONSTANTS,
  NodeModuleName.CRYPTO,
  NodeModuleName.DGRAM,
  NodeModuleName.DNS,
  NodeModuleName.DOMAIN,
  NodeModuleName.EVENTS,
  NodeModuleName.FS,
  NodeModuleName.HTTP,
  NodeModuleName.HTTP2,
  NodeModuleName.HTTPS,
  NodeModuleName.INSPECTOR,
  NodeModuleName.MODULE,
  NodeModuleName.NET,
  NodeModuleName.OS,
  NodeModuleName.PATH,
  NodeModuleName.PERF_HOOKS,
  NodeModuleName.PROCESS,
  NodeModuleName.PUNYCODE,
  NodeModuleName.TRACE_EVENTS,
  NodeModuleName.QUERYSTRING,
  NodeModuleName.READLINE,
  NodeModuleName.REPL,
  NodeModuleName.STREAM,
  NodeModuleName.STRING_DECODER,
  NodeModuleName.TIMERS,
  NodeModuleName.TLS,
  NodeModuleName.TTY,
  NodeModuleName.URL,
  NodeModuleName.UTIL,
  NodeModuleName.V8,
  NodeModuleName.VM,
  NodeModuleName.WORKER_THREADS,
  NodeModuleName.ZLIB,
)

// All built-in Node modules.
@Suppress("VARIABLE_NAME_INCORRECT") public object NodeModuleName {
  public const val ASSERT: String = "assert"
  public const val ASYNC_HOOKS: String = "async_hooks"
  public const val BUFFER: String = "buffer"
  public const val CHILD_PROCESS: String = "child_process"
  public const val CLUSTER: String = "cluster"
  public const val CONSOLE: String = "console"
  public const val CONSTANTS: String = "constants"
  public const val CRYPTO: String = "crypto"
  public const val DGRAM: String = "dgram"
  public const val DNS: String = "dns"
  public const val DOMAIN: String = "domain"
  public const val EVENTS: String = "events"
  public const val FS: String = "fs"
  public const val FS_PROMISES: String = "fs/promises"
  public const val HTTP: String = "http"
  public const val HTTP2: String = "http2"
  public const val HTTPS: String = "https"
  public const val INSPECTOR: String = "inspector"
  public const val MODULE: String = "module"
  public const val NET: String = "net"
  public const val OS: String = "os"
  public const val PATH: String = "path"
  public const val PERF_HOOKS: String = "perf_hooks"
  public const val PROCESS: String = "process"
  public const val PUNYCODE: String = "punycode"
  public const val QUERYSTRING: String = "querystring"
  public const val READLINE: String = "readline"
  public const val REPL: String = "repl"
  public const val STREAM: String = "stream"
  public const val STRING_DECODER: String = "string_decoder"
  public const val TIMERS: String = "timers"
  public const val TLS: String = "tls"
  public const val TRACE_EVENTS: String = "trace_events"
  public const val TTY: String = "tty"
  public const val URL: String = "url"
  public const val UTIL: String = "util"
  public const val V8: String = "v8"
  public const val VM: String = "vm"
  public const val WORKER_THREADS: String = "worker_threads"
  public const val ZLIB: String = "zlib"
}

// Implements Elide's internal ECMA-compliant module loader.
internal class ElideUniversalJsModuleLoader private constructor(realm: JSRealm) : NpmCompatibleESModuleLoader(realm) {
  // Cache of injected modules.
  private val injectedModuleCache: MutableMap<String, AbstractModuleRecord> = ConcurrentSkipListMap()

  // Synthesize an injected module.
  private fun synthesizeInjected(
    referencingModule: ScriptOrModule,
    moduleRequest: ModuleRequest,
    prefix: String?,
    name: String,
  ): AbstractModuleRecord {
    if (name in moduleMap) {
      return requireNotNull(moduleMap[name])
    }
    return when (val info = ModuleInfo.find(name)) {
      // not a registered module; fall back but with un-prefixed name, unless the prefix is `node`
      null -> if (prefix == NODE_MODULE_PREFIX) {
        super.resolveImportedModule(referencingModule, moduleRequest)
      } else {
        // rewrite the module request not to use a prefix unrecognized within graaljs
        ModuleRequest.create(Strings.constant(name)).let {
          super.resolveImportedModule(referencingModule, it)
        }
      }

      // present within module impl registry?
      in ModuleRegistry -> {
        val surface = requireNotNull(ModuleRegistry.load(info)) { "No such synthesized module: $info" }
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
          /* requestedModules = */
          emptyList(),
          /* importEntries = */
          emptyList(),
          /* localExportEntries = */
          localExportEntries,
          /* indirectExportEntries = */
          emptyList(),
          /* starExportEntries = */
          emptyList(),
          /* imports = */
          null,
          /* exports = */
          null,
        )
        val source = Source.newBuilder("js", "", "$name.mjs")
          .internal(true)
          .cached(false)
          .build()

        val rootNode: JavaScriptRootNode = object : JavaScriptRootNode(
          realm.context.language,
          source.createUnavailableSection(),
          frameDescriptor,
        ) {
          @TruffleBoundary
          override fun execute(frame: VirtualFrame): Any {
            val module = JSArguments.getUserArgument(frame.arguments, 0) as JSModuleRecord

            module.environment?.let {
              assert(module.status == CyclicModuleRecord.Status.Evaluating)
              setSyntheticModuleExport(module)
            } ?: run {
              assert(module.status == CyclicModuleRecord.Status.Linking)
              module.environment = frame.materialize()
            }
            return Undefined.instance
          }

          private fun setSyntheticModuleExport(module: JSModuleRecord) {
            module.environment.setObject(defaultExportSlot.index, surface)
            when (surface) {
              is ProxyObject -> {
                for ((slotName, slot) in mappedSlots) {
                  // module.namespace.X
                  val member = surface.getMember(slotName)
                  module.environment.setObject(slot.index, realm.env.asGuestValue(member))
                }
              }

              else -> error("Object is usable synthetic module: $surface")
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
          true,
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
            base.exportedNames.size != modRecord.localExportEntries.size -> error(
              "Mismatch between expected export count ('${base.exportedNames.size}') and " +
                "actual export count ('${modRecord.localExportEntries.size}') for module '$name'",
            )
          }
        }
        ElideSyntheticModuleRecord(
          prefix?.let { ModuleQualifier.resolve(it) } ?: ModuleQualifier.ELIDE,
          data,
          surface,
        )
      }

      // registered but not implemented somehow; warn and fall back
      else -> {
        Logging.root().warn("No such synthesized module: '$name'. Falling back to VFS.")
        super.resolveImportedModule(referencingModule, moduleRequest)
      }
    }
  }

  // Load a module which needs pre-compilation (or some other special load step).
  private fun loadDelegatedSourceModule(
    referrer: ScriptOrModule,
    moduleRequest: ModuleRequest?,
    orElse: () -> AbstractModuleRecord,
  ): AbstractModuleRecord {
    val src = referrer.source
    val req = DelegatedModuleRequest.of(src)
    return when (DelegatedModuleLoaderRegistry.test(req)) {
      // allow the delegated registry to load the module
      true -> DelegatedModuleLoaderRegistry.resolve(req, realm).resolveImportedModule(referrer, moduleRequest).let {
        when (it) {
          // the loader returned `null`, opting out of resolution; continue with defaults
          null -> orElse.invoke()

          // we're good to return from here
          else -> it
        }
      }

      // the delegated registry has indicated there is no matching loader for this
      false -> orElse.invoke()
    }
  }

  // Load a module which needs pre-compilation (or some other special load step).
  private fun resolveDelegatedImportedModule(
    ref: ScriptOrModule,
    req: ModuleRequest,
    name: String,
  ): AbstractModuleRecord {
    val delegatedReq = DelegatedModuleRequest.of(name)
    return when (DelegatedModuleLoaderRegistry.test(delegatedReq)) {
      // allow the delegated registry to load the module
      true -> DelegatedModuleLoaderRegistry.resolve(delegatedReq, realm).resolveImportedModule(ref, req).let {
        when (it) {
          // the loader returned `null`, opting out of resolution; continue with defaults
          null -> super.resolveImportedModule(ref, req)

          // we're good to return from here
          else -> it
        }
      }

      // the delegated registry has indicated there is no matching loader for this
      false -> super.resolveImportedModule(ref, req)
    }
  }

  override fun resolveImportedModule(
    referencingModule: ScriptOrModule,
    moduleRequest: ModuleRequest,
  ): AbstractModuleRecord {
    val requested = moduleRequest.specifier.toString()
    val (prefix, unprefixed) = parsePrefixedMaybe(requested)

    return when (determineModuleStrategy(requested)) {
      FALLBACK -> super.resolveImportedModule(referencingModule, moduleRequest)
      DELEGATED -> resolveDelegatedImportedModule(referencingModule, moduleRequest, unprefixed)
      SYNTHETIC -> injectedModuleCache.computeIfAbsent(unprefixed) {
        synthesizeInjected(referencingModule, moduleRequest, prefix, unprefixed)
      }
    }
  }

  override fun loadModuleFromFile(
    referrer: ScriptOrModule,
    moduleRequest: ModuleRequest?,
    moduleFile: TruffleFile?,
    maybeCanonicalPath: String?,
  ): AbstractModuleRecord? {
    return when (determineModuleStrategy(requireNotNull(referrer.source))) {
      // source modules (aside from meta-languages) are always handled by the base loader
      FALLBACK -> super.loadModuleFromFile(referrer, moduleRequest, moduleFile, maybeCanonicalPath)
      DELEGATED -> loadDelegatedSourceModule(referrer, moduleRequest) {
        super.loadModuleFromFile(referrer, moduleRequest, moduleFile, maybeCanonicalPath)
      }
      else -> error("Mode is not supported for source loading")
    }
  }

  override fun loadModuleFromURL(
    referrer: ScriptOrModule,
    moduleRequest: ModuleRequest?,
    moduleURI: URI?,
  ): AbstractModuleRecord? {
    return when (determineModuleStrategy(requireNotNull(referrer.source))) {
      // source modules (aside from meta-languages) are always handled by the base loader
      FALLBACK -> super.loadModuleFromURL(referrer, moduleRequest, moduleURI)
      DELEGATED -> loadDelegatedSourceModule(referrer, moduleRequest) {
        super.loadModuleFromURL(referrer, moduleRequest, moduleURI)
      }
      else -> error("Mode is not supported for source loading")
    }
  }

  // Strategies for loading modules.
  enum class ModuleStrategy {
    // Delegate to other module loaders (for instance, for TypeScript or other meta-lang support).
    DELEGATED,

    // Fallback to built-in behaviors.
    FALLBACK,

    // Use a synthetic injected module.
    SYNTHETIC,
;
  }

  // Qualifies the type of synthesized module, as applicable.
  enum class ModuleQualifier(override val symbol: String) : Symbolic<String> {
    BUN(BUN_MODULE_PREFIX),
    DENO(DENO_MODULE_PREFIX),
    ELIDE(ELIDE_MODULE_PREFIX),
    NODE(NODE_MODULE_PREFIX),
    ;

    companion object : Symbolic.SealedResolver<String, ModuleQualifier> {
      override fun resolve(symbol: String): ModuleQualifier = when (symbol) {
        DENO.symbol -> DENO
        BUN.symbol -> BUN
        NODE.symbol -> NODE
        ELIDE.symbol -> ELIDE
        else -> throw unresolved("Unknown module qualifier: $symbol")
      }
    }
  }

  // Module record which holds a synthesized module object.
  inner class ElideSyntheticModuleRecord(
    @Suppress("UNUSED") val qualifier: ModuleQualifier,
    data: JSModuleData,
    module: Any
  ) : JSModuleRecord(
    data,
    this,
    module,
  )

  companion object : CommonJSModuleResolver {
    private fun parsePrefixedMaybe(identifier: String): Pair<String?, String> {
      val indexOfSplit = identifier.indexOf(':')
      val prefix = if (indexOfSplit != -1) identifier.substring(0, indexOfSplit) else null
      val unprefixed = prefix?.let { identifier.substring(indexOfSplit + 1, identifier.length) } ?: identifier
      return prefix to unprefixed
    }

    // Parse the module qualifier, if any, and return a `ModuleInfo` if the identifier represents a built-in module.
    private fun toModuleInfo(identifier: String): ModuleInfo? {
      val (_, unprefixed) = parsePrefixedMaybe(identifier)
      return ModuleInfo.find(unprefixed)
    }

    // Check an un-prefixed module name against the built-in modules.
    private fun resolveUnprefixed(name: String): ModuleStrategy = when (name) {
      in allElideModules, in allNodeModules -> SYNTHETIC
      else -> DELEGATED
    }

    // Determine the loading strategy to use for a given module request.
    private fun determineModuleStrategy(source: Source): ModuleStrategy {
      if (ALWAYS_FALLBACK) {
        return FALLBACK
      }
      return when (source.language) {
        // typescript/tsx/etc. is delegated, always
        ELIDE_TS_LANGUAGE_ID -> DELEGATED

        // otherwise, fall back to regular module loader behavior
        else -> FALLBACK
      }
    }

    // Determine the loading strategy to use for a given module request.
    private fun determineModuleStrategy(requested: String): ModuleStrategy {
      if (ALWAYS_FALLBACK) {
        return FALLBACK
      }
      if (requested.contains(File.separatorChar) || requested.contains('.')) {
        return FALLBACK // we are not a filesystem loader
      }

      val (prefix, unprefixed) = parsePrefixedMaybe(requested)
      return when (prefix) {
        // un-prefixed modules may still be built-in (for example, `fs`).
        null -> resolveUnprefixed(unprefixed)

        // special prefixes are always synthetic
        in specialModulePrefixes -> SYNTHETIC

        // otherwise we try delegation
        else -> FALLBACK
      }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun loadStaticDelegatedModule(realm: JSRealm, mod: ModuleInfo): CommonJSModuleProvider<*>? {
      TODO("Delegated module loading in static contexts is not implemented yet")
    }

    // Load a synthetic module in a static context; typically only from CommonJS.
    private fun loadStaticSynthesizedModule(mod: ModuleInfo): CommonJSModuleProvider<*>? = when (mod) {
      in ModuleRegistry -> object : CommonJSModuleProvider<Any> {
        override fun provide(): Any = ModuleRegistry.load(mod)
      }
      else -> null
    }

    // Resolver for CommonJS modules.
    override fun resolve(realm: JSRealm, moduleIdentifier: String): CommonJSModuleProvider<*>? {
      if (ALWAYS_FALLBACK) {
        return null
      } // bail out in always-fallback mode
      val mod = toModuleInfo(moduleIdentifier) ?: return null // bail out if it's not a module we recognize

      return when (determineModuleStrategy(moduleIdentifier)) {
        FALLBACK -> null // bail out if the strategy fn says so
        DELEGATED -> loadStaticDelegatedModule(realm, mod)
        SYNTHETIC -> loadStaticSynthesizedModule(mod)
      }
    }

    /**
     * Create a new ESM loader; this method should be used with care (mostly for testing).
     *
     * @param realm The realm to install the loader to.
     * @return The ES module loader singleton.
     */
    @JvmStatic fun create(realm: JSRealm): ElideUniversalJsModuleLoader = ElideUniversalJsModuleLoader(realm)
  }
}
