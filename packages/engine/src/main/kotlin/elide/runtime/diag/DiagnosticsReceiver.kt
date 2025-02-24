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

import java.io.Closeable

/**
 * ## Diagnostics Receiver
 *
 * Receives diagnostics emitted from a compiler, tool, or other developer-oriented non-runtime step; diagnostics model
 * such concepts as compiler warnings, linter notices, and so on. Diagnostics are received in several ways: for JVM,
 * diagnostics can be reported to any [DiagnosticsReceiver]-compliant instance. For native code, a singleton static
 * instance of [DiagnosticsReceiver] is provided at [Diagnostics].
 *
 * In all cases, the receiver buffers diagnostics for later querying, with a hook being made available to indicate that
 * no diagnostics will be consumed past a certain point.
 */
public interface DiagnosticsReceiver : AutoCloseable, Closeable {
  /**
   * Lock this diagnostics receiver from further modification; if not called by first read, will be called by first
   * operation.
   *
   * This method is idempotent.
   */
  public fun lock()

  /**
   * Report a single diagnostic record to the receiver.
   *
   * @param diag The diagnostic record to report.
   */
  public fun report(diag: DiagnosticInfo)

  /**
   * Report one or more individual diagnostic record(s) to the receiver.
   *
   * @param first The first diagnostic record to report.
   * @param rest The remaining diagnostic records to report.
   */
  public fun report(first: DiagnosticInfo, vararg rest: DiagnosticInfo)

  /**
   * Report an iterable of diagnostic records to the receiver.
   *
   * @param diags The diagnostic records to report.
   */
  public fun report(diags: Iterable<DiagnosticInfo>)

  /**
   * Report a sequence of diagnostic records to the receiver.
   *
   * @param diags The diagnostic records to report.
   */
  public fun report(diags: Sequence<DiagnosticInfo>)
}
