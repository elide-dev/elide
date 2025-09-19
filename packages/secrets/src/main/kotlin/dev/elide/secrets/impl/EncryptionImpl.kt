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
package dev.elide.secrets.impl

import dev.elide.secrets.Encryption
import dev.elide.secrets.GPGHandler
import dev.elide.secrets.Utils
import dev.elide.secrets.Values
import elide.annotations.Singleton
import kotlinx.io.bytestring.ByteString
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.modes.SICBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV

/**
 * Implementation of [Encryption], using `AES` for encryption and `SHA-256` for hashing.
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class EncryptionImpl() : Encryption {
  override fun encryptAES(key: ByteString, data: ByteString): ByteString {
    val iv: ByteArray = Utils.generateBytes(Values.IV_SIZE).toByteArray()
    val out: ByteArray = iv.copyOf(Values.IV_SIZE + data.size)
    val cipher = createCipher()
    cipher.init(true, ParametersWithIV(KeyParameter(key.toByteArray()), iv))
    cipher.processBytes(data.toByteArray(), 0, data.size, out, Values.IV_SIZE)
    return ByteString(out)
  }

  override fun decryptAES(key: ByteString, encrypted: ByteString): ByteString {
    val out = ByteArray(encrypted.size - Values.IV_SIZE)
    val cipher = createCipher()
    cipher.init(
      false,
      ParametersWithIV(KeyParameter(key.toByteArray()), encrypted.toByteArray(0, Values.IV_SIZE), 0, Values.IV_SIZE),
    )
    cipher.processBytes(encrypted.toByteArray(Values.IV_SIZE), 0, out.size, out, 0)
    return ByteString(out)
  }

  override fun hashKeySHA256(data: ByteString): ByteString {
    val parameterGenerator = PKCS5S2ParametersGenerator(SHA256Digest())
    parameterGenerator.init(data.toByteArray(), null, Values.HASH_ITERATIONS)
    return ByteString((parameterGenerator.generateDerivedParameters(Values.KEY_SIZE * 8) as KeyParameter).key)
  }

  override fun hashDataSHA1(data: ByteString): ByteString {
    val digest = SHA1Digest()
    digest.update(data.toByteArray(), 0, data.size)
    val out = ByteArray(digest.digestSize)
    digest.doFinal(out, 0)
    return ByteString(out)
  }

  override fun encryptGPG(id: String, data: ByteString): ByteString = GPGHandler.runGPG(data, "-e", "-r", id)

  override fun decryptGPG(id: String, encrypted: ByteString): ByteString = GPGHandler.runGPG(encrypted, "-d", "-u", id)

  private fun createCipher() = SICBlockCipher.newInstance(AESEngine.newInstance())
}
