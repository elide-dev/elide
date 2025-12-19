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
package elide.tooling.project.codecs.adopt

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.name
import kotlin.io.path.writeBytes
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.adopt.PklGenerator
import elide.tooling.project.adopt.gradle.GradleDescriptor
import elide.tooling.project.adopt.gradle.GradleParser
import elide.tooling.project.codecs.ManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.DependencyResolution
import elide.tooling.project.manifest.ElidePackageManifest.MavenDependencies
import elide.tooling.project.manifest.ElidePackageManifest.MavenPackage
import elide.tooling.project.manifest.ElidePackageManifest.MavenRepository
import elide.tooling.project.manifest.PackageManifest

/**
 * Manifest wrapper for Gradle projects parsed via the adopt parser.
 *
 * This wraps a [GradleDescriptor] from the adopt parsing infrastructure
 * and implements [PackageManifest] for integration with the codec system.
 */
public data class AdoptGradleManifest(
  public val descriptor: GradleDescriptor,
  public val path: Path? = null,
) : PackageManifest {
  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.Gradle
}

/**
 * Codec adapter for Gradle build files using the adopt parser infrastructure.
 *
 * This codec leverages [GradleParser] from the adopt package, which provides:
 * - Kotlin DSL (build.gradle.kts) parsing
 * - Groovy DSL (build.gradle) parsing
 * - Version catalog integration (libs.versions.toml)
 * - Multi-module project detection via settings files
 * - Plugin and repository extraction
 *
 * The parsed [GradleDescriptor] is wrapped in [AdoptGradleManifest] and can be
 * converted to [ElidePackageManifest] for cross-ecosystem compatibility.
 */
@ManifestCodec(ProjectEcosystem.Gradle)
public class AdoptGradleCodec : PackageManifestCodec<AdoptGradleManifest> {

  override fun defaultPath(): Path = Path(DEFAULT_FILENAME_KTS)

  override fun supported(path: Path): Boolean {
    val name = path.name
    return name == DEFAULT_FILENAME_KTS ||
      name == DEFAULT_FILENAME_GROOVY ||
      name == "settings.gradle.kts" ||
      name == "settings.gradle"
  }

  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): AdoptGradleManifest {
    // GradleParser requires a file path, so write stream to temp file
    val tempFile = createTempFile(prefix = "build", suffix = ".gradle.kts")
    try {
      tempFile.writeBytes(source.readBytes())
      return parseAsFile(tempFile, state)
    } finally {
      tempFile.toFile().delete()
    }
  }

  override fun parseAsFile(path: Path, state: PackageManifestCodec.ManifestBuildState): AdoptGradleManifest {
    val descriptor = GradleParser.parse(path)
    return AdoptGradleManifest(descriptor = descriptor, path = path)
  }

  override fun write(manifest: AdoptGradleManifest, output: OutputStream) {
    // Generate PKL representation for writing
    val pklContent = PklGenerator.generate(manifest.descriptor)
    output.write(pklContent.toByteArray(Charsets.UTF_8))
  }

  override fun fromElidePackage(source: ElidePackageManifest): AdoptGradleManifest {
    // Convert ElidePackageManifest back to GradleDescriptor
    val descriptor = GradleDescriptor(
      name = source.name ?: "",
      group = source.dependencies.maven.coordinates?.group ?: "",
      version = source.version ?: "0.0.0",
      description = source.description,
      dependencies = source.dependencies.maven.packages.map { pkg ->
        GradleDescriptor.Dependency(
          group = pkg.group,
          name = pkg.name,
          version = pkg.version,
          configuration = "implementation",
        )
      } + source.dependencies.maven.testPackages.map { pkg ->
        GradleDescriptor.Dependency(
          group = pkg.group,
          name = pkg.name,
          version = pkg.version,
          configuration = "testImplementation",
        )
      },
      repositories = source.dependencies.maven.repositories.map { (name, repo) ->
        GradleDescriptor.Repository(name = name, url = repo.url)
      },
    )
    return AdoptGradleManifest(descriptor = descriptor)
  }

  override fun toElidePackage(source: AdoptGradleManifest): ElidePackageManifest {
    val descriptor = source.descriptor
    val deps = descriptor.dependencies

    // Separate compile and test dependencies based on configuration
    val compileDeps = deps.filter { !it.isTestScope() }
    val testDeps = deps.filter { it.isTestScope() }

    return ElidePackageManifest(
      name = descriptor.name,
      version = descriptor.version,
      description = descriptor.description,
      dependencies = DependencyResolution(
        maven = MavenDependencies(
          packages = compileDeps.map { dep ->
            MavenPackage(
              group = dep.group,
              name = dep.name,
              version = dep.version,
              coordinate = dep.coordinate(),
            )
          },
          testPackages = testDeps.map { dep ->
            MavenPackage(
              group = dep.group,
              name = dep.name,
              version = dep.version,
              coordinate = dep.coordinate(),
            )
          },
          repositories = descriptor.repositories.associate { repo ->
            (repo.name ?: repo.url) to MavenRepository(url = repo.url, name = repo.name)
          },
        ),
      ),
    )
  }

  private companion object {
    const val DEFAULT_FILENAME_KTS = "build.gradle.kts"
    const val DEFAULT_FILENAME_GROOVY = "build.gradle"
  }
}
