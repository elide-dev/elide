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

import java.nio.file.Path
import kotlinx.serialization.Serializable
import elide.tooling.project.ProjectEcosystem

// Type definitions for primitive types used by the catalog manifest record.

public typealias CatalogVersionName = String
public typealias CatalogVersionString = String
public typealias CatalogPluginId = String
public typealias CatalogLibraryId = String
public typealias CatalogBundleId = String
public typealias CatalogVersionsMap = Map<CatalogVersionName, CatalogVersionString>
public typealias CatalogPluginsMap = Map<String, GradleCatalogManifest.CatalogPluginDefinition>
public typealias CatalogLibrariesMap = Map<String, GradleCatalogManifest.CatalogLibraryDefinition>
public typealias CatalogBundlesMap = Map<String, GradleCatalogManifest.CatalogBundleDefinition>

/**
 * ## Gradle Catalog
 *
 * Describes the structure of a Gradle Version Catalog manifest, which is a TOML file that declares dependency versions
 * for libraries, plugins (Gradle plugins) and "bundles," which are groups of libraries.
 *
 * Gradle Catalog files are supported by Elide in limited form: dependencies can be pulled and referenced for catalogs,
 * but no support is available for plugins.
 *
 * @property path Path where this manifest was read from.
 * @property versions Version mappings present in the manifest.
 * @property plugins Plugin mappings present in the manifest.
 * @property libraries Library mappings present in the manifest.
 * @property bundles Bundle mappings present in the manifest.
 */
@Serializable @JvmRecord public data class GradleCatalogManifest internal constructor(
  public val path: Path? = null,
  public val versions: CatalogVersionsMap = emptyMap(),
  public val plugins: CatalogPluginsMap = emptyMap(),
  public val libraries: CatalogLibrariesMap = emptyMap(),
  public val bundles: CatalogBundlesMap = emptyMap(),
) : PackageManifest {
  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.GradleCatalog

  /** Represents a symbolically named entry in a Gradle catalog. */
  public sealed interface SymbolicCatalogEntry

  /** Catalog versions: Describes a uniform version reference or string. */
  @Serializable public sealed interface CatalogVersionSpec {
    public fun provideVersion(versions: CatalogVersionsMap): String? = null
  }

  /** No version provided. */
  @Serializable public data object NoVersion: CatalogVersionSpec

  /** Specifies a string version as a literal. */
  @Serializable @JvmRecord public data class VersionSpec internal constructor(
    public val version: String,
  ): CatalogVersionSpec, SymbolicCatalogEntry {
    override fun provideVersion(versions: CatalogVersionsMap): String = version
  }

  /** Specifies a reference to the [versions] block, by name. */
  @Serializable @JvmRecord public data class VersionRef internal constructor(
    public val ref: String,
  ): CatalogVersionSpec, SymbolicCatalogEntry {
    override fun provideVersion(versions: CatalogVersionsMap): String =
      requireNotNull(versions[ref]) { "No such version at ref: $ref" }
  }

  /** Models the definition block for a Gradle plugin. */
  @Serializable public sealed interface CatalogPluginDefinition : SymbolicCatalogEntry {
    /** ID of the plugin reference. */
    public val id: String

    /** Version reference or string, if present, or [NoVersion]. */
    public val version: CatalogVersionSpec
  }

  /** Models the definition block for a library. */
  @Serializable public sealed interface CatalogLibraryDefinition : SymbolicCatalogEntry {
    /** Group string for the library reference. */
    public val group: String

    /** Module name for the library reference. */
    public val name: String

    /** Full module (group + name) for the library reference. */
    public val module: String

    /** Full coordinate (group + name + version, as applicable) for the library reference. */
    public val coordinate: String

    /** Version reference or string, if present, or [NoVersion]. */
    public val version: CatalogVersionSpec
  }

  /** Models the definition block for a bundle of library references. */
  @Serializable public sealed interface CatalogBundleDefinition : SymbolicCatalogEntry {
    /** Library references for this bundle. */
    public val libraries: List<CatalogLibraryId>
  }

  // Implementation of a library mapping as a `module` reference.
  @JvmRecord @Serializable public data class CatalogLibraryModule internal constructor(
    override val module: String,
    override val version: CatalogVersionSpec = NoVersion,
  ) : CatalogLibraryDefinition {
    override val group: String get() = module.substringBefore(":")
    override val name: String get() = module.substringAfter(":")
    override val coordinate: String get() = "$group:$name"
  }

  // Implementation of a library mapping as a `group`/`name` reference.
  @JvmRecord @Serializable public data class CatalogLibraryGroupName internal constructor(
    override val group: String,
    override val name: String,
    override val version: CatalogVersionSpec = NoVersion,
  ) : CatalogLibraryDefinition {
    override val module: String get() = "$group:$name"
    override val coordinate: String get() = "$group:$name"
  }

  // Implementation of a plugin mapping.
  @JvmRecord @Serializable public data class CatalogPlugin internal constructor(
    override val id: String = "",
    override val version: CatalogVersionSpec = NoVersion,
  ) : CatalogPluginDefinition

  // Implementation of a bundle mapping.
  @JvmRecord @Serializable public data class CatalogBundle internal constructor(
    override val libraries: List<CatalogLibraryId> = emptyList(),
  ) : CatalogBundleDefinition
}
