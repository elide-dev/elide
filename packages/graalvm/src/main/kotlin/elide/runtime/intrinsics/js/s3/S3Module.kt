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

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.DeleteObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.HeadObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletionException
import elide.annotations.Singleton
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.Blob
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.node.buffer.GuestBufferView
import elide.runtime.node.buffer.GuestBytes
import elide.vm.annotations.Polyglot


private const val S3_MODULE_NAME = "s3"
private const val S3_CLIENT_SYMBOL = "S3Client"

private const val CONTENT_TYPE_DEFAULT = "application/octet-stream"

private const val DEFAULT_PRESIGN_METHOD = "GET"
private const val DEFAULT_PRESIGN_DURATION: Long = 86400 // 24 hours

@Singleton
@Intrinsic
internal class S3Module (private val guestExec: GuestExecutorProvider) : AbstractJsIntrinsic() {
  private val constructor = ProxyInstantiable { arguments ->
    val options = arguments.getOrNull(0)
      ?: throw JsError.typeError("No options passed!")
    val config = S3ClientConstructorOptions.from(options)
    S3ClientProxy(this.guestExec, config)
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(S3_MODULE_NAME)) {
      ProxyObject.fromMap(mapOf(
        S3_CLIENT_SYMBOL to constructor,
      ))
    }
  }
}

public class S3ClientProxy (private val guestExec: GuestExecutorProvider,
                            private val config: S3ClientConstructorOptions) : S3Client {
  private val s3Client: S3AsyncClient
  private val credentialsProvider: AwsCredentialsProvider
  private val clientRegion: Region?

  // This map represents the JavaScript object representation of the class to the guest environment
  // i.e. it is a map from strings to methods (in this case)
  private val jsObj: Map<String, Any> = mapOf(
    "file" to ProxyExecutable { args ->
      val path = args.getOrNull(0)?.asString() ?:
        throw JsError.typeError("S3Client.file() requires one string argument")
      file(path)
    }
  )

  init {
    val credentials = AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey)
    this.credentialsProvider = StaticCredentialsProvider.create(credentials)
    this.clientRegion = when {
      config.region != null -> Region.of(config.region)
      config.endpoint == null -> Region.US_EAST_1
      else -> null
    }

    this.s3Client = S3AsyncClient.builder()
      .credentialsProvider(credentialsProvider)
      .apply {
        config.endpoint?.let {
          endpointOverride(URI.create(it))
        }

        clientRegion?.let {
          region(clientRegion)
        }

        if (config.virtualHostedStyle != true) {
          forcePathStyle(true)
        }
      }
      .build()
  }
  @Polyglot override fun file(path: String): S3File {
    return S3FileProxy(guestExec, s3Client, clientRegion, credentialsProvider, path, config.bucket, config.acl)
  }

  override fun getMember(key: String?): Any? = jsObj[key]
  override fun getMemberKeys(): Any? = ProxyArray.fromArray(jsObj.keys)
  override fun hasMember(key: String?): Boolean = jsObj.containsKey(key)
  override fun putMember(key: String?, value: Value?): Nothing =
    throw JsError.typeError("Cannot set property '$key' on S3Client object.")
}

public class S3FileProxy(
  private val guestExec: GuestExecutorProvider,
  private val s3Client: S3AsyncClient,
  private val region: Region?,
  private val credentialsProvider: AwsCredentialsProvider,
  private val path: String,
  private val bucket: String?,
  private val acl: String?) : S3File {

  // This map represents the JavaScript object representation of the class to the guest environment
  // i.e. it is a map from strings to methods (in this case)
  private val jsObj: Map<String, Any> = mapOf(
    "write" to ProxyExecutable { args ->
      val data = args.getOrNull(0)
        ?: throw JsError.typeError("S3File.write() requires one argument for data.")

      val contentType = args.getOrNull(1)?.let { options ->
        if (!options.hasMembers()) // options isn't a javascript object
          throw JsError.typeError("S3File.write() second argument must be an object containing a 'type' field " +
                                          "signifying MIME type of the data to be written.")

        if (options.hasMember("type")) {
          val typeMember = options.getMember("type")
          if (typeMember.isString) {
            typeMember.asString()
          } else {
            throw JsError.typeError(
              "Option 'type' must be a string, but received a value of type '${typeMember.metaObject.metaSimpleName}'"
            )
          }
        } else {
          null
        }
      }

      val bufferView = GuestBufferView.tryFrom(data)
      when {
        data.isString -> write(data.asString(), contentType)
        bufferView != null -> write(bufferView.bytes(), contentType)
        else -> throw JsError.typeError("Unsupported data type for S3File.write().")
      }
    },
    "text" to ProxyExecutable { args ->
      if (args.isNotEmpty()) {
        throw JsError.typeError("S3File.text() takes zero arguments.")
      }
      text()
    },
    "presign" to ProxyExecutable { args ->
      val (method, expiresIn) = args.getOrNull(0)?.let { options ->
        if (!options.hasMembers()) {
          throw JsError.typeError("S3File.presign() takes an object with optional 'method' and 'expiresIn' properties.")
        }

        val method = when {
          !options.hasMember("method") -> DEFAULT_PRESIGN_METHOD
          options.getMember("method").isString -> options.getMember("method").asString()
          else -> throw JsError.typeError("S3File.presign() 'method' property must be a string.")
        }

        val expiresIn = when {
          !options.hasMember("expiresIn") -> DEFAULT_PRESIGN_DURATION
          options.getMember("expiresIn").isNumber -> options.getMember("expiresIn").asLong()
          else -> throw JsError.typeError("S3File.presign() 'expiresIn' property must be a number.")
        }

        method to expiresIn
      } ?: (DEFAULT_PRESIGN_METHOD to DEFAULT_PRESIGN_DURATION)

      presign(method, expiresIn)
    },
    "json" to ProxyExecutable { args ->
      if (args.isNotEmpty()) {
        throw JsError.typeError("S3File.json() takes zero arguments.")
      }
      json()
    },
    "bytes" to ProxyExecutable { args ->
      if (args.isNotEmpty()) {
        throw JsError.typeError("S3File.bytes() takes zero arguments.")
      }
      bytes()
    },
    "arrayBuffer" to ProxyExecutable { args ->
      if (args.isNotEmpty()) {
        throw JsError.typeError("S3File.arrayBuffer() takes zero arguments.")
      }
      arrayBuffer()
    },
    "delete" to ProxyExecutable { args ->
      if (args.isNotEmpty()) {
        throw JsError.typeError("S3File.delete() takes zero arguments.")
      }
      delete()
    },
    "unlink" to ProxyExecutable { args ->
      if (args.isNotEmpty()) {
        throw JsError.typeError("S3File.unlink() takes zero arguments.")
      }
      delete()
    },
    "exists" to ProxyExecutable { args ->
      if (args.isNotEmpty()) {
        throw JsError.typeError("S3File.exists() takes zero arguments.")
      }
      exists()
    },
    "stat" to ProxyExecutable { args ->
      if (args.isNotEmpty()) {
        throw JsError.typeError("S3File.exists() takes stat arguments.")
      }
      stat()
    },
  )

  private fun write(data: AsyncRequestBody, contentType: String?): JsPromise<Number> {
    // if content type not specified, infer from path
    // default to "application/octet-stream" if content type cannot be inferred from path
    val contentType = contentType ?: (URLConnection.guessContentTypeFromName(path) ?: CONTENT_TYPE_DEFAULT)
    val request = PutObjectRequest.builder()
      .bucket(bucket)
      .key(path)
      .contentType(contentType)
      .apply {
        acl?.let { acl(acl) }
      }
      .build()
    val future = guestExec.executor().submit<Number> {
      s3Client.putObject(request, data).join().size()
    }
    return JsPromise.wrap(future)
  }

  @Polyglot override fun write(data: String, contentType: String?): JsPromise<Number> {
    return write(AsyncRequestBody.fromString(data), contentType)
  }

  @Polyglot override fun write(data: GuestBytes, contentType: String?): JsPromise<Number> {
    val bytes = ByteArray(data.size) { i -> data[i] }
    return write(AsyncRequestBody.fromBytes(bytes), contentType)
  }

  @Polyglot override fun write(data: Blob, contentType: String?): JsPromise<Number> {
    TODO("Blob interface not yet implemented.")
  }

  override fun write(
    data: ReadableStream,
    contentType: String?
  ): JsPromise<Number> {
    TODO("Not yet implemented")
  }

  private fun <T: Any> readS3Object(transform: (ResponseBytes<GetObjectResponse>) -> T): JsPromise<T> {
    val request = GetObjectRequest.builder()
      .bucket(bucket)
      .key(path)
      .build()
    val future = guestExec.executor().submit<T> {
      val bytes: ResponseBytes<GetObjectResponse> =
        this.s3Client.getObject(request, AsyncResponseTransformer.toBytes()).get()
      transform(bytes)
    }
    return JsPromise.wrap(future)
  }

  @Polyglot override fun text(): JsPromise<String> {
    return readS3Object { bytes ->
      bytes.asString(StandardCharsets.UTF_8)
    }
  }

  @Polyglot override fun json(): JsPromise<Value> {
    return readS3Object { bytes ->
      val jsonString = bytes.asString(StandardCharsets.UTF_8)
      val context = Context.getCurrent()
      context.eval("js", "JSON.parse").execute(jsonString)
    }
  }

  @Polyglot override fun bytes(): JsPromise<Value> {
    return readS3Object { bytes ->
      val buffer = bytes.asByteBuffer()
      ArrayBufferViews.newView(ArrayBufferViewType.Uint8Array, buffer)
    }
  }

  @Polyglot override fun arrayBuffer(): JsPromise<Value> {
    return readS3Object { bytes ->
      val buffer = bytes.asByteBuffer()
      val uint8Array = ArrayBufferViews.newView(ArrayBufferViewType.Uint8Array, buffer)
      uint8Array.getMember("buffer")
    }
  }

  @Polyglot override fun presign(method: String, duration: Long): String {
    // There has to be a better way to do this...
    val presigner = S3Presigner.builder()
      .credentialsProvider(credentialsProvider)
      .apply { region?.let { region(region) } }
      .build()
    val presigned = when (method) {
        "PUT" -> {
          val putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(path)
            .apply {
              acl?.let { acl(acl) }
            }
            .build()

          val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(duration))
            .putObjectRequest(putRequest)
            .build()

          presigner.presignPutObject(presignRequest)
        }
      "GET" -> {
        val getRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(path)
          .build()

        val presignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofSeconds(duration))
          .getObjectRequest(getRequest)
          .build()

        presigner.presignGetObject(presignRequest)
      }
      "DELETE" -> {
        val deleteRequest = DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(path)
          .build()
        val presignRequest = DeleteObjectPresignRequest.builder()
          .signatureDuration(Duration.ofSeconds(duration))
          .deleteObjectRequest(deleteRequest)
          .build()
        presigner.presignDeleteObject(presignRequest)
      }
      "HEAD" -> {
        val headRequest = HeadObjectRequest.builder()
          .bucket(bucket)
          .key(path)
          .build()
        val presignRequest = HeadObjectPresignRequest.builder()
          .signatureDuration(Duration.ofSeconds(duration))
          .headObjectRequest(headRequest)
          .build()
        presigner.presignHeadObject(presignRequest)
      }
      else -> throw JsError.typeError("S3File.presign() only takes GET, PUT, DELETE, and HEAD methods.")
    }
    return presigned.url().toExternalForm()
  }

  @Polyglot override fun delete(): JsPromise<Unit> {
    val request = DeleteObjectRequest.builder()
      .key(path)
      .bucket(bucket)
      .build()
    val future = guestExec.executor().submit<Unit> {
      s3Client.deleteObject(request).join()
    }
    return JsPromise.wrap(future)
  }

  @Polyglot override fun exists(): JsPromise<Boolean> {
    val request = HeadObjectRequest.builder()
      .key(path)
      .bucket(bucket)
      .build()
    val future = guestExec.executor().submit<Boolean> {
      try {
        s3Client.headObject(request).join()
        true
      } catch (e: CompletionException) {
        if (e.cause is NoSuchKeyException) false
        else throw e
      }
    }
    return JsPromise.wrap(future)
  }

  @Polyglot override fun stat(): JsPromise<ProxyObject> {
    val request = HeadObjectRequest.builder()
      .key(path)
      .bucket(bucket)
      .build()
    val future = guestExec.executor().submit<ProxyObject> {
      val response = s3Client.headObject(request).join()
      val statData = mapOf(
        "eTag" to response.eTag(),
        "contentType" to response.contentType(),
        "contentLength" to response.contentLength(),
        "lastModified" to response.lastModified().toString(),
        "metadata" to ProxyObject.fromMap(response.metadata() as Map<String, Any>)
      )
      ProxyObject.fromMap(statData)
    }
    return JsPromise.wrap(future)
  }

  override fun getMember(key: String?): Any? = jsObj[key]
  override fun getMemberKeys(): Any? = ProxyArray.fromArray(jsObj.keys)
  override fun hasMember(key: String?): Boolean = jsObj.containsKey(key)
  override fun putMember(key: String?, value: Value?): Nothing =
    throw JsError.typeError("Cannot set property '$key' on S3File object.")
}
