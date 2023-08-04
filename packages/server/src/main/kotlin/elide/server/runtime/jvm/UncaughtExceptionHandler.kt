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

package elide.server.runtime.jvm

import jakarta.inject.Singleton
import elide.runtime.Logger
import elide.runtime.Logging

/**
 * Default uncaught exception handler; logs the error to the root logger, along with a stacktrace and message from the
 * exception, if any.
 *
 * Application-level code can override this default handler by using the `@Replaces` annotation from Micronaut, as
 * demonstrated below:
 *
 * ```kotlin
 * @Singleton @Replaces(UncaughtExceptionHandler::class)
 * class MyHandler: Thread.UncaughtExceptionHandler {
 *   // ...
 * }
 * ```
 */
@Singleton public open class UncaughtExceptionHandler: Thread.UncaughtExceptionHandler {
  // Root logger.
  private val logging: Logger = Logging.root()

  override fun uncaughtException(thread: Thread, err: Throwable) {
    // not yet implemented
    logging.error(
      "Encountered critical uncaught error (thread: '${thread.name}'): '${err.message ?: "NO_MESSAGE"}'",
      err
    )
  }
}
