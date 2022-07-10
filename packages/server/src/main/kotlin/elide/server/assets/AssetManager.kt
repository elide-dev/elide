package elide.server.assets

import com.google.common.util.concurrent.Futures
import elide.annotations.API
import elide.server.StreamedAsset
import elide.server.StreamedAssetResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.guava.asDeferred
import org.slf4j.Logger

/**
 * Describes the API surface of an *Asset Manager*, which controls access to embedded server-side assets such as CSS and
 * JS documents.
 *
 * The asset manager is responsible for maintaining a runtime registry of all available assets, which is typically built
 * at server startup. Assets should be held in memory so they may easily be resolved, dynamically transformed, and
 * ultimately served to end-users through either user-defined controllers or the built-in asset controller.
 *
 * ### Asset descriptors
 *
 * During the app build, the Elide build tools package scripts for use with SSR or client-side serving. After packaging
 * assets from internal and external builds, protocol buffer descriptors are computed and embedded in the application,
 * which describe how each asset should be served, addressed, and treated with regard to dependencies.
 *
 * At server startup, the encoded descriptors are loaded, assets are interpreted and loaded, and the server refers to
 * the resulting mapping when called upon to serve assets.
 *
 * ### Offloading
 *
 * Static assets do exist on disk, technically, but when embedded in an application JAR or native image, they are
 * automatically zipped and inlined into the application binary. Therefore, OS-level tools like `sendfile` aren't an
 * option.
 *
 * However, Micronaut and KotlinX Coroutines both have built-in IO scheduling support, which is usually backed by a pool
 * of cached POSIX threads. This offers a good alternative for offloading I/O from a user-defined request handler.
 *
 * ### Customizing the asset system
 *
 * The [AssetManager] is a simple interface which mediates between an [AssetResolver] and [AssetReader] implementation
 * pair to serve assets at mapped HTTP paths. In some cases, the developer may want to customize this behavior, either
 * in the way asset paths are translated or interpreted (via a custom [AssetResolver]), or the way asset content is
 * loaded and returned (via a custom [AssetReader]). Both can be used at once if needed.
 *
 * To customize a given asset system component, use the `Replaces` annotation from Micronaut's context module. For
 * example:
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
 * The [AssetManager] participates in the DI container, so the developer only needs to provide a component definition.
 * Later, when an [AssetManager] instance is requested by the app, the main implementation will load and use the
 * developer's custom implementation.
 *
 * @see AssetResolver which is responsible for checking, translating, and loading asset paths.
 * @see AssetReader which is responsible for reading asset content efficiently.
 * @see RenderedAsset for the generic return value model leveraged by [AssetManager].
 * @see ServerAsset for the symbolic asset reference model leveraged by [AssetManager].
 */
@API public interface AssetManager {
  /**
   * Asset reader which is in use for this asset manager; responsible for translating an absolute asset resource path
   * into a stream of actual resource content.
   */
  public val reader: AssetReader

  /**
   * Logger which should be used to emit not-found warnings and other messages from the asset manager implementation
   * which is live for the current server lifecycle.
   */
  public val logging: Logger

  /**
   * Resolve the provided asset [path] to a [ServerAsset] descriptor; if the file cannot be found, return `null`, and
   * otherwise, throw an error.
   *
   * @param path Relative path to the asset which we want to resolve.
   * @return Resolved server asset, or `null` if one could not be located at the provided [path].
   */
  public fun resolve(path: String): ServerAsset? {
    return reader.resolve(path)
  }

  /**
   * Resolve the asset requested by the provided HTTP [request]; if the corresponding file cannot be found, return
   * `null`, and otherwise, throw an error.
   *
   * @param request HTTP request to interpret into a relative asset path and return a descriptor for.
   * @return Resolved server asset, or `null` if one could not be located at the calculated path provided by [request].
   */
  public fun resolve(request: HttpRequest<*>): ServerAsset? {
    return reader.resolve(request)
  }

  /**
   * Serve a response of status HTTP 404 (Not Found), in response to a request for an asset which could not be located
   * by the built-in asset system.
   *
   * @param request HTTP request which prompted this 404-not-found response.
   * @return Deferred task which resolves to an HTTP 404 response.
   */
  public fun serveNotFoundAsync(request: HttpRequest<*>): Deferred<StreamedAssetResponse> {
    return Futures.immediateFuture(HttpResponse.notFound<StreamedAsset>()).asDeferred()
  }

  /**
   * Responsible for converting a known-good asset held by the server into an efficient [StreamedAssetResponse] which
   * serves the asset to the invoking client.
   *
   * This method is the core of the runtime portion of the asset system. When an asset is requested via an endpoint
   * managed by the [AssetController], it effectively calls into this method, after resolving the asset, in order to
   * actually serve it.
   *
   * [StreamedAssetResponse] is mapped to Netty/Micronaut classes under the hood which are optimal for serving static
   * asset data.
   *
   * ### Dynamic asset transformation
   *
   * If the asset must be transformed before being returned, especially in some computationally-expensive manner, then
   * the underlying method should switch out to the I/O scheduler (or some other scheduler) in order to avoid any
   * blocking behavior.
   *
   * ### Response variability
   *
   * If the response needs to be customized based on the provided [request], make sure to include any relevant request
   * headers as `Vary` values in the response, so that HTTP caching can work correctly.
   *
   * @param request HTTP request which this render cycle is responding to.
   * @param asset Resolved server asset which we intend to render and serve.
   * @return Deferred task which resolves to a [StreamedAssetResponse] satisfying a request to serve the provided
   *   resolved [asset] data.
   */
  public suspend fun renderAssetAsync(request: HttpRequest<*>, asset: ServerAsset): Deferred<StreamedAssetResponse>

  /**
   * Asynchronously produce an HTTP response which serves the asset described by the provided [request]; if the asset in
   * question cannot be located, serve a `404 Not Found`, and for any other error, serve a `500 Internal Server Error`.
   *
   * @param request HTTP request which should be translated into an asset path and served.
   * @return Deferred task which resolves to an HTTP response serving the requested asset.
   */
  public suspend fun serveAsync(request: HttpRequest<*>): Deferred<StreamedAssetResponse> {
    return renderAssetAsync(
      request,
      resolve(request) ?: return (
        serveNotFoundAsync(request)
      )
    )
  }
}
