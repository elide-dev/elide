package elide.tool.project

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import elide.tool.project.manifest.*

interface PackageManifestService {
  fun resolve(root: Path, ecosystem: ProjectEcosystem = ProjectEcosystem.Elide): Path

  fun parse(source: Path): PackageManifest

  fun parse(source: InputStream, ecosystem: ProjectEcosystem): PackageManifest

  fun merge(manifests: List<PackageManifest>): ElidePackageManifest

  fun export(manifest: ElidePackageManifest, ecosystem: ProjectEcosystem): PackageManifest

  fun encode(manifest: PackageManifest, ecosystem: ProjectEcosystem, output: OutputStream)
}

