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
package elide.tooling.project

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.PackageManifest

/**
 * # Package Manifest Service
 *
 * Provides a service for loading Elide and foreign package manifests, and for resolving codecs for a given manifest
 * file or type.
 */
public interface PackageManifestService {
  public fun configure(state: PackageManifestCodec.ManifestBuildState)

  public fun resolve(root: Path, ecosystem: ProjectEcosystem = ProjectEcosystem.Elide): Path

  public fun parse(source: Path): PackageManifest

  public fun parse(source: InputStream, ecosystem: ProjectEcosystem): PackageManifest

  public fun merge(manifests: Iterable<PackageManifest>): ElidePackageManifest

  public fun export(manifest: ElidePackageManifest, ecosystem: ProjectEcosystem): PackageManifest

  public fun encode(manifest: PackageManifest, output: OutputStream)
}
