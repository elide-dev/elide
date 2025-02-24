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
import java.util.function.Predicate

/**
 * ## Diagnostics Buffer
 *
 * Provides the readable end of a [DiagnosticsReceiver]; supplies facilities for querying against diagnostics held from
 * the receiver. In most cases, a receiver is also a buffer.
 */
public interface DiagnosticsBuffer : AutoCloseable, Closeable {
  /**
   * Clear all held diagnostics.
   */
  public fun clear()

  /**
   * Indicate whether this buffer has received diagnostics of type [lang], or of any type if `null` is provided.
   *
   * @return Dirty-state of the buffer.
   */
  public fun dirty(lang: String? = null): Boolean

  /**
   * Return a sequence of all reported diagnostic info records for the lifetime of this buffer.
   *
   * @return Sequence of all diagnostic info records
   */
  public fun all(): Sequence<DiagnosticInfo>

  /**
   * Return a sequence of all reported diagnostic info records for the lifetime of this buffer that match the given
   * language and/or tool.
   *
   * @param lang Language of the diagnostics.
   * @param tool Tool that reported the diagnostics; if `null`, all tools are considered.
   * @param consume Whether to consume the diagnostics after querying.
   * @return Sequence of all diagnostic info records that match the given language and tool
   */
  public fun query(lang: String, tool: String? = null, consume: Boolean = false): Sequence<DiagnosticInfo>

  /**
   * Return a sequence of all reported diagnostic info records for the lifetime of this buffer that match the given
   * criteria.
   *
   * @param consume Whether to consume the diagnostics after querying.
   * @param criteria Predicate to match against diagnostic info records.
   * @return Sequence of all diagnostic info records that match the given criteria
   */
  public fun query(consume: Boolean = false, criteria: Predicate<DiagnosticInfo>): Sequence<DiagnosticInfo>
}
