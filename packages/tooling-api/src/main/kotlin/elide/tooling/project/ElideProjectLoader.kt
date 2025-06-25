package elide.tooling.project

import elide.tooling.lockfile.LockfileLoader
import java.nio.file.Path

/**
 * Loader which transforms a regular Elide project into a configured Elide project.
 */
public interface ElideProjectLoader {
  /**
   * Factory for producing source sets.
   */
  public val sourceSetFactory: SourceSetFactory

  /**
   * Elide's binary resources path.
   */
  public val resourcesPath: Path

  /**
   * Lockfile provider.
   */
  public val lockfileLoader: LockfileLoader
}
