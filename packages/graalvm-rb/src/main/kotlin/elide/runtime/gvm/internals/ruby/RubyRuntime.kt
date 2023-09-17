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

package elide.runtime.gvm.internals.ruby

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
import elide.runtime.gvm.internals.GraalVMGuest.RUBY
import elide.runtime.gvm.internals.ruby.RubyExecutableScript as RubyScript
import elide.runtime.gvm.internals.ruby.RubyInvocationBindings as RubyBindings
import elide.runtime.gvm.ruby.cfg.RubyRuntimeConfig as RubyConfig
import org.graalvm.polyglot.Engine as VMEngine
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Context.Builder as VMBuilder
import org.graalvm.polyglot.Value as GuestValue

/**
 * TBD.
 */
@Requires(property = "elide.gvm.enabled", value = "true", defaultValue = "true")
@Requires(property = "elide.gvm.ruby.enabled", value = "true", defaultValue = "true")
@GuestRuntime(engine = RubyRuntime.ENGINE_RUBY)
internal class RubyRuntime : AbstractVMEngine<RubyConfig, RubyScript, RubyBindings>(RUBY) {
  internal companion object {
    const val ENGINE_RUBY: String = "ruby"
    private const val RUNTIME_PREINIT: String = "__runtime__.rb"
    private const val RUBY_MIMETYPE: String = "application/x-ruby"

    // Whether runtime assets have loaded.
    private val runtimeReady: AtomicBoolean = AtomicBoolean(false)

    // Info about the runtime, loaded from the runtime bundle manifest.
    internal val runtimeInfo: AtomicReference<RuntimeInfo> = AtomicReference(null)

    // Assembled runtime init code, loaded from `runtimeInfo`.
    private val runtimeInit: AtomicReference<Source> = AtomicReference(null)

    init {
      check(!runtimeReady.get()) {
        "Runtime cannot be prepared more than once (Ruby runtime must operate as a singleton)"
      }

      resolveRuntimeInfo(RUBY.symbol, RUNTIME_PREINIT, RUBY_MIMETYPE).let { (info, facade) ->
        runtimeInfo.set(info)
        runtimeInit.set(facade)
        runtimeReady.set(true)
      }
    }
  }

  /** Configurator: VFS. Injects JavaScript runtime assets as a VFS component. */
  @Singleton @Context class RubyRuntimeVFSConfigurator : GuestVFSConfigurator(
    RUBY,
    { runtimeInfo.get() }
  )

  @Inject lateinit var rubyConfig: RubyConfig

  override fun resolveConfig(): RubyConfig = rubyConfig

  override fun configure(engine: VMEngine, context: VMBuilder): Stream<out VMProperty> = listOfNotNull(
    VMStaticProperty.active("ruby.embedded"),
    VMStaticProperty.active("ruby.no-home-provided"),
    VMStaticProperty.active("ruby.platform-native-interrupt"),
    VMStaticProperty.active("ruby.platform-native"),
    VMStaticProperty.active("ruby.polyglot-stdio"),
    VMStaticProperty.active("ruby.rubygems"),
    VMStaticProperty.active("ruby.lazy-default"),
    VMStaticProperty.active("ruby.lazy-builtins"),
    VMStaticProperty.active("ruby.lazy-calltargets"),
    VMStaticProperty.active("ruby.lazy-rubygems"),
    VMStaticProperty.active("ruby.lazy-translation-core"),
    VMStaticProperty.active("ruby.lazy-translation-user"),
    VMStaticProperty.active("ruby.shared-objects"),
    VMStaticProperty.active("ruby.experimental-engine-caching"),
    VMStaticProperty.inactive("ruby.virtual-thread-fibers"),
    VMStaticProperty.inactive("ruby.cexts"),
    VMStaticProperty.of("log.level", "OFF"),
  ).stream()

  override fun prepare(context: VMContext, globals: GuestValue) {
    // nothing at this time
  }

  override fun resolve(
    context: VMContext,
    script: RubyScript,
    mode: DispatchStyle?
  ): RubyBindings {
    TODO("Not yet implemented")
  }

  override fun <Inputs : ExecutionInputs> execute(
    context: VMContext,
    script: RubyScript,
    bindings: RubyBindings,
    inputs: Inputs
  ): GuestValue {
    TODO("Not yet implemented")
  }
}
