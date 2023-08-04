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

package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable

/**
 * # Guest VM: Chrome Inspector
 *
 * Configures the Chrome Inspector to run for a given guest VM, to enable debugging / inspection features during guest
 * code execution. To use the inspector, activate these features (either via configuration or command line flags), and
 * then connect to the guest VM via the Chrome DevTools interface.
 *
 * Note that the inspector will only show the VM once it has booted and initialized; based on configured server warmup
 * settings, this may be at server boot, or upon the first request which invokes the guest VM.
 *
 * Also note that when the inspector is enabled, only one guest VM instance will be booted at a time. This ensures that
 * all guest VM execution is visible to the debugger.
 *
 * ## Inspector: Suspension
 *
 * To force the inspector to pause execution at the initial invocation, [suspend] and [wait] can be set to `true`; in
 * this case, un-suspension occurs at the moment the debugger is attached after the guest VM begins execution; it will
 * not continue processing until the debugger is attached.
 *
 * Effectively, this is the same as setting a breakpoint at the first line of guest code. Internals are hidden from view
 * to the debugger unless otherwise specified, in order to simplify user code debugging.
 *
 * You can also suspend the VM in any number of ways:
 *
 * - **Breakpoints.** You can set a breakpoint via the Chrome Inspector _Source_ panel. When encountered, the VM will
 *   suspend and the debugger will attach to the breakpoint line.
 *
 * - **Debugger statements.** Literal debug triggers in guest languages will also suspend the VM; for example, the
 *   `debugger` statement in JavaScript, or `import pdb; pdb.set_trace()` in Python.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.inspect")
internal interface GuestVMInspectConfig : Toggleable {
  companion object {
    /** Default controlling whether the inspector is enabled by default (`false`). */
    private const val DEFAULT_ENABLED: Boolean = false

    /** Default controlling whether the inspector suspends execution (`false`). */
    private const val DEFAULT_SUSPEND: Boolean = false

    /** Default controlling whether the VM waits for the debugger to attach to begin execution (`false`). */
    private const val DEFAULT_WAIT: Boolean = false

    /** Default controlling whether the inspector binds with TLS (`false`). */
    private const val DEFAULT_SECURE: Boolean = false

    /** Default path value to pass (`null`, i.e. generate randomly). */
    private val DEFAULT_PATH: String? = null
  }

  override fun isEnabled(): Boolean = DEFAULT_ENABLED

  /**
   * @return Whether to suspend the guest VM at initial invocation. Defaults to `false`.
   */
  val suspend: Boolean get() = DEFAULT_SUSPEND

  /**
   * @return Whether the VM should wait until the debugger attaches before beginning execution. Defaults to `false`.
   */
  val wait: Boolean get() = DEFAULT_WAIT

  /**
   * @return Whether to configure TLS for the inspector socket. Defaults to `false`, requires configuration.
   */
  val secure: Boolean get() = DEFAULT_SECURE

  /**
   * @return From GraalVM: Path to the Chrome Inspector to bind to. This path should be unpredictable. Do note that any
   *  website opened in your browser that has knowledge of the URL can connect to the debugger. A predictable path can
   *  thus be abused by a malicious website to execute arbitrary code on your computer, even if you are behind a
   *  firewall (default: randomly generated).
   */
  val path: String? get() = DEFAULT_PATH
}
