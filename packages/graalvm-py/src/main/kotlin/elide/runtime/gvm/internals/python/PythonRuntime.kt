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
import java.util.stream.Stream
import elide.annotations.Inject
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.api.GuestRuntime
import elide.runtime.gvm.internals.AbstractVMEngine
import elide.runtime.gvm.internals.GVMInvocationBindings.DispatchStyle
import elide.runtime.gvm.internals.GraalVMGuest.PYTHON
import elide.runtime.gvm.internals.VMProperty
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
  }

  @Inject lateinit var pyConfig: PythonConfig

  override fun resolveConfig(): PythonConfig = pyConfig

  override fun configure(engine: VMEngine, context: VMBuilder): Stream<VMProperty> {
    return Stream.empty()
  }

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
