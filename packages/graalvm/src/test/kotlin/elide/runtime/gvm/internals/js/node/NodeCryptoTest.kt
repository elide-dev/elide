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
package elide.runtime.gvm.internals.js.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.crypto.NodeCryptoModule
import elide.runtime.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `crypto` built-in module. */
@TestCase internal class NodeCryptoTest : NodeModuleConformanceTest<NodeCryptoModule>() {
  override val moduleName: String get() = "crypto"
  override fun provide(): NodeCryptoModule = NodeCryptoModule()
  @Inject lateinit var crypto: NodeCryptoModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Certificate")
    yield("Cipher")
    yield("Decipher")
    yield("DiffieHellman")
    yield("DiffieHellmanGroup")
    yield("ECDH")
    yield("Hash")
    yield("Hmac")
    yield("KeyObject")
    yield("Sign")
    yield("Verify")
    yield("X509Certificate")
    yield("constants")
    yield("fips")
    yield("checkPrime")
    yield("checkPrimeSync")
    yield("createCipheriv")
    yield("createDecipheriv")
    yield("createDiffieHellman")
    yield("createDiffieHellmanGroup")
    yield("createECDH")
    yield("createHash")
    yield("createHmac")
    yield("createPrivateKey")
    yield("createPublicKey")
    yield("createSecretKey")
    yield("createSign")
    yield("createVerify")
    yield("diffieHellman")
    yield("hash")
    yield("generateKey")
    yield("generateKeySync")
    yield("generateKeyPair")
    yield("generateKeyPairSync")
    yield("generatePrime")
    yield("generatePrimeSync")
    yield("getCipherInfo")
    yield("getCiphers")
    yield("getCurves")
    yield("getDiffieHellman")
    yield("getFips")
    yield("getHashes")
    yield("getRandomValues")
    yield("hkdf")
    yield("hkdfSync")
    yield("pbkdf2")
    yield("pbkdf2Sync")
    yield("privateDecrypt")
    yield("privateEncrypt")
    yield("publicDecrypt")
    yield("publicEncrypt")
    yield("randomBytes")
    yield("randomFill")
    yield("randomFillSync")
    yield("randomInt")
    yield("randomUUID")
    yield("scrypt")
    yield("scryptSync")
    yield("secureHeapUsed")
    yield("setEngine")
    yield("setFips")
    yield("sign")
    yield("subtle")
    yield("timingSafeEqual")
    yield("verify")
    yield("webcrypto")
  }

  @Test override fun testInjectable() {
    assertNotNull(crypto)
  }
}
