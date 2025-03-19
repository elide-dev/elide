package elide.tool.project.manifest

import kotlinx.serialization.Serializable
import elide.tool.project.manifest.ElidePackageManifest.*

@JvmRecord @Serializable
data class ElidePackageManifest(
  val name: String? = null,
  val version: String? = null,
  val description: String? = null,
  val entrypoint: String? = null,
  val scripts: Map<String, String> = emptyMap(),
  val dependencies: DependencyResolution = DependencyResolution(),
) : PackageManifest {
  sealed interface DependencyEcosystemConfig {
    sealed interface PackageSpec
  }

  @JvmRecord @Serializable data class NpmDependencies(
    val packages: List<NpmPackage> = emptyList(),
    val devPackages: List<NpmPackage> = emptyList(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable data class NpmPackage(
    val name: String,
    val version: String,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable data class PipDependencies(
    val packages: List<PipPackage> = emptyList(),
    val optionalPackages: Map<String, List<PipPackage>> = emptyMap(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable data class PipPackage(
    val name: String,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable data class GemDependencies(
    val packages: List<NpmPackage> = emptyList(),
    val devPackages: List<NpmPackage> = emptyList(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable data class GemPackage(
    val name: String,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable data class DependencyResolution(
    val npm: NpmDependencies = NpmDependencies(),
    val pip: PipDependencies = PipDependencies(),
    val gem: GemDependencies = GemDependencies(),
  )
}

fun NpmDependencies.merge(other: NpmDependencies): NpmDependencies {
  return NpmDependencies(
    packages = packages.union(other.packages).toList(),
    devPackages = devPackages.union(other.devPackages).toList(),
  )
}

fun PipDependencies.merge(other: PipDependencies): PipDependencies {
  return PipDependencies(
    packages = packages.union(other.packages).toList(),
  )
}

fun GemDependencies.merge(other: GemDependencies): GemDependencies {
  return GemDependencies(
    packages = packages.union(other.packages).toList(),
  )
}

fun DependencyResolution.merge(other: DependencyResolution): DependencyResolution {
  return DependencyResolution(
    npm = npm.merge(other.npm),
    pip = pip.merge(other.pip),
    gem = gem.merge(other.gem),
  )
}

fun ElidePackageManifest.merge(other: ElidePackageManifest): ElidePackageManifest {
  return ElidePackageManifest(
    name = name ?: other.name,
    version = version ?: other.version,
    description = description ?: other.description,
    entrypoint = entrypoint ?: other.entrypoint,
    scripts = scripts + other.scripts,
    dependencies = dependencies.merge(other.dependencies),
  )
}
