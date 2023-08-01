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

package elide.runtime

/** Enumerates log levels on a given platform. */
public enum class LogLevel {
  /**
   * `TRACE`-level logging; the most detailed level.
   *
   * Trace logging is typically used to follow values or calls through a program's execution. Trace logging can produce
   * an immense amount of output.
   */
  TRACE,

  /**
   * `DEBUG`-level logging.
   *
   * Debug-level logging is typically used to express program conditions which are expected under normal operating
   * circumstances. This level may be useful for diagnosing problems or unidentified behavior.
   */
  DEBUG,

  /**
   * `WARN`-level logging.
   *
   * Logs warnings emitted by the compiler or at runtime. Warnings are typically used to indicate a condition that is
   * tolerable to the application, but sub-optimal or dangerous to leave in place.
   */
  WARN,

  /**
   * `INFO`-level logging.
   *
   * Logs informative messages about program state which are typically not verbose in nature. Info-level logging may be
   * used to confirm expected application state at notable points in a program's lifecycle.
   */
  INFO,

  /**
   * `ERROR`-level logging.
   *
   * Logs information about error states that arise during a program's execution. This logging level should almost
   * always be left active in order to retroactively diagnose issues during a program's run.
   */
  ERROR,
}
