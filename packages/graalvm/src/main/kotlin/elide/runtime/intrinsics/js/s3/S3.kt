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
package elide.runtime.intrinsics.js.s3

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.Blob
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.node.buffer.GuestBytes
import elide.vm.annotations.Polyglot

private const val ACCESS_KEY_ID = "accessKeyId"
private const val SECRET_ACCESS_KEY = "secretAccessKey"
private const val BUCKET = "bucket"
private const val SESSION_TOKEN = "sessionToken"
private const val ACL = "acl"
private const val ENDPOINT = "endpoint"
private const val REGION = "region"
private const val VIRTUAL_HOSTED_STYLE = "virtualHostedStyle"

/**
 * # S3
 *
 * Facade for calling out to AWS S3 SDK. Meant to be used to provide an S3
 * API from within the Elide runtime.
 *
 * It consists of two major objects, the S3Client which tracks the configuration for authentication and how to connect
 * and the S3File which represents a file in S3 that can be read, written to, or presigned.
 *
 * &nbsp;
 *
 * ## Usage
 *
 * In the following example, the S3Client will connect to Backblaze B2 (an S3 compatible service)
 * using a path style endpoint. If the endpoint is not specified, by default it will use AWS S3.
 * If region is unspecified it will use us-east-1.
 * It will use path style URLs by default.
 *
 * Please look at AWS's S3 documentation or the documentation for whatever S3 compatible API is being used for more
 * information on what these options mean.
 *
 * ```javascript
 * const { S3Client } = require("elide:s3");
 * const client = new S3Client({
 *   // required options
 *   accessKeyId: "...",
 *   secretAccessKey: "...",
 *   bucket: "my-bucket",
 *   // optional options
 *   sessionToken: "...",
 *   acl: "public-read",
 *   endpoint: "https://s3.us-east-005.backblazeb2.com/my-bucket",
 *   region: "us-east-005",
 *   virtualHostedStyle: false
 * });
 * const file = client.file("test.txt");
 * await file.write("This is a test", { type: "text/plain" });
 * console.assert(await file.text() === "This is a test");
 * ```
 *
 * Write method takes either a String or any array type object and returns a promise
 * containing the content length.
 *
 * Read methods include: text, json, bytes, and arrayBuffer. Which are self-explanatory.
 *
 * Presign allows one to presign a URL to be passed to the client so the client can do the
 * actual action, minimizing a redundant data transfer, a common pattern with S3 usage.
 * eg
 * ```
 * file.presign({ method: "GET", expiresIn: 3600 }); // expiresIn is in seconds
 * ```
 *
 * `delete` and `unlink` are identical. Both allow the user to delete a file from S3.
 *
 * Finally, `stat` returns the metadata and objects in the HTTP header in a JSON format. It
 * is equivalent to the "HeadObject" action.
 */
public interface S3Client : ProxyObject {
  @Polyglot public fun file(path: String): S3File
}

public interface S3File : ProxyObject {
  // write methods
  @Polyglot public fun write(data: String, contentType: String?): JsPromise<Number>
  @Polyglot public fun write(data: GuestBytes, contentType: String?): JsPromise<Number>
  @Polyglot public fun write(data: Blob, contentType: String?): JsPromise<Number>
  @Polyglot public fun write(data: ReadableStream, contentType: String?): JsPromise<Number>

  // read methods
  @Polyglot public fun text(): JsPromise<String>
  @Polyglot public fun json(): JsPromise<Value>
  @Polyglot public fun bytes(): JsPromise<Value>
  @Polyglot public fun arrayBuffer(): JsPromise<Value>

  @Polyglot public fun presign(method: String, duration: Long): String
  @Polyglot public fun delete(): JsPromise<Unit>

  @Polyglot public fun exists(): JsPromise<Boolean>
  @Polyglot public fun stat(): JsPromise<ProxyObject>
}

@JvmRecord public data class S3ClientConstructorOptions(
  @get:Polyglot val accessKeyId: String,
  @get:Polyglot val secretAccessKey: String,
  @get:Polyglot val bucket: String,
  @get:Polyglot val sessionToken: String?,
  @get:Polyglot val acl: String?,
  @get:Polyglot val endpoint: String?,
  @get:Polyglot val region: String?,
  @get:Polyglot val virtualHostedStyle: Boolean?
) {
  public companion object {
    @JvmStatic public fun from(value: Value): S3ClientConstructorOptions {
      return S3ClientConstructorOptions(
        // required
        accessKeyId = value.getMember(ACCESS_KEY_ID).asString()
          ?: throw JsError.typeError("S3Client constructor missing accessKeyId"),
        secretAccessKey = value.getMember(SECRET_ACCESS_KEY).asString()
          ?: throw JsError.typeError("S3Client constructor missing secretAccessKey"),
        bucket = value.getMember(BUCKET).asString()
          ?: throw JsError.typeError("S3Client constructor missing bucket"),

        // optional
        sessionToken = value.getMember(SESSION_TOKEN)?.asString(),
        acl = value.getMember(ACL)?.asString(),
        endpoint = value.getMember(ENDPOINT)?.asString(),
        region = value.getMember(REGION)?.asString(),
        virtualHostedStyle = value.getMember(VIRTUAL_HOSTED_STYLE)?.asBoolean()
      )
    }
  }
}
