/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.ssg

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests for the [ManifestReader] interface and [FilesystemManifestReader] implementation. */
@MicronautTest(startApplication = false) class ManifestReaderTests {
  companion object {
    private const val testManifest = "classpath:app.manifest.pb"
    private const val testManifestInvalid = "classpath:example-manifest.txt.pb"
  }

  @Inject lateinit var reader: ManifestReader

  @Test fun testInjectable() {
    assertNotNull(reader, "should be able to inject instance of `ManifestReader`")
  }

  @Test fun testReadManifestResource() = runTest {
    val manifest = reader.readManifest(testManifest)
    assertNotNull(manifest, "should be able to read manifest")
    assertTrue(manifest.isInitialized, "manifest should be initialized after reading")
    val manifest2 = reader.readManifest(testManifest)
    assertNotNull(manifest2, "should be able to read manifest")
    assertTrue(manifest2.isInitialized, "manifest should be initialized after reading")
    assertEquals(manifest, manifest2, "manifest should not vary across read calls")
  }

  @Test fun testReadInvalidPathDisk() = runTest {
    assertThrows<SSGCompilerError> {
      reader.readManifest("/some/invalid/path/app.manifest.pb")
    }
  }

  @Test fun testReadInvalidPathResource() = runTest {
    assertThrows<SSGCompilerError> {
      reader.readManifest("classpath:does-not-exist.manifest.pb")
    }
  }

  @Test fun testReadManifestFilesystem() = runTest {
    val value = System.getProperty("tests.exampleManifest")
    assertNotNull(value, "should be able to example manifest path from system property")
    val manifest = reader.readManifest(value)
    assertNotNull(manifest, "should be able to read manifest")
    assertTrue(manifest.isInitialized, "manifest should be initialized after reading")
    val manifest2 = reader.readManifest(value)
    assertNotNull(manifest, "should be able to read manifest")
    assertTrue(manifest2.isInitialized, "manifest should be initialized after reading")
    assertEquals(manifest, manifest2, "manifest should not vary across read calls")
  }

  @Test fun testReadInvalidManifest() = runTest {
    assertThrows<SSGCompilerError> {
      reader.readManifest(testManifestInvalid)
    }
  }

  @Test fun testCloseable() = runTest {
    val target = FilesystemManifestReader()
    target.close()
  }

  @Test fun testReadFromParameters() = runTest {
    val manifest = reader.readManifest(
      SiteCompilerParams(
      testManifest,
      "i-am-invalid.jar",
      SiteCompilerParams.Output.fromParams("/sample/directory"),
    )
    )
    assertNotNull(manifest, "should be able to read manifest")
    assertTrue(manifest.isInitialized, "manifest should be initialized after reading")
  }
}
