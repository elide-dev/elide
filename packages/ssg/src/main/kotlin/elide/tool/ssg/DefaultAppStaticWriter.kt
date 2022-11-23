package elide.tool.ssg

import jakarta.inject.Singleton
import kotlinx.coroutines.Deferred
import java.io.Closeable

/** Default writer implementation for static sites. */
@Singleton internal class DefaultAppStaticWriter : AppStaticWriter {
  // Compiler params.
  private lateinit var params: SiteCompilerParams

  // Resources that must be closed.
  private val closeables: ArrayList<Closeable> = ArrayList()

  // Register a stream as a closeable.
  private fun <C: Closeable, R> register(resource: C, op: (C) -> R): R {
    closeables.add(resource)
    return op.invoke(resource)
  }

  // Write buffered compiler outputs to a directory structure.
  private suspend fun writeOutputsToDirectory(buffer: StaticSiteBuffer): Deferred<String> {
    TODO("not yet implemented")
  }

  // Write buffered compiler outputs to a zip file.
  private suspend fun writeOutputsToZip(buffer: StaticSiteBuffer): Deferred<String> {
    TODO("not yet implemented")
  }

  // Write buffered compiler outputs to a tarball.
  private suspend fun writeOutputsToTarball(buffer: StaticSiteBuffer): Deferred<String> {
    TODO("not yet implemented")
  }

  /** @inheritDoc */
  override suspend fun writeOutputsAsync(params: SiteCompilerParams, buffer: StaticSiteBuffer): Deferred<String> {
    this.params = params
    return when (params.output.mode) {
      // handle directory output
      SiteCompilerParams.OutputMode.DIRECTORY -> writeOutputsToDirectory(buffer)

      // handle output for each file type
      SiteCompilerParams.OutputMode.FILE -> when ((params.output as SiteCompilerParams.Output.File).format) {
        SiteCompilerParams.OutputFormat.ZIP -> writeOutputsToZip(buffer)
        SiteCompilerParams.OutputFormat.TAR -> writeOutputsToTarball(buffer)
      }
    }
  }

  /** @inheritDoc */
  override fun close(): Unit = closeables.forEach {
    try {
      it.close()
    } catch (err: Throwable) {
      // no op
    }
  }
}
