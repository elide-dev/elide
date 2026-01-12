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

import org.graalvm.polyglot.Value
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import elide.annotations.Inject
import elide.runtime.intrinsics.js.err.RangeError
import elide.runtime.intrinsics.js.err.TypeError
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

  @Test fun `randomInt should return a Long when valid min and max are provided with no callback`() = conforms {
    val min = Value.asValue(5L)
    val max = Value.asValue(10L)
    val result = crypto.provide().randomInt(min, max)

    assertIs<Long>(result)
    assertTrue(result in 5 until 10)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const result = crypto.randomInt(5, 10);
        
    assert.equal(typeof result, "number");
    assert.ok(result >= 5 && result < 10);
    """
  }

  @Test fun `randomInt should throw a RangeError when min is greater than or equal to max`() = conforms {

    assertFailsWith<RangeError> { crypto.provide().randomInt(Value.asValue(10L), Value.asValue(10L)) }
    assertFailsWith<RangeError> { crypto.provide().randomInt(Value.asValue(10L), Value.asValue( 5L)) }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    assert.throws(() => crypto.randomInt(10, 10), RangeError);
    assert.throws(() => crypto.randomInt(10, 5), RangeError);
    """
  }

  @Test fun `randomInt should default min to 0 when only max is provided`() = conforms {
    val result = crypto.provide().randomInt(Value.asValue(5L))

    assertIs<Long>(result)
    assertTrue(result in 0 until 5)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const result = crypto.randomInt(5);
        
    assert.equal(typeof result, "number");
    assert.ok(result >= 0 && result < 5);
    """
  }

  @Test fun `randomInt should invoke callback when callback is provided`() = conforms {
    val latch = CountDownLatch(1)
    var called = false
    val result = crypto.provide().randomInt(10L, 20L) { err, value ->
      called = true
      assertNull(err)
      assertTrue(value in 10L until 20L)
      latch.countDown()
    }
    assertIs<Unit>(result)
    latch.await(1, TimeUnit.SECONDS)
    assertTrue(called)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto")
    const assert = require("assert")

    function randomIntPromise(min, max) {
      return new Promise((resolve, reject) => {
        crypto.randomInt(min, max, (err, int) => {
          callbackInvoked = true;
          assert.equal(err, null, "Callback error should be null");
          resolve(int);
        });
      });
    };

    let callbackInvoked = false;

    randomIntPromise(10, 20)
      .then((int) => {
        assert.equal(typeof int, "number");
        assert.ok(int >= 10 && int < 20, "randomInt should be within the range");
        assert.ok(callbackInvoked, "Callback should have been invoked");
      })
    """
  }

  @Test fun `randomInt should return min when range is 1`() = conforms {
    val min = 7L
    val max = 8L

    val result = crypto.provide().randomInt(min,max)

    assertEquals(min, result)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const min = 7;
    const max = 8;
        
    const result = crypto.randomInt(min, max);
        
    assert.equal(result, min);
    """
  }

  @Test fun `randomInt should handle large ranges correctly`() = conforms {
    val min = 0L
    val max = 100_000_000_000L
    val result = crypto.provide().randomInt(min, max)

    assertIs<Long>(result)
    assertTrue(result in min until max)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const min = 0;
    const max = 100000000000;
    const result = crypto.randomInt(min, max);
        
    assert.ok(result >= min && result < max);
    """
  }

  @Test fun `randomInt should throw TypeError for non-numeric arguments`() = conforms {

    val invalidMin: Value = Value.asValue("a")
    val validMax: Value = Value.asValue(10L)
    val validMin: Value = Value.asValue(0L)
    val invalidMax: Value = Value.asValue("b")

    assertFailsWith<TypeError> { crypto.provide().randomInt(invalidMin, validMax) }
    assertFailsWith<TypeError> { crypto.provide().randomInt(validMin, invalidMax) }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    assert.throws(() => crypto.randomInt("a", 10), TypeError);
    assert.throws(() => crypto.randomInt(0, "b"), TypeError);
    """
  }

  @Test fun `randomInt callback should be async`() = conforms {
    var callbackCalled = false
    val min = 0L
    val max = 10L

    fun callbackFn (err: Throwable?, value: Long?) {
      callbackCalled = true

      if (err != null) {
        throw err
      } else {
        assertTrue(value in min until max)
      }
    }

    val result = crypto.provide().randomInt(min, max, ::callbackFn)
    // Callback should not have been called yet
    assertTrue(!callbackCalled, "Callback should not have been invoked yet")

    // The result should be Unit since a callback was provided
    assertIs<Unit>(result)

    // Callback should have been called by this point
    assertTrue(callbackCalled, "Callback should have been invoked asynchronously")
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    let callbackCalled = false;
    crypto.randomInt(1, 10, (err, val) => {
      callbackCalled = true;
      
      assert.equal(err, null);
      assert.ok(val >= 1 && val < 10);
      assert.ok(callbackCalled, "Callback should have been invoked asynchronously");
     });
        
     assert.equal(callbackCalled, false);
     """
  }

  @Test fun `randomInt should throw TypeError for float arguments`() = conforms {
    assertFailsWith<TypeError> { crypto.provide().randomInt(Value.asValue(1.5), Value.asValue(10L)) }
    assertFailsWith<TypeError> { crypto.provide().randomInt(Value.asValue(1L), Value.asValue(10.5)) }
    assertFailsWith<TypeError> { crypto.provide().randomInt(Value.asValue(1.5), Value.asValue(10.5)) }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    assert.throws(() => crypto.randomInt(1.5, 10), TypeError);
    assert.throws(() => crypto.randomInt(1, 10.5), TypeError);
    assert.throws(() => crypto.randomInt(1.5, 10.5), TypeError);
    """
  }

  @Test fun `randomInt should throw when range exceeds MAX_SAFE_INTEGER`() = conforms {
    val min = Value.asValue(-9007199254740991L)
    val max = Value.asValue(9007199254740991L)

    assertFailsWith<RangeError> { crypto.provide().randomInt(min, max) }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const min = Number.MIN_SAFE_INTEGER;
    const max = Number.MAX_SAFE_INTEGER;

    assert.throws(() => crypto.randomInt(min, max), RangeError);
    """
  }

  @Test fun `randomInt large positive range`() = conforms {
    val min = Value.asValue(0L)
    val max = Value.asValue(281474976710655L)

    val result = crypto.provide().randomInt(min, max)

    assertTrue(result in 0L until 281474976710655L)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const min = 0;
    const max = 281474976710655;
    const result = crypto.randomInt(min, max);
    
    assert.ok(result >= min && result < max, "Value is within the expected range");
    """
  }

  @Test fun `randomInt large negative range`() = conforms {
    val min = -9_000_000_000_000L
    val max = 0L
    val result = crypto.provide().randomInt(min, max)

    // @TODO The assertion is causing failures, possibly due to number type coercion?
    assertTrue(result in -9000000000000L until 0L)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const min = -9000000000000;
    const max = 0;
    const result = crypto.randomInt(min, max);

    assert.ok(result >= min && result < max, "Value is within the expected range");
    """
  }

  @Test fun `randomInt max safe integer`() = conforms {
    val min = Value.asValue(0)
    val max = Value.asValue(9007199254740991)

    assertThrows<RangeError>{ crypto.provide().randomInt(min, max) }
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const min = 0;
    const max = Number.MAX_SAFE_INTEGER;

    assert.throws(() => crypto.randomInt(min, max), RangeError);
    """
  }

  @Test fun `randomInt range crossing zero`() = conforms {
    val min = -100L
    val max = 100L
    val result = crypto.provide().randomInt(min, max)

    assertTrue(result in min until max)
  }.guest {
    //language=javascript
    """
    const crypto = require("crypto");
    const assert = require("assert");

    const min = -100;
    const max = 100;
    const result = crypto.randomInt(min, max);

    assert.ok(result >= min && result < max);
    """
  }
}
