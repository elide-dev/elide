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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import elide.annotations.Inject
import elide.runtime.intrinsics.js.err.RangeError
import elide.runtime.node.crypto.NodeCryptoModule
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

  @Test fun `randomInt should return an Int when valid min and max are provided with no callback`() = conforms {
    val randomInt = crypto.provide().randomInt(5, 10)
    assertIs<Int>(randomInt, "randomInt should return an Int")
    assertTrue(randomInt in 5 until 10, "randomInt should be within the specified range")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    const int = crypto.randomInt();
    assert.equal(typeof int, "Number");
    """
  }

  @Test fun `randomInt should invoke callback with error when min is greater than or equal to max`() = conforms {
    var callbackInvoked = false
    crypto.provide().randomInt(10, 5) { err, value ->
      callbackInvoked = true
      assertNotNull(err, "Error should be provided when min >= max")
      assertIs<RangeError>(err, "Error should be of type RangeError")
    }
    assertTrue(callbackInvoked, "Callback should have been invoked")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    let callbackInvoked = false;
    crypto.randomInt(10, 5, (err, value) => {
      callbackInvoked = true;
      assert.ok(err, "Error should be provided when min >= max");
      assert.equal(err.name, "RangeError", "Error should be of type RangeError");
    });

    assert.ok(callbackInvoked, "Callback should have been invoked");
    """
  }

  @Test fun `randomInt should return nothing when a callback is provided`() = conforms {
    var callbackInvoked = false
    val result = crypto.provide().randomInt(5, 10) { err, value ->
      callbackInvoked = true
      assertNull(err, "Error should be null for valid range")
      assertTrue(value in 5 until 10, "Value should be within the specified range")
    }
    assertTrue(callbackInvoked, "Callback should have been invoked")
    assertEquals(Unit, result, "randomInt should return Unit when a callback is provided")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    let callbackInvoked = false;
    const result = crypto.randomInt(5, 10, (err, value) => {
      callbackInvoked = true;
      assert.ifError(err);
      assert.ok(value >= 5 && value < 10, "Value should be within the specified range");
    });

    assert.ok(callbackInvoked, "Callback should have been invoked");
    assert.equal(result, undefined, "randomInt should return undefined when a callback is provided");
    """
  }
}
