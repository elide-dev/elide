package elide.tool.project.codecs

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import jakarta.inject.Singleton
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import elide.tool.project.ProjectEcosystem
import elide.tool.project.manifest.ElidePackageManifest
import elide.tool.project.manifest.PythonRequirementsManifest

@Singleton @ManifestCodec(ProjectEcosystem.PythonRequirements)
class PythonRequirementsManifestCodec : PackageManifestCodec<PythonRequirementsManifest> {
  override fun defaultPath(): Path = Path("$DEFAULT_NAME.$DEFAULT_EXTENSION")
  override fun supported(path: Path): Boolean {
    return path.nameWithoutExtension == DEFAULT_NAME && path.extension == DEFAULT_EXTENSION
  }

  override fun parse(source: InputStream): PythonRequirementsManifest {
    return source.bufferedReader().useLines {
      PythonRequirementsManifest(it.toList())
    }
  }

  override fun write(manifest: PythonRequirementsManifest, output: OutputStream) {
    output.bufferedWriter().use { writer ->
      manifest.dependencies.forEach { writer.appendLine(it) }
    }
  }

  override fun fromElidePackage(source: ElidePackageManifest): PythonRequirementsManifest {
    return PythonRequirementsManifest(
      dependencies = source.dependencies.pip.packages.map { it.name },
    )
  }

  override fun toElidePackage(source: PythonRequirementsManifest): ElidePackageManifest {
    return ElidePackageManifest(
      dependencies = ElidePackageManifest.DependencyResolution(
        pip = ElidePackageManifest.PipDependencies(
          source.dependencies.map { ElidePackageManifest.PipPackage(it) },
        ),
      ),
    )
  }

  companion object {
    const val DEFAULT_EXTENSION = "txt"
    const val DEFAULT_NAME = "requirements"
  }
}
