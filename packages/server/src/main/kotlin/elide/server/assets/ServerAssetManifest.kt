package elide.server.assets

import tools.elide.assets.AssetBundle
import java.util.*
import elide.server.AssetModuleId
import elide.server.AssetTag

/**
 * Loaded/interpreted asset manifest data structure.
 *
 * @param bundle De-serialized asset bundle, embedded in the application.
 * @param moduleIndex Sorted map (index) of asset module IDs to asset pointers.
 * @param tagIndex Sorted map (index) of asset tags to content payload indexes.
 */
internal class ServerAssetManifest(
  internal val bundle: AssetBundle,
  internal val moduleIndex: SortedMap<AssetModuleId, AssetPointer>,
  internal val tagIndex: SortedMap<AssetTag, Int>,
)
