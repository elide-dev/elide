package elide.server.assets

import com.google.protobuf.ByteString
import tools.elide.crypto.HashAlgorithm
import tools.elide.data.CompressionMode
import elide.server.AssetModuleId

/**
 * Intermediary class which represents an asset that has been fully prepared to serve to an end-user request, including
 * any headers which should apply to the response.
 *
 * @param module Asset module which was rendered to produce this record.
 * @param type Type of asset being served.
 * @param variant Compression mode for this asset response.
 * @param headers Headers to apply to this asset response.
 * @param size Size of the data expected from this asset variant.
 * @param lastModified Unix epoch timestamp indicating when this asset was last modified.
 * @param digest Raw bytes of the attached digest for this asset.
 * @param producer Data payload callable for this asset response.
 */
public class RenderedAsset(
  public val module: AssetModuleId,
  public val type: AssetType,
  public val variant: CompressionMode,
  public val headers: Map<String, String>,
  public val size: Long,
  public val lastModified: Long,
  public val digest: Pair<HashAlgorithm, ByteString>?,
  public val producer: () -> ByteString,
)
