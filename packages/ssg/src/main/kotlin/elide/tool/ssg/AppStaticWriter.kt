package elide.tool.ssg

import kotlinx.coroutines.Deferred
import java.io.Closeable

/**
 * # SSG: Static Site Writer
 *
 * This interface defines the expected API surface of an object which can take a completed Static Site Generator compile
 * routine and write the output. Typically, this is handled by the default implementation, which simply writes according
 * to CLI or programmatic invocation parameters.
 *
 * During testing, an alternate implementation can be provided which holds results in memory, or does something else
 * fancy with the compiler results.
 */
public interface AppStaticWriter : Closeable, AutoCloseable {
  /**
   *
   */
  public suspend fun writeOutputs(params: SiteCompilerParams, buffer: StaticSiteBuffer): String = writeOutputsAsync(
    params,
    buffer,
  ).await()

  /**
   *
   */
  public suspend fun writeOutputsAsync(params: SiteCompilerParams, buffer: StaticSiteBuffer): Deferred<String>
}
