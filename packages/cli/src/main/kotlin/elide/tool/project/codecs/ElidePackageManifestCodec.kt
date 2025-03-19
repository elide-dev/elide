package elide.tool.project.codecs

import org.pkl.config.java.ConfigEvaluator
import org.pkl.config.kotlin.forKotlin
import org.pkl.config.kotlin.to
import org.pkl.core.ModuleSource
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import jakarta.inject.Singleton
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import elide.tool.project.ProjectEcosystem
import elide.tool.project.manifest.ElidePackageManifest

@Singleton @ManifestCodec(ProjectEcosystem.Elide)
class ElidePackageManifestCodec : PackageManifestCodec<ElidePackageManifest> {
  override fun defaultPath(): Path = Path("$DEFAULT_NAME.$DEFAULT_EXTENSION")

  override fun supported(path: Path): Boolean {
    return path.nameWithoutExtension == DEFAULT_NAME && path.extension == DEFAULT_EXTENSION
  }

  override fun parse(source: InputStream): ElidePackageManifest {
    val manifestText = source.bufferedReader().use { it.readText() }
    val config = ConfigEvaluator.preconfigured().forKotlin().use { it.evaluate(ModuleSource.text(manifestText)) }

    return config.to<ElidePackageManifest>()
  }

  override fun write(manifest: ElidePackageManifest, output: OutputStream) {
    error("Writing elide manifests is not yet supported")
  }

  override fun fromElidePackage(source: ElidePackageManifest): ElidePackageManifest = source.copy()
  override fun toElidePackage(source: ElidePackageManifest): ElidePackageManifest = source.copy()

  companion object {
    const val DEFAULT_EXTENSION = "pkl"
    const val DEFAULT_NAME = "elide"
  }
}
