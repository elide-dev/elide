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
package elide.tooling.web.css

import elide.runtime.diag.DiagnosticInfo
import elide.runtime.diag.Diagnostics
import elide.tooling.web.css.CssBuilder.CssOptions

// Implements native methods for CSS parsing and building via LightningCSS in Rust.
internal object CssNative {
  @Suppress("unused")
  @JvmName("reportCssError") @JvmStatic internal fun reportCssError(error: String?) {
    Diagnostics.report(DiagnosticInfo.mutable().apply {
      message = error ?: "Unknown CSS error"
    })
  }

  // Builds CSS and returns a success or error code.
  @JvmName("buildCss") @JvmStatic internal external fun buildCss(
    css: String,
    options: CssOptions,
    minify: Boolean,
    sourceMaps: Boolean,
  ): String?
}
