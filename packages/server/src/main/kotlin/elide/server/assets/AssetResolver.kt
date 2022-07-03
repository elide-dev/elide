package elide.server.assets

import elide.annotations.API
import io.micronaut.http.HttpRequest

/**
 * Describes the expected API surface for a resolver of server-side assets, which, in cooperation with an [AssetReader]
 * and under the management of an [AssetManager], is responsible for checking asset paths for existence and translating
 * them to absolute paths which may be read and served.
 *
 * @see AssetReader which is charged with efficiently reading asset content.
 * @see AssetManager which coordinates between the [AssetReader] and [AssetResolver].
 * @see RenderedAsset for the generic return value model leveraged by [AssetManager].
 * @see ServerAsset for the symbolic asset reference model leveraged by [AssetManager].
 */
@API public interface AssetResolver {
  /**
   *
   */
  public fun resolve(path: String): ServerAsset?

  /**
   *
   */
  public fun resolve(request: HttpRequest<*>): ServerAsset? {
    return resolve(request.path)
  }
}
