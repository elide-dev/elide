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
import elide.tooling.project.adopt.node.NodeParser
import elide.tooling.project.adopt.node.PackageJsonDescriptor
import elide.tooling.project.codecs.ManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.DependencyResolution
import elide.tooling.project.manifest.ElidePackageManifest.NpmDependencies
import elide.tooling.project.manifest.ElidePackageManifest.NpmPackage
import elide.tooling.project.manifest.PackageManifest

/**
 * Manifest wrapper for Node.js projects parsed via the adopt parser.
 *
 * This wraps a [PackageJsonDescriptor] from the adopt parsing infrastructure
 * and implements [PackageManifest] for integration with the codec system.
 */
public data class AdoptNodeManifest(
  public val descriptor: PackageJsonDescriptor,
  public val path: Path? = null,
) : PackageManifest {
  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.Node
}

/**
 * Codec adapter for Node.js package.json files using the adopt parser infrastructure.
 *
 * This codec leverages [NodeParser] from the adopt package, which provides:
 * - JSON parsing with kotlinx.serialization
 * - Workspace/monorepo detection (array and object formats)
 * - Dependency categorization (dependencies, devDependencies, peerDependencies)
 * - Script extraction
 *
 * The parsed [PackageJsonDescriptor] is wrapped in [AdoptNodeManifest] and can be
 * converted to [ElidePackageManifest] for cross-ecosystem compatibility.
 */
@ManifestCodec(ProjectEcosystem.Node)
public class AdoptNodeCodec : PackageManifestCodec<AdoptNodeManifest> {

  override fun defaultPath(): Path = Path(DEFAULT_FILENAME)

  override fun supported(path: Path): Boolean = path.name == DEFAULT_FILENAME

  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): AdoptNodeManifest {
    // NodeParser requires a file path, so write stream to temp file
    val tempFile = createTempFile(prefix = "package", suffix = ".json")
    try {
      tempFile.writeBytes(source.readBytes())
      return parseAsFile(tempFile, state)
    } finally {
      tempFile.toFile().delete()
    }
  }

  override fun parseAsFile(path: Path, state: PackageManifestCodec.ManifestBuildState): AdoptNodeManifest {
    val descriptor = NodeParser.parse(path)
    return AdoptNodeManifest(descriptor = descriptor, path = path)
  }

  override fun write(manifest: AdoptNodeManifest, output: OutputStream) {
    // Generate PKL representation for writing
    val pklContent = PklGenerator.generate(manifest.descriptor)
    output.write(pklContent.toByteArray(Charsets.UTF_8))
  }

  override fun fromElidePackage(source: ElidePackageManifest): AdoptNodeManifest {
    // Convert ElidePackageManifest back to PackageJsonDescriptor
    val descriptor = PackageJsonDescriptor(
      name = source.name ?: "",
      version = source.version,
      description = source.description,
      dependencies = source.dependencies.npm.packages.associate { it.name to (it.version ?: "*") },
      devDependencies = source.dependencies.npm.devPackages.associate { it.name to (it.version ?: "*") },
      peerDependencies = emptyMap(),
      optionalDependencies = emptyMap(),
      scripts = source.scripts,
      workspaces = source.workspaces,
    )
    return AdoptNodeManifest(descriptor = descriptor)
  }

  override fun toElidePackage(source: AdoptNodeManifest): ElidePackageManifest {
    val descriptor = source.descriptor

    // Combine peerDependencies and optionalDependencies with regular packages
    // since NpmDependencies only supports packages and devPackages
    val allPackages = descriptor.dependencies +
      descriptor.peerDependencies +
      descriptor.optionalDependencies

    return ElidePackageManifest(
      name = descriptor.name,
      version = descriptor.version,
      description = descriptor.description,
      scripts = descriptor.scripts,
      workspaces = descriptor.workspaces,
      dependencies = DependencyResolution(
        npm = NpmDependencies(
          packages = allPackages.map { (name, version) ->
            NpmPackage(name = name, version = version.takeIf { it != "*" })
          },
          devPackages = descriptor.devDependencies.map { (name, version) ->
            NpmPackage(name = name, version = version.takeIf { it != "*" })
          },
        ),
      ),
    )
  }

  private companion object {
    const val DEFAULT_FILENAME = "package.json"
  }
}
