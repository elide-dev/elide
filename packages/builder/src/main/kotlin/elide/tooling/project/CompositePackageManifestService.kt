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
@file:Suppress("MnInjectionPoints")

package elide.tooling.project

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import jakarta.inject.Provider
import kotlinx.atomicfu.atomic
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.tooling.project.codecs.ManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.GradleCatalogManifest
import elide.tooling.project.manifest.MavenPomManifest
import elide.tooling.project.manifest.NodePackageManifest
import elide.tooling.project.manifest.PackageManifest
import elide.tooling.project.manifest.PyProjectManifest
import elide.tooling.project.manifest.PythonRequirementsManifest
import elide.tooling.project.manifest.merge

@Singleton
public class CompositePackageManifestService @Inject constructor (
  @ManifestCodec(ProjectEcosystem.Elide)
  private val elideCodecProvider: Provider<PackageManifestCodec<ElidePackageManifest>>,
  @ManifestCodec(ProjectEcosystem.Node)
  private val nodeCodecProvider: Provider<PackageManifestCodec<NodePackageManifest>>,
  @ManifestCodec(ProjectEcosystem.Python)
  private val pyProjectCodecProvider: Provider<PackageManifestCodec<PyProjectManifest>>,
  @ManifestCodec(ProjectEcosystem.PythonRequirements)
  private val pythonRequirementsCodecProvider: Provider<PackageManifestCodec<PythonRequirementsManifest>>,
  @ManifestCodec(ProjectEcosystem.MavenPom)
  private val mavenPomCodecProvider: Provider<PackageManifestCodec<MavenPomManifest>>,
  @ManifestCodec(ProjectEcosystem.GradleCatalog)
  private val gradleCatalogCodecProvider: Provider<PackageManifestCodec<GradleCatalogManifest>>,
) : PackageManifestService {

  private val elideCodec by lazy { elideCodecProvider.get() }
  private val nodeCodec by lazy { nodeCodecProvider.get() }
  private val pyProjectCodec by lazy { pyProjectCodecProvider.get() }
  private val pythonRequirementsCodec by lazy { pythonRequirementsCodecProvider.get() }
  private val mavenPomCodec by lazy { mavenPomCodecProvider.get() }
  private val gradleCatalogCodec by lazy { gradleCatalogCodecProvider.get() }
  private val buildHintState = atomic<PackageManifestCodec.ManifestBuildState>(
    object: PackageManifestCodec.ManifestBuildState {}
  )

  private val allCodecs by lazy {
    sequenceOf(
      elideCodec,
      nodeCodec,
      pyProjectCodec,
      pythonRequirementsCodec,
      mavenPomCodec,
      gradleCatalogCodec,
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun codecForEcosystem(ecosystem: ProjectEcosystem): PackageManifestCodec<PackageManifest> = when (ecosystem) {
    ProjectEcosystem.Elide -> elideCodec
    ProjectEcosystem.Node -> nodeCodec
    ProjectEcosystem.Python -> pyProjectCodec
    ProjectEcosystem.PythonRequirements -> pythonRequirementsCodec
    ProjectEcosystem.MavenPom -> mavenPomCodec
    ProjectEcosystem.GradleCatalog -> gradleCatalogCodec
    ProjectEcosystem.Ruby -> error("Ruby environments are not supported yet")
    ProjectEcosystem.Gradle -> error("Use MavenPom codec with Gradle build files for now")
    ProjectEcosystem.Bazel -> error("Bazel environments are not supported yet")
  } as PackageManifestCodec<PackageManifest>

  @Suppress("UNCHECKED_CAST")
  private fun codecForManifest(manifest: PackageManifest): PackageManifestCodec<PackageManifest> = when (manifest) {
    is ElidePackageManifest -> elideCodec
    is NodePackageManifest -> nodeCodec
    is PyProjectManifest -> pyProjectCodec
    is PythonRequirementsManifest -> pythonRequirementsCodec
    is MavenPomManifest -> mavenPomCodec
    is GradleCatalogManifest -> gradleCatalogCodec
    else -> error("Unsupported manifest type: ${manifest::class.simpleName}")
  } as PackageManifestCodec<PackageManifest>

  override fun configure(state: PackageManifestCodec.ManifestBuildState) {
    buildHintState.value = state
  }

  override fun resolve(root: Path, ecosystem: ProjectEcosystem): Path {
    return root.resolve(codecForEcosystem(ecosystem).defaultPath())
  }

  override fun parse(source: Path): PackageManifest {
    return allCodecs.first { it.supported(source) }.parseAsFile(source, buildHintState.value).also {
      enforce(it)
    }
  }

  override fun parse(source: InputStream, ecosystem: ProjectEcosystem): PackageManifest {
    return codecForEcosystem(ecosystem).parse(source, buildHintState.value).also {
      enforce(it)
    }
  }

  override fun merge(manifests: Iterable<PackageManifest>): ElidePackageManifest {
    return manifests.fold(ElidePackageManifest()) { merged, manifest ->
      val codec = codecForManifest(manifest)
      merged.merge(codec.toElidePackage(manifest))
    }
  }

  override fun export(manifest: ElidePackageManifest, ecosystem: ProjectEcosystem): PackageManifest {
    return codecForEcosystem(ecosystem).fromElidePackage(manifest).also {
      enforce(it)
    }
  }

  override fun encode(manifest: PackageManifest, output: OutputStream) {
    val codec = codecForManifest(manifest)
    codec.write(manifest, output)
  }

  override fun enforce(manifest: PackageManifest): PackageManifestService.ManifestValidation {
    return codecForEcosystem(manifest.ecosystem).enforce(
      manifest,
      buildHintState.value,
    )
  }
}
