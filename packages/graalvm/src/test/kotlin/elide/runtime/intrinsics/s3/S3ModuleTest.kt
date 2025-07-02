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
package elide.runtime.intrinsics.s3

import com.robothy.s3.rest.LocalS3
import com.robothy.s3.rest.bootstrap.LocalS3Mode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.fail
import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.s3.S3Module
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

@TestCase internal class S3ModuleTest : AbstractJsIntrinsicTest<S3Module>() {
  @Inject lateinit var module: S3Module
  override fun provide(): S3Module = module

  companion object {
    private lateinit var s3Server: LocalS3

    // Generates an S3 client for creating test scenarios.
    @JvmStatic
    fun s3Client() = S3Client.builder()
      .forcePathStyle(true)
      .credentialsProvider(AnonymousCredentialsProvider.create())
      .endpointOverride(URI.create(testServerEndpoint()))
      .region(Region.US_WEST_2)
      .build()

    // Generates the endpoint string for testing. Do not run before the server has started.
    @JvmStatic
    fun testServerEndpoint() = "http://localhost:${s3Server.port}"

    @JvmStatic
    fun clientPrelude() = """
      const { S3Client } = require("elide:s3");
      const client = new S3Client({
        endpoint: "${testServerEndpoint()}",
        region: "us-west-2",
        accessKeyId: "test",
        secretAccessKey: "test",
        bucket: "test"
      });
    """

    @JvmStatic
    @BeforeAll
    fun startup() {
      s3Server = LocalS3.builder()
        .port(-1)
        .mode(LocalS3Mode.IN_MEMORY)
        .build()
      s3Server.start()

      // create test bucket
      val request = CreateBucketRequest.builder()
        .bucket("test")
        .build()

      s3Client().createBucket(request)
    }

    @JvmStatic
    @AfterAll
    fun shutdown() {
      s3Server.shutdown()
    }
  }

  @Test override fun testInjectable() {
    assertNotNull(module) { "S3 module should be injectable" }
  }

  @Test fun testClientConstructor() {
    // required options
    executeGuest {
      // language=javascript
      """
        const { S3Client } = require("elide:s3");
        const client = new S3Client({});
      """
    }.fails()

    // not enough args
    executeGuest {
      // language=javascript
      """
        const { S3Client } = require("elide:s3");
        const client = new S3Client();
      """
    }.fails()

    // args wrong type
    executeGuest {
      // language=javascript
      """
        const { S3Client } = require("elide:s3");
        const client = new S3Client("test");
      """
    }.fails()
  }

  @Test fun testFileConstructor() {
    // zero args
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file();
      """
    }.fails()

    // wrong type
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file({});
      """
    }.fails()
  }

  @Test fun testWriteString() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("write-test.txt");
        file.write("test").then(_ => {});
      """
    }.doesNotFail()
    val request = GetObjectRequest.builder()
      .key("write-test.txt")
      .bucket("test")
      .build()
    val response = s3Client().getObject(request)
    assertEquals("test", response.readAllBytes().toString(StandardCharsets.UTF_8))
    assertEquals("text/plain", response.response().contentType())
  }

  @Test fun testDefaultContentType() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("default-content-type-test");
        file.write("test").then(_ => {});
      """
    }.doesNotFail()
    val request = GetObjectRequest.builder()
      .key("default-content-type-test")
      .bucket("test")
      .build()
    val response = s3Client().getObject(request)
    assertEquals("test", response.readAllBytes().toString(StandardCharsets.UTF_8))
    assertEquals("application/octet-stream", response.response().contentType())
  }

  @Test fun testContentType() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("content-type-test");
        file.write("test", { type: "fake/this" }).then(_ => {});
      """
    }.doesNotFail()
    val request = GetObjectRequest.builder()
      .key("content-type-test")
      .bucket("test")
      .build()
    val response = s3Client().getObject(request)
    assertEquals("test", response.readAllBytes().toString(StandardCharsets.UTF_8))
    assertEquals("fake/this", response.response().contentType())
  }

  @Test fun testContentTypeFailures() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("content-type-test");
        file.write("test", "plain/text").then(_ => {});
      """
    }.fails()

    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("content-type-test");
        file.write("test", { type: {} }).then(_ => {});
      """
    }.fails()
  }

  @Test fun testWriteArgsFailures() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("write-fail-test.txt");
        file.write();
      """
    }.fails()

    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("write-fail-test.txt");
        file.write({ hello: "world" });
      """
    }.fails()
  }

  @Test fun testWriteArray() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("write-array-test.txt");
        file.write(new Uint8Array([1, 2, 3])).then(_ => {});
      """
    }.doesNotFail()
    val request = GetObjectRequest.builder()
      .key("write-array-test.txt")
      .bucket("test")
      .build()
    val response = s3Client().getObject(request)
    assertContentEquals(byteArrayOf(1, 2, 3), response.readAllBytes())
  }

  @Test fun testText() {
    val request = PutObjectRequest.builder()
      .key("text-test.txt")
      .bucket("test")
      .build()
    s3Client().putObject(request, RequestBody.fromString("text test"))
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("text-test.txt");
        file.text().then(out => {
          test(out).isNotNull();
          test(out).isEqualTo("text test");
        });
      """
    }.doesNotFail()
  }

  @Test fun testJson() {
    val request = PutObjectRequest.builder()
      .key("json-test.json")
      .bucket("test")
      .build()
    s3Client().putObject(request, RequestBody.fromString("""{"test": "value"}"""))
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("json-test.json");
        file.json().then(out => {
          test(out).isNotNull();
          test(out.test).isNotNull();
          test(out.test).isEqualTo("value");
        });
      """
    }.doesNotFail()
  }

  @Test fun testRoundtripString() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("test.txt");
        file.write("test").then(_ => {
          file.text().then(output => {
            test(output).isNotNull();
            test(output).isEqualTo("test");
          });
        });
      """
    }.doesNotFail()
  }

  @Test fun testRoundtripJson() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("test.json");
        const data = { "test": "value" };
        file.write(JSON.stringify(data)).then(_ => {
          file.json().then(output => {
            test(output).isNotNull();
            test(output.test).isNotNull();
            test(output.test).isEqualTo("value");
          });
        });
      """
    }.doesNotFail()
  }

  @Test fun testBytes() {
    val request = PutObjectRequest.builder()
      .key("bytes-test.bin")
      .bucket("test")
      .build()
    val content = byteArrayOf(1, 2, 3, 4, 5)
    s3Client().putObject(request, RequestBody.fromBytes(content))
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("bytes-test.bin");
        file.bytes().then(out => {
          test(out).isNotNull();
          test(out instanceof Uint8Array).isEqualTo(true);
          const expected = new Uint8Array([1, 2, 3, 4, 5]);
          test(out.length).isEqualTo(expected.length);
          test(out.every((v, i) => v === expected[i])).isEqualTo(true);
        });
      """
    }.doesNotFail()
  }

  @Test fun testRoundtripBytes() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("roundtrip-bytes.bin");
        const data = new Uint8Array([5, 4, 3, 2, 1]);
        file.write(data).then(_ => {
          file.bytes().then(out => {
            test(out).isNotNull();
            test(out instanceof Uint8Array).isEqualTo(true);
            test(out.length).isEqualTo(data.length);
            test(out.every((v, i) => v === data[i])).isEqualTo(true);
          });
        });
      """
    }.doesNotFail()
  }

  @Test fun testArrayBuffer() {
    val request = PutObjectRequest.builder()
      .key("arraybuffer-test.bin")
      .bucket("test")
      .build()
    val content = byteArrayOf(1, 2, 3, 4, 5)
    s3Client().putObject(request, RequestBody.fromBytes(content))
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("arraybuffer-test.bin");
        file.arrayBuffer().then(out => {
          test(out).isNotNull();
          test(out instanceof ArrayBuffer).isEqualTo(true);
          const expected = [1, 2, 3, 4, 5]
          const view = new Uint8Array(out);
          test(view.length).isEqualTo(expected.length);
          test(view.every((v, i) => v === expected[i])).isEqualTo(true);
        });
      """
    }.doesNotFail()
  }

  @Test fun testRoundtripArrayBuffer() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("roundtrip-arraybuffer.bin");
        const data = new Uint8Array([5, 4, 3, 2, 1]);
        file.write(data).then(_ => {
          file.arrayBuffer().then(output => {
            test(output).isNotNull();
            test(output instanceof ArrayBuffer).isEqualTo(true);
            const view = new Uint8Array(output);
            test(view.length).isEqualTo(data.length);
            test(view.every((v, i) => v === data[i])).isEqualTo(true);
          });
        });
      """
    }.doesNotFail()
  }

  @Test fun testPresign() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("presign-test.txt");
        const url = file.presign();
        test(url).isNotNull();
        test(url.includes("presign-test.txt")).isEqualTo(true);
      """
    }.doesNotFail()
  }

  @Test fun testPresignWithMethod() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("presign-method-test.txt");
        const url = file.presign({ method: "PUT" });
        test(url).isNotNull();
        test(url.includes("presign-method-test.txt")).isEqualTo(true);
      """
    }.doesNotFail()
  }

  @Test fun testPresignWithExpiresIn() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("presign-expires-test.txt");
        const url = file.presign({ expiresIn: 100 });
        test(url).isNotNull();
        test(url.includes("presign-expires-test.txt")).isEqualTo(true);
      """
    }.doesNotFail()
  }

  @Test fun testPresignWithMethodAndExpiresIn() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("presign-full-test.txt");
        const url = file.presign({ method: "DELETE", expiresIn: 200 });
        test(url).isNotNull();
        test(url.includes("presign-full-test.txt")).isEqualTo(true);
      """
    }.doesNotFail()
  }

  @Test fun testPresignFailures() {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("presign-fail-test.txt");
        file.presign({ method: 123 });
      """
    }.fails()

    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("presign-fail-test.txt");
        file.presign({ expiresIn: "123" });
      """
    }.fails()

    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("presign-fail-test.txt");
        file.presign({ method: "INVALID" });
      """
    }.fails()
  }

  @ParameterizedTest
  @ValueSource(strings = ["delete", "unlink"])
  fun testDelete(funcName: String) {
    val putRequest = PutObjectRequest.builder()
      .key("${funcName}-test.txt")
      .bucket("test")
      .build()
    s3Client().putObject(putRequest, RequestBody.fromString("${funcName} test"))
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("${funcName}-test.txt");
        file.${funcName}();
      """
    }.doesNotFail()
    val request = GetObjectRequest.builder()
      .key("${funcName}-test.txt")
      .bucket("test")
      .build()
    try {
      s3Client().getObject(request)
    } catch (_: NoSuchKeyException) {
      return
    }
    fail("Object was not deleted.")
  }

  @ParameterizedTest
  @ValueSource(strings = ["delete", "unlink", "exists", "stat", "arrayBuffer",
    "bytes", "json", "text"])
  fun testNoArgs(funcName: String) {
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("${funcName}-fail-test.txt");
        file.${funcName}("some argument");
      """
    }.fails()
  }

  @Test fun testExists() {
    val request = PutObjectRequest.builder()
      .key("exists-test.bin")
      .bucket("test")
      .build()
    val content = byteArrayOf(1, 2, 3, 4, 5)
    s3Client().putObject(request, RequestBody.fromBytes(content))
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("exists-test.bin");
        file.exists().then(out => {
          test(out).isEqualTo(true);
        });
        const notAFile = client.file("exists-test-does-not-exist.bin");
        notAFile.exists().then(out => {
          test(out).isEqualTo(false);
        });
      """
    }.doesNotFail()
  }

  @Test fun testStat() {
    val metadata = mapOf("test-key" to "test-value")
    val putRequest = PutObjectRequest.builder()
      .key("stat-test.txt")
      .bucket("test")
      .metadata(metadata)
      .contentType("text/plain")
      .build()
    s3Client().putObject(putRequest, RequestBody.fromString("stat test"))
    executeGuest {
      // language=javascript
      """
        ${clientPrelude()}
        const file = client.file("stat-test.txt");
        file.stat().then(out => {
          test(out).isNotNull();
          test(out.eTag).isNotNull();
          test(out.contentType).isEqualTo("text/plain");
          test(out.contentLength === 9).isEqualTo(true);
          test(out.lastModified).isNotNull();
          test(out.metadata).isNotNull();
          test(out.metadata["test-key"]).isEqualTo("test-value");
        });
      """
    }.doesNotFail()
  }
}
