package elide.tool.ssg

import com.google.protobuf.InvalidProtocolBufferException
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import jakarta.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import tools.elide.meta.AppManifest
import java.io.File
import java.io.IOException

/** Implementation of a [ManifestReader] which reads off disk. */
@Singleton internal class FilesystemManifestReader : ManifestReader {
  // Private logger.
  private val logging: Logger = Logging.of(FilesystemManifestReader::class)

  override fun close() {
    // nothing at this time.
  }

  /** @inheritDoc */
  override suspend fun readManifestAsync(path: String): Deferred<AppManifest> {
    logging.debug("Locating manifest at path '$path'...")

    // locate file
    return withContext(Dispatchers.IO) {
      async {
        val file = try {
          File(path).let {
            if (!it.exists()) throw SSGCompilerError.InvalidArgument(
              "Manifest not found at path '$path'"
            ) else {
              logging.debug("Manifest file located at path '$path'.")
            }
            it
          }
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
        if (logging.isEnabled(LogLevel.TRACE)) logging.trace("Loaded app manifest: $manifest")
        manifest
      }
    }
  }
}
