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
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.PythonRequirementsManifest

@ManifestCodec(ProjectEcosystem.PythonRequirements)
public class PythonRequirementsManifestCodec : PackageManifestCodec<PythonRequirementsManifest> {
  override fun defaultPath(): Path = Path("$DEFAULT_NAME.$DEFAULT_EXTENSION")
  override fun supported(path: Path): Boolean {
    return path.nameWithoutExtension == DEFAULT_NAME && path.extension == DEFAULT_EXTENSION
  }

  override fun parse(source: InputStream, state: PackageManifestCodec.ManifestBuildState): PythonRequirementsManifest {
    return source.bufferedReader().useLines {
      PythonRequirementsManifest(it.toList())
    }
  }

  override fun write(manifest: PythonRequirementsManifest, output: OutputStream) {
    output.bufferedWriter().use { writer ->
      manifest.dependencies.forEach { writer.appendLine(it) }
    }
  }

  override fun fromElidePackage(source: ElidePackageManifest): PythonRequirementsManifest {
    return PythonRequirementsManifest(
      dependencies = source.dependencies.pip.packages.map { it.name },
    )
  }

  override fun toElidePackage(source: PythonRequirementsManifest): ElidePackageManifest {
    return ElidePackageManifest(
      dependencies = ElidePackageManifest.DependencyResolution(
        pip = ElidePackageManifest.PipDependencies(
          source.dependencies.map { ElidePackageManifest.PipPackage(it) },
        ),
      ),
    )
  }

  private companion object {
    const val DEFAULT_EXTENSION = "txt"
    const val DEFAULT_NAME = "requirements"
  }
}
