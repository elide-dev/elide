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

import com.google.common.annotations.VisibleForTesting
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tool.ssg.AppStaticWriter.FragmentOutputs
import elide.tool.ssg.AppStaticWriter.FragmentWrite
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import tools.elide.meta.EndpointType
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.Path

/** Default writer implementation for static sites. */
@Singleton internal class DefaultAppStaticWriter (
  private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AppStaticWriter {
  companion object {
    /** @return Relative path from input. */
    @JvmStatic private fun relativize(path: Path): Path {
      return if (path.startsWith("/")) {
        Path(path.toString().drop(1))
      } else {
        path
      }
    }

    /** @return Relative path from input. */
    @JvmStatic private fun relativize(path: String): Path = relativize(Path(path))
  }

  /** Defines types of output writer implementations. */
  private sealed interface OutputWriter : Closeable, AutoCloseable {
    /** Prepare the provided [path] as an output target. */
    suspend fun prepare(path: Path) = Unit

    /** Ensure that a directory exists at the provided [path]. */
    suspend fun ensureDirectory(base: Path, path: Path) = Unit

    /** Write a file from a static [fragment] and calculated [path], within the output [base]. */
    suspend fun writeFile(base: Path, path: Path, fragment: StaticFragment): FragmentWrite

    /** Flush all pending output to disk. */
    suspend fun flush() = Unit // default: no-op

    /** Close any pending resources. */
    override fun close() = Unit // default: no-op
  }

  /** Writes outputs to a directory. */
  private inner class DirectoryOutputWriter(private val parent: File): OutputWriter {
    override suspend fun prepare(path: Path): Unit = if (!parent.exists()) ioOperation("createOutputTree") {
      if (!parent.mkdirs()) throw IOException("Failed to create output directory tree '$path'")
    } else { /* no-op */ }

    override suspend fun ensureDirectory(base: Path, path: Path): Unit = ioOperation("ensureDirectory") {
      // relativize archive file path
      val relativePath = relativize(path)
      val resolved = base.resolve(relativePath)
      File(resolved.toAbsolutePath().toUri()).let {
        if (!it.parentFile.exists() && !it.parentFile.mkdirs()) {
          throw IOException("Failed to create tree directory '${resolved.parent}'")
        } else if (!it.parentFile.canWrite()) {
          throw IOException("No permission to create tree directory '${resolved.parent}'")
        } else {
          logging.trace("Ensure directory: '$resolved'")
        }
      }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeFile(
      base: Path,
      path: Path,
      fragment: StaticFragment,
    ): FragmentWrite = ioOperation("write") {
      File(base.resolve(path).toAbsolutePath().toUri()).let {
        if (it.exists()) {
          throw IOException("Output file already exists: '$path'")
        } else {
          logging.trace("Writing file: '$path'")
          val size = AtomicLong(0)
          registerAsync(it.outputStream()) { stream ->
            val data = fragment.content.array()
            size.set(data.size.toLong())

            withContext(dispatcher) {
              stream.write(data)
            }
          }.await()

          FragmentWrite.success(
            fragment,
            path.toString(),
            size.get(),
          )
        }
      }
    }
  }

  /** Writes outputs to an archive. */
  private abstract inner class ArchiveOutputWriter<A: ArchiveOutputStream, E: ArchiveEntry>(
    protected val stream: A,
  ): OutputWriter {
    // Write an archive entry.
    @Synchronized private fun writeArchiveEntry(entry: E, write: OutputStream.() -> Unit): E {
      stream.putArchiveEntry(entry)
      write.invoke(stream)
      stream.closeArchiveEntry()
      return entry
    }

    override suspend fun prepare(path: Path): Unit = when {
      !path.toFile().canWrite() -> throw SSGCompilerError.IOError("Output file is not writable: $path")
      else -> Unit
    }

    override suspend fun writeFile(
      base: Path,
      path: Path,
      fragment: StaticFragment,
    ): FragmentWrite {
      // acquire raw bytes of target file
      val bytes = fragment.content.array()
      val pathString = path.toString()

      // relativize archive file path, then package, write, and close as archive entry
      val relativePath = relativize(pathString)
      val entry = writeArchiveEntry(entryFromFile(relativePath.toString(), fragment)) {
        write(bytes)
      }

      // return as written fragment
      return FragmentWrite.success(
        fragment = fragment,
        path = path.toString(),
        size = bytes.size.toLong(),
        compressed = entry.size,
      )
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun flush() = withContext(dispatcher) {
      stream.flush()
    }

    override fun close() {
      stream.flush()
      stream.close()
    }

    /** @return Archive entry [E] from the provided [fragment]. */
    abstract fun entryFromFile(path: String, fragment: StaticFragment): E
  }

  /** Writes outputs to a tar archive. */
  private inner class TarArchiveWriter private constructor (target: TarArchiveOutputStream):
    ArchiveOutputWriter<TarArchiveOutputStream, TarArchiveEntry>(target) {

    // Alternate constructor which loads from a file.
    constructor(file: File): this(
      TarArchiveOutputStream(file.outputStream()),
    )

    override fun entryFromFile(path: String, fragment: StaticFragment): TarArchiveEntry = TarArchiveEntry(
      path,
    ).apply {
      size = fragment.content.array().size.toLong()
      setModTime(FileTime.fromMillis(0))
    }
  }

  /** Writes outputs to a zip archive. */
  private inner class ZipArchiveWriter private constructor (target: ZipArchiveOutputStream):
    ArchiveOutputWriter<ZipArchiveOutputStream, ZipArchiveEntry>(target) {

    // Alternate constructor which loads from a file.
    constructor(file: File): this(
      ZipArchiveOutputStream(file.outputStream()),
    )

    override fun entryFromFile(path: String, fragment: StaticFragment): ZipArchiveEntry = ZipArchiveEntry(
      path,
    ).apply {
      size = fragment.content.array().size.toLong()
      setTime(FileTime.fromMillis(0))
    }
  }

  // Private logger.
  private val logging: Logger = Logging.of(DefaultAppStaticWriter::class)

  // Compiler params.
  private lateinit var params: SiteCompilerParams

  // Resources that must be closed.
  private val closeables: ArrayList<Closeable> = ArrayList()

  // Register a stream as a closeable.
  private suspend fun <C: Closeable, R> registerAsync(resource: C, op: suspend (C) -> R): Deferred<R> = coroutineScope {
    closeables.add(resource)
    async {
      op.invoke(resource)
    }
  }

  // Build a virtualized output file-path based on the input `fragment`. Relative to output base.
  @VisibleForTesting @Suppress("unused") internal fun filepathForFragment(fragment: StaticFragment): String {
    val basepath = fragment.basePath()
    val tailpath = fragment.tailPath()
    val produces = fragment.produces()

    val filepathVirtual = if (!tailpath.isNullOrBlank()) {
      if (tailpath.startsWith("/")) {
        if (basepath == "/") {
          tailpath  // it's a duplicate or empty base
        } else {
          "$basepath$tailpath"
        }
      } else {
        if (basepath == "/") {
          "/$tailpath"  // it's a duplicate or empty base, just with a relative tail
        } else {
          "$basepath/$tailpath"
        }
      }
    } else {
      basepath
    }
    val extensionVirtual = if (fragment.endpointType() == EndpointType.PAGE || produces.contains("text/html")) {
      "html"
    } else {
      null
    }
    val finalUrlSegment = if (filepathVirtual != "/") {
      (tailpath ?: basepath).split("/").last()
    } else {
      ""
    }
    val filename = when {
      // if the virtual URL is `/` and we guessed `html` for the extension, it should be an `index.html` file within the
      // enclosing folder (potentially at the root).
      filepathVirtual == "/" && extensionVirtual == "html" -> "index.html"

      // or, if the final URL segment is non-empty and contains a `.` with characters after it, it is probably a file
      // name with an extension, so we can use that.
      finalUrlSegment.isNotBlank() && finalUrlSegment.contains(".") && (
        finalUrlSegment.split(".").last().isNotEmpty()
      ) -> finalUrlSegment

      // otherwise, we don't really have a way to guess the filename and should fail.
      else -> throw SSGCompilerError.OutputError("Cannot determine filename for fragment: $fragment")
    }

    // if the base path is at the root, or it is an empty string, then our folder base path should be an empty string,
    // indicating that this file belongs at the root of the output tree.
    val folderBase = when {
      (basepath == "/" || basepath == "") && (tailpath == "/" || tailpath == "") -> ""

      // otherwise, we need a base folder path. if the virtual path contains the filename, that means the filename is
      // not virtualized/computed, so we can safely trim it from the virtualized path to get the base.
      else -> filepathVirtual.split("/").dropLast(1).joinToString("/")
    }
    return if (folderBase.isNotEmpty() && filename.isNotEmpty()) {
      listOf(
        folderBase,
        filename,
      ).joinToString(
        "/"
      )
    } else {
      // folder base is empty; we can just use a filename at the root.
      "/$filename"
    }
  }

  // Run an I/O operation, protecting for errors.
  @VisibleForTesting internal suspend fun <R: Any> ioOperation(phase: String, op: suspend () -> R): R = try {
    op.invoke()
  } catch (err: IOException) {
    logging.error("Failed to write static site in I/O phase '$phase'", err)
    throw SSGCompilerError.IOError("Failed to write static site in I/O phase '$phase'", err)
  } catch (err: Throwable) {
    logging.error("Failed to write static site in I/O phase '$phase'", err)
    throw SSGCompilerError.Generic(err)
  }

  /** @inheritDoc */
  override suspend fun writeAsync(params: SiteCompilerParams, buffer: StaticSiteBuffer): Deferred<FragmentOutputs> {
    // assign params, build file for target
    this.params = params
    val base = Path(params.output.path)

    // resolve output facade
    return registerAsync(when (params.output.mode) {
      // handle directory output
      SiteCompilerParams.OutputMode.DIRECTORY -> DirectoryOutputWriter(base.toFile())

      // handle output for each file type
      SiteCompilerParams.OutputMode.FILE -> when ((params.output as SiteCompilerParams.Output.File).format) {
        SiteCompilerParams.OutputFormat.ZIP -> ZipArchiveWriter(base.toFile())
        SiteCompilerParams.OutputFormat.TAR -> TarArchiveWriter(base.toFile())
      }
    }) {
      // prepare the path, which includes permission tests, creating empty directories, and so on.
      ioOperation("prepare") {
        it.prepare(base)
      }

      // seal buffer, we're ready to write
      buffer.seal()

      // for each fragment in the buffer, (1) call `ensureDirectory`, and (2) call `writeFile`.
      val jobs = buffer.consumeAsync { fragment ->
        // translate the fragment into a file path
        val path = Path(filepathForFragment(fragment))
        val relativePath = relativize(path)

        // ensure that any parent directories exist, as applicable.
        ioOperation("ensureDirectory") {
          it.ensureDirectory(base, path)
        }

        // write the file in question.
        ioOperation("writeFile") {
          it.writeFile(base, relativePath, fragment)
        }
      }

      // wait for all jobs to finish, then flush all remaining output.
      jobs.joinAll()
      it.flush()

      // return the output path as the result
      FragmentOutputs.of(
        path = base.toAbsolutePath().toString(),
        fragments = emptyList(),
      )
    }
  }

  /** @inheritDoc */
  override fun close(): Unit = closeables.forEach {
    it.close()
  }
}
