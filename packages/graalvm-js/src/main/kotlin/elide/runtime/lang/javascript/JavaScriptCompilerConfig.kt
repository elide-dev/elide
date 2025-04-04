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
package elide.runtime.lang.javascript

import elide.runtime.precompiler.Precompiler

/**
 * ## Java Script Compiler Config
 *
 * @property sourceMaps Whether to generate source maps.
 * @property jsx Whether to support JSX.
 * @property esm Whether to support ECMAScript modules.
 * @property typescript Whether to support TypeScript.
 */
public data class JavaScriptCompilerConfig(
  val sourceMaps: Boolean = true,
  val jsx: Boolean = true,
  val esm: Boolean = true,
  val typescript: Boolean = true,
) : Precompiler.Configuration {
  public companion object {
    /** Default JavaScript compiler configuration. */
    @JvmStatic public val DEFAULT: JavaScriptCompilerConfig = JavaScriptCompilerConfig()
  }
}
