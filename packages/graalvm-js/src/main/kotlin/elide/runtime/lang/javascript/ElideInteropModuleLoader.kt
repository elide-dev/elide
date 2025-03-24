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

import com.oracle.graal.python.PythonLanguage
import com.oracle.graal.python.pegparser.FutureFeature
import com.oracle.graal.python.pegparser.InputType
import com.oracle.graal.python.runtime.PythonContext
import com.oracle.js.parser.ir.Module
import com.oracle.js.parser.ir.Module.ExportEntry
import com.oracle.truffle.api.CallTarget
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.TruffleFile
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.source.Source
import com.oracle.truffle.js.lang.JavaScriptLanguage
import com.oracle.truffle.js.nodes.JSFrameDescriptor
import com.oracle.truffle.js.runtime.JSArguments
import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.JavaScriptRootNode
import com.oracle.truffle.js.runtime.Strings
import com.oracle.truffle.js.runtime.builtins.JSFunctionData
import com.oracle.truffle.js.runtime.objects.CyclicModuleRecord
import com.oracle.truffle.js.runtime.objects.JSModuleData
import com.oracle.truffle.js.runtime.objects.JSModuleLoader
import com.oracle.truffle.js.runtime.objects.JSModuleRecord
import com.oracle.truffle.js.runtime.objects.ScriptOrModule
import com.oracle.truffle.js.runtime.objects.Undefined
import org.graalvm.polyglot.Engine
import java.util.EnumSet
import java.util.TreeSet
import java.util.concurrent.ConcurrentSkipListMap
import kotlinx.atomicfu.atomic

// Constants for use by the interop loader.
private const val LANGUAGE_ID_PYTHON = "python"
private const val PYTHON_FILE_EXTENSION = "py"
private const val PYTHON_BYTECODE_FILE_EXTENSION = "pyc"

/**
 * ## Interop Module Loader
 *
 * This loader acts as a delegate loader in cooperation with [ElideUniversalJsModuleLoader] in order to facilitate
 * cross-language import sugar; such imports effectively result in a call to evaluate the target file (after resolution)
 * and then return a synthesized object built from polyglot binding accessors.
 *
 * Foreign code is expected to be made available in the polyglot bindings via its own mechanisms. For example, Python
 * guests have access to the `elide` built-in module, which offers the `bind` and `poly` decorators. These decorators
 * expose Python symbols or values within the polyglot context, making them eligible for loading via imports (and for
 * destructuring).
 */
internal object ElideInteropModuleLoader : DelegatedModuleLoaderRegistry.DelegateFactory, JSModuleLoader {
  private val supportedExtensions = ConcurrentSkipListMap<String, TreeSet<String>>()
  private val supportedMimes = ConcurrentSkipListMap<String, String>()

  /**
   * ### Foreign Module Facade
   *
   * Represents an aggregate type of [JSModuleRecord] for foreign (non-JavaScript) module imports; all foreign modules
   * ultimately extend this class, which is sealed for easy behavioral type checking.
   */
  sealed class ElideForeignModuleFacade(
    moduleData: JSModuleData,
    moduleLoader: JSModuleLoader,
    hostDefined: Any? = null,
  ) : JSModuleRecord(moduleData, moduleLoader, hostDefined) {
    /** Abstract language ID for this module. */
    internal abstract val lang: String
  }

  /**
   * ### Foreign Module Facade: Python
   *
   * Implements a [ElideForeignModuleFacade] for Python sources imported from JavaScript.
   */
  class PythonForeignModuleFacade(
    moduleData: JSModuleData,
    moduleLoader: JSModuleLoader,
    hostDefined: Any? = null,
  ) : ElideForeignModuleFacade(moduleData, moduleLoader, hostDefined) {
    override val lang: String get() = LANGUAGE_ID_PYTHON
  }

  init {
    val supportedLangs = Engine.newBuilder().build().languages

    if (LANGUAGE_ID_PYTHON in supportedLangs.keys) {
      supportedExtensions[LANGUAGE_ID_PYTHON] = sortedSetOf(
        PYTHON_FILE_EXTENSION,
        PYTHON_BYTECODE_FILE_EXTENSION,
      )
      supportedMimes["text/x-python"] = LANGUAGE_ID_PYTHON
      supportedMimes["application/x-python"] = LANGUAGE_ID_PYTHON
    }
  }


  // Determine whether the file is foreign based on the filename.
  private fun supportedFilename(name: String): Boolean {
    val ext = name.substringAfterLast('.')
    return supportedExtensions.values.any { it.contains(ext) }
  }

  // Resolve a foreign lang by source lang ID.
  private fun resolveLang(source: Source): String? {
    return when (source.mimeType) {
      null -> source.name?.let { resolveLang(it) }
      else -> supportedMimes[source.mimeType]
    }
  }

  // Resolve a foreign lang by filename.
  private fun resolveLang(name: String): String? {
    val ext = name.substringAfterLast('.')
    return supportedExtensions.entries.firstOrNull { it.value.contains(ext) }?.key
  }

  // Determine whether the file is foreign based on parsed sources.
  private fun isForeignInteropSource(source: Source): Boolean {
    return when (source.mimeType) {
      null -> source.name?.let { supportedFilename(it) } == true
      else -> source.mimeType in supportedMimes
    }
  }

  private fun spawnInteropModule(
    referencingModule: ScriptOrModule,
    moduleRequest: Module.ModuleRequest,
    file: TruffleFile,
    realm: JSRealm,
  ): ElideForeignModuleFacade {
    val targetLang = resolveLang(file.name) ?: error("Could not resolve language for file: '${file.name}'")
    val defaultExportName = Strings.DEFAULT
    val defaultExport = ExportEntry.exportSpecifier(defaultExportName)
    val frameDescBuilder = JSFrameDescriptor(Undefined.instance)
    val defaultExportSlot = frameDescBuilder.addFrameSlot(defaultExportName)
    val frameDescriptor = frameDescBuilder.toFrameDescriptor()
    val localExportEntries = buildList {
      add(defaultExport)
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
    val source = Source.newBuilder(targetLang, "", file.name)
      .internal(true)
      .cached(false)
      .build()

    val facade = atomic<ElideForeignModuleFacade?>(null)
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
          setInteropModuleDefaultExport(frame, module)
        } ?: run {
          assert(module.status == CyclicModuleRecord.Status.Linking)
          module.environment = frame.materialize()
        }
        return Undefined.instance
      }

      private fun setInteropModuleDefaultExport(frame: VirtualFrame, module: JSModuleRecord) {
        val surface = facade.value ?: error("Foreign module facade not initialized")
        val currentEnv = realm.env
        val langInfo = currentEnv.internalLanguages[surface.lang]
          ?: error("Language not initialized: '${surface.lang}'")

        // @TODO don't hard-code python here
        // make sure the language has initialized in the current context
        currentEnv.initializeLanguage(langInfo)
        val py = PythonLanguage.get(null)
        val pyCtx = PythonContext(py, currentEnv)
        val src = Source.newBuilder(surface.lang, file)
          .cached(true)
          .build()

        val rootCallTarget = py.parse(
          pyCtx,
          src,
          InputType.FILE,
          true,
          1,
          false,
          emptyList<String>(),
          EnumSet.noneOf(FutureFeature::class.java),
        )

        val exec = rootCallTarget.rootNode.execute(frame)
        module.environment.setObject(defaultExportSlot.index, realm.env.asGuestValue(exec))
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
    when (targetLang) {
      LANGUAGE_ID_PYTHON -> facade.value = PythonForeignModuleFacade(
        data,
        this,
      )

      else -> error("Unsupported language for foreign imports: '$targetLang'")
    }
    return facade.value!!
  }

  override fun test(t: DelegatedModuleLoaderRegistry.DelegatedModuleRequest): Boolean {
    return when (val source = t.source) {
      // if we have no source to work with, hint based on the filename
      null -> supportedFilename(t.label)

      // prefer sources
      else -> isForeignInteropSource(source)
    }
  }

  override fun invoke(realm: JSRealm): JSModuleLoader = this

  override fun resolveImportedModule(
    referencingModule: ScriptOrModule,
    moduleRequest: Module.ModuleRequest,
  ): JSModuleRecord {
    val basePath = referencingModule.source?.path
    val baseUri = referencingModule.source?.uri
    val env = JavaScriptLanguage.getCurrentEnv()
    val realm = JavaScriptLanguage.getCurrentJSRealm()
    val spec = moduleRequest.specifier.toString()

    val parentFile = when {
      basePath != null -> env.getPublicTruffleFile(basePath).parent

      baseUri != null -> if (baseUri.scheme == "truffle") null else {
        env.getPublicTruffleFile(baseUri).parent
      }

      // fall-back to the default cwd root if we don't have a proper parent to resolve from
      else -> null
    } ?: env.getPublicTruffleFile(realm.contextOptions.requireCwd)

    // resolve a file path for the module if possible
    val resolvedFile = parentFile.resolve(spec)
    if (resolvedFile != null && resolvedFile.exists()) {
      // spawn a resolved module record which can be evaluated eventually
      return spawnInteropModule(referencingModule, moduleRequest, resolvedFile, realm)
    }

    error("Cannot import: '$spec'")
  }
}
