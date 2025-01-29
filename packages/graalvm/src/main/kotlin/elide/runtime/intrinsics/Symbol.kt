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
package elide.runtime.intrinsics

import elide.core.api.Symbolic

/**
 * # Symbol
 *
 * Describes the concept of a symbol which is made available to guest code; symbols are attached to intrinsic values,
 * which are typically provided by the outer host system, and are optionally made available to guest code.
 *
 * ## Symbol Naming
 *
 * Symbols always carry a simple [String] name; this name is exposed at the [symbol] property. Symbol names are the
 * names at which intrinsics are bound in guest VMs.
 *
 * Because symbol names are used as actual code symbols, their character set is restricted to valid variable names in
 * each target language. Please only use `A-Z`, `a-z`, and `0-9`, with the first character as a letter, always. No
 * punctuation or special characters are allowed.
 *
 * ## Internal Symbols
 *
 * Elide supports a concept of "internal" symbols, which are marked at build-time. These symbols are provided to the
 * guest VM during initialization, for the purpose of providing internal module APIs. Once the VM has finished the
 * initialization routine, these symbols are withheld, and then guest code is executed.
 *
 * A symbol is internal if [isInternal] returns `true`. Internal symbols mask or otherwise obfuscate their public
 * [symbol] value. You can obtain the actual internal symbol name with [internalSymbol].
 *
 * ## Public Symbols
 *
 * If a symbol is _not_ marked as "internal," it is considered to be public, in which case it is exposed to guest
 * code at the literal [symbol] assigned to it.
 */
public interface Symbol : Symbolic<String> {
  /**
   * ## Symbol Name
   *
   * Holds the name assigned to this symbol for guest code access.
   */
  public override val symbol: String

  /**
   * ## Internal Symbol
   *
   * Holds the name assigned to this symbol without transforms or obfuscation.
   */
  public val internalSymbol: String

  /**
   * ## Internal State
   *
   * Describes whether this symbol is "internal," in which case it is not made available publicly to guest code, but
   * only to internal VM init code.
   */
  public val isInternal: Boolean
}
