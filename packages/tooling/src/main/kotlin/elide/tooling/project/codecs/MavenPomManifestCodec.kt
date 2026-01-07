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
import org.apache.maven.model.Plugin
import org.apache.maven.model.PluginExecution
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.codehaus.plexus.util.xml.Xpp3Dom
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
    val model = source.model
    val deps = model.dependencies ?: emptyList()
    val managed = model.dependencyManagement?.dependencies ?: emptyList()
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

    // Extract JVM target from maven.compiler.target or maven.compiler.source properties
    val jvmTarget = model.properties?.let { props ->
      (props["maven.compiler.target"] ?: props["maven.compiler.source"])?.toString()
    }?.let { target ->
      // Normalize "1.8" -> "8", "1.5" -> "5", etc.
      val normalized = if (target.startsWith("1.")) target.substring(2) else target
      normalized.toUIntOrNull()?.let { ElidePackageManifest.JvmTarget.NumericJvmTarget(it) }
    }

    // Extract Maven coordinates
    val coordinates = if (!model.groupId.isNullOrBlank() && !model.artifactId.isNullOrBlank()) {
      ElidePackageManifest.MavenCoordinates(
        group = model.groupId,
        name = model.artifactId,
      )
    } else null

    // Extract source directories from Maven build configuration
    // Use Maven's configured directories, falling back to Maven conventions
    val build = model.build
    val mainSourceDir = build?.sourceDirectory ?: DEFAULT_MAIN_SOURCE_DIR
    val testSourceDir = build?.testSourceDirectory ?: DEFAULT_TEST_SOURCE_DIR

    val sources = buildMap {
      put("main", ElidePackageManifest.SourceSet(
        type = ElidePackageManifest.SourceSet.SourceSetType.Main,
        paths = listOf("$mainSourceDir/**/*.java"),
      ))
      put("test", ElidePackageManifest.SourceSet(
        type = ElidePackageManifest.SourceSet.SourceSetType.Test,
        paths = listOf("$testSourceDir/**/*.java"),
      ))
    }

    // Parse maven-jar-plugin configuration to discover JAR artifacts
    val artifacts = parseJarArtifacts(model)

    return ElidePackageManifest(
      name = model.artifactId ?: model.name,
      version = model.version,
      description = model.description,
      jvm = jvmTarget?.let { ElidePackageManifest.JvmSettings(target = it) },
      sources = sources,
      artifacts = artifacts,
      dependencies = ElidePackageManifest.DependencyResolution(
        maven = ElidePackageManifest.MavenDependencies(
          coordinates = coordinates,
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

  /** Parse maven-jar-plugin configuration to discover all JAR artifacts */
  private fun parseJarArtifacts(model: Model): Map<String, ElidePackageManifest.Artifact> {
    // If not a jar packaging, return empty
    if (model.packaging != null && model.packaging != "jar") {
      return emptyMap()
    }

    val jarPlugin = model.build?.plugins?.find {
      it.groupId == MAVEN_PLUGINS_GROUP && it.artifactId == MAVEN_JAR_PLUGIN
    }

    // No jar plugin configured - return default jar artifact
    if (jarPlugin == null) {
      return mapOf("jar" to ElidePackageManifest.Jar())
    }

    val artifacts = mutableMapOf<String, ElidePackageManifest.Artifact>()

    // Parse each execution of the jar plugin
    val executions = jarPlugin.executions ?: emptyList()

    for (execution in executions) {
      val config = execution.configuration as? Xpp3Dom ?: continue
      val artifactInfo = parseJarExecutionConfig(execution.id, config)
      if (artifactInfo != null) {
        artifacts[artifactInfo.first] = artifactInfo.second
      }
    }

    // If no executions produced artifacts, check for default-jar or add a default
    if (artifacts.isEmpty()) {
      // Check plugin-level configuration for default jar
      val defaultConfig = jarPlugin.configuration as? Xpp3Dom
      if (defaultConfig != null) {
        val artifactInfo = parseJarExecutionConfig("jar", defaultConfig)
        if (artifactInfo != null) {
          artifacts[artifactInfo.first] = artifactInfo.second
        }
      } else {
        artifacts["jar"] = ElidePackageManifest.Jar()
      }
    }

    // Ensure we have a default jar if only classified jars were found
    val hasDefaultJar = artifacts.values.any { jar ->
      jar is ElidePackageManifest.Jar && jar.name == null
    } || artifacts.containsKey("jar") || artifacts.containsKey("default-jar")

    if (!hasDefaultJar && artifacts.isNotEmpty()) {
      artifacts["jar"] = ElidePackageManifest.Jar()
    }

    return artifacts
  }

  /** Parse a single jar plugin execution configuration */
  private fun parseJarExecutionConfig(
    executionId: String,
    config: Xpp3Dom
  ): Pair<String, ElidePackageManifest.Jar>? {
    val classifier = config.getChild("classifier")?.value
    val excludesNode = config.getChild("excludes")
    val archiveNode = config.getChild("archive")

    // Parse excludes (patterns like org/joda/time/tz/data/**)
    val excludes = excludesNode?.children?.mapNotNull { it.value } ?: emptyList()

    // Parse manifest entries from archive/manifestEntries
    val manifestEntries = mutableMapOf<String, String>()
    archiveNode?.getChild("manifestEntries")?.children?.forEach { entry ->
      entry.value?.let { manifestEntries[entry.name] = it }
    }

    // Parse manifest file path from archive/manifestFile
    val manifestFile = archiveNode?.getChild("manifestFile")?.value

    // Determine artifact name: use classifier if present, otherwise execution id
    val artifactName = when {
      classifier != null -> classifier
      executionId == "default-jar" -> "jar"
      else -> executionId
    }

    return artifactName to ElidePackageManifest.Jar(
      name = classifier,
      manifest = manifestEntries,
      manifestFile = manifestFile,
      excludes = excludes,
    )
  }

  private companion object {
    const val DEFAULT_EXTENSION = "xml"
    const val DEFAULT_NAME = "pom"
    const val DEFAULT_MAIN_SOURCE_DIR = "src/main/java"
    const val DEFAULT_TEST_SOURCE_DIR = "src/test/java"
    const val MAVEN_PLUGINS_GROUP = "org.apache.maven.plugins"
    const val MAVEN_JAR_PLUGIN = "maven-jar-plugin"
  }
}
