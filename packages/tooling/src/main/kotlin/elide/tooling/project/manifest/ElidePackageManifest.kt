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
import kotlinx.serialization.Serializable
import elide.tool.Argument
import elide.tool.MutableArguments
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.manifest.ElidePackageManifest.*

@JvmRecord @Serializable
public data class ElidePackageManifest(
  val name: String? = null,
  val version: String? = null,
  val description: String? = null,
  val entrypoint: List<String>? = null,
  val workspaces: List<String> = emptyList(),
  val scripts: Map<String, String> = emptyMap(),
  val dependencies: DependencyResolution = DependencyResolution(),
  val javascript: JavaScriptSettings? = null,
  val typescript: TypeScriptSettings? = null,
  val jvm: JvmSettings? = null,
  val kotlin: KotlinSettings? = null,
  val python: PythonSettings? = null,
  val ruby: RubySettings? = null,
  val pkl: PklSettings? = null,
  val sources: Map<String, SourceSet> = emptyMap(),
  val tests: TestingSettings? = null,
) : PackageManifest {
  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.Elide

  @JvmRecord @Serializable public data class SourceSet(
    val spec: List<String>,
  ) {
    public companion object {
      @JvmStatic public fun parse(str: String): SourceSet {
        return SourceSet(listOf(str))
      }
    }
  }

  public sealed interface DependencyEcosystemConfig {
    public sealed interface PackageSpec
    public sealed interface RepositorySpec
  }

  @JvmRecord @Serializable public data class NpmDependencies(
    val packages: List<NpmPackage> = emptyList(),
    val devPackages: List<NpmPackage> = emptyList(),
    val repositories: Map<String, NpmRepository> = emptyMap(),
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
    val group: String? = null,
    val name: String? = null,
    val version: String? = null,
    val repository: String? = null,
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
            repository = if ('@' in str) str.substringAfterLast('@') else null,
          )
          2 -> MavenPackage(
            group = str.substringBefore(':'),
            name = str.substringAfter(':').substringBefore(':'),
            version = str.substringAfterLast(':'),
            coordinate = str,
            repository = if ('@' in str) str.substringAfterLast('@') else null,
          )
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
      var result = group?.hashCode() ?: 0
      result = 31 * result + (name?.hashCode() ?: 0)
      result = 31 * result + (version?.hashCode() ?: 0)
      result = 31 * result + coordinate.hashCode()
      result = 31 * result + (repository?.hashCode() ?: 0)
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

  @JvmRecord @Serializable public data class MavenDependencies(
    val packages: List<MavenPackage> = emptyList(),
    val testPackages: List<MavenPackage> = emptyList(),
    val catalogs: List<GradleCatalog> = emptyList(),
    val repositories: Map<String, MavenRepository> = emptyMap(),
    val enableDefaultRepositories: Boolean = true,
  ) : DependencyEcosystemConfig {
    public fun hasPackages(): Boolean = packages.isNotEmpty() || testPackages.isNotEmpty()
    public fun allPackages(): Sequence<MavenPackage> = (packages.asSequence() + testPackages.asSequence()).distinct()
  }

  @JvmRecord @Serializable public data class PipDependencies(
    val packages: List<PipPackage> = emptyList(),
    val optionalPackages: Map<String, List<PipPackage>> = emptyMap(),
  ) : DependencyEcosystemConfig

  @JvmRecord @Serializable public data class PipPackage(
    val name: String,
  ) : DependencyEcosystemConfig.PackageSpec

  @JvmRecord @Serializable public data class GemDependencies(
    val packages: List<GemPackage> = emptyList(),
    val devPackages: List<GemPackage> = emptyList(),
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
    @JvmInline @Serializable public value class NumericJvmTarget(public val number: UInt) : JvmTarget
    @JvmInline @Serializable public value class StringJvmTarget(public val name: String) : JvmTarget
  }

  @JvmRecord @Serializable public data class JvmSettings(
    val target: JvmTarget? = null,
    val javaHome: String? = null,
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
    IGNORE
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
    /**
     * Amend the provided [args] with Kotlin Compiler option arguments.
     *
     * @param args Mutable arguments to amend.
     */
    public fun amend(args: MutableArguments) {
      // opt-ins
      args.addAll(optIn.map { Argument.of("-Xopt-in" to it) })

      // compiler options
      if (progressiveMode) args.add(Argument.of("-progressive"))
      if (extraWarnings) args.add(Argument.of("-Wextra"))
      if (allWarningsAsErrors) args.add(Argument.of("-Werror"))
      if (suppressWarnings) args.add(Argument.of("-nowarn"))
      if (verbose) args.add(Argument.of("-verbose"))
      if (apiVersion != "auto") args.add(Argument.of("-api-version" to apiVersion))
      if (languageVersion != "auto") args.add(Argument.of("-language-version" to languageVersion))
      if (freeCompilerArgs.isNotEmpty()) {
        args.addAll(freeCompilerArgs.map { Argument.of(it) })
      }
    }
  }

  @JvmRecord @Serializable public data class KotlinFeatureOptions(
    val injection: Boolean = true,
    val testing: Boolean = true,
    val kotlinx: Boolean = true,
    val serialization: Boolean = kotlinx,
    val coroutines: Boolean = kotlinx,
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
  )

  @JvmRecord @Serializable public data class RubySettings(
    val debug: Boolean = false,
  )

  @JvmRecord @Serializable public data class PklSettings(
    val debug: Boolean = false,
  )

  @JvmRecord @Serializable public data class CoverageSettings(
    val enabled: Boolean = false,
  )

  @JvmRecord @Serializable public data class TestingSettings(
    val coverage: CoverageSettings = CoverageSettings(),
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
    dependencies = dependencies.merge(other.dependencies),
    jvm = (other.jvm ?: jvm),
    kotlin = (other.kotlin ?: kotlin),
    python = (other.python ?: python),
    ruby = (other.ruby ?: ruby),
    pkl = (other.pkl ?: pkl),
  )
}
