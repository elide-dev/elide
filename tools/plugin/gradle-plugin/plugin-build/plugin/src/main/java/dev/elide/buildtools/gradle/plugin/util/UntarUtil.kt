package dev.elide.buildtools.gradle.plugin.util

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

/** Utility to un-tar (while decompressing) to a given destination directory. */
object UntarUtil {
    /**
     * Consume the provided [archive] input stream, decompressing it as a `.tar.gz` to the provided [destination], which
     * is expected to be a directory.
     *
     * @param archive Input stream for the archive to de-compress.
     * @param destination Directory (as [File]) where the archive will be decompressed.
     * @param options Copy options to apply to the operation. See [StandardCopyOption].
     */
    @Throws(IOException::class)
    fun untar(archive: InputStream, destination: File, vararg options: CopyOption) {
        val pathOutput: Path = destination.toPath()
        val tarInputStream = TarArchiveInputStream(
            GzipCompressorInputStream(
                archive
            )
        )
        var entry: ArchiveEntry? = tarInputStream.nextEntry
        while (entry != null) {
            val pathEntryOutput: Path = pathOutput.resolve(entry.name)
            if (entry.isDirectory) {
                if (!Files.exists(pathEntryOutput)) {
                    Files.createDirectory(pathEntryOutput)
                }
            } else if (!pathEntryOutput.exists()) {
                Files.copy(
                    tarInputStream,
                    pathEntryOutput,
                    *options
                )
            }
            entry = tarInputStream.nextEntry
        }
        tarInputStream.close()
    }
}
