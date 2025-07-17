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
package elide.runtime.diag

// Default diagnostics receiver/buffer.
private val DEFAULT: DiagnosticsContainer = DiagnosticsContainer.create(lockable = false)

/**
 * # Diagnostics
 *
 * Provides static utility methods for working with [Diagnostic] and [MutableDiagnostic] records, as well as native
 * entrypoints for reporting to the diagnostics buffer.
 */
public data object Diagnostics : DiagnosticsReceiver by DEFAULT, DiagnosticsBuffer by DEFAULT {
  /**
   * Create an empty mutable diagnostic record.
   *
   * @return A new mutable diagnostic record.
   */
  public fun mutable(): MutableDiagnostic = MutableDiagnostic.create()

  override fun lock() {
    // Locking the default diagnostics buffer is not supported, so this is a no-op.
  }

  override fun close() {
    // Closing the default diagnostics buffer is not supported, so this is a no-op.
  }
}

// Must be exposed for native access.
@Suppress("UNUSED") public class NativeDiagnostics private constructor () {
  public companion object {
    // Entrypoint for native code to report a diagnostic.
    @JvmName("reportNativeDiagnostic") @JvmStatic public fun reportNativeDiagnostic(mut: DiagnosticInfo) {
      DEFAULT.report(mut)
    }

    // Native entrypoint for creating a diagnostic record.
    @JvmName("createDiagnostic") @JvmStatic internal external fun createDiagnostic(): MutableDiagnostic
  }
}
