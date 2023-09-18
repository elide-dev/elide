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

package elide.runtime.gvm.internals

import java.util.*
import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.InvocationMode

/** Enumerates known/supported GraalVM guest languages. */
public enum class GraalVMGuest (
  override val symbol: String,
  override val engine: String = symbol,
  override val label: String,
) : GuestLanguage {
  /**
   * ECMA2022-compliant JavaScript via Graal JS+JVM.
   */
  JAVASCRIPT(symbol = "js", label = "JavaScript") {
    override val supportsSSR: Boolean get() = true
    override val supportsStreamingSSR: Boolean get() = true
    override val invocationModes: EnumSet<InvocationMode> get() = EnumSet.allOf(InvocationMode::class.java)
  },

  /**
   * Python v3.0+ support via GraalPy
   */
  PYTHON(symbol = "python", label = "Python"),

  /**
   * Ruby support via TruffleRuby
   */
  RUBY(symbol = "ruby", label = "Ruby"),

  /**
   * JVM support via SVM and Espresso
   */
  JVM(symbol = "jvm", engine = "java", label = "JVM"),

  /**
   * WASM support via GraalWasm
   */
  WASM(symbol = "wasm", label = "WebAssembly"),
}
