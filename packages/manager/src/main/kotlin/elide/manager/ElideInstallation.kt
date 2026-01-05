package elide.manager

import kotlinx.serialization.Serializable

/**
 * Defines a single installation of Elide on the current system.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
public data class ElideInstallation(
  val version: ElideVersionDto,
  val path: String,
)
