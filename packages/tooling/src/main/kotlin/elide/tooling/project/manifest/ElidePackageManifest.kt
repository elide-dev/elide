/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.tooling.project.manifest

import java.net.URI
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import elide.core.api.Symbolic
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.manifest.ElidePackageManifest.*
import elide.tooling.web.Browsers

// Default Java target version for JVM projects
private const val DEFAULT_JAVA_TARGET = 21u

// Optimization level strings.
private const val OPTIMIZATION_LEVEL_AUTO = "auto"
private const val OPTIMIZATION_LEVEL_BUILD = "b"
private const val OPTIMIZATION_LEVEL_ZERO = "0"
private const val OPTIMIZATION_LEVEL_ONE = "1"
private const val OPTIMIZATION_LEVEL_TWO = "2"
private const val OPTIMIZATION_LEVEL_THREE = "3"
private const val OPTIMIZATION_LEVEL_FOUR = "4"

@Serializable
public data class ElidePackageManifest(
  val name: String? = null,
  val version: String? = null,
  val description: String? = null,
  val entrypoint: List<String>? = null,
  val workspaces: List<String> = emptyList(),
  val scripts: Map<String, String> = emptyMap(),
  val artifacts: Map<String, Artifact> = emptyMap(),
  val dependencies: DependencyResolution = DependencyResolution(),
  val javascript: JavaScriptSettings? = null,
  val typescript: TypeScriptSettings? = null,
  val jvm: JvmSettings? = null,
  val kotlin: KotlinSettings? = null,
  val python: PythonSettings? = null,
  val ruby: RubySettings? = null,
  val pkl: PklSettings? = null,
  val nativeImage: NativeImageSettings? = null,
  val dev: DevSettings? = null,
  val sources: Map<String, SourceSet> = emptyMap(),
  val tests: TestingSettings? = null,
  val lockfile: LockfileSettings? = null,
  val web: WebSettings? = null,
) : PackageManifest {
  @Transient private val workspace: AtomicReference<Pair<Path, ElidePackageManifest>> = AtomicReference(null)

  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.Elide

  public fun within(path: Path, workspace: ElidePackageManifest): ElidePackageManifest = apply {
    this.workspace.set(path to workspace)
  }

  public fun activeWorkspace(): Pair<Path, ElidePackageManifest>? {
    return this.workspace.get()
  }

  @Serializable public sealed interface Artifact {
    public val from: List<String>
    public val dependsOn: List<String>
  }

  @Serializable public data class JarResource(
    val path: String,
    val position: String,
  )

  @Serializable public data class ProjectSourceSpec(
    val platform: String? = null,
    val project: String? = null,
    val subpath: String? = null,
  )

  @Serializable public data class LspSettings(
    val delegates: List<String>? = null,
  )

  @Serializable public data class McpResource(
    val path: String,
    val name: String,
    val description: String = "",
    val mimeType: String? = null,
  )

  @Serializable public data class McpSettings(
    val resources: List<McpResource>? = null,
    val advice: Boolean = true,
    val registerElide: Boolean = true,
  )

  @Serializable public data class DevServerSettings(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
  )

  @Serializable public data class DevSettings(
    val source: ProjectSourceSpec? = null,
    val lsp: LspSettings? = null,
    val mcp: McpSettings? = null,
    val server: DevServerSettings? = null,
  )

  @Serializable public data class Jar(
    val name: String? = null,
    val sources: List<String> = emptyList(),
    val resources: List<JarResource> = emptyList(),
    val manifest: Map<String, String> = emptyMap(),
    val options: JarOptions = JarOptions(),
    override val from: List<String> = emptyList(),
    override val dependsOn: List<String> = emptyList(),
  ) : Artifact

  @Serializable public data class JarOptions(
    val compress: Boolean = true,
    val defaultManifestProperties: Boolean = true,
    val entrypoint: String? = null,
  )

  @Serializable public data class ContainerImage(
    val image: String? = null,
    val base: String? = null,
    val tags: List<String> = emptyList(),
    override val from: List<String> = emptyList(),
    override val dependsOn: List<String> = emptyList(),
  ) : Artifact

  @JvmRecord @Serializable public data class SourceSet(
    val spec: List<String>,
  ) {
    public companion object {
      @JvmStatic public fun parse(str: String): SourceSet {
        return SourceSet(listOf(str))
      }
    }
  }

  @Serializable public data class WebSettings(
    val debug: Boolean = false,
    val css: CssSettings = CssSettings(),
    val browsers: Browsers = Browsers.Defaults,
  )

  @Serializable public data class CssTarget(
    val browser: String,
    val version: String? = null,
  )

  @Serializable public data class CssSettings(
    val minify: Boolean = true,
    val targets: List<CssTarget> = emptyList(),
  )

  public sealed interface DependencyEcosystemConfig {
    public sealed interface PackageSpec
    public sealed interface RepositorySpec
  }

  @JvmRecord @Serializable public data class NpmDependencies(
    val packages: List<NpmPackage> = emptyList(),
    val devPackages: List<NpmPackage> = emptyList(),
    val repositories: Map<String, NpmRepository> = emptyMap(),
    val from: List<String> = emptyList(),
  ) : DependencyEcosystemConfig {
    public fun hasPackages(): Boolean = packages.isNotEmpty() || devPackages.isNotEmpty()
  }

  @JvmRecord @Serializable public data class NpmPackage(
    val name: String,
    val version: String?,
  ) : DependencyEcosystemConfig.PackageSpec {
    public companion object {
      @JvmStatic public fun parse(str: String): NpmPackage {
        val version = str.substringAfterLast('@')
        val name = str.substringBeforeLast('@')
        return NpmPackage(
          name = name,
          version = version.ifEmpty { null },
        )
      }
    }
  }

  @JvmRecord @Serializable public data class NpmRepository(
    val name: String,
    val url: String,
  ) : DependencyEcosystemConfig.RepositorySpec

  @JvmRecord @Serializable public data class GradleCatalog(
    val name: String? = null,
    val path: String,
  ) : Comparable<GradleCatalog> {
    public companion object {
      @JvmStatic public fun parse(str: String): GradleCatalog = GradleCatalog(
        name = str.substringBefore("."),
        path = str,
      )
    }

    override fun compareTo(other: GradleCatalog): Int = path.compareTo(other.path)
  }

  @JvmRecord @Serializable public data class MavenPackage(
    val group: String = "",
    val name: String = "",
    val version: String = "",
    val classifier: String = "",
    val repository: String = "",
    val coordinate: String,
  ) : DependencyEcosystemConfig.PackageSpec, Comparable<MavenPackage> {
    public companion object {
      @JvmStatic public fun parse(str: String): MavenPackage {
        return when (str.count { it == ':' }) {
          0 -> error("Maven package missing group or artifact: '$str'")
          1 -> MavenPackage(
            group = str.substringBefore(':'),
            name = str.substringAfter(':'),
            coordinate = str,
            repository = if ('@' in str) str.substringAfterLast('@') else "",
          )

          2 -> MavenPackage(
            group = str.substringBefore(':'),
            name = str.substringAfter(':').substringBefore(':'),
            version = str.substringAfterLast(':'),
            coordinate = str,
            repository = if ('@' in str) str.substringAfterLast('@') else "",
          )

          3 -> str.split(':').let { split ->
            MavenPackage(
              group = split.first(),
              name = split[1],
              classifier = split[2],
              version = str.substringAfterLast(':'),
              coordinate = str,
              repository = if ('@' in str) str.substringAfterLast('@') else "",
            )
          }

          else -> error("Too many separators in Maven coordinate: '$str'")
        }
      }
    }

    override fun compareTo(other: MavenPackage): Int = coordinate.compareTo(other.coordinate)

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as MavenPackage

      if (group != other.group) return false
      if (name != other.name) return false
      if (version != other.version) return false
      if (coordinate != other.coordinate) return false
      if (repository != other.repository) return false

      return true
    }

    override fun hashCode(): Int {
      var result = group.ifBlank { null }?.hashCode() ?: 0
      result = 31 * result + (name.ifBlank { null }?.hashCode() ?: 0)
      result = 31 * result + (version.ifBlank { null }?.hashCode() ?: 0)
      result = 31 * result + coordinate.hashCode()
      result = 31 * result + (repository.ifBlank { null }?.hashCode() ?: 0)
      return result
    }
  }

  @Serializable public data class MavenRepository(
    val url: String,
    var name: String? = null,
    val description: String? = null,
  ) : DependencyEcosystemConfig.RepositorySpec {
    public companion object {
      @JvmStatic public fun parse(str: String): MavenRepository {
        return try {
          URI.create(str)
        } catch (_: IllegalArgumentException) {
          error("Invalid URI for Maven repository: '$str'")
        }.let {
          MavenRepository(
            url = str,
          )
        }
      }
    }
  }

  @JvmRecord @Serializable public data class MavenCoordinates(
    val group: String,
    val name: String,
    val classifier: String? = null,
  )

  @JvmRecord @Serializable public data class MavenDependencies(
    val coordinates: MavenCoordinates? = null,
    val packages: List<MavenPackage> = emptyList(),
    val testPackages: List<MavenPackage> = emptyList(),
    val processors: List<MavenPackage> = emptyList(),
    val catalogs: List<GradleCatalog> = emptyList(),
    val repositories: Map<String, MavenRepository> = emptyMap(),
    val enableDefaultRepositories: Boolean = true,
    val from: List<String> = emptyList(),
  ) : DependencyEcosystemConfig {
    public fun hasPackages(): Boolean = packages.isNotEmpty() || testPackages.isNotEmpty()
    public fun allPackages(): Sequence<MavenPackage> = (packages.asSequence() + testPackages.asSequence()).distinct()
  }

  @JvmRecord @Serializable public data class PipDependencies(
    val packages: List<PipPackage> = emptyList(),
    val optionalPackages: Map<String, List<PipPackage>> = emptyMap(),
  ) : DependencyEcosystemConfig {
    public fun hasPackages(): Boolean = packages.isNotEmpty() || optionalPackages.isNotEmpty()
    public fun allPackages(): Sequence<PipPackage> {
      return (packages.asSequence() + optionalPackages.values.flatten().asSequence()).distinct()
    }
  }

  @JvmRecord @Serializable public data class PipPackage(
    val name: String,
    val version: String? = null,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable public data class GemDependencies(
    val packages: List<GemPackage> = emptyList(),
    val devPackages: List<GemPackage> = emptyList(),
    val from: List<String> = emptyList(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable public data class GemPackage(
    val name: String,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable public data class DependencyResolution(
    val maven: MavenDependencies = MavenDependencies(),
    val npm: NpmDependencies = NpmDependencies(),
    val pip: PipDependencies = PipDependencies(),
    val gems: GemDependencies = GemDependencies(),
  )

  @Serializable
  public sealed interface JvmTarget {
    @JvmRecord @Serializable public data class NumericJvmTarget(public val number: UInt) : JvmTarget {
      override val argValue: String get() = number.toString()
    }

    @JvmRecord @Serializable public data class StringJvmTarget(public val name: String) : JvmTarget {
      override val argValue: String get() = name
    }

    public val argValue: String

    public companion object {
      public val DEFAULT: JvmTarget = NumericJvmTarget(DEFAULT_JAVA_TARGET)
    }
  }

  @JvmRecord @Serializable public data class JvmFeatures(
    val testing: Boolean = true,
  )

  @JvmRecord @Serializable public data class JvmTesting(
    val enabled: Boolean = true,
    val driver: JvmTestDriver = JvmTestDriver.Elide
  ) {
    public enum class JvmTestDriver {
      Elide,
      JUnit,
    }
  }

  @JvmRecord @Serializable public data class JavaCompilerSettings(
    val flags: List<String> = emptyList(),
  )

  @JvmRecord @Serializable public data class JavaLanguage(
    val source: JvmTarget? = null,
    val release: JvmTarget? = null,
    val compiler: JavaCompilerSettings = JavaCompilerSettings(),
  )

  @JvmRecord @Serializable public data class JvmSettings(
    val main: String? = null,
    val target: JvmTarget? = null,
    val javaHome: String? = null,
    val features: JvmFeatures = JvmFeatures(),
    val java: JavaLanguage = JavaLanguage(),
    val flags: List<String> = emptyList(),
  )

  @JvmRecord @Serializable public data class JavaScriptSettings(
    val debug: Boolean = false,
  )

  @JvmRecord @Serializable public data class TypeScriptSettings(
    val debug: Boolean = false,
  )

  @Serializable @Suppress("UNUSED") public enum class JvmTargetValidationMode {
    WARNING,
    ERROR,
    IGNORE,
  }

  @JvmRecord @Serializable public data class KotlinJvmCompilerOptions(
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
  ) {
    public fun collect(): Sequence<String> = sequence {
      // opt-ins
      optIn.forEach { yield("-Xopt-in=$it") }

      // compiler options
      if (progressiveMode) yield("-progressive")
      if (extraWarnings) yield("-Wextra")
      if (allWarningsAsErrors) yield("-Werror")
      if (suppressWarnings) yield("-nowarn")
      if (verbose) yield("-verbose")
      if (apiVersion != "auto") yield("-api-version=$apiVersion")
      if (languageVersion != "auto") yield("-language-version=$languageVersion")
      if (freeCompilerArgs.isNotEmpty()) yieldAll(freeCompilerArgs)
    }
  }

  @JvmRecord @Serializable public data class KotlinFeatureOptions(
    val injection: Boolean = true,
    val testing: Boolean = true,
    val kotlinx: Boolean = true,
    val enableDefaultPlugins: Boolean = true,
    val experimental: Boolean = true,
    val incremental: Boolean = true,
    val serialization: Boolean = kotlinx && enableDefaultPlugins && experimental,
    val coroutines: Boolean = kotlinx,
    val redaction: Boolean = enableDefaultPlugins && experimental,
    val autoClasspath: Boolean = true,
  )

  @JvmRecord @Serializable public data class KotlinSettings(
    val apiLevel: String = "auto",
    val languageLevel: String = "auto",
    val compilerOptions: KotlinJvmCompilerOptions = KotlinJvmCompilerOptions(),
    val features: KotlinFeatureOptions = KotlinFeatureOptions(),
  )

  @JvmRecord @Serializable public data class PythonSettings(
    val debug: Boolean = false,
    val wsgi: WsgiSettings = WsgiSettings(),
  )

  @JvmRecord @Serializable public data class WsgiSettings(
    val app: String? = null,
    val factory: String? = null,
    val args: List<String>? = null,
    val port: Int? = null,
    val workers: Int? = null,
  )

  @JvmRecord @Serializable public data class RubySettings(
    val debug: Boolean = false,
  )

  @JvmRecord @Serializable public data class PklSettings(
    val debug: Boolean = false,
  )

  @JvmRecord @Serializable public data class NativeImageLinkAtBuildTime(
    val enabled: Boolean = true,
    val packages: List<String> = emptyList(),
  )

  @JvmRecord @Serializable public data class NativeImageClassInit(
    val enabled: Boolean = true,
    val buildtime: List<String> = emptyList(),
    val runtime: List<String> = emptyList(),
  )

  @Serializable public enum class OptimizationLevel(override val symbol: String) : Symbolic<String> {
    AUTO(OPTIMIZATION_LEVEL_AUTO),
    BUILD(OPTIMIZATION_LEVEL_BUILD),
    ZERO(OPTIMIZATION_LEVEL_ZERO),
    ONE(OPTIMIZATION_LEVEL_ONE),
    TWO(OPTIMIZATION_LEVEL_TWO),
    THREE(OPTIMIZATION_LEVEL_THREE),
    FOUR(OPTIMIZATION_LEVEL_FOUR);

    public companion object : Symbolic.SealedResolver<String, OptimizationLevel> {
      override fun resolve(symbol: String): OptimizationLevel = when (symbol.lowercase().trim()) {
        OPTIMIZATION_LEVEL_AUTO -> AUTO
        OPTIMIZATION_LEVEL_BUILD -> BUILD
        OPTIMIZATION_LEVEL_ZERO -> ZERO
        OPTIMIZATION_LEVEL_ONE -> ONE
        OPTIMIZATION_LEVEL_TWO -> TWO
        OPTIMIZATION_LEVEL_THREE -> THREE
        OPTIMIZATION_LEVEL_FOUR -> FOUR
        else -> throw unresolved(symbol)
      }
    }
  }

  @JvmRecord @Serializable public data class ProfileGuidedOptimization(
    val enabled: Boolean = true,
    val autoprofile: Boolean = false,
    val instrument: Boolean = false,
    val sampling: Boolean = false,
    val profiles: List<String> = emptyList(),
  )

  @JvmRecord @Serializable public data class NativeImageOptions(
    val verbose: Boolean = false,
    val linkAtBuildTime: NativeImageLinkAtBuildTime = NativeImageLinkAtBuildTime(),
    val classInit: NativeImageClassInit = NativeImageClassInit(),
    val optimization: OptimizationLevel = OptimizationLevel.AUTO,
    val pgo: ProfileGuidedOptimization = ProfileGuidedOptimization(),
    val flags: List<String> = emptyList(),
  )

  @JvmRecord @Serializable public data class NativeImageSettings(
    val verbose: Boolean = false,
  )

  @Serializable public enum class NativeImageType(override val symbol: String) : Symbolic<String> {
    BINARY("binary"),
    LIBRARY("library");

    public companion object : Symbolic.SealedResolver<String, NativeImageType> {
      override fun resolve(symbol: String): NativeImageType = when (symbol.lowercase().trim()) {
        "binary" -> BINARY
        "library" -> LIBRARY
        else -> throw unresolved(symbol)
      }
    }
  }

  @JvmRecord @Serializable public data class NativeImage(
    val name: String? = null,
    val type: NativeImageType = NativeImageType.BINARY,
    val entrypoint: String? = null,
    val moduleName: String? = null,
    val options: NativeImageOptions = NativeImageOptions(),
    override val from: List<String> = emptyList(),
    override val dependsOn: List<String> = emptyList(),
  ) : Artifact

  @JvmRecord @Serializable public data class StaticSite(
    val srcs: String,
    val prefix: String = "/",
    val assets: String? = null,
    val domain: String? = null,
    val preview: String? = null,
    val stylesheets: List<String> = emptyList(),
    val scripts: List<String> = emptyList(),
    val hosting: String? = null,
    override val from: List<String> = emptyList(),
    override val dependsOn: List<String> = emptyList(),
  ) : Artifact

  @JvmRecord @Serializable public data class CoverageSettings(
    val enabled: Boolean = false,
    val paths: List<String> = emptyList(),
  )

  @JvmRecord @Serializable public data class TestingSettings(
    val coverage: CoverageSettings = CoverageSettings(),
    val jvm: JvmTesting = JvmTesting(),
  )

  @JvmRecord @Serializable public data class LockfileSettings(
    val enabled: Boolean = true,
    val format: String = "auto",
  )
}

public fun NpmDependencies.merge(other: NpmDependencies): NpmDependencies {
  return NpmDependencies(
    packages = packages.union(other.packages).toList(),
    devPackages = devPackages.union(other.devPackages).toList(),
    repositories = repositories.plus(other.repositories),
  )
}

public fun PipDependencies.merge(other: PipDependencies): PipDependencies {
  return PipDependencies(
    packages = packages.union(other.packages).toList(),
  )
}

public fun GemDependencies.merge(other: GemDependencies): GemDependencies {
  return GemDependencies(
    packages = packages.union(other.packages).toList(),
  )
}

public fun MavenDependencies.merge(other: MavenDependencies): MavenDependencies {
  return MavenDependencies(
    packages = packages.union(other.packages).toList(),
    testPackages = testPackages.union(other.testPackages).toList(),
    processors = processors.union(other.processors).toList(),
    repositories = repositories.plus(other.repositories),
  )
}

public fun DependencyResolution.merge(other: DependencyResolution): DependencyResolution {
  return DependencyResolution(
    npm = npm.merge(other.npm),
    pip = pip.merge(other.pip),
    gems = gems.merge(other.gems),
    maven = maven.merge(other.maven),
  )
}

public fun ElidePackageManifest.merge(other: ElidePackageManifest): ElidePackageManifest {
  return ElidePackageManifest(
    name = name ?: other.name,
    version = version ?: other.version,
    description = description ?: other.description,
    entrypoint = (entrypoint ?: emptyList()).plus(other.entrypoint ?: emptyList()).distinct(),
    scripts = scripts + other.scripts,
    artifacts = artifacts + other.artifacts,
    dependencies = dependencies.merge(other.dependencies),
    jvm = (other.jvm ?: jvm),
    kotlin = (other.kotlin ?: kotlin),
    python = (other.python ?: python),
    ruby = (other.ruby ?: ruby),
    pkl = (other.pkl ?: pkl),
    nativeImage = (other.nativeImage ?: nativeImage),
  )
}
