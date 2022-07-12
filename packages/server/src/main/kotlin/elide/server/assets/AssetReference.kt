package elide.server.assets

import elide.server.AssetModuleId
import kotlinx.serialization.Serializable

/**
 * Represents a resolved reference to an asset at serving-time, before it is rendered into a link or other tag.
 *
 * @param module ID of the asset module being referenced.
 * @param assetType Type of asset being referenced.
 * @param href Relative link to serve the asset.
 * @param type Type override for the tag, if applicable.
 * @param inline Whether this asset is eligible to be inlined into the page.
 * @param preload Whether this asset is eligible to be preloaded.
 */
@Serializable public data class AssetReference(
  val module: AssetModuleId,
  val assetType: AssetType,
  val href: String,
  val type: String? = null,
  val inline: Boolean = false,
  val preload: Boolean = false,
) : java.io.Serializable {
  internal companion object {
    /** @return [AssetReference] from the specified [pointer]. */
    @JvmStatic internal fun fromPointer(pointer: AssetPointer, uri: String): AssetReference = AssetReference(
      module = pointer.moduleId,
      assetType = pointer.type,
      href = uri,
    )
  }
}
