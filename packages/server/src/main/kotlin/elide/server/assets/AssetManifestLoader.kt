package elide.server.assets

import tools.elide.assets.AssetBundle
import tools.elide.assets.ManifestFormat
import java.io.InputStream
import elide.annotations.API
import elide.server.assets.AssetManifestLoader.Companion.assetManifestCandidates

/**
 * Defines the API surface of a loader which is capable of searching for an asset manifest at server startup, and then
 * de-serializing the manifest, so it can be indexed and then made available via an [AssetManager] implementation.
 *
 * The manifest loader is responsible only for two things:
 * 1. **Locating** the manifest on the classpath, if it exists, from a given or default set of candidate locations.
 * 2. **De-serializing** the manifest from some known format, and producing the result as an [AssetBundle].
 *
 * At server startup, the default set of manifest locations ([assetManifestCandidates]) is searched unless otherwise
 * specified by the developer.
 *
 * ### Implementation
 *
 * Typically, this interface is implemented by [ServerAssetManifestProvider]. The developer may replace this default
 * implementation using the `@Replaces` annotation from Micronaut. For example:
 *
 * ```kotlin
 * @Replaces(ServerAssetManifestProvider::class)
 * class MyAssetManifestProvider : AssetManifestLoader {
 *   // implementation here
 * }
 * ```
 */
@API public interface AssetManifestLoader {
  public companion object {
    private const val assetRoot = "/assets"
    private val assetManifestCandidates = listOf(
      ManifestFormat.BINARY to "$assetRoot/app.assets.pb",
      ManifestFormat.JSON to "$assetRoot/app.assets.pb.json",
    )
  }

  /**
   * Find and load an asset manifest embedded within the scope of the current application, using the provided set of
   * [candidates] as locations in the classpath to search.
   *
   * Each pair carries a [ManifestFormat] and path, indicating the expected format of the bundle.
   *
   * @param candidates Candidate locations for the server asset manifest.
   * @return Decoded [AssetBundle] from one of the candidate locations; which location is lost, and any decoding errors
   *   are returned as `null`.
   */
  public fun findLoadManifest(candidates: List<Pair<ManifestFormat, String>>): AssetBundle?

  /**
   * Find and load an asset manifest embedded within the scope of the current application; search in the default path
   * locations, and if no manifest is found, or no manifest could safely be decoded, return `null`.
   *
   * @return Decoded asset manifest, or `null` if no manifest could be found, or a manifest was found and could not be
   *   safely de-serialized.
   */
  public fun findLoadManifest(): AssetBundle? {
    return findLoadManifest(
      assetManifestCandidates
    )
  }

  /**
   * Find an asset manifest present as a resource in the application classpath from the set of provided [candidates],
   * each of which represents a potential path and [ManifestFormat].
   *
   * If a manifest is found at any of the candidate locations, a pair of [ManifestFormat]/[InputStream] is returned,
   * which can be used to load a de-serializer and run it. If no manifest is found or anything else goes wrong (i.e. a
   * manifest is found but it is in the wrong format), `null` is returned.
   *
   * @param candidates Candidate paths and [ManifestFormat] values where we should search for the manifest.
   * @return Pair of the located [ManifestFormat] and an [InputStream], or `null` if no manifest was found.
   */
  public fun findManifest(candidates: List<Pair<ManifestFormat, String>>): Pair<ManifestFormat, InputStream>?

  /**
   * Find an asset manifest embedded within the scope of the current application; search in the default path locations,
   * and if no manifest is found, return `null`; otherwise, return the expected format of the found manifest, and an
   * input stream to consume it.
   *
   * @return Pair carrying the found [ManifestFormat] and an [InputStream] to consume it, or `null` if no manifest could
   *   be located in the default classpath resource locations.
   */
  public fun findManifest(): Pair<ManifestFormat, InputStream>? {
    return findManifest(
      assetManifestCandidates
    )
  }
}
