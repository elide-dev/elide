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
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.PackageManifest

public interface PackageManifestCodec<T : PackageManifest> {
  public interface ManifestBuildState {
    public val isRelease: Boolean get() = false
    public val isDebug: Boolean get() = false
  }

  public fun defaultPath(): Path

  public fun supported(path: Path): Boolean

  public fun parse(source: InputStream, state: ManifestBuildState): T

  public fun parseAsFile(path: Path, state: ManifestBuildState): T {
    return parse(source = path.toFile().inputStream(), state)
  }

  public fun write(manifest: T, output: OutputStream)

  public fun fromElidePackage(source: ElidePackageManifest): T

  public fun toElidePackage(source: T): ElidePackageManifest
}
