/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

import java.util.EnumSet

/**
 * ## Guest Language
 *
 * Describes a guest language supported by Elide. Various implementations exist for this API, most notably GraalVM's
 * languages via Truffle.
 */
public interface GuestLanguage {
  /**
   * String symbol to use when referring to this language in the API.
   */
  public val symbol: String

  /**
   * Engine which this language is based on; for example, Kotlin and Scala are "java" engines, and TypeScript is a
   * "js" engine.
   */
  public val engine: String

  /**
   * Label to show in UI circumstances for this language.
   */
  public val label: String

  /**
   * Whether this language supports server-side rendering (SSR) or not.
   */
  public val supportsSSR: Boolean get() = false

  /**
   * Whether this language supports streaming-style SSR.
   */
  public val supportsStreamingSSR: Boolean get() = invocationModes.contains(InvocationMode.STREAMING)

  /**
   * Invocation modes supported by this language.
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
