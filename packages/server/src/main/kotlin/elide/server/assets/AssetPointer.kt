package elide.server.assets

import elide.server.AssetModuleId
import java.util.SortedSet

/**
 * Reference to an application-embedded asset.
 *
 * @param moduleId Developer-assigned ID for this asset module.
 * @param type Type of asset represented by this reference.
 * @param index Index of the asset within the asset content payload list of the active asset bundle.
 */
internal data class AssetPointer(
  val moduleId: AssetModuleId,
  val type: AssetType,
  val index: SortedSet<Int>?,
) : java.io.Serializable
