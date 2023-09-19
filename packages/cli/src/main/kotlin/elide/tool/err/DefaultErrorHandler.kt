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

import io.micronaut.context.annotation.Replaces
import java.util.concurrent.atomic.AtomicReference
import jakarta.inject.Provider
import elide.annotations.*
import elide.annotations.Factory
import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.runtime.jvm.UncaughtExceptionHandler
import elide.tool.err.ErrorHandler.*

/** Default universal error handler, with support for Elide's [ErrorRecorder]. */
internal class DefaultErrorHandler private constructor (
  private val recorder: ErrorRecorder,
) : ErrorHandler {
  companion object {
    private const val LOGGER_NAME = "elide:flight-recorder"
    private val singleton: AtomicReference<DefaultErrorHandler> = AtomicReference(null)

    /** @return Created or acquired [DefaultErrorHandler] singleton. */
    @JvmStatic fun acquire(): DefaultErrorHandler = synchronized(this) {
      if (singleton.get() == null) {
        singleton.set(DefaultErrorHandler(DefaultStructuredErrorRecorder.acquire()))
      }
      singleton.get()
    }
  }

  override val logging: Logger by lazy {
    Logging.named(LOGGER_NAME)
  }

  /** Provides an injection factory for resolving the singleton [DefaultErrorHandler]. */
  @Factory class DefaultErrorHandlerProvider : Provider<DefaultErrorHandler> {
    @Replaces(UncaughtExceptionHandler::class)
    @Context @Singleton override fun get(): DefaultErrorHandler = acquire()
  }

  override suspend fun ErrorHandlerContext.handleError(event: ErrorEvent): ErrorActionStrategy {
    return default().also {
      recorder.recordError(event)
    }
  }
}
