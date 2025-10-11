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

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.GradleCatalogManifest
import elide.tooling.project.manifest.GradleCatalogManifest.NoVersion

@ManifestCodec(ProjectEcosystem.GradleCatalog)
public class GradleCatalogCodec : PackageManifestCodec<GradleCatalogManifest> {
  private fun toMavenPackage(
    manifest: GradleCatalogManifest,
    lib: GradleCatalogManifest.CatalogLibraryDefinition
  ): ElidePackageManifest.MavenPackage = ElidePackageManifest.MavenPackage(
    group = lib.group,
    name = lib.name,
    version = lib.version.provideVersion(manifest.versions) ?: "",
    coordinate = lib.coordinate,
  )

  private fun libFromPackage(lib: ElidePackageManifest.MavenPackage): GradleCatalogManifest.CatalogLibraryDefinition {
    return GradleCatalogManifest.CatalogLibraryGroupName(
      group = lib.group,
      name = lib.name,
      version = lib.version?.let { GradleCatalogManifest.VersionSpec(it) } ?: NoVersion,
    )
  }

  override fun defaultPath(): Path = Path.of("gradle/$DEFAULT_NAME")
  override fun supported(path: Path): Boolean = path.fileName.toString().endsWith(".versions.toml")

  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): GradleCatalogManifest {
    return source.bufferedReader(StandardCharsets.UTF_8).use { reader ->
      GradleCatalog.decodeFromString<GradleCatalogManifest>(reader.readText())
    }
  }

  override fun write(manifest: GradleCatalogManifest, output: OutputStream) {
    output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
      writer.write(GradleCatalog.encodeToString(manifest))
    }
  }

  override fun fromElidePackage(source: ElidePackageManifest): GradleCatalogManifest {
    // @TODO actually serialize versions
    return GradleCatalogManifest(
      path = Path.of("gradle/libs.versions.toml"),
      versions = emptyMap(),
      plugins = emptyMap(),
      bundles = emptyMap(),
      libraries = source.dependencies.maven.allPackages().map { pkg ->
        libFromPackage(pkg)
      }.toList().associateBy { lib ->
        // @TODO smarter name generation
        lib.name.substringBefore("-")
      },
    )
  }

  override fun toElidePackage(source: GradleCatalogManifest): ElidePackageManifest {
    return ElidePackageManifest(
      dependencies = ElidePackageManifest.DependencyResolution(
        maven = ElidePackageManifest.MavenDependencies(
          packages = source.libraries.values.map {
            toMavenPackage(source, it)
          },
          catalogs = listOf(ElidePackageManifest.GradleCatalog(
            path = source.path.toString(),
          )),
        )
      )
    )
  }

  private companion object {
    // Default name for a Gradle catalog file.
    const val DEFAULT_NAME = "libs.versions.toml"

    // TOML codec for Gradle catalogs.
    private val GradleCatalog: Toml by lazy {
      Toml(
        inputConfig = TomlInputConfig(
          ignoreUnknownNames = false,
          allowEmptyValues = false,
          allowNullValues = false,
          allowEscapedQuotesInLiteralStrings = false,
          allowEmptyToml = true,
        ),
        outputConfig = TomlOutputConfig(
          indentation = TomlIndentation.TWO_SPACES,
        )
      )
    }
  }
}
