package elide.server.assets

import elide.annotations.API
import elide.server.AssetModuleId
import io.micronaut.http.HttpRequest
import kotlinx.coroutines.Deferred
import java.io.FileNotFoundException

/**
 * Describes the API surface expected for a reader of static assets from some data source; responsible for efficiently
 * reading assets from a resource path and producing resulting content.
 *
 * Asset readers can cache the results of their reads, if desired, and may base caching decisions on the asset
 * descriptors provided when resolving assets.
 *
 * ### Resource assets
 *
 * Typically, resources are embedded in the application JAR or native image, in zipped form, alongside embedded SSR
 * assets and compiled JVM or native classes. The main [AssetReader] implementation knows how to interpret these assets
 * based on a binary-encoded protocol buffer message *also* embedded within the application.
 *
 * ### Replacing the main reader
 *
 * The developer may find it desirable to write and provide their own [AssetReader] implementation, which can be
 * accomplished via Micronaut's DI system (read on below). In particular, this may be a requirement for stable testing
 * of a broader [AssetManager] implementation.
 *
 * #### Replacing components of the [AssetManager]
 *
 * To replace the stock [AssetReader] implementation, Micronaut's `Replaces` annotation can be used:
 * ```kotlin
 * package your.package.here;
 *
 * import elide.server.assets.AssetReader;
 * import io.micronaut.context.annotation.Replaces;
 *
 * @Replaces(ServerAssetReader::class)
 * public class MyCoolAssetReader: AssetReader {
 *   // (impl here)
 * }
 * ```
 *
 * @see AssetManager which coordinates between the [AssetReader] and [AssetResolver].
 * @see RenderedAsset for the generic return value model leveraged by [AssetManager].
 * @see ServerAsset for the symbolic asset reference model leveraged by [AssetManager].
 */
@API public interface AssetReader : AssetResolver {
  /**
   * Given a resolved asset [descriptor] which should be known to exist, read the associated asset content, and return
   * it as an async [Deferred] task which can be awaited, and then consumed.
   *
   * If the asset underlying the provided asset descriptor is found not to exist, a [FileNotFoundException] is raised.
   *
   * @throws FileNotFoundException if the provided asset cannot be located.
   * @param descriptor Resolved asset descriptor, which is expected to exist.
   * @param request HTTP request which is asking to be served this asset.
   * @return Deferred task which resolves to a rendered asset which may be consumed, corresponding to [descriptor].
   */
  @Throws(FileNotFoundException::class)
  public suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>): Deferred<RenderedAsset>

  /**
   * Resolve a reference to an asset identified by the provided [moduleId], in the form of an [AssetPointer]; if no
   * matching asset can be found, return `null` to indicate a not-found failure.
   *
   * @param moduleId ID of the module which we should resolve from the active asset bundle.
   * @return Asset pointer resolved for the provided [moduleId], or `null`.
   */
  public fun pointerTo(moduleId: AssetModuleId): AssetPointer?
}
