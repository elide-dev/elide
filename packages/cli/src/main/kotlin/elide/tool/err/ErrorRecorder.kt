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

package elide.tool.err

import kotlinx.coroutines.Job
import elide.tool.err.ErrorHandler.ErrorEvent

/**
 * # Elide Tool: Error Recorder
 *
 * Handles errors which should be recorded i some persistent fashion, so they can be inspected or reported at a later
 * time by the user or developer.
 */
interface ErrorRecorder {
  /**
   * ## Record an Error
   *
   * This method records an [error] which was thrown while running Elide; further context about the error may be
   * provided, if available.
   *
   * @param event Error event which we are being asked to record.
   * @return Job which concludes when the error has been safely recorded.
   */
  suspend fun recordError(event: ErrorEvent): Job
}
