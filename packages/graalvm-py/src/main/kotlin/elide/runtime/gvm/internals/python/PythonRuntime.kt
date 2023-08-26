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

package elide.runtime.gvm.internals.python

import io.micronaut.context.annotation.Requires
import org.graalvm.polyglot.Source
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream
import elide.annotations.Context
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.api.GuestRuntime
import elide.runtime.gvm.internals.*
import elide.runtime.gvm.internals.GVMInvocationBindings.DispatchStyle
import elide.runtime.gvm.internals.GraalVMGuest.PYTHON
import elide.runtime.gvm.internals.VMStaticProperty.Companion
import elide.runtime.gvm.internals.python.PythonExecutableScript as PythonScript
import elide.runtime.gvm.internals.python.PythonInvocationBindings as PythonBindings
import elide.runtime.gvm.python.cfg.PythonRuntimeConfig as PythonConfig
import org.graalvm.polyglot.Engine as VMEngine
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Context.Builder as VMBuilder
import org.graalvm.polyglot.Value as GuestValue

/**
 * TBD.
 */
@Requires(property = "elide.gvm.enabled", value = "true", defaultValue = "true")
@Requires(property = "elide.gvm.python.enabled", value = "true", defaultValue = "true")
@GuestRuntime(engine = PythonRuntime.ENGINE_PYTHON)
internal class PythonRuntime : AbstractVMEngine<PythonConfig, PythonScript, PythonBindings>(PYTHON) {
  internal companion object {
    const val ENGINE_PYTHON: String = "python"
    private const val RUNTIME_PREINIT: String = "__runtime__.py"
    private const val PYTHON_MIMETYPE: String = "text/x-python"

    // Whether runtime assets have loaded.
    private val runtimeReady: AtomicBoolean = AtomicBoolean(false)

    // Info about the runtime, loaded from the runtime bundle manifest.
    internal val runtimeInfo: AtomicReference<RuntimeInfo> = AtomicReference(null)

    // Assembled runtime init code, loaded from `runtimeInfo`.
    private val runtimeInit: AtomicReference<Source> = AtomicReference(null)

    init {
      check(!runtimeReady.get()) {
        "Runtime cannot be prepared more than once (Python runtime must operate as a singleton)"
      }

      resolveRuntimeInfo(ENGINE_PYTHON, RUNTIME_PREINIT, PYTHON_MIMETYPE).let { (info, facade) ->
        runtimeInfo.set(info)
        runtimeInit.set(facade)
        runtimeReady.set(true)
      }
    }
  }

  /** Configurator: VFS. Injects JavaScript runtime assets as a VFS component. */
  @Singleton @Context class PythonRuntimeVFSConfigurator : GuestVFSConfigurator(
    PYTHON,
    { runtimeInfo.get() }
  )

  /** Python-specific engine configuration. */
  @Inject lateinit var pyConfig: PythonConfig

  override fun resolveConfig(): PythonConfig = pyConfig

  override fun configure(engine: VMEngine, context: VMBuilder): Stream<out VMProperty> = listOfNotNull(
    VMStaticProperty.of("llvm.OSR", "BYTECODE"),
    VMStaticProperty.of("python.PosixModuleBackend", "java"),
    VMStaticProperty.active("llvm.AOTCacheStore"),
    VMStaticProperty.active("llvm.AOTCacheLoad"),
    VMStaticProperty.active("llvm.C++Interop"),
    VMStaticProperty.active("llvm.lazyParsing"),
    VMStaticProperty.active("python.NativeModules"),
    VMStaticProperty.active("python.LazyStrings"),
    VMStaticProperty.active("python.WithCachedSources"),
    VMStaticProperty.active("python.WithTRegex"),
    VMStaticProperty.inactive("python.UsePanama"),
    VMStaticProperty.inactive("python.EmulateJython"),
    VMStaticProperty.of("python.CoreHome", "/python/lib/graalpy23.1"),
    VMStaticProperty.of("python.PythonHome", "/python"),
  ).stream()

  override fun prepare(context: VMContext, globals: GuestValue) {
    // nothing at this time
  }

  override fun resolve(
    context: VMContext,
    script: PythonScript,
    mode: DispatchStyle?
  ): PythonBindings {
    TODO("Not yet implemented")
  }

  override fun <Inputs : ExecutionInputs> execute(
    context: VMContext,
    script: PythonScript,
    bindings: PythonBindings,
    inputs: Inputs
  ): GuestValue {
    TODO("Not yet implemented")
  }
}
