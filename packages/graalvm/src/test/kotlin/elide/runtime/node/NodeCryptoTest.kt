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
package elide.runtime.node

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.buffer.NodeHostBuffer
import elide.runtime.node.crypto.NodeCryptoModule
import elide.runtime.node.crypto.NodeHash
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

  @Test fun `randomUUID should return a string`() = conforms {
    val uuid = crypto.provide().randomUUID(null)
    assertIs<String>(uuid, "randomUUID should return a String")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const uuid = crypto.randomUUID();
    assert.equal(typeof uuid, "string");
    """
  }

  @Test fun `randomUUID should return lowercase format`() = conforms {
    val uuid = crypto.provide().randomUUID(null)
    assertEquals(uuid, uuid.lowercase(), "UUID should be in lowercase format")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const uuid = crypto.randomUUID();
    assert.equal(uuid, uuid.toLowerCase(), "UUID should be lowercase");
    """
  }

  @Test fun `randomUUID should return valid UUID v4 format`() = conforms {
    val uuid = crypto.provide().randomUUID(null)

    // UUID v4 format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
    // where y is one of [8,9,a,b]
    val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    assertTrue(
      uuidRegex.matches(uuid),
      "UUID should match v4 format: $uuid"
    )
    assertEquals(36, uuid.length, "UUID should be 36 characters long")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const uuid = crypto.randomUUID();
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

    assert.equal(uuid.length, 36, "UUID should be 36 characters");
    assert.ok(uuidRegex.test(uuid), "UUID should match v4 format: " + uuid);
    """
  }

  @Test fun `randomUUID should generate unique values`() = conforms {
    val uuid1 = crypto.provide().randomUUID(null)
    val uuid2 = crypto.provide().randomUUID(null)
    val uuid3 = crypto.provide().randomUUID(null)

    assertNotEquals(uuid1, uuid2, "UUIDS should be unique")
    assertNotEquals(uuid2, uuid3, "UUIDS should be unique")
    assertNotEquals(uuid1, uuid3, "UUIDS should be unique")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const uuid1 = crypto.randomUUID();
    const uuid2 = crypto.randomUUID();
    const uuid3 = crypto.randomUUID();

    assert.notEqual(uuid1, uuid2, "UUIDs should be unique");
    assert.notEqual(uuid2, uuid3, "UUIDs should be unique");
    assert.notEqual(uuid1, uuid3, "UUIDs should be unique");
    """
  }

  @Test fun `randomUUID should accept optional options parameter`() = conforms {
    // Options parameter is accepted but currently ignored
    val uuid = crypto.provide().randomUUID(null)
    assertNotNull(uuid)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const uuid1 = crypto.randomUUID({ disableEntropyCache: true });
    const uuid2 = crypto.randomUUID({ disableEntropyCache: false });

    assert.equal(typeof uuid1, "string");
    assert.equal(typeof uuid2, "string");
    """
  }

  @Test fun `createHash should create a NodeHash object`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    assertNotNull(hash, "createHash should return a hash object")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    assert.ok(hash, "createHash should return a node hash object");
    """
  }

  @Test fun `createHash should support md5 algorithm`() = conforms {
    val hash = crypto.provide().createHash("md5")
    hash.update("hello world")
    val digest = hash.digest("hex")
    assertEquals(
      "5eb63bbbe01eeed093cb22bb8f5acdc3",
      digest,
      "MD5 hash of 'hello world' should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("md5");
    hash.update("hello world");
    const digest = hash.digest("hex");

    assert.equal(
      "5eb63bbbe01eeed093cb22bb8f5acdc3",
      digest,
      "MD5 hash of 'hello world' should match expected value"
    );
    """
  }

  @Test fun `createHash should support sha1 algorithm`() = conforms {
    val hash = crypto.provide().createHash("sha1")
    hash.update("hello world")
    val digest = hash.digest("hex")
    assertEquals(
      "2aae6c35c94fcfb415dbe95f408b9ce91ee846ed",
      digest,
      "SHA1 hash of 'hello world' should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha1");
    hash.update("hello world");
    const digest = hash.digest("hex");

    assert.equal(
      "2aae6c35c94fcfb415dbe95f408b9ce91ee846ed",
      digest,
      "SHA1 hash of 'hello world' should match expected value"
    );
    """
  }

  @Test fun `createHash should support sha256 algorithm`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    hash.update("hello world")
    val digest = hash.digest("hex")
    assertEquals(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digest,
      "SHA256 hash of 'hello world' should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    hash.update("hello world");
    const digest = hash.digest("hex");

    assert.equal(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digest,
      "SHA256 hash of 'hello world' should match expected value"
    );
    """
  }

  @Test fun `createHash should support sha512 algorithm`() = conforms {
    val hash = crypto.provide().createHash("sha512")
    hash.update("hello world")
    val digest = hash.digest("hex")
    assertEquals(
      "309ecc489c12d6eb4cc40f50c902f2b4d0ed77ee511a7c7a9bcd3ca86d4cd86f989dd35bc5ff499670da34255b45b0cfd830e81f605dcf7dc5542e93ae9cd76f",
      digest,
      "SHA512 hash of 'hello world' should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha512");
    hash.update("hello world");
    const digest = hash.digest("hex");

    assert.equal(
      "309ecc489c12d6eb4cc40f50c902f2b4d0ed77ee511a7c7a9bcd3ca86d4cd86f989dd35bc5ff499670da34255b45b0cfd830e81f605dcf7dc5542e93ae9cd76f",
      digest,
      "SHA512 hash of 'hello world' should match expected value"
    );
    """
  }

  @Test fun `createHash should support sha3-256 algorithm`() = conforms {
    val hash = crypto.provide().createHash("sha3-256")
    hash.update("hello world")
    val digest = hash.digest("hex")
    assertEquals(
      "644bcc7e564373040999aac89e7622f3ca71fba1d972fd94a31c3bfbf24e3938",
      digest,
      "SHA3-256 hash of 'hello world' should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha3-256");
    hash.update("hello world");
    const digest = hash.digest("hex");

    assert.equal(
      "644bcc7e564373040999aac89e7622f3ca71fba1d972fd94a31c3bfbf24e3938",
      digest,
      "SHA3-256 hash of 'hello world' should match expected value"
    );
    """
  }

  @Test fun `createHash should throw for unsupported algorithm`() = conforms {
    assertThrows<IllegalArgumentException> { crypto.provide().createHash("unsupported-algorithm") }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    assert.throws(() => crypto.createHash("unsupported-algo"), "Should throw for unsupported algorithm");
    """
  }

  @Test fun `createHash should create a hash and generate the correct digest`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    hash.update("hello world")
    val digest = hash.digest("hex")
    assertEquals(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digest,
      "SHA-256 hash of 'hello world' should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    hash.update("hello world");
    const digest = hash.digest("hex");

    assert.equal(digest, "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", "SHA-256 hash of 'hello world' should match expected value");
    """
  }

  @Test fun `createHash should allow chainable updates`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    hash.update("hello").update(" ").update("world")
    val digest = hash.digest("hex")

    assertEquals(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digest,
      "SHA-256 hash of 'hello world' should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    hash.update("hello").update(" ").update("world");
    const digest = hash.digest("hex");

    assert.equal(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digest,
      "SHA-256 hash of 'hello world' should match expected value"
    )
    """
  }

  @Test fun `createHash should accept a ByteArray input`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    val input = "hello world".toByteArray(Charsets.UTF_8)
    hash.update(input)
    val digest = hash.digest("hex")

    assertEquals(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digest,
      "SHA-256 hash of 'hello world' byte array should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")
    
    const bytes = new Uint8Array([104,101,108,108,111,32,119,111,114,108,100])
    const hash = crypto.createHash("sha256");
    hash.update(bytes);
    const digestHex = hash.digest("hex");
    
    assert.equal(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digestHex,
      "SHA-256 hash of 'hello world' byte array should match expected value"
    )
    """
  }

  @Test fun `createHash should throw if digest is called twice`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    hash.update("data")
    hash.digest("hex")
    assertThrows<IllegalStateException> {
      hash.digest("hex")
    }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    hash.update("data");
    hash.digest("hex");

    assert.throws(() => {
      hash.digest("hex");
    }, "Should throw if digest called twice");
    """
  }

  @Test fun `createHash with digested empty input SHA-256`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    val digest = hash.digest("hex")

    assertEquals(
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      digest.toString(),
      "SHA-256 of empty input should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    const digest = hash.digest("hex")
    
    assert.equal(
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      digest,
      "SHA-256 of empty input should match expected value"
    );
    """
  }

  @Test fun `createHash should be copyable`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    hash.update("hello world")
    val hashCopy = hash.copy()
    val digestOriginal = hash.digest("hex")
    val digestCopy = hashCopy.digest("hex")

    assertIs<NodeHash>(hashCopy)
    assertEquals(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digestOriginal,
      "Original SHA-256 hash of 'hello world' should match expected value"
    )
    assertEquals(
      digestOriginal,
      digestCopy,
      "Copied hash digest should match original digest"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    hash.update("hello world");
    const hashCopy = hash.copy();
    const digestOriginal = hash.digest("hex");
    const digestCopy = hashCopy.digest("hex");

    assert.equal(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digestOriginal,
      "Original SHA-256 hash of 'hello world' should match expected value"
    );
    assert.equal(
      digestOriginal,
      digestCopy,
      "Copied hash digest should match original digest"
    );
    """
  }

  @Test fun `createHash should throw when attempting to copy a digested Hash`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    hash.update("data")
    hash.digest("hex")
    assertThrows<IllegalStateException> {
      hash.copy()
    }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    hash.update("data");
    hash.digest("hex");

    assert.throws(() => {
      hash.copy();
    }, "Should throw if trying to copy a digested hash");
    """
  }

  @Test fun `createHash should throw when calling update with invalid data type`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    assertThrows<IllegalArgumentException> {
      hash.update({})
    }
    assertThrows<IllegalArgumentException> {
      hash.update(12345)
    }
    assertThrows<IllegalArgumentException> {
      hash.update(12.34)
    }
    assertThrows<IllegalArgumentException> {
      hash.update(true)
    }
    assertThrows<IllegalArgumentException> {
      hash.update({ str: String -> str })
    }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");

    assert.throws(() => {
      hash.update({});
    }, "Should throw when calling update with invalid data");
    assert.throws(() => {
      hash.update(12345);
    }, "Should throw when calling update with invalid data");
    assert.throws(() => {
      hash.update(12.34);
    }, "Should throw when calling update with invalid data");
    assert.throws(() => {
      hash.update(true);
    }, "Should throw when calling update with invalid data");
    assert.throws(() => {
      hash.update((str) => str);
    }, "Should throw when calling update with invalid data");
    """
  }

  @Test fun `createHash should accept TypedArray input`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    val input = ByteArray(11)
    val str = "hello world"
    for (i in str.indices) {
      input[i] = str[i].code.toByte()
    }
    hash.update(input)
    val digest = hash.digest("hex")

    assertEquals(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digest,
      "SHA-256 hash of 'hello world' typed array should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")
    
    const bytes = new Uint8Array([104,101,108,108,111,32,119,111,114,108,100])
    const hash = crypto.createHash("sha256");
    hash.update(bytes);
    const digestHex = hash.digest("hex");
    
    assert.equal(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digestHex,
      "SHA-256 hash of 'hello world' typed array should match expected value"
    )
    """
  }

  @Test fun `createHash should accept multiple update calls with different data types`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    hash.update("hello ")
    val byteArray = ByteArray(5)
    val str = "world"
    for (i in str.indices) {
      byteArray[i] = str[i].code.toByte()
    }
    hash.update(byteArray)
    val digest = hash.digest("hex")

    assertEquals(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digest,
      "SHA-256 hash of 'hello world' with mixed updates should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    hash.update("hello ");
    
    const bytes = new Uint8Array([119,111,114,108,100]);
    hash.update(bytes);
    
    const digestHex = hash.digest("hex");

    assert.equal(
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      digestHex,
      "SHA-256 hash of 'hello world' with mixed updates should match expected value"
    );
    """
  }

  @Test fun `createHash should handle large input data`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    val largeInput = "a".repeat(10_000_000)
    hash.update(largeInput)
    val digest = hash.digest("hex")

    assertEquals(
      "01f4a87c04b40af59aadc0e812293509709c9a8763a60b7f9e19303322f8b03c",
      digest,
      "SHA-256 hash of large input should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const hash = crypto.createHash("sha256");
    const largeInput = "a".repeat(10000000);
    hash.update(largeInput);
    const digestHex = hash.digest("hex");

    assert.equal(
      "01f4a87c04b40af59aadc0e812293509709c9a8763a60b7f9e19303322f8b03c",
      digestHex,
      "SHA-256 hash of large input should match expected value"
    );
    """
  }

  @Test fun `createHash should produce consistent results across multiple instances`() = conforms {
    val input = "consistent input data"

    val hash1 = crypto.provide().createHash("sha256")
    hash1.update(input)
    val digest1 = hash1.digest("hex")

    val hash2 = crypto.provide().createHash("sha256")
    hash2.update(input)
    val digest2 = hash2.digest("hex")

    assertEquals(
      digest1,
      digest2,
      "SHA-256 digests from separate instances with same input should match"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const input = "consistent input data";

    const hash1 = crypto.createHash("sha256");
    hash1.update(input);
    const digest1 = hash1.digest("hex");

    const hash2 = crypto.createHash("sha256");
    hash2.update(input);
    const digest2 = hash2.digest("hex");

    assert.equal(
      digest1,
      digest2,
      "SHA-256 digests from separate instances with same input should match"
    );
    """
  }

  @Test fun `createHash should digest to Buffer by default`() = conforms {
    val hash = crypto.provide().createHash("sha256")
    hash.update("hello world")
    val digest = hash.digest()

    assertIs<NodeHostBuffer>(digest, "Default digest output should be a NodeHostBuffer")

    val hexDigest = digest.toString("hex", null, null)

    // @TODO(elijahkotyluk) find a better way to validate the digest content on host side
    assertEquals(
      hexDigest,
      "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "Hash should match expected value"
    )
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")
    const { Buffer } = require("buffer");

    const hash = crypto.createHash("sha256");
    hash.update("hello world");
    const digest = hash.digest();
    
    assert.equal(Buffer.isBuffer(digest), true, "Default digest output should be a Buffer");
    
    const expectedHex = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";
    const actualHex = digest.toString("hex");
    
    assert.equal(
      expectedHex,
      actualHex,
      "SHA-256 hash of 'hello world' should match expected value"
    );
    """
  }
}
