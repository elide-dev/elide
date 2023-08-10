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
import java.util.stream.Stream
import elide.annotations.Inject
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.api.GuestRuntime
import elide.runtime.gvm.internals.AbstractVMEngine
import elide.runtime.gvm.internals.GVMInvocationBindings.DispatchStyle
import elide.runtime.gvm.internals.GraalVMGuest.RUBY
import elide.runtime.gvm.internals.VMProperty
import elide.runtime.gvm.internals.VMStaticProperty
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
  }

  @Inject lateinit var rubyConfig: RubyConfig

  override fun resolveConfig(): RubyConfig = rubyConfig

  override fun configure(engine: VMEngine, context: VMBuilder): Stream<out VMProperty> = listOfNotNull(
    VMStaticProperty.active("ruby.embedded"),
    VMStaticProperty.active("ruby.no-home-provided"),
    VMStaticProperty.inactive("ruby.cexts"),
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
