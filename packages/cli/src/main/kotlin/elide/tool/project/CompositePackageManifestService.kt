package elide.tool.project

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import jakarta.inject.Provider
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.codecs.ManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.*

@Singleton
class CompositePackageManifestService @Inject constructor (
  @ManifestCodec(ProjectEcosystem.Elide)
  private val elideCodecProvider: Provider<PackageManifestCodec<ElidePackageManifest>>,
  @ManifestCodec(ProjectEcosystem.Node)
  private val nodeCodecProvider: Provider<PackageManifestCodec<NodePackageManifest>>,
  @ManifestCodec(ProjectEcosystem.Python)
  private val pyProjectCodecProvider: Provider<PackageManifestCodec<PyProjectManifest>>,
  @ManifestCodec(ProjectEcosystem.PythonRequirements)
  private val pythonRequirementsCodecProvider: Provider<PackageManifestCodec<PythonRequirementsManifest>>,
  @ManifestCodec(ProjectEcosystem.MavenPom)
  private val mavenPomCodecProvider: Provider<PackageManifestCodec<MavenPomManifest>>,
) : PackageManifestService {

  private val elideCodec by lazy { elideCodecProvider.get() }
  private val nodeCodec by lazy { nodeCodecProvider.get() }
  private val pyProjectCodec by lazy { pyProjectCodecProvider.get() }
  private val pythonRequirementsCodec by lazy { pythonRequirementsCodecProvider.get() }
  private val mavenPomCodec by lazy { mavenPomCodecProvider.get() }

  private val allCodecs by lazy {
    sequenceOf(
      elideCodec,
      nodeCodec,
      pyProjectCodec,
      pythonRequirementsCodec,
      mavenPomCodec,
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun codecForEcosystem(ecosystem: ProjectEcosystem): PackageManifestCodec<PackageManifest> = when (ecosystem) {
    ProjectEcosystem.Elide -> elideCodec
    ProjectEcosystem.Node -> nodeCodec
    ProjectEcosystem.Python -> pyProjectCodec
    ProjectEcosystem.PythonRequirements -> pythonRequirementsCodec
    ProjectEcosystem.Ruby -> error("Ruby environments are not supported yet")
    ProjectEcosystem.MavenPom -> error("Maven POMs are not supported yet")
  } as PackageManifestCodec<PackageManifest>

  @Suppress("UNCHECKED_CAST")
  private fun codecForManifest(manifest: PackageManifest): PackageManifestCodec<PackageManifest> = when (manifest) {
    is ElidePackageManifest -> elideCodec
    is NodePackageManifest -> nodeCodec
    is PyProjectManifest -> pyProjectCodec
    is PythonRequirementsManifest -> pythonRequirementsCodec
    is MavenPomManifest -> mavenPomCodec
  } as PackageManifestCodec<PackageManifest>

  override fun resolve(root: Path, ecosystem: ProjectEcosystem): Path {
    return root.resolve(codecForEcosystem(ecosystem).defaultPath())
  }

  override fun parse(source: Path): PackageManifest {
    return allCodecs.first { it.supported(source) }.parseAsFile(source)
  }

  override fun parse(source: InputStream, ecosystem: ProjectEcosystem): PackageManifest {
    return codecForEcosystem(ecosystem).parse(source)
  }

  override fun merge(manifests: Iterable<PackageManifest>): ElidePackageManifest {
    return manifests.fold(ElidePackageManifest()) { merged, manifest ->
      val codec = codecForManifest(manifest)
      merged.merge(codec.toElidePackage(manifest))
    }
  }

  override fun export(manifest: ElidePackageManifest, ecosystem: ProjectEcosystem): PackageManifest {
    return codecForEcosystem(ecosystem).fromElidePackage(manifest)
  }

  override fun encode(manifest: PackageManifest, output: OutputStream) {
    val codec = codecForManifest(manifest)
    codec.write(manifest, output)
  }
}
