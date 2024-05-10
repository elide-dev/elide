/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
@Suppress("unused", "MemberVisibilityCanBePrivate") public object RuntimeFlag {
  /** Default port to listen on for the VM inspector. */
  public const val DEFAULT_INSPECT_PORT: Int = 4200

  // Server arguments provided by the boot entrypoint.
  private val args: AtomicReference<Array<String>> = AtomicReference(emptyArray())

  /** Whether to enable VM pre-warming. */
  public val warmup: Boolean get() = resolve("elide.vm.prewarm", "true") {
    it.trim().toBoolean()
  }

  // Resolve an enumerated flag value.
  public fun <R> resolve(name: String, defaultValue: String, then: (String) -> R): R {
    val value = System.getProperty(name, System.getenv(name))
    return if (value?.isNotBlank() == true) {
      then.invoke(value)
    } else {
      then.invoke(defaultValue)
    }
  }
}
