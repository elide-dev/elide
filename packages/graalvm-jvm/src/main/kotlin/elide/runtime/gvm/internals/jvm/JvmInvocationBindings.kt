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

import org.graalvm.polyglot.Value
import java.util.*
import elide.runtime.gvm.InvocationBindings
import elide.runtime.gvm.internals.GVMInvocationBindings
import elide.runtime.gvm.internals.GVMInvocationBindings.DispatchStyle

/**
 * TBD.
 */
internal class JvmInvocationBindings (
  private val mapped: Map<EntrypointInfo, JvmEntrypoint>,
  private val modes: EnumSet<DispatchStyle>,
  private val types: EnumSet<JvmEntrypointType>,
) : InvocationBindings, GVMInvocationBindings<JvmInvocationBindings, JvmExecutableScript>() {
  /** Enumerates types of resolved JVM entrypoints; a [JvmInvocationBindings] subclass exists for each. */
  internal enum class JvmEntrypointType {
    /** Indicates a "default" entrypoint of `main`. */
    MAIN,

    /** Indicates a server-capable interface, which exports a `fetch` function (async). */
    SERVER,

    /** Indicates an SSR-capable interface, which exports a `render` function (async). */
    RENDER,

    /** Special type of entrypoint which indicates support for multiple [JvmEntrypointType]s. */
    COMPOUND,
  }

  /**
   * ## Entrypoint info.
   *
   * Used as a key in a mapping of JVM entrypoints to their resolved [GuestValue] instances.
   *
   * @param type JVM entrypoint type specified by this info key.
   * @param name Name of the function, or entrypoint, etc. Defaults to `null` (anonymous).
   */
  internal data class EntrypointInfo(
    internal val type: JvmEntrypointType,
    internal val name: String? = null,
  )

  /**
   * ## Entrypoint spec.
   *
   * Describes a JVM entrypoint, including the basic (serializable) [EntrypointInfo], and any additional context
   * needed to dispatch the entrypoint.
   *
   * @param info Basic serializable and comparable info about the entrypoint.
   * @param path Full path to the entrypoint.
   * @param value Guest value resolved for, and corresponding to, this entrypoint. Should be executable.
   */
  internal class JvmEntrypoint(
    internal val info: EntrypointInfo,
    private val path: List<String>,
    internal val value: Value,
  )
}
