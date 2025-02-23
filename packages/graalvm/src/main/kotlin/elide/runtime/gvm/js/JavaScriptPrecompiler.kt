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
package elide.runtime.gvm.js

import elide.annotations.API
import elide.runtime.diag.Diagnostics
import elide.runtime.diag.DiagnosticsContainer
import elide.runtime.precompiler.Precompiler
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest
import elide.runtime.precompiler.PrecompilerError
import elide.runtime.precompiler.PrecompilerNotice

private const val LANG_TAG_JS = "js"
private const val TOOL_PRECOMPILER = "precompiler"

/**
 * # Native JavaScript Parser
 *
 * Bridges via JNI to the OXC parsing and code-generator tools.
 */
@API public object JavaScriptPrecompiler : Precompiler.SourcePrecompiler<JavaScriptCompilerConfig> {
  @Suppress("TooGenericExceptionCaught")
  override fun invoke(req: PrecompileSourceRequest<JavaScriptCompilerConfig>, input: String): String? = try {
    precompile(req.source.name, input).also {
      // after running the precompiler, consume all matching diagnostics (if present), and throw them back to the caller
      if (Diagnostics.dirty(LANG_TAG_JS)) Diagnostics.query(LANG_TAG_JS, TOOL_PRECOMPILER, true).let { diag ->
        PrecompilerNotice.from(DiagnosticsContainer.from(diag))
      }
    }
  } catch (err: RuntimeException) {
    throw PrecompilerError("Failed to precompile JavaScript source", err)
  }

  /**
   * ## Precompile JavaScript
   *
   * This method accepts code in the form of JSX, TSX, TypeScript, or JavaScript, and will act to parse it using the
   * native parser, and then lower it into compliant ECMA code which is runnable by Elide.
   *
   * @param name Filename for the code provided herein
   * @param code Code contents from the file in question
   * @return Lowered source-code which should be used instead
   */
  @JvmStatic @JvmName("precompile") private external fun precompile(name: String, code: String): String?

  /** Provider for the [JavaScriptPrecompiler]. */
  public class Provider : Precompiler.Provider<JavaScriptPrecompiler> {
    override fun get(): JavaScriptPrecompiler = JavaScriptPrecompiler
  }
}
