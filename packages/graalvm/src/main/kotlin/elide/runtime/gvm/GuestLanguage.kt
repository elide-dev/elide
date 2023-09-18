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

package elide.runtime.gvm

import java.util.*
import elide.runtime.gvm.internals.GraalVMGuest

/**
 * TBD.
 */
public interface GuestLanguage {
  /**
   * TBD.
   */
  public val symbol: String

  /**
   * TBD.
   */
  public val engine: String

  /**
   * TBD.
   */
  public val label: String

  /**
   * TBD.
   */
  public val supportsSSR: Boolean get() = false

  /**
   * TBD.
   */
  public val supportsStreamingSSR: Boolean get() = invocationModes.contains(InvocationMode.STREAMING)

  /**
   * TBD.
   */
  public val invocationModes: EnumSet<InvocationMode> get() = EnumSet.noneOf(InvocationMode::class.java)

  /** Well-known guest languages. */
  public companion object {
    /** JavaScript as a guest language. */
    public val JAVASCRIPT: GuestLanguage = GraalVMGuest.JAVASCRIPT

    /** Python as a guest language. */
    public val PYTHON: GuestLanguage = GraalVMGuest.PYTHON

    /** Ruby as a guest language. */
    public val RUBY: GuestLanguage = GraalVMGuest.RUBY

    /** Java Virtual Machine (JVM) as a guest language. */
    public val JVM: GuestLanguage = GraalVMGuest.JVM

    /** WebAssembly (WASM) as a guest language. */
    public val WASM: GuestLanguage = GraalVMGuest.WASM
  }
}
