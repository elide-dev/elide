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

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import elide.annotations.Inject
import elide.secrets.dto.persisted.EncryptionMode
import elide.secrets.impl.SecretManagementImpl
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Tests for [SecretManagement].
 *
 * @author Lauri Heino <datafox>
 */
@TestCase
class ManagementTest : AbstractSecretTest() {
  @Inject private lateinit var secrets: SecretManagementImpl
  @Inject private lateinit var parser: PackageManifestCodec<ElidePackageManifest>

  private val managementFiles =
    secretFiles.map { "management/$it" } +
      listOf("management/secret/secrets-other.db", "management/secret/secrets-other.key")
  private val remoteFiles =
    listOf(
      "management/remote/.access",
      "management/remote/access.access",
      "management/remote/metadata.json",
      "management/remote/secrets-other.db",
      "management/remote/secrets-test.db",
    )

  @BeforeTest
  fun `reset management state`() {
    secrets.resetInitialized()
  }

  @Test
  fun `test calls to secrets`() = withTemp { path ->
    // almost direct copy of SecretsTest.`test accessing secrets`.
    createEnvironment(path, secretFiles)

    assertFalse(secrets.initialized)
    queuePrompt(secretPass)
    secrets.init(path, null)
    assertTrue(secrets.initialized)

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

    secrets.unloadProfile()
    assertThrows<NullPointerException> { secrets.getSecret("test") }
    assertEquals(mapOf(), secrets.getEnv())
    assertEquals(setOf("test"), secrets.listProfiles())
    assertNull(secrets.getProfile())
    assertThrows<NullPointerException> { secrets.listSecrets() }
    assertThrows<IllegalArgumentException>(SecretValues.profileDoesNotExistException("nope")) { secrets.loadProfile("nope") }
  }

  @Test
  fun `test initializing secrets`() = withTemp { path ->
    // secrets.init() asks for encryption mode, passphrase twice,
    // if secrets should be initialized locally and the project name.
    queuePrompts(EncryptionMode.PASSPHRASE, secretPass, secretPass, true, "test")
    secrets.init(path, null)
    assertEquals(
      "{\"name\":\"test\",\"profiles\":{},\"localEncryption\":\"PASSPHRASE\"}",
      path.resolve(SecretValues.DEFAULT_PATH).resolve(SecretValues.METADATA_FILE).readText(),
    )
  }

  @Test
  fun `test managing secrets`() = withTemp { path ->
    val secretsDir = createEnvironment(path, secretFiles)

    queuePrompt(secretPass)
    secrets.init(path, null)

    // load profile, check for content.
    secrets.loadProfile("test")
    assertEquals("content", secrets.getSecret("test"))
    assertEquals(secretTypes, secrets.listSecrets())

    // replace "test" value.
    secrets.setTextSecret("test", "different")
    assertEquals("different", secrets.getSecret("test"))
    assertEquals(secretTypes, secrets.listSecrets())

    // create new environment variable secret.
    assertEquals(mapOf("ENV_VAR" to "environment"), secrets.getEnv())
    secrets.setTextSecret("new", "something", "SOME_VAR")
    var secretTypes = secretTypes + ("new" to SecretType.TEXT)
    assertEquals(secretTypes, secrets.listSecrets())
    assertEquals(mapOf("ENV_VAR" to "environment", "SOME_VAR" to "something"), secrets.getEnv())

    // create new binary secret.
    secrets.setBinarySecret("bytes", "data".encodeToByteString())
    secretTypes = secretTypes + ("bytes" to SecretType.BINARY)
    assertEquals(secretTypes, secrets.listSecrets())
    assertEquals("data".encodeToByteString(), secrets.getSecret("bytes"))

    // remove a secret.
    secrets.removeSecret("env")
    secretTypes = secretTypes - "env"
    assertEquals(secretTypes, secrets.listSecrets())
    assertEquals(mapOf("SOME_VAR" to "something"), secrets.getEnv())

    // remove a non-existent secret.
    secrets.removeSecret("nope")
    assertEquals(secretTypes, secrets.listSecrets())
    assertEquals(mapOf("SOME_VAR" to "something"), secrets.getEnv())
    secrets.unloadProfile()

    // change passphrase
    val oldProfile = ByteString(secretsDir.resolve("secrets-test.db").readBytes())
    val oldKey = ByteString(secretsDir.resolve("secrets-test.key").readBytes())
    // secrets.changeEncryption() asks for encryption mode and passphrase twice.
    queuePrompts(EncryptionMode.PASSPHRASE, "sas", "sas")
    secrets.changeEncryption()
    assertEquals(oldProfile, ByteString(secretsDir.resolve("secrets-test.db").readBytes()))
    assertNotEquals(oldKey, ByteString(secretsDir.resolve("secrets-test.key").readBytes()))
  }

  @Test
  fun `test managing profiles`() = withTemp { path ->
    createEnvironment(path, secretFiles)
    queuePrompt(secretPass)
    secrets.init(path, null)

    // create new profile.
    assertEquals(setOf("test"), secrets.listProfiles())
    secrets.createProfile("new")
    assertEquals(setOf("test", "new"), secrets.listProfiles())

    // load new profile and write secret.
    secrets.loadProfile("new")
    secrets.setTextSecret("test", "test")
    secrets.writeChanges()
    secrets.unloadProfile()

    // load new profile again and check secret.
    secrets.loadProfile("new")
    assertEquals("test", secrets.getSecret("test"))

    // remove profile.
    assertThrows<IllegalStateException>(SecretValues.REMOVED_PROFILE_NOT_SELECTED_EXCEPTION) { secrets.deleteProfile("test") }
    secrets.unloadProfile()
    assertThrows<IllegalArgumentException>(SecretValues.profileAlreadyExistsException("new")) { secrets.createProfile("new") }
    secrets.deleteProfile("test")
    assertEquals(setOf("new"), secrets.listProfiles())
    assertThrows<IllegalArgumentException>(SecretValues.profileDoesNotExistException("test")) {
      secrets.deleteProfile("test")
    }
  }

  @Test
  fun `test managing remote`() = withTemp { path ->
    createEnvironment(path, secretFiles)
    secrets.queuePrompt(secretPass)
    secrets.init(path, null)

    // secrets.manageRemote() asks for remote type, directory,
    // encryption mode and passphrase twice.
    queuePrompts("project", ".secrets", EncryptionMode.PASSPHRASE, "sos", "sos")
    val remote = secrets.manageRemote()
    assertThrows<IllegalArgumentException>(SecretValues.accessDoesNotExistException("test")) { remote.selectAccess("test") }
    assertThrows<IllegalStateException>(SecretValues.NO_ACCESS_SELECTED_EXCEPTION) { remote.addProfile("test") }

    // create new access file and add a profile to it.
    // remote.createAccess() asks for encryption mode and passphrase twice.
    queuePrompts(EncryptionMode.PASSPHRASE, "sos", "sos")
    remote.createAccess("test")
    remote.selectAccess("test")
    remote.addProfile("test")
    assertThrows<IllegalArgumentException>(SecretValues.profileDoesNotExistException("nope")) { remote.addProfile("nope") }

    // change access passphrase
    // remote.changeEncryption() asks for encryption mode and passphrase twice.
    queuePrompts(EncryptionMode.PASSPHRASE, "sas", "sas")
    remote.changeEncryption()
    remote.deselectAccess()

    // try to remove profile but cancel the action.
    // remote.deleteProfile() asks if the user wants to proceed.
    queuePrompt(false)
    remote.deleteProfile("test")
    assertTrue(remote.deletedProfiles().isEmpty())

    // select access, remove profile from it and add it back.
    remote.selectAccess("test")
    remote.removeProfile("test")
    assertThrows<IllegalArgumentException>(SecretValues.profileNotInAccessException("test")) { remote.removeProfile("test") }
    assertThrows<IllegalArgumentException>(SecretValues.profileDoesNotExistException("nope")) { remote.removeProfile("nope") }
    remote.addProfile("test")
    remote.deselectAccess()

    // try to remove profile but cancel the action.
    // remote.deleteProfile() asks if the user wants to proceed,
    // and then again if a profile is a part of an access file.
    queuePrompts(true, false)
    remote.deleteProfile("test")
    assertTrue(remote.deletedProfiles().isEmpty())

    // actually remove profile this time.
    queuePrompts(true, true)
    remote.deleteProfile("test")
    assertEquals(setOf("test"), remote.deletedProfiles())

    // restore profile and add it back to access.
    remote.restoreProfile("test")
    assertThrows<IllegalStateException>(SecretValues.PROFILE_NOT_DELETED_EXCEPTION) { remote.restoreProfile("test") }
    remote.selectAccess("test")
    assertEquals(setOf(), remote.listProfiles())
    remote.addProfile("test")
    assertEquals(setOf("test"), remote.listProfiles())
    remote.deselectAccess()

    // regenerate profile key
    remote.rekeyProfile("test")

    // change superuser access passphrase
    // remote.changeSuperEncryption() asks for encryption mode and passphrase twice.
    queuePrompts(EncryptionMode.PASSPHRASE, "seus", "seus")
    remote.changeSuperEncryption()

    // push changes and check for files
    remote.push()
    assertTrue(path.resolve(SecretValues.PROJECT_REMOTE_DEFAULT_PATH).resolve(SecretValues.METADATA_FILE).exists())
    assertTrue(path.resolve(SecretValues.PROJECT_REMOTE_DEFAULT_PATH).resolve(SecretValues.SUPER_ACCESS_FILE).exists())
    assertTrue(path.resolve(SecretValues.PROJECT_REMOTE_DEFAULT_PATH).resolve(SecretUtils.accessName("test")).exists())
    assertTrue(path.resolve(SecretValues.PROJECT_REMOTE_DEFAULT_PATH).resolve(SecretUtils.profileName("test")).exists())
  }

  @Test
  fun `test remote as user`() = withTemp { path ->
    createEnvironment(path, managementFiles)

    // copy remote files.
    val remoteDir = Files.createDirectory(path.resolve(SecretValues.PROJECT_REMOTE_DEFAULT_PATH))
    copyFiles(remoteDir, remoteFiles)
    queuePrompt(secretPass)
    secrets.init(path, null)

    // load profile and remove secret.
    secrets.loadProfile("test")
    assertEquals(mapOf("secret" to SecretType.TEXT, "another" to SecretType.TEXT), secrets.listSecrets())
    assertEquals("stuff", secrets.getStringSecret("secret"))
    assertEquals("yep", secrets.getStringSecret("another"))
    secrets.removeSecret("secret")
    assertEquals(mapOf("another" to SecretType.TEXT), secrets.listSecrets())
    assertNull(secrets.getStringSecret("secret"))
    assertEquals("yep", secrets.getStringSecret("another"))
    secrets.writeChanges()
    secrets.unloadProfile()

    // pull changes from remote (state resets) and remove secret again.
    secrets.pullFromRemote()
    secrets.loadProfile("test")
    assertEquals(mapOf("secret" to SecretType.TEXT, "another" to SecretType.TEXT), secrets.listSecrets())
    assertEquals("stuff", secrets.getStringSecret("secret"))
    assertEquals("yep", secrets.getStringSecret("another"))
    secrets.removeSecret("secret")
    assertEquals(mapOf("another" to SecretType.TEXT), secrets.listSecrets())
    assertNull(secrets.getStringSecret("secret"))
    assertEquals("yep", secrets.getStringSecret("another"))
    secrets.writeChanges()
    secrets.unloadProfile()

    // push changes to remote.
    // secrets.pushToRemote() asks for changed profiles to push.
    queuePrompt("test")
    secrets.pushToRemote()

    // remove profile and pull it back.
    secrets.deleteProfile("test")
    secrets.pullFromRemote()
    secrets.loadProfile("test")
    assertEquals(mapOf("another" to SecretType.TEXT), secrets.listSecrets())
    assertNull(secrets.getStringSecret("secret"))
    assertEquals("yep", secrets.getStringSecret("another"))
    secrets.unloadProfile()
  }

  @Test
  fun `initialize from remote`() = withTemp { path ->
    assumeTrue(isGpgAvailable, "GPG is not installed, skipping test")
    // copy remote files.
    val remoteDir = Files.createDirectory(path.resolve(SecretValues.PROJECT_REMOTE_DEFAULT_PATH))
    copyFiles(remoteDir, remoteFiles)

    // initialize secrets from remote.
    // secrets.init() asks for encryption mode, passphrase twice, if secrets should be initialized locally,
    // remote type, directory, if project should be pulled as superuser, access file and access file passphrase.
    queuePrompts(
      EncryptionMode.PASSPHRASE,
      secretPass,
      secretPass,
      false,
      "project",
      ".secrets",
      false,
      "access",
      "sos",
    )
    secrets.init(path, null)

    secrets.loadProfile("test")
    assertEquals(mapOf("secret" to SecretType.TEXT, "another" to SecretType.TEXT), secrets.listSecrets())
    assertEquals("stuff", secrets.getStringSecret("secret"))
    assertEquals("yep", secrets.getStringSecret("another"))
    secrets.unloadProfile()
    secrets.loadProfile("other")
    assertEquals(mapOf(), secrets.listSecrets())
    secrets.unloadProfile()
  }

  @Test
  fun `initialize from remote non-interactively`() = withTemp { path ->
    assumeTrue(isGpgAvailable, "GPG is not installed, skipping test")
    // copy remote files.
    val remoteDir = Files.createDirectory(path.resolve(SecretValues.PROJECT_REMOTE_DEFAULT_PATH))
    copyFiles(remoteDir, remoteFiles)
    val stream = ManagementTest::class.java.getResourceAsStream("management/elide.pkl")!!
    val manifest = parser.parse(stream, object: PackageManifestCodec.ManifestBuildState {})

    // these prompts replace environment variables.
    queuePrompts(secretPass, "access", "sos")
    secrets.initNonInteractive(path, manifest)
    secrets.loadProfile("test")
    assertEquals(mapOf("secret" to SecretType.TEXT, "another" to SecretType.TEXT), secrets.listSecrets())
    assertEquals("stuff", secrets.getStringSecret("secret"))
    assertEquals("yep", secrets.getStringSecret("another"))
  }

  private fun createEnvironment(path: Path, files: List<String>): Path {
    val secretsDir = Files.createDirectory(path.resolve(SecretValues.DEFAULT_PATH))
    copyFiles(secretsDir, files)
    return secretsDir
  }

  private fun queuePrompt(prompt: Any) {
    when (prompt) {
      is String -> secrets.queuePrompt(prompt)
      is Enum<*> -> secrets.queuePrompt(prompt)
      is Boolean -> secrets.queuePrompt(prompt)
      else -> throw IllegalArgumentException("Unsupported type: ${prompt::class}, $prompt")
    }
  }

  private fun queuePrompts(vararg prompts: Any) = prompts.forEach { queuePrompt(it) }
}
