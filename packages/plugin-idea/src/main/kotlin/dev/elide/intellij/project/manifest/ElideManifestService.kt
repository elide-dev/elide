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
package dev.elide.intellij.project.manifest

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import elide.tooling.project.PackageManifestService
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.codecs.ElidePackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.PackageManifest

/** Basic manifest service that only resolves Elide manifests. */
class ElideManifestService : PackageManifestService {
  private val elideCodec = ElidePackageManifestCodec()

  private fun ProjectEcosystem.requireElide() {
    require(this == ProjectEcosystem.Elide) { "Only Elide package manifests are supported for resolution" }
  }

  override fun resolve(root: Path, ecosystem: ProjectEcosystem): Path {
    ecosystem.requireElide()
    return root.resolve(elideCodec.defaultPath())
  }

  override fun parse(source: Path): ElidePackageManifest {
    return elideCodec.parseAsFile(source)
  }

  override fun parse(source: InputStream, ecosystem: ProjectEcosystem): ElidePackageManifest {
    ecosystem.requireElide()
    return elideCodec.parse(source)
  }

  override fun merge(manifests: Iterable<PackageManifest>): ElidePackageManifest {
    throw UnsupportedOperationException()
  }

  override fun export(manifest: ElidePackageManifest, ecosystem: ProjectEcosystem): PackageManifest {
    throw UnsupportedOperationException()
  }

  override fun encode(manifest: PackageManifest, output: OutputStream) {
    require(manifest is ElidePackageManifest) { "Only Elide package manifests are supported" }
    elideCodec.write(manifest, output)
  }
}
