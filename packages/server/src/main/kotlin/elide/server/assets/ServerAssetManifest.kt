package elide.server.assets

import elide.server.AssetModuleId
import elide.server.AssetTag
import tools.elide.assets.AssetBundle
import java.util.SortedMap

/** Loaded/interpreted asset manifest data structure. */
internal class ServerAssetManifest(
  internal val bundle: AssetBundle,
  internal val moduleIndex: SortedMap<AssetModuleId, AssetPointer>,
  internal val tagIndex: SortedMap<AssetTag, Int>,
)
