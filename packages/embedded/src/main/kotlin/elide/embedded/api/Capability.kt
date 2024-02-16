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

package elide.embedded.api

import elide.core.api.Symbolic
import elide.embedded.NativeApi.Capability.ELIDE_BASELINE
import elide.embedded.NativeApi.Capability as NativeCapability

/**
 * # Embedded Capability
 *
 * Native enum which maps "embedded capability" instances to their underlying C and JVM values; Embedded Capabilities
 * are used to mediate API features and access between native host systems and Elide.
 */
public enum class Capability (override val symbol: NativeCapability): Symbolic<NativeCapability> {
  /**
   * ## Capability: Baseline
   *
   * Encloses all basic expected API access, including native initialization with capabilities, and basic entrypoint
   * support for fetch, scheduled execution, and queued execution.
   */
  BASELINE(ELIDE_BASELINE);

  /** Companion resolution functions for native capabilities. */
  public companion object : Symbolic.SealedResolver<NativeCapability, Capability> {
    @JvmStatic override fun resolve(symbol: NativeCapability): Capability = when (symbol) {
      ELIDE_BASELINE -> BASELINE
    }
  }
}
