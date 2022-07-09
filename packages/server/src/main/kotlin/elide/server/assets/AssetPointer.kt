package elide.server.assets

import elide.server.AssetModuleId
import kotlinx.serialization.Serializable

/** Reference to an application-embedded asset. */
@Serializable internal data class AssetPointer(
  val moduleId: AssetModuleId,
  val type: AssetType,
  val index: Int?,
) : java.io.Serializable
