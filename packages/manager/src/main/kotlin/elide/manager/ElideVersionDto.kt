package elide.manager

import kotlinx.serialization.Serializable
import elide.runtime.core.HostPlatform
import elide.runtime.version.ElideVersion
import elide.runtime.version.ElideVersionInfo

/**
 * DTO object for a platform-specific version of Elide.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
public data class ElideVersionDto(
  public val version: String,
  public val platform: @Serializable(with = HostPlatformSerializer::class) HostPlatform,
) {
  public val info: ElideVersion by lazy { ElideVersionInfo(version) }
}
