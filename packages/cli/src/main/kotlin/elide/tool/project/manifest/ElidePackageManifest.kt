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
  val jvm: JvmSettings = JvmSettings(),
  val kotlin: KotlinSettings = KotlinSettings(),
) : PackageManifest {
  sealed interface DependencyEcosystemConfig {
    sealed interface PackageSpec
    sealed interface RepositorySpec
  }

  @JvmRecord @Serializable data class NpmDependencies(
    val packages: List<NpmPackage> = emptyList(),
    val devPackages: List<NpmPackage> = emptyList(),
    val repositories: List<NpmRepository> = emptyList(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable data class NpmPackage(
    val name: String,
    val version: String,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable data class NpmRepository(
    val name: String,
    val url: String,
  ) : DependencyEcosystemConfig.RepositorySpec

  @JvmRecord @Serializable data class MavenPackage(
    val group: String?,
    val name: String?,
    val version: String,
    val coordinate: String?,
    val repository: String?,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable data class MavenRepository(
    val name: String,
    val url: String,
  ) : DependencyEcosystemConfig.RepositorySpec

  @JvmRecord @Serializable data class MavenDependencies(
    val packages: List<MavenPackage> = emptyList(),
    val repositories: List<MavenRepository> = emptyList(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable data class PipDependencies(
    val packages: List<PipPackage> = emptyList(),
    val optionalPackages: Map<String, List<PipPackage>> = emptyMap(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable data class PipPackage(
    val name: String,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable data class GemDependencies(
    val packages: List<GemPackage> = emptyList(),
    val devPackages: List<GemPackage> = emptyList(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable data class GemPackage(
    val name: String,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable data class DependencyResolution(
    val maven: MavenDependencies = MavenDependencies(),
    val npm: NpmDependencies = NpmDependencies(),
    val pip: PipDependencies = PipDependencies(),
    val gems: GemDependencies = GemDependencies(),
  )

  @Serializable
  sealed interface JvmTarget {
    @JvmInline @Serializable value class NumericJvmTarget(val number: UInt) : JvmTarget
    @JvmInline @Serializable value class StringJvmTarget(val name: String) : JvmTarget
  }

  @JvmRecord @Serializable data class JvmSettings(
    val jvmTarget: JvmTarget? = null,
    val javaHome: String? = null,
  )

  @Serializable @Suppress("UNUSED") enum class JvmTargetValidationMode {
    WARNING,
    ERROR,
    IGNORE;
  }

  @JvmRecord @Serializable data class KotlinJvmCompilerOptions(
    // Abstract Options
    val optIn: List<String> = emptyList(),
    val progressiveMode: Boolean = false,
    val extraWarnings: Boolean = false,
    val allWarningsAsErrors: Boolean = false,
    val suppressWarnings: Boolean = false,
    val verbose: Boolean = false,
    val freeCompilerArgs: List<String> = emptyList(),
    val apiVersion: String = "auto",
    val languageVersion: String = "auto",

    // JVM Options
    val javaParameters: Boolean = false,
    val jvmTarget: JvmTarget? = null,
    val noJdk: Boolean = false,
    val jvmTargetValidationMode: JvmTargetValidationMode = JvmTargetValidationMode.ERROR,
  )

  @JvmRecord @Serializable data class KotlinFeatureOptions(
    val injection: Boolean = true,
    val kotlinx: Boolean = true,
    val serialization: Boolean = kotlinx,
    val coroutines: Boolean = kotlinx,
    val autoClasspath: Boolean = true,
  )

  @JvmRecord @Serializable data class KotlinSettings(
    val apiLevel: String = "auto",
    val languageLevel: String = "auto",
    val compilerOptions: KotlinJvmCompilerOptions = KotlinJvmCompilerOptions(),
    val features: KotlinFeatureOptions = KotlinFeatureOptions(),
  )
}

fun NpmDependencies.merge(other: NpmDependencies): NpmDependencies {
  return NpmDependencies(
    packages = packages.union(other.packages).toList(),
    devPackages = devPackages.union(other.devPackages).toList(),
    repositories = repositories.union(other.repositories).toList(),
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

fun MavenDependencies.merge(other: MavenDependencies): MavenDependencies {
  return MavenDependencies(
    packages = packages.union(other.packages).toList(),
    repositories = repositories.union(other.repositories).toList(),
  )
}

fun DependencyResolution.merge(other: DependencyResolution): DependencyResolution {
  return DependencyResolution(
    npm = npm.merge(other.npm),
    pip = pip.merge(other.pip),
    gems = gems.merge(other.gems),
    maven = maven.merge(other.maven),
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
    jvm = jvm.copy(
      javaHome = jvm.javaHome ?: other.jvm.javaHome,
      jvmTarget = jvm.jvmTarget ?: other.jvm.jvmTarget,
    ),
    kotlin = kotlin.copy(
      apiLevel = kotlin.apiLevel.takeIf { it != "auto" } ?: other.kotlin.apiLevel,
      languageLevel = kotlin.languageLevel.takeIf { it != "auto" } ?: other.kotlin.languageLevel,
      compilerOptions = kotlin.compilerOptions.copy(
        jvmTarget = kotlin.compilerOptions.jvmTarget ?: other.kotlin.compilerOptions.jvmTarget,
        javaParameters = kotlin.compilerOptions.javaParameters || other.kotlin.compilerOptions.javaParameters,
        noJdk = kotlin.compilerOptions.noJdk || other.kotlin.compilerOptions.noJdk,
        extraWarnings = kotlin.compilerOptions.extraWarnings || other.kotlin.compilerOptions.extraWarnings,
        suppressWarnings = kotlin.compilerOptions.suppressWarnings || other.kotlin.compilerOptions.suppressWarnings,
        verbose = kotlin.compilerOptions.verbose || other.kotlin.compilerOptions.verbose,
        allWarningsAsErrors = (
          kotlin.compilerOptions.allWarningsAsErrors || other.kotlin.compilerOptions.allWarningsAsErrors
        ),
      ),
      features = kotlin.features.copy(
        injection = kotlin.features.injection && other.kotlin.features.injection,
        kotlinx = kotlin.features.kotlinx && other.kotlin.features.kotlinx,
        serialization = kotlin.features.serialization && other.kotlin.features.serialization,
        coroutines = kotlin.features.coroutines && other.kotlin.features.coroutines,
        autoClasspath = kotlin.features.autoClasspath && other.kotlin.features.autoClasspath,
      )
    )
  )
}
