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
package elide.tooling.archive

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path

private typealias ZipStream = ArchiveOutputStream<ZipArchiveEntry>
private typealias TarStream = ArchiveOutputStream<TarArchiveEntry>

/**
 * ## Archive Builder
 *
 * Context types used when building archives during an Elide project build.
 */
public sealed interface ArchiveBuilder {
  /**
   * Create a file from a real file at [from] placed at [to] within the archive.
   *
   * @param from Path of the file to add.
   * @param to Path within the archive to create the file at.
   * @return Archive entry for the file at the specified path.
   */
  public fun packFile(from: Path, to: String): ArchiveEntry

  /**
   * Finalize the archive being built, flushing any pending entries, and closing the underlying stream; if there are
   * archive build errors, they should surface here.
   */
  public fun finalizeArchive()

  /**
   * ### Zip Builder
   *
   * Specializes [ArchiveBuilder] into functions that relate to Zip archives.
   */
  public class ZipBuilder internal constructor (private val stream: ZipStream): ArchiveBuilder {
    override fun packFile(from: Path, to: String): ArchiveEntry = from.toFile().let { file ->
      stream.createArchiveEntry(file, to).also {
        file.inputStream().use { input ->
          stream.putArchiveEntry(it)
          input.copyTo(stream, DEFAULT_BUFFER_SIZE)
          stream.closeArchiveEntry()
        }
      }
    }

    override fun finalizeArchive() {
      stream.finish()
    }
  }

  /**
   * ### Tar.gz Builder
   *
   * Specializes [ArchiveBuilder] into functions that relate to gzip-compressed tar archives.
   */
  public class TarGzBuilder internal constructor (
    private val stream: TarStream,
    private val gzipStream: GzipCompressorOutputStream? = null,
  ): ArchiveBuilder {
    override fun packFile(from: Path, to: String): ArchiveEntry = from.toFile().let { file ->
      TarArchiveEntry(file, to).also { entry ->
        entry.size = file.length()
        stream.putArchiveEntry(entry)
        file.inputStream().use { input ->
          input.copyTo(stream, DEFAULT_BUFFER_SIZE)
        }
        stream.closeArchiveEntry()
      }
    }

    override fun finalizeArchive() {
      stream.finish()
      stream.close()
      gzipStream?.close()
    }
  }

  /** Methods for obtaining [ArchiveBuilder] context instances of various types. */
  public companion object {
    /** @return Zip builder using the provided [stream]. */
    @JvmStatic public fun zipBuilder(stream: ZipStream): ZipBuilder = ZipBuilder(stream)

    /** @return Zip builder using the provided [path]. */
    @JvmStatic public fun zipBuilder(path: Path, opts: ZipArchiveOutputStream.() -> Unit = {}): ZipBuilder {
      return zipBuilder(
        ZipArchiveOutputStream(path.toFile()).apply {
          opts(this)
        }
      )
    }

    /** @return Tar.gz builder using the provided [stream] and [gzipStream]. */
    @JvmStatic public fun tarGzBuilder(stream: TarStream, gzipStream: GzipCompressorOutputStream? = null): TarGzBuilder {
      return TarGzBuilder(stream, gzipStream)
    }

    /** @return Tar.gz builder using the provided [path]. */
    @JvmStatic public fun tarGzBuilder(path: Path, opts: TarArchiveOutputStream.() -> Unit = {}): TarGzBuilder {
      val gzipStream = GzipCompressorOutputStream(BufferedOutputStream(Files.newOutputStream(path)))
      val tarStream = TarArchiveOutputStream(gzipStream).apply {
        setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
        setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
        opts(this)
      }
      return TarGzBuilder(tarStream, gzipStream)
    }
  }
}
