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
package elide.versions.repository

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import elide.annotations.Singleton
import elide.runtime.core.HostPlatform

/**
 * Implementation of [VersionCatalogFactory].
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class VersionCatalogFactoryImpl : VersionCatalogFactory {
  override fun createLocalCatalog(directory: Path, relativePaths: Boolean): String = createCatalog(directory, relativePaths)

  override fun createRemoteCatalog(directory: Path, root: String): String = createCatalog(directory, true, root)

  private fun createCatalog(directory: Path, relativePaths: Boolean, prefix: String? = null): String {
    val versions = mutableMapOf<String, MutableMap<HostPlatform, String>>()
    directory.recursive().forEach {
      if (!it.name.startsWith("elide-") ||
        !it.name.endsWith(".txz") ||
        !SystemFileSystem.exists(Path(it.parent!!, "${it.name}.sha256"))) return@forEach
      val versionString = it.name.substringAfter("elide-").substringBeforeLast(".txz")
      val parts = versionString.split('-')
      if (parts.size < 3) return@forEach
      val arch = parts[parts.size - 1]
      val os = parts[parts.size - 2]
      val version = versionString.substringBeforeLast("-$os-$arch")
      val platform = HostPlatform(HostPlatform.parseOperatingSystem(os), HostPlatform.parseArchitecture(arch))
      var path = if (relativePaths) {
        it.toString().substringAfter(directory.toString()).substring(1).substringBeforeLast(".txz")
      } else {
        it.toString().substringBeforeLast(".txz")
      }
      if (prefix != null) {
        path = "$prefix/${path.replace('\\', '/')}"
      }
      versions.getOrPut(version) { mutableMapOf() }.put(platform, path)
    }
    return Json.Default.encodeToString(ElideVersionCatalog(versions.mapValues { (_, platforms) ->
      ElideSystemCatalog(platforms)
    }))
  }

  private fun Path.recursive(): Sequence<Path> = SystemFileSystem.list(this).asSequence().flatMap {
    if (SystemFileSystem.metadataOrNull(it)!!.isDirectory) {
      it.recursive()
    } else {
      sequenceOf(it)
    }
  }
}
