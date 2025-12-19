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
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.writeBytes
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.adopt.PklGenerator
import elide.tooling.project.adopt.python.PyProjectParser
import elide.tooling.project.adopt.python.PythonDescriptor
import elide.tooling.project.adopt.python.RequirementsTxtParser
import elide.tooling.project.codecs.ManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.DependencyResolution
import elide.tooling.project.manifest.ElidePackageManifest.PipDependencies
import elide.tooling.project.manifest.ElidePackageManifest.PipPackage
import elide.tooling.project.manifest.PackageManifest

/**
 * Manifest wrapper for Python projects parsed via the adopt parser.
 *
 * This wraps a [PythonDescriptor] from the adopt parsing infrastructure
 * and implements [PackageManifest] for integration with the codec system.
 */
public data class AdoptPythonManifest(
  public val descriptor: PythonDescriptor,
  public val path: Path? = null,
) : PackageManifest {
  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.Python
}

/**
 * Codec adapter for Python project files using the adopt parser infrastructure.
 *
 * This codec leverages [PyProjectParser] and [RequirementsTxtParser] from the
 * adopt package, which provide:
 * - pyproject.toml parsing (PEP 621) with ktoml
 * - requirements.txt parsing with -r include support
 * - Dev dependency detection (requirements-dev.txt, dev-requirements.txt)
 * - Optional dependencies grouping
 * - Python version requirement extraction
 * - Entry points and scripts extraction
 *
 * The parsed [PythonDescriptor] is wrapped in [AdoptPythonManifest] and can be
 * converted to [ElidePackageManifest] for cross-ecosystem compatibility.
 */
@ManifestCodec(ProjectEcosystem.Python)
public class AdoptPythonCodec : PackageManifestCodec<AdoptPythonManifest> {

  override fun defaultPath(): Path = Path(DEFAULT_PYPROJECT)

  override fun supported(path: Path): Boolean {
    val name = path.name
    return name == DEFAULT_PYPROJECT ||
      name == DEFAULT_REQUIREMENTS ||
      name.matches(Regex("requirements.*\\.txt", RegexOption.IGNORE_CASE))
  }

  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): AdoptPythonManifest {
    // Python parsers require file paths, so write stream to temp file
    val tempFile = createTempFile(prefix = "pyproject", suffix = ".toml")
    try {
      tempFile.writeBytes(source.readBytes())
      return parseAsFile(tempFile, state)
    } finally {
      tempFile.toFile().delete()
    }
  }

  override fun parseAsFile(path: Path, state: PackageManifestCodec.ManifestBuildState): AdoptPythonManifest {
    val fileName = path.name
    val descriptor = when {
      fileName == DEFAULT_PYPROJECT -> {
        val parsed = PyProjectParser.parse(path)
        PyProjectParser.extractDevDependencies(parsed)
      }
      fileName.matches(Regex("requirements.*\\.txt", RegexOption.IGNORE_CASE)) -> {
        RequirementsTxtParser.detectAndParse(path.parent, path.parent.name)
          ?: throw IllegalArgumentException("Failed to parse requirements.txt at $path")
      }
      else -> throw IllegalArgumentException("Unsupported Python configuration file: $fileName")
    }
    return AdoptPythonManifest(descriptor = descriptor, path = path)
  }

  override fun write(manifest: AdoptPythonManifest, output: OutputStream) {
    // Generate PKL representation for writing
    val pklContent = PklGenerator.generateFromPython(manifest.descriptor)
    output.write(pklContent.toByteArray(Charsets.UTF_8))
  }

  override fun fromElidePackage(source: ElidePackageManifest): AdoptPythonManifest {
    // Convert ElidePackageManifest back to PythonDescriptor
    val descriptor = PythonDescriptor(
      name = source.name ?: "",
      version = source.version,
      description = source.description,
      pythonVersion = source.python?.version,
      dependencies = source.dependencies.pip.packages.map { it.name + (it.version?.let { v -> ">=$v" } ?: "") },
      devDependencies = emptyList(),
      optionalDependencies = source.dependencies.pip.optionalPackages.mapValues { (_, pkgs) ->
        pkgs.map { it.name + (it.version?.let { v -> ">=$v" } ?: "") }
      },
      scripts = emptyMap(),
      sourceType = PythonDescriptor.SourceType.PYPROJECT,
    )
    return AdoptPythonManifest(descriptor = descriptor)
  }

  override fun toElidePackage(source: AdoptPythonManifest): ElidePackageManifest {
    val descriptor = source.descriptor

    // Parse version specifiers from dependency strings
    fun parseDep(dep: String): PipPackage {
      val match = Regex("^([a-zA-Z0-9_-]+)(.*)$").find(dep)
      return if (match != null) {
        PipPackage(name = match.groupValues[1], version = match.groupValues[2].takeIf { it.isNotEmpty() })
      } else {
        PipPackage(name = dep, version = null)
      }
    }

    return ElidePackageManifest(
      name = descriptor.name,
      version = descriptor.version,
      description = descriptor.description,
      dependencies = DependencyResolution(
        pip = PipDependencies(
          packages = descriptor.dependencies.map { parseDep(it) },
          optionalPackages = descriptor.optionalDependencies.mapValues { (_, deps) ->
            deps.map { parseDep(it) }
          },
        ),
      ),
    )
  }

  private companion object {
    const val DEFAULT_PYPROJECT = "pyproject.toml"
    const val DEFAULT_REQUIREMENTS = "requirements.txt"
  }
}
