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

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPOutputStream
import jakarta.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import elide.annotations.Context
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.tool.err.ErrorHandler.ErrorEvent
import elide.tool.io.RuntimeWorkdirManager
import elide.tool.io.WorkdirManager

/** Records reported errors to a rendered Markdown-like format which can later be parsed and reported to GitHub. */
internal class DefaultStructuredErrorRecorder private constructor (
  private val workdirManager: WorkdirManager,
) : ErrorRecorder {
  companion object {
    private val singleton: AtomicReference<DefaultStructuredErrorRecorder> = AtomicReference(null)

    /** @return Created or acquired [DefaultStructuredErrorRecorder] singleton. */
    @JvmStatic fun acquire(): DefaultStructuredErrorRecorder = synchronized(this) {
      if (singleton.get() == null) {
        singleton.set(DefaultStructuredErrorRecorder(RuntimeWorkdirManager.acquire()))
      }
      singleton.get()
    }
  }

  /** Provides an injection factory for resolving the singleton [DefaultStructuredErrorRecorder]. */
  @Factory class DefaultMarkdownErrorRecorderProvider : Provider<DefaultStructuredErrorRecorder> {
    @Context @Singleton override fun get(): DefaultStructuredErrorRecorder = acquire()
  }

  // Generate a filename for a `PersistedError`.
  private fun PersistedError.filename(): String = "error-${timestamp.epochSeconds}-$id.json.gz"

  // Serialize and write an error.
  @OptIn(ExperimentalSerializationApi::class)
  private fun PersistedError.writeErrorToTempDir(destination: File) {
    check(destination.exists() && destination.canWrite()) {
      "Cannot write to flight recorder directory: Does not exist or is not writable"
    }
    GZIPOutputStream(destination.resolve(filename()).outputStream().buffered()).use { stream ->
      Json.encodeToStream(PersistedError.serializer(), this, stream)
    }
  }

  override suspend fun recordError(event: ErrorEvent): Job = withContext(Dispatchers.IO) {
    async {
      // build error record
      PersistedError.create(event).apply {
        writeErrorToTempDir(workdirManager.flightRecorderDirectory().toFile())
      }
    }
  }
}
