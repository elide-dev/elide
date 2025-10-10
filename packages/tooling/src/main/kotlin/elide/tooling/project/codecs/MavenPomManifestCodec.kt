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
@file:Suppress("INACCESSIBLE_TYPE", "MnInjectionPoints")

package elide.tooling.project.codecs

import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.building.ModelProblem
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import org.apache.maven.project.ProjectBuildingRequest
import org.apache.maven.project.ProjectModelResolver
import org.apache.maven.project.createMavenReactorPool
import org.apache.maven.repository.internal.DefaultVersionResolver
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.impl.RemoteRepositoryManager
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider
import org.eclipse.aether.internal.impl.DefaultMetadataResolver
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager
import org.eclipse.aether.internal.impl.DefaultRepositorySystem
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer
import org.eclipse.aether.metadata.DefaultMetadata
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Properties
import jakarta.inject.Provider
import kotlin.io.path.Path
import kotlin.io.path.name
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.ProjectEcosystem.MavenPom
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.MavenPomManifest

@ManifestCodec(MavenPom) public class MavenPomManifestCodec (
  private val repoSystemProvider: Provider<RepositorySystem>,
  private val repoSessionProvider: Provider<RepositorySystemSession>,
  private val remoteRepositoryManagerProvider: Provider<RemoteRepositoryManager>,
) : PackageManifestCodec<MavenPomManifest> {
  public class MavenModelProblems (public val problems: List<ModelProblem>): IOException()

  private val repoSystem: RepositorySystem by lazy { repoSystemProvider.get() }
  private val repoSession: RepositorySystemSession by lazy { repoSessionProvider.get() }
  private val remoteRepositoryManager: RemoteRepositoryManager by lazy { remoteRepositoryManagerProvider.get() }

  override fun defaultPath(): Path = Path("$DEFAULT_NAME.$DEFAULT_EXTENSION")

  override fun supported(path: Path): Boolean = path.name == "$DEFAULT_NAME.$DEFAULT_EXTENSION"

  private fun renderedBuildTimeSystemProperties(): Properties {
    return System.getProperties()
  }

  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): MavenPomManifest {
    throw UnsupportedOperationException("Parsing from InputStream is not supported for POMs")
  }

  override fun parseAsFile(path: Path, state: PackageManifestCodec.ManifestBuildState): MavenPomManifest {
    val pomFile = path.toFile()
    val request = DefaultModelBuildingRequest()
    request.setPomFile(pomFile)

    request.setModelResolver(ProjectModelResolver(
      repoSession,
      null,
      repoSystem,
      remoteRepositoryManager,
      emptyList(),
      ProjectBuildingRequest.RepositoryMerging.POM_DOMINANT,
      createMavenReactorPool(),
    ))

    request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
    request.setSystemProperties(renderedBuildTimeSystemProperties())

    // We can also set active profiles here if needed
    // request.setActiveProfileIds(Arrays.asList("someProfile"))
    val modelBuilder = DefaultModelBuilderFactory().newInstance()
    val result = modelBuilder.build(request)
    if (!result.problems.isEmpty()) {
      throw MavenModelProblems(result.problems)
    }
    return MavenPomManifest(model = result.effectiveModel, path = path)
  }

  override fun fromElidePackage(source: ElidePackageManifest): MavenPomManifest {
    return MavenPomManifest(
      model = Model().apply {
        name = source.name
        version = source.version
        dependencies.addAll(source.dependencies.maven.packages.map {
          Dependency().apply {
            groupId = it.group
            artifactId = it.name
            version = it.version
          }
        })
        dependencies.addAll(source.dependencies.maven.testPackages.map {
          Dependency().apply {
            groupId = it.group
            artifactId = it.name
            version = it.version
            scope = "test"
          }
        })
      }
    )
  }

  override fun toElidePackage(source: MavenPomManifest): ElidePackageManifest {
    val deps = source.model.dependencies ?: emptyList()
    val managed = source.model.dependencyManagement?.dependencies ?: emptyList()
    val resolvedDeps = deps.map {
      val group = it.groupId
      val artifact = it.artifactId
      val version = it.version ?: managed.find { candidate ->
        candidate.groupId == it.groupId && candidate.artifactId == it.artifactId
      }?.version

      it.scope to ElidePackageManifest.MavenPackage(
        group = group,
        name = artifact,
        version = version ?: "",
        coordinate = buildString {
          append(group)
          append(':')
          append(artifact)
          version?.let {
            append(':')
            append(it)
          }
        },
      )
    }

    return ElidePackageManifest(
      dependencies = ElidePackageManifest.DependencyResolution(
        maven = ElidePackageManifest.MavenDependencies(
          packages = resolvedDeps.filter { it.first != "test" }.map { it.second },
          testPackages = resolvedDeps.filter { it.first == "test" }.map { it.second },
        )
      )
    )
  }

  override fun write(manifest: MavenPomManifest, output: OutputStream) {
    OutputStreamWriter(output, StandardCharsets.UTF_8).use { writer ->
      MavenXpp3Writer().write(writer, manifest.model)
    }
  }

  private companion object {
    const val DEFAULT_EXTENSION = "xml"
    const val DEFAULT_NAME = "pom"
  }
}
