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

package elide.runtime.typescript

import kotlin.with
import elide.runtime.lang.javascript.JavaScriptCompilerConfig
import elide.runtime.lang.javascript.JavaScriptLang.precompiler
import elide.runtime.precompiler.Precompiler
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest

/**
 * ## TypeScript Precompiler
 *
 * Implements Elide's [Precompiler] interface for native TypeScript execution; this interface uses OXC via Elide's Rust
 * layer to strip TypeScript types and transpile where necessary (e.g. TSX).
 */
public object TypeScriptPrecompiler : Precompiler.SourcePrecompiler<JavaScriptCompilerConfig> {
  override fun invoke(req: PrecompileSourceRequest<JavaScriptCompilerConfig>, input: String): String? {
    // use the regular js precompiler
    return with(precompiler()) { invoke(req, input) }
  }

  @JvmStatic public fun obtain(): TypeScriptPrecompiler = TypeScriptPrecompiler
}
