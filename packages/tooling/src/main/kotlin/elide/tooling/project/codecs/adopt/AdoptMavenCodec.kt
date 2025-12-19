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
import elide.tooling.project.adopt.maven.MavenParser
import elide.tooling.project.adopt.maven.PomDescriptor
import elide.tooling.project.codecs.ManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.DependencyResolution
import elide.tooling.project.manifest.ElidePackageManifest.MavenDependencies
import elide.tooling.project.manifest.ElidePackageManifest.MavenPackage
import elide.tooling.project.manifest.ElidePackageManifest.MavenRepository
import elide.tooling.project.manifest.PackageManifest

/**
 * Manifest wrapper for Maven projects parsed via the adopt parser.
 *
 * This wraps a [PomDescriptor] from the adopt parsing infrastructure
 * and implements [PackageManifest] for integration with the codec system.
 */
public data class AdoptMavenManifest(
  public val descriptor: PomDescriptor,
  public val path: Path? = null,
) : PackageManifest {
  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.MavenPom
}

/**
 * Codec adapter for Maven POM files using the adopt parser infrastructure.
 *
 * This codec leverages [MavenParser] from the adopt package, which provides
 * enhanced features over the standard Maven API codec:
 * - BOM import resolution with multi-tier fallback
 * - Parent POM resolution (.m2, cache, Maven Central)
 * - Profile activation and merging
 * - Property interpolation (env, os, java, project)
 * - Custom repository support
 *
 * The parsed [PomDescriptor] is wrapped in [AdoptMavenManifest] and can be
 * converted to [ElidePackageManifest] for cross-ecosystem compatibility.
 */
@ManifestCodec(ProjectEcosystem.MavenPom)
public class AdoptMavenCodec : PackageManifestCodec<AdoptMavenManifest> {

  override fun defaultPath(): Path = Path(DEFAULT_FILENAME)

  override fun supported(path: Path): Boolean =
    path.name == DEFAULT_FILENAME || path.name.endsWith(".pom") || path.name.endsWith(".pom.xml")

  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): AdoptMavenManifest {
    // MavenParser requires a file path, so write stream to temp file
    val tempFile = createTempFile(prefix = "pom", suffix = ".xml")
    try {
      tempFile.writeBytes(source.readBytes())
      return parseAsFile(tempFile, state)
    } finally {
      tempFile.toFile().delete()
    }
  }

  override fun parseAsFile(path: Path, state: PackageManifestCodec.ManifestBuildState): AdoptMavenManifest {
    val descriptor = MavenParser.parse(path)
    return AdoptMavenManifest(descriptor = descriptor, path = path)
  }

  override fun write(manifest: AdoptMavenManifest, output: OutputStream) {
    // Generate PKL representation for writing
    val pklContent = PklGenerator.generate(manifest.descriptor)
    output.write(pklContent.toByteArray(Charsets.UTF_8))
  }

  override fun fromElidePackage(source: ElidePackageManifest): AdoptMavenManifest {
    // Convert ElidePackageManifest back to PomDescriptor
    val descriptor = PomDescriptor(
      groupId = source.dependencies.maven.coordinates?.group ?: "",
      artifactId = source.dependencies.maven.coordinates?.name ?: source.name ?: "",
      version = source.version ?: "0.0.0",
      name = source.name,
      description = source.description,
      dependencies = source.dependencies.maven.packages.map { pkg ->
        elide.tooling.project.adopt.maven.MavenDependency(
          groupId = pkg.group,
          artifactId = pkg.name,
          version = pkg.version,
          scope = null,
        )
      } + source.dependencies.maven.testPackages.map { pkg ->
        elide.tooling.project.adopt.maven.MavenDependency(
          groupId = pkg.group,
          artifactId = pkg.name,
          version = pkg.version,
          scope = "test",
        )
      },
      repositories = source.dependencies.maven.repositories.map { (name, repo) ->
        elide.tooling.project.adopt.maven.MavenRepository(
          id = name,
          url = repo.url,
        )
      },
    )
    return AdoptMavenManifest(descriptor = descriptor)
  }

  override fun toElidePackage(source: AdoptMavenManifest): ElidePackageManifest {
    val descriptor = source.descriptor
    val deps = descriptor.dependencies

    // Separate compile and test dependencies
    val compileDeps = deps.filter { it.scope == null || it.scope == "compile" || it.scope == "runtime" }
    val testDeps = deps.filter { it.scope == "test" }

    return ElidePackageManifest(
      name = descriptor.name ?: descriptor.artifactId,
      version = descriptor.version,
      description = descriptor.description,
      dependencies = DependencyResolution(
        maven = MavenDependencies(
          packages = compileDeps.map { dep ->
            MavenPackage(
              group = dep.groupId,
              name = dep.artifactId,
              version = dep.version,
              coordinate = "${dep.groupId}:${dep.artifactId}${dep.version?.let { ":$it" } ?: ""}",
            )
          },
          testPackages = testDeps.map { dep ->
            MavenPackage(
              group = dep.groupId,
              name = dep.artifactId,
              version = dep.version,
              coordinate = "${dep.groupId}:${dep.artifactId}${dep.version?.let { ":$it" } ?: ""}",
            )
          },
          repositories = descriptor.repositories.associate { repo ->
            repo.id to MavenRepository(url = repo.url, name = repo.id)
          },
        ),
      ),
    )
  }

  private companion object {
    const val DEFAULT_FILENAME = "pom.xml"
  }
}
