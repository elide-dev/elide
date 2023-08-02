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

package elide.util

import java.util.concurrent.atomic.AtomicReference
import elide.AppEnvironment


/** Static server flags, which may be set via Java system properties or environment variables. */
@Suppress("unused", "MemberVisibilityCanBePrivate", "RedundantVisibilityModifier") public object RuntimeFlag {
  /** Default port to listen on for the VM inspector. */
  public const val DEFAULT_INSPECT_PORT: Int = 4200

  // Server arguments provided by the boot entrypoint.
  private val args: AtomicReference<Array<String>> = AtomicReference(emptyArray())

  /** Operating environment for the application. */
  public val appEnv: AppEnvironment get() = resolve("elide.appEnv", AppEnvironment.LIVE.name) {
      AppEnvironment.valueOf(it.trim().uppercase())
  }

  /** Whether to enable VM inspection. */
  public val inspect: Boolean get() = resolve("elide.vm.inspect", "false") {
    it.trim().toBoolean()
  }

  /** Host where inspection should mount. */
  public val inspectHost: String get() = resolve("elide.vm.inspect.host", "localhost") {
    it.trim().ifBlank {
      "localhost"
    }
  }

  /** Port where inspection should mount. */
  public val inspectPort: Int get() = resolve("elide.vm.inspect.port", "4200") {
    it.trim().toIntOrNull() ?: DEFAULT_INSPECT_PORT
  }

  /** Path where inspection should mount. */
  public val inspectPath: String? get() = resolve("elide.vm.inspect.path", "") {
    it.trim().ifBlank {
      null
    }
  }

  /** Whether to suspend the VM at first execution. */
  public val inspectSuspend: Boolean get() = resolve("elide.vm.inspect.suspend", "false") {
    it.trim().toBoolean()
  }

  /** Whether to show internal sources in the inspector. */
  public val inspectInternal: Boolean get() = resolve("elide.vm.inspect.internal", "false") {
    it.trim().toBoolean()
  }

  /** Whether to wait for the debugger to attach before executing. */
  public val inspectWait: Boolean get() = resolve("elide.vm.inspect.wait", "false") {
    it.trim().toBoolean()
  }

  /** Whether to enable VM inspection secure mode (TLS). */
  public val inspectSecure: Boolean get() = resolve("elide.vm.inspect.secure", "false") {
    it.trim().toBoolean()
  }

  /** Whether to enable VM pre-warming. */
  public val warmup: Boolean get() = resolve("elide.vm.prewarm", "true") {
    it.trim().toBoolean()
  }

  // Resolve an enumerated flag value.
  public fun <R> resolve(name: String, defaultValue: String, then: (String) -> R): R {
    val value = System.getProperty(name, System.getenv(name) ?: resolveArg(name))
    return if (value?.isNotBlank() == true) {
      then.invoke(value)
    } else {
      then.invoke(defaultValue)
    }
  }

  // Resolve the named argument or return `null`.
  private fun resolveArg(name: String): String? {
    val args = args.get()
    for (arg in args) {
      if (arg.startsWith("--$name=")) {
        return arg.substring("--$name=".length)
      }
    }
    return null
  }

  /** Install server flag value state. */
  @JvmStatic public fun setArgs(args: Array<String>) {
    this.args.set(
      args
    )
  }
}
