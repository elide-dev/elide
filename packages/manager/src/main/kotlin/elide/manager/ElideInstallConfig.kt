package elide.manager

import kotlinx.serialization.Serializable

/**
 * Configuration for install management.
 *
 * @property installDirs Paths to directories that Elide may be installed to.
 * @property searchDirs Paths to directories that Elide may be installed to, but should not be installed to with install
 * management.
 * @property repositories Repositories to search for versions of Elide.
 * @property defaultInstallDir Default directory to install Elide versions to.
 * @author Lauri Heino <datafox>
 */
@Serializable
public data class ElideInstallConfig(
  public val installDirs: List<String> = emptyList(),
  public val searchDirs: List<String> = emptyList(),
  public val repositories: List<String> = emptyList(),
  public val defaultInstallDir: String? = null,
)
