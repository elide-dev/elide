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

package elide.tool.ssg

import com.google.protobuf.InvalidProtocolBufferException
import tools.elide.meta.AppManifest
import java.io.File
import java.io.IOException
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging

/** Implementation of a [ManifestReader] which reads off disk. */
@Singleton internal class FilesystemManifestReader (
  private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ManifestReader {
  // Private logger.
  private val logging: Logger = Logging.of(FilesystemManifestReader::class)

  override fun close() {
    // nothing at this time.
  }

  override suspend fun readManifestAsync(path: String): Deferred<AppManifest> {
    logging.debug("Locating manifest at path '$path'...")

    // locate file
    return withContext(dispatcher) {
      async {
        val file = try {
          val target = if (path.startsWith("classpath:")) {
            (this::class.java.getResource("/" + path.drop("classpath:".length)) ?: throw SSGCompilerError
              .InvalidArgument("Failed to locate manifest as classpath resource at path '$path'")).let {
              File(it.toURI())
            }
          } else {
            val f = File(path)
            if (!f.exists()) throw IOException(
              "Manifest not found at path '$path'"
            )
            f
          }
          logging.debug("Manifest file located at path '$path'.")
          target
        } catch (ioe: IOException) {
          throw SSGCompilerError.IOError("Failed to read app manifest: IO Error", ioe)
        }
        val manifest = try {
          logging.debug("Reading manifest file at path '$path'...")
          file.inputStream().buffered().use { buf ->
            AppManifest.parseFrom(buf)
          }
        } catch (ipbe: InvalidProtocolBufferException) {
          throw SSGCompilerError.IOError("Failed to read app manifest: Invalid protocol buffer error", ipbe)
        }
        logging.debug("App manifest loaded.")
        if (logging.isEnabled(LogLevel.TRACE))
          logging.trace("Loaded app manifest: $manifest")
        manifest
      }
    }
  }
}
