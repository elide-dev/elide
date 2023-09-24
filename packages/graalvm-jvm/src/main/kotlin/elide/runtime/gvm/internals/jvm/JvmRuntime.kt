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

package elide.runtime.gvm.internals.jvm

import io.micronaut.context.annotation.Requires
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Context.Builder
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream
import elide.annotations.Context
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.api.GuestRuntime
import elide.runtime.gvm.internals.AbstractVMEngine
import elide.runtime.gvm.internals.GVMInvocationBindings.DispatchStyle
import elide.runtime.gvm.internals.GraalVMGuest.JVM
import elide.runtime.gvm.internals.VMProperty
import elide.runtime.gvm.internals.VMStaticProperty
import elide.runtime.gvm.jvm.cfg.JvmRuntimeConfig

/**
 * TBD.
 */
@Requires(property = "elide.gvm.enabled", value = "true", defaultValue = "true")
@Requires(property = "elide.gvm.jvm.enabled", value = "true", defaultValue = "true")
@GuestRuntime(engine = JvmRuntime.ENGINE_JVM)
internal class JvmRuntime : AbstractVMEngine<JvmRuntimeConfig, JvmExecutableScript, JvmInvocationBindings>(JVM) {
  internal companion object {
    const val ENGINE_JVM: String = "jvm"
    private const val RUNTIME_PREINIT: String = "__runtime__.kt"
    private const val JVM_MIMETYPE: String = "text/x-java"

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

      resolveRuntimeInfo(ENGINE_JVM, RUNTIME_PREINIT, JVM_MIMETYPE).let { (info, facade) ->
        runtimeInfo.set(info)
        runtimeInit.set(facade)
        runtimeReady.set(true)
      }
    }
  }

  /** Configurator: VFS. Injects JavaScript runtime assets as a VFS component. */
  @Singleton @Context class PythonRuntimeVFSConfigurator : GuestVFSConfigurator(
    JVM,
    { runtimeInfo.get() }
  )

  /** Python-specific engine configuration. */
  @Inject lateinit var jvmConfig: JvmRuntimeConfig

  override fun resolveConfig(): JvmRuntimeConfig = jvmConfig

  override fun configure(engine: Engine, context: Builder): Stream<out VMProperty> = listOfNotNull(
    VMStaticProperty.active("java.EnablePreview"),
    VMStaticProperty.active("java.BuiltInPolyglotCollections"),
    VMStaticProperty.active("java.BytecodeLevelInlining"),
    VMStaticProperty.active("java.CHA"),
    VMStaticProperty.active("java.HotSwapAPI"),
    VMStaticProperty.active("java.InlineMethodHandle"),
    VMStaticProperty.active("java.MultiThreaded"),
    VMStaticProperty.active("java.Polyglot"),
    VMStaticProperty.active("java.SoftExit"),
    VMStaticProperty.active("java.SplitMethodHandles"),
    VMStaticProperty.inactive("java.EnableAgents"),
    VMStaticProperty.inactive("java.EnableManagement"),
    VMStaticProperty.inactive("java.ExposeNativeJavaVM"),
//    VMStaticProperty.of("java.JImageMode", "native"),
  ).stream()

  override fun prepare(context: VMContext, globals: Value) {
    // nothing at this time
  }

  override fun resolve(
    context: VMContext,
    script: JvmExecutableScript,
    mode: DispatchStyle?
  ): JvmInvocationBindings {
    TODO("Not yet implemented")
  }

  override fun <Inputs : ExecutionInputs> execute(
    context: VMContext,
    script: JvmExecutableScript,
    bindings: JvmInvocationBindings,
    inputs: Inputs
  ): Value {
    TODO("Not yet implemented")
  }
}
