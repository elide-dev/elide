package elide.tooling.project

import java.nio.file.Path
import elide.tooling.cli.Statics
import elide.tooling.lockfile.ElideLockfile
import elide.tooling.lockfile.LockfileLoader
import elide.tooling.lockfile.loadLockfileSafe

/**
 * Default implementation of [ElideProjectLoader] which uses the default source set factory.
 */
public object DefaultProjectLoader : ElideProjectLoader {
  override val resourcesPath: Path get() = Statics.resourcesPath

  override val lockfileLoader: LockfileLoader
    get() = LockfileLoader { root ->
      loadLockfileSafe(root, ElideLockfile.latest())
    }

  override val sourceSetFactory: SourceSetFactory get() = SourceSetFactory.Default
}
