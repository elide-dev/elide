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
package elide.tooling.project.codecs

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import elide.tooling.project.ProjectEcosystem.Node
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.NodePackageManifest

@ManifestCodec(Node) public class NodeManifestCodec : PackageManifestCodec<NodePackageManifest> {
  override fun defaultPath(): Path = Path("$DEFAULT_NAME.$DEFAULT_EXTENSION")
  override fun supported(path: Path): Boolean {
    return path.nameWithoutExtension == DEFAULT_NAME && path.extension == DEFAULT_EXTENSION
  }

  @OptIn(ExperimentalSerializationApi::class)
  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): NodePackageManifest {
    return PackageJson.decodeFromStream<NodePackageManifest>(source)
  }

  @OptIn(ExperimentalSerializationApi::class)
  override fun write(manifest: NodePackageManifest, output: OutputStream) {
    PackageJson.encodeToStream<NodePackageManifest>(manifest, output)
  }

  override fun fromElidePackage(source: ElidePackageManifest): NodePackageManifest {
    return NodePackageManifest(
      name = source.name,
      version = source.version,
      description = source.description,
      main = source.entrypoint?.first(),
      scripts = source.scripts,
      dependencies = source.dependencies.npm.packages.associate { it.name to it.version },
      devDependencies = source.dependencies.npm.devPackages.associate { it.name to it.version },
    )
  }

  override fun toElidePackage(source: NodePackageManifest): ElidePackageManifest {
    return ElidePackageManifest(
      name = source.name,
      version = source.version,
      description = source.description,
      entrypoint = source.main?.let { listOf(it) },
      scripts = source.scripts.orEmpty(),
      dependencies = ElidePackageManifest.DependencyResolution(
        npm = ElidePackageManifest.NpmDependencies(
          packages = source.dependencies.orEmpty().map { ElidePackageManifest.NpmPackage(it.key, it.value) },
          devPackages = source.devDependencies.orEmpty().map { ElidePackageManifest.NpmPackage(it.key, it.value) },
        ),
      ),
    )
  }

  private companion object {
    const val DEFAULT_EXTENSION = "json"
    const val DEFAULT_NAME = "package"

    @OptIn(ExperimentalSerializationApi::class)
    private val PackageJson by lazy {
      Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        allowComments = true
        allowTrailingComma = true
        isLenient = true
      }
    }
  }
}
