package elide.tool.project.codecs

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import jakarta.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.codecs.ManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.PyProjectManifest

@Singleton @ManifestCodec(ProjectEcosystem.Python)
class PyProjectManifestCodec : PackageManifestCodec<PyProjectManifest> {
  override fun defaultPath(): Path = Path("$DEFAULT_NAME.$DEFAULT_EXTENSION")
  override fun supported(path: Path): Boolean {
    return path.nameWithoutExtension == DEFAULT_NAME && path.extension == DEFAULT_EXTENSION
  }

  override fun parse(source: InputStream): PyProjectManifest {
    val text = source.bufferedReader().use { it.readText() }
    return ManifestToml.decodeFromString(text)
  }

  override fun write(manifest: PyProjectManifest, output: OutputStream) {
    output.bufferedWriter().use { it.write(ManifestToml.encodeToString(manifest)) }
  }

  override fun fromElidePackage(source: ElidePackageManifest): PyProjectManifest {
    return PyProjectManifest(
      buildSystem = PyProjectManifest.BuildSystemConfig(
        buildBackend = null,
        requires = listOf(),
      ),
      project = PyProjectManifest.ProjectConfig(
        name = source.name ?: DEFAULT_PROJECT_NAME,
        version = source.version,
        description = source.description,
        dependencies = source.dependencies.pip.packages.map { it.name },
        optionalDependencies = source.dependencies.pip.optionalPackages.entries.associate { entry ->
          entry.key to entry.value.map { it.name }
        },
      ),
    )
  }

  override fun toElidePackage(source: PyProjectManifest): ElidePackageManifest {
    return ElidePackageManifest(
      name = source.project.name,
      version = source.project.version,
      description = source.project.description,
      dependencies = ElidePackageManifest.DependencyResolution(
        pip = ElidePackageManifest.PipDependencies(
          packages = source.project.dependencies.map { ElidePackageManifest.PipPackage(it) },
          optionalPackages = source.project.optionalDependencies.mapValues { entry ->
            entry.value.map { ElidePackageManifest.PipPackage(it) }
          },
        ),
      ),
    )
  }

  companion object {
    const val DEFAULT_NAME = "pyproject"
    const val DEFAULT_EXTENSION = "toml"

    const val DEFAULT_PROJECT_NAME = "elide-project"

    private val ManifestToml by lazy {
      Toml(
        inputConfig = TomlInputConfig(ignoreUnknownNames = true),
        outputConfig = TomlOutputConfig(
          indentation = TomlIndentation.NONE,
          ignoreDefaultValues = true,
          ignoreNullValues = true,
          explicitTables = false,
        ),
      )
    }
  }
}
