package elide.server.assets

import io.micronaut.http.HttpRequest
import elide.annotations.API
import elide.server.AssetModuleId

/**
 * Describes the expected API surface for a resolver of server-side assets, which, in cooperation with an [AssetReader]
 * and under the management of an [AssetManager], is responsible for checking asset paths for existence and translating
 * them to absolute paths which may be read and served.
 *
 * ### Asset tags
 * "Asset tags" are short strings which are computed at build time from a cryptographic hash of the asset's contents.
 * Tags can be used to serve an asset from the asset controller, using URLs like `/_/asset/{tag}.{ext}`, where `{tag}`
 * is the asset tag and `{ext}` is the asset's file extension (prefix adjustable by user).
 *
 * Developers reference assets by their *module ID*, which is a short recognizable string assigned by the developer to a
 * given asset.
 *
 * @see AssetReader which is charged with efficiently reading asset content.
 * @see AssetManager which coordinates between the [AssetReader] and [AssetResolver].
 * @see RenderedAsset for the generic return value model leveraged by [AssetManager].
 * @see ServerAsset for the symbolic asset reference model leveraged by [AssetManager].
 */
@API public interface AssetResolver {
  /**
   * Return the asset module corresponding to the provided [moduleId], if possible, or return `null` to indicate that
   * the asset could not be located.
   *
   * @param moduleId ID for the asset module, assigned by the developer.
   * @return Resolved server asset, or `null`, indicating that the asset could not be located.
   */
  public fun findByModuleId(moduleId: AssetModuleId): ServerAsset?

  /**
   * Resolve the provided [path] to a server asset, if possible, or return `null` to indicate that the asset could not
   * be located; the given [path] value can be prefixed with the asset serving prefix (`/_/asset` by default) or not
   * prefixed at all.
   *
   * **Note:** Only asset tags should be used to serve resources at runtime in production circumstances.
   *
   * @param path Path for the asset which we should resolve.
   * @return Resolved server asset, or `null`, indicating that the asset could not be located.
   */
  public fun resolve(path: String): ServerAsset?

  /**
   * Resolve the provided HTTP [request] to an asset path string, and then resolve the asset path string to a loaded
   * [ServerAsset], if possible; return `null` if the asset cannot be located.
   *
   * @param request HTTP request for the asset which we should resolve.
   * @return Resolved server-held asset, or `null` to indicate that the asset could not be located.
   */
  public fun resolve(request: HttpRequest<*>): ServerAsset? {
    return resolve(request.path)
  }
}
