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

import elide.runtime.gvm.InvocationBindings
import elide.runtime.gvm.internals.GVMInvocationBindings
import java.util.*
import org.graalvm.polyglot.Value as GuestValue

/**
 * # Python: Invocation Bindings
 *
 * Implementation of [InvocationBindings] for the Python runtime provided by Elide, and powered by GraalVM; this class
 * is charged with resolving a set of invocation bindings from an input script and evaluated [GuestValue] pair. This
 * involves interrogating the result of executing the user's guest code entrypoint, matching to an invocation style, and
 * then wiring together appropriate bindings to dispatch those symbols.
 *
 * &nbsp;
 *
 * ## Supported dispatch styles
 *
 * Python guest code can be invoked in a number of different ways, depending on the shape of the exported bindings and
 * the intended purpose of the guest application. The simplest option is to run as if `__main__` was invoked, and to
 * print content back to the output buffer.
 *
 * Main invocation is only recommended for very small scripts because: (1) it generally doesn't scale well, (2) it's
 * impossible to accept structured inputs without serialization, and (3) it complicates debugging, maintainability, and
 * performance.
 *
 * There is also a "server" dispatch style, which invokes an entrypoint dedicated to processing HTTP requests. If the
 * user is using Elide's provided Flask-style API, this entrypoint is configured and used automatically.
 *
 * An entrypoint function can be "exported" to the Elide runtime by annotating it with <TBD>.
 *
 * **Exhaustive list of dispatch styles supported by the Python runtime:**
 * TBD.
 *
 * ### Unary binding example
 *
 * TBD.
 *
 * ### Server binding example
 *
 * TBD.
 *
 * ## Render binding style
 *
 * TBD.
 *
 * ## Safety of bindings
 *
 * [PythonInvocationBindings] are tied to the VM context which they originate from. This means that execution of
 * bindings, and indeed even interrogating binding values, must be confined to the same native thread as the VM context.
 * For this reason, it is expected that only the VM implementation will touch internal binding state.
 *
 * @param modes Supported dispatch modes for this invocation bindings instance.
 * @param mapped Resolved values for each binding.
 * @param types Entrypoint types expressed in [mapped].
 */
internal sealed class PythonInvocationBindings (
  private val mapped: Map<EntrypointInfo, PythonEntrypoint>,
  private val modes: EnumSet<DispatchStyle>,
  private val types: EnumSet<PythonEntrypointType>,
) : InvocationBindings, GVMInvocationBindings<PythonInvocationBindings, PythonExecutableScript>() {
  /** Enumerates types of resolved Python entrypoints; a [PythonInvocationBindings] subclass exists for each. */
  internal enum class PythonEntrypointType {
    /** Indicates a "default" entrypoint of `__main__`. */
    MAIN,

    /** Indicates a server-capable interface, which exports a `fetch` function (async). */
    SERVER,

    /** Indicates an SSR-capable interface, which exports a `render` function (async). */
    RENDER,

    /** Special type of entrypoint which indicates support for multiple [PythonEntrypointType]s. */
    COMPOUND,
  }

  /**
   * ## Entrypoint info.
   *
   * Used as a key in a mapping of Python entrypoints to their resolved [GuestValue] instances.
   *
   * @param type Python entrypoint type specified by this info key.
   * @param name Name of the function, or entrypoint, etc. Defaults to `null` (anonymous).
   */
  internal data class EntrypointInfo(
    internal val type: PythonEntrypointType,
    internal val name: String? = null,
  )

  /**
   * ## Entrypoint spec.
   *
   * Describes a Python entrypoint, including the basic (serializable) [EntrypointInfo], and any additional context
   * needed to dispatch the entrypoint.
   *
   * @param info Basic serializable and comparable info about the entrypoint.
   * @param path Full path to the entrypoint.
   * @param value Guest value resolved for, and corresponding to, this entrypoint. Should be executable.
   */
  internal class PythonEntrypoint(
    internal val info: EntrypointInfo,
    private val path: List<String>,
    internal val value: GuestValue,
  )
}
