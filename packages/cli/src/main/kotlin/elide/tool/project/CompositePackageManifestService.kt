package elide.tool.project

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import jakarta.inject.Singleton
import kotlin.io.path.inputStream
import elide.tool.project.codecs.ManifestCodec
import elide.tool.project.codecs.PackageManifestCodec
import elide.tool.project.manifest.*

@Singleton
class CompositePackageManifestService(
  @ManifestCodec(ProjectEcosystem.Elide)
  private val elideCodec: PackageManifestCodec<ElidePackageManifest>,
  @ManifestCodec(ProjectEcosystem.Node)
  private val nodeCodec: PackageManifestCodec<NodePackageManifest>,
  @ManifestCodec(ProjectEcosystem.Python)
  private val pyProjectCodec: PackageManifestCodec<PyProjectManifest>,
  @ManifestCodec(ProjectEcosystem.PythonRequirements)
  private val pythonRequirementsCodec: PackageManifestCodec<PythonRequirementsManifest>,
) : PackageManifestService {

  private val allCodecs = sequenceOf(
    elideCodec,
    nodeCodec,
    pyProjectCodec,
    pythonRequirementsCodec,
  )

  private fun codecForEcosystem(ecosystem: ProjectEcosystem): PackageManifestCodec<PackageManifest> = when (ecosystem) {
    ProjectEcosystem.Elide -> elideCodec
    ProjectEcosystem.Node -> nodeCodec
    ProjectEcosystem.Python -> pyProjectCodec
    ProjectEcosystem.PythonRequirements -> pythonRequirementsCodec
    ProjectEcosystem.Ruby -> error("Ruby environments are not supported yet")
  } as PackageManifestCodec<PackageManifest>

  private fun codecForManifest(manifest: PackageManifest): PackageManifestCodec<PackageManifest> = when (manifest) {
    is ElidePackageManifest -> elideCodec
    is NodePackageManifest -> nodeCodec
    is PyProjectManifest -> pyProjectCodec
    is PythonRequirementsManifest -> pythonRequirementsCodec
  } as PackageManifestCodec<PackageManifest>

  override fun resolve(root: Path, ecosystem: ProjectEcosystem): Path {
    return root.resolve(codecForEcosystem(ecosystem).defaultPath())
  }

  override fun parse(source: Path): PackageManifest {
    return allCodecs.first { it.supported(source) }.let { codec ->
      source.inputStream().use(codec::parse)
    }
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

  override fun encode(manifest: PackageManifest, ecosystem: ProjectEcosystem, output: OutputStream) {
    val codec = codecForManifest(manifest)
    codec.write(manifest, output)
  }
}
