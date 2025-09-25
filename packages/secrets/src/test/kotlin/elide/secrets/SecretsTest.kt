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
package elide.secrets

import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.io.bytestring.encodeToByteString
import kotlin.test.*
import elide.annotations.Inject
import elide.secrets.impl.SecretsImpl
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** @author Lauri Heino <datafox> */
@TestCase
class SecretsTest : AbstractSecretTest() {
  @Inject private lateinit var secrets: SecretsImpl

  @BeforeTest
  fun `reset passphrase override`() {
    secrets.overridePassphrase(null)
  }

  @Test
  fun `test accessing secrets`() = withTemp { path ->
    createEnvironment(path, secretPass, secretFiles)

    // initialize secrets and check initialization state.
    assertFalse(secrets.initialized)
    secrets.init(path, null)
    assertTrue(secrets.initialized)

    // load profile and test for values.
    secrets.loadProfile("test")
    assertEquals("content", secrets.getSecret("test"))
    assertEquals("environment", secrets.getStringSecret("env"))
    assertEquals("test file\n".encodeToByteString(), secrets.getSecret("binary"))
    assertEquals("test file\n".encodeToByteString(), secrets.getBinarySecret("binary"))
    assertNull(secrets.getSecret("nope"))
    assertNull(secrets.getStringSecret("nope"))
    assertNull(secrets.getBinarySecret("nope"))
    assertEquals(mapOf("ENV_VAR" to "environment"), secrets.getEnv())
    assertEquals(setOf("test"), secrets.listProfiles())
    assertEquals("test", secrets.getProfile())
    assertEquals(secretTypes, secrets.listSecrets())

    // unload profile and test for values.
    secrets.unloadProfile()
    assertThrows<NullPointerException> { secrets.getSecret("test") }
    assertEquals(mapOf(), secrets.getEnv())
    assertEquals(setOf("test"), secrets.listProfiles())
    assertNull(secrets.getProfile())
    assertThrows<NullPointerException> { secrets.listSecrets() }
    assertThrows<IllegalArgumentException>(Values.profileDoesNotExistException("nope")) { secrets.loadProfile("nope") }
  }

  @Test
  fun `test no passphrase`() = withTemp { path ->
    createEnvironment(path, null, secretFiles)
    assertThrows<IllegalStateException>(Values.PASSPHRASE_READ_EXCEPTION) { secrets.init(path, null) }
  }

  @Test fun `test uninitialized secrets`() = withTemp { path -> secrets.init(path, null) }

  private fun createEnvironment(path: Path, passphrase: String?, files: List<String>): Path {
    val secretsDir = Files.createDirectory(path.resolve(Values.DEFAULT_PATH))
    copyFiles(secretsDir, files)
    secrets.overridePassphrase(passphrase)
    return secretsDir
  }
}
