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
package elide.versions

import kotlinx.coroutines.test.runTest
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.annotations.Inject
import elide.runtime.core.HostPlatform
import elide.runtime.version.ElideVersionInfo
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.versions.repository.VersionCatalogFactory

/** @author Lauri Heino <datafox> */
@TestCase
class VersionsInstallTest : AbstractVersionsTest() {
  @Inject private lateinit var manager: VersionManagerImpl
  @Inject private lateinit var factory: VersionCatalogFactory

  val installFiles = listOf("install/install.txz", "install/install.txz.sha256", "install/catalog.json")

  @AfterTest
  fun cleanup() {
    manager.clearExtraRepositories()
  }

  @Test
  fun `test installing from local repository`() = withTemp { path ->
    val repoDir = path.resolve("repo")
    repoDir.toFile().mkdir()
    val installDir = path.resolve("install")
    installDir.toFile().mkdir()
    copyFiles(repoDir, "install", installFiles)
    val catalogFile = repoDir.resolve("catalog.json")
    catalogFile.writeText(catalogFile.readText().replace("!PLATFORM!", HostPlatform.resolve().platformString()))
    manager.addExtraRepository("local:${catalogFile.absolutePathString()}")
    runTest { manager.install(true, "1.0.0-dummy", installDir.toString()) }
  }

  @Test
  fun `test installing from generated local repository`() = withTemp { path ->
    val repoDir = path.resolve("repo")
    repoDir.toFile().mkdir()
    val installDir = path.resolve("install")
    installDir.toFile().mkdir()
    copyFiles(repoDir, "install", installFiles - "install/catalog.json")
    val platformString = HostPlatform.resolve().platformString().replace('_', '-')
    repoDir.resolve("install.txz").toFile().renameTo(repoDir.resolve("elide-1.0.0-dummy-$platformString.txz").toFile())
    repoDir
      .resolve("install.txz.sha256")
      .toFile()
      .renameTo(repoDir.resolve("elide-1.0.0-dummy-$platformString.txz.sha256").toFile())
    val catalogFile = repoDir.resolve("catalog.json")
    catalogFile.writeText(factory.createLocalCatalog(kotlinx.io.files.Path(repoDir.absolutePathString()), true))
    manager.addExtraRepository("local:${catalogFile.absolutePathString()}")
    runTest { manager.install(true, "1.0.0-dummy", installDir.toString()) }
  }

  @Test
  fun `test uninstalling`() = withTemp { path ->
    val repoDir = path.resolve("repo")
    repoDir.toFile().mkdir()
    val installDir = path.resolve("install")
    installDir.toFile().mkdir()
    copyFiles(repoDir, "install", installFiles)
    val catalogFile = repoDir.resolve("catalog.json")
    catalogFile.writeText(catalogFile.readText().replace("!PLATFORM!", HostPlatform.resolve().platformString()))
    manager.addExtraRepository("local:${catalogFile.absolutePathString()}")
    runTest {
      manager.install(true, "1.0.0-dummy", installDir.toString())
      assertTrue(installDir.exists())
      manager.uninstall(
        true,
        ElideInstallation(ElideVersionInfo("1.0.0-dummy"), installDir.absolutePathString()))
      assertFalse(installDir.exists())
    }
  }
}
