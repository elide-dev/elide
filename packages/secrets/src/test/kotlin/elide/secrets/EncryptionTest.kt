package elide.secrets

import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.bytestring.toHexString
import kotlin.test.assertEquals
import elide.annotations.Inject
import elide.secrets.impl.EncryptionImpl
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** @author Lauri Heino <datafox> */
@TestCase class EncryptionTest {
  @Inject private lateinit var encryption: EncryptionImpl

  @Test fun `test encryption`() {
    val key = Utils.generateBytes(Values.KEY_SIZE)
    val data = "test data".encodeToByteString()
    val encrypted = encryption.encryptAES(key, data)
    val decrypted = encryption.decryptAES(key, encrypted)
    assertEquals(data, decrypted)
  }

  @OptIn(ExperimentalStdlibApi::class)
  @Test fun `test hashing`() {
    val data = "test data".encodeToByteString()
    val keyHash = encryption.hashKeySHA256(data)
    assertEquals("b8197369ccc3d898f59b75b371311be5d8766541865d6d132a1831b29b9e90a3", keyHash.toHexString())
    val dataHash = encryption.hashDataSHA1(data)
    assertEquals("f48dd853820860816c75d54d0f584dc863327a7c", dataHash.toHexString())
    val gitHash = encryption.hashGitDataSHA1(data)
    assertEquals("0aa6fb54678c17a33af5295b7d161709f29b2680", gitHash.toHexString())
  }
}
