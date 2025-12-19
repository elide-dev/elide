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
import kotlin.io.path.createTempDirectory
import kotlin.io.path.name
import kotlin.io.path.writeBytes
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.adopt.PklGenerator
import elide.tooling.project.adopt.bazel.BazelDescriptor
import elide.tooling.project.adopt.bazel.BazelParser
import elide.tooling.project.codecs.ManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.DependencyResolution
import elide.tooling.project.manifest.ElidePackageManifest.MavenDependencies
import elide.tooling.project.manifest.ElidePackageManifest.MavenPackage
import elide.tooling.project.manifest.PackageManifest

/**
 * Manifest wrapper for Bazel projects parsed via the adopt parser.
 *
 * This wraps a [BazelDescriptor] from the adopt parsing infrastructure
 * and implements [PackageManifest] for integration with the codec system.
 */
public data class AdoptBazelManifest(
  public val descriptor: BazelDescriptor,
  public val path: Path? = null,
) : PackageManifest {
  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.Bazel
}

/**
 * Codec adapter for Bazel build files using the adopt parser infrastructure.
 *
 * This codec leverages [BazelParser] from the adopt package, which provides:
 * - WORKSPACE file parsing for maven_install dependencies
 * - MODULE.bazel file parsing for bzlmod dependencies
 * - BUILD/BUILD.bazel file parsing for targets and sources
 * - Java and Kotlin rule extraction (java_library, java_binary, kt_jvm_*)
 * - Target dependency graph extraction
 *
 * The parsed [BazelDescriptor] is wrapped in [AdoptBazelManifest] and can be
 * converted to [ElidePackageManifest] for cross-ecosystem compatibility.
 */
@ManifestCodec(ProjectEcosystem.Bazel)
public class AdoptBazelCodec : PackageManifestCodec<AdoptBazelManifest> {

  override fun defaultPath(): Path = Path(DEFAULT_WORKSPACE)

  override fun supported(path: Path): Boolean {
    val name = path.name
    return name == DEFAULT_WORKSPACE ||
      name == DEFAULT_MODULE_BAZEL ||
      name == "BUILD" ||
      name == "BUILD.bazel"
  }

  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): AdoptBazelManifest {
    // BazelParser requires a directory path, so write stream to temp directory
    val tempDir = createTempDirectory(prefix = "bazel")
    val tempFile = tempDir.resolve("WORKSPACE")
    try {
      tempFile.writeBytes(source.readBytes())
      return parseAsFile(tempDir, state)
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  override fun parseAsFile(path: Path, state: PackageManifestCodec.ManifestBuildState): AdoptBazelManifest {
    // BazelParser expects a directory, not a file
    val projectDir = if (path.toFile().isDirectory) path else path.parent
    val descriptor = BazelParser.parse(projectDir)
    return AdoptBazelManifest(descriptor = descriptor, path = path)
  }

  override fun write(manifest: AdoptBazelManifest, output: OutputStream) {
    // Generate PKL representation for writing
    val pklContent = PklGenerator.generate(manifest.descriptor)
    output.write(pklContent.toByteArray(Charsets.UTF_8))
  }

  override fun fromElidePackage(source: ElidePackageManifest): AdoptBazelManifest {
    // Convert ElidePackageManifest back to BazelDescriptor
    val descriptor = BazelDescriptor(
      name = source.name ?: "",
      dependencies = source.dependencies.maven.packages.map { pkg ->
        BazelDescriptor.Dependency(
          group = pkg.group,
          artifact = pkg.name,
          version = pkg.version ?: "",
        )
      },
      targets = emptyList(),
    )
    return AdoptBazelManifest(descriptor = descriptor)
  }

  override fun toElidePackage(source: AdoptBazelManifest): ElidePackageManifest {
    val descriptor = source.descriptor

    return ElidePackageManifest(
      name = descriptor.name,
      dependencies = DependencyResolution(
        maven = MavenDependencies(
          packages = descriptor.dependencies.map { dep ->
            MavenPackage(
              group = dep.group,
              name = dep.artifact,
              version = dep.version,
              coordinate = "${dep.group}:${dep.artifact}:${dep.version}",
            )
          },
        ),
      ),
    )
  }

  private companion object {
    const val DEFAULT_WORKSPACE = "WORKSPACE"
    const val DEFAULT_MODULE_BAZEL = "MODULE.bazel"
  }
}
