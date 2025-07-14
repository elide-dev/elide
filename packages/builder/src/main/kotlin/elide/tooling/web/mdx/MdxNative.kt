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
package elide.tooling.web.mdx

import elide.runtime.diag.DiagnosticInfo
import elide.runtime.diag.Diagnostics

private const val REPORT_MDX_ERROR_METHOD = "reportMdxError"
private const val BUILD_MDX_METHOD = "buildMdx"

// Native methods via JNI for MDX processing.
internal object MdxNative {
  /**
   * Report an error encountered while processing MDX code.
   *
   * @param errMessage The error message to report.
   * @return The processed MDX as a string, or null if processing fails.
   */
  @Suppress("unused") // Used from JNI.
  @JvmName(REPORT_MDX_ERROR_METHOD) @JvmStatic internal fun reportMdxError(errMessage: String?) {
    Diagnostics.report(DiagnosticInfo.mutable().apply {
      message = errMessage
    })
  }

  /**
   * Build MDX from the given code string.
   *
   * @param code The MDX code to process.
   * @return The processed MDX as a string, or null if processing fails.
   */
  @JvmName(BUILD_MDX_METHOD) @JvmStatic internal external fun buildMdx(code: String): String?
}
