package elide.tool.ssg

import kotlinx.coroutines.Deferred
import tools.elide.meta.AppManifest
import java.io.Closeable

/**
 * # SSG: Manifest Reader
 *
 * This interface defines the API surface of implementations responsible for loading an Elide application manifest
 * during execution of the SSG (Static Site Generator) compiler.
 *
 * ## Filesystem & Classpath Resources
 *
 * By default, the manifest reader is expected to interpret given paths as filesystem paths, unless a given path is
 * prefixed with `classpath:`. In this case, the path is interpreted as a classpath resource.
 */
public interface ManifestReader : Closeable, AutoCloseable {
  /**
   * Read an application manifest, given the provided [params].
   *
   * @param params Compiler params to read a manifest from.
   * @return Application manifest read from the given parameters.
   * @throws SSGCompilerError the manifest cannot be located based on the input parameters.
   */
  @Throws(SSGCompilerError::class)
  public suspend fun readManifest(params: SiteCompilerParams): AppManifest = readManifest(params.manifest)

  /**
   * Read an application manifest, given the provided filesystem [path].
   *
   * @param path File-system path to the manifest.
   * @return Application manifest read from the given parameters.
   * @throws SSGCompilerError the manifest cannot be located based on the input parameters.
   */
  @Throws(SSGCompilerError::class)
  public suspend fun readManifest(path: String): AppManifest = readManifestAsync(path).await()

  /**
   * Read an application manifest asynchronously, given the provided filesystem [path].
   *
   * If a manifest cannot be located, an exception is raised after calling `await` on the returned job.
   *
   * @param path File-system path to the manifest.
   * @return Deferred job which resolved to the application manifest read from the given parameters.
   */
  public suspend fun readManifestAsync(path: String): Deferred<AppManifest>
}
