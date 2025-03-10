/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

@file:Suppress("unused", "TooManyFunctions", "WildcardImport")

package elide.server

import io.micronaut.http.*
import org.reactivestreams.Publisher
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import elide.server.controller.PageController

/**
 * Raw bytes body type used internally by Elide.
 */
public typealias RawPayload = ByteArray

/**
 * Raw bytes response typealias used internally by Elide.
 */
public typealias RawResponse = HttpResponse<RawPayload>

/**
 * Raw streamed file alias, used internally for assets.
 */
public typealias StreamedAsset = Pair<MediaType, ByteArray>

/**
 * Raw streamed file response, used internally for assets with a type attached.
 */
public typealias StreamedAssetResponse = MutableHttpResponse<StreamedAsset>

/**
 * Raw streamed file wrapper, after unwrapping the type.
 */
public typealias FinalizedAsset = ByteArray

/**
 * Raw streamed asset response, used internally for assets.
 */
public typealias FinalizedAssetResponse = MutableHttpResponse<FinalizedAsset>

/** Describes the expected interface for a response rendering object which leverages co-routines. */
public interface SuspensionRenderer<R> {
  /** @return Rendered result. */
  public suspend fun render(): R
}

/** Describes the expected interface for a streaming response rendered via co-routines. */
public interface StreamingSuspensionRenderer<R> {
  /** @return Rendered result. */
  public suspend fun render(): Publisher<R>
}

/**
 * Describes a handler object which can respond to a request with a given [ResponseBody] type; these throw-away handlers
 * are typically spawned by utility functions to create a context where rendering can take place.
 */
public interface ResponseHandler<ResponseBody> {
  /**
   * Respond to the request with the provided [response].
   *
   * @param response Response to provide.
   * @return Response, after registration with the object.
   */
  public suspend fun respond(response: MutableHttpResponse<ResponseBody>): MutableHttpResponse<ResponseBody>
}

// Shared logic for response handler contexts internal to Elide.
public abstract class BaseResponseHandler<ResponseBody> : ResponseHandler<ResponseBody> {
  private val acquired: AtomicBoolean = AtomicBoolean(false)
  private val response: AtomicReference<MutableHttpResponse<ResponseBody>?> = AtomicReference(null)


  override suspend fun respond(response: MutableHttpResponse<ResponseBody>): MutableHttpResponse<ResponseBody> {
    this.acquired.compareAndSet(false, true)
    this.response.set(response)
    return response
  }

  /**
   * Finalize the request being handled by this [ResponseHandler], by producing a terminal [HttpResponse].
   *
   * @return Finalized HTTP response.
   */
  internal abstract suspend fun finalize(): HttpResponse<ResponseBody>
}

/**
 * Serve a static file which is embedded in the application JAR, at the path `/static/[file]`.
 *
 * @param file Filename to load from the `/static` root directory.
 * @param contentType `Content-Type` value to send back for this file.
 * @return HTTP response wrapping the desired asset, or an HTTP response which serves a 404 if the asset could not be
 *    located at the specified path.
 */
public fun staticFile(file: String, contentType: String): HttpResponse<*> {
  val cleanedPath = file.removePrefix("/static").removePrefix("/")
  val target = HtmlRenderer::class.java.getResourceAsStream("/static/$cleanedPath")
  return if (target != null) {
    HttpResponse.ok(
      target.bufferedReader(StandardCharsets.UTF_8).readText()
    ).contentType(
      contentType
    )
  } else {
    HttpResponse.notFound<Any>()
  }
}

/**
 * Typealias for a registered string which is used as an Asset Module ID.
 */
public typealias AssetModuleId = String

/**
 * Typealias for a registered string which is used as an Asset Tag.
 */
public typealias AssetTag = String

/**
 * Responds to a client with an HTML response, using specified [block] to build an HTML page via Kotlin's HTML DSL.
 *
 * @param block Block to execute to build the HTML page.
 * @return HTTP response wrapping the HTML page, with a content type of `text/html; charset=utf-8`.
 */
public suspend fun PageController.html(block: suspend HTML.() -> Unit): RawResponse {
  return HttpResponse.ok(
    HtmlRenderer(
      builder = block,
      handler = this,
    ).render().toByteArray()
  ).characterEncoding(
    StandardCharsets.UTF_8
  ).contentType(
    "text/html;charset=utf-8",
  )
}

// HTML content rendering and container utility.
public class HtmlRenderer(
  private val prettyhtml: Boolean = false,
  private val handler: PageController? = null,
  private val builder: suspend HTML.() -> Unit,
) : SuspensionRenderer<ByteArrayOutputStream> {
  override suspend fun render(): ByteArrayOutputStream {
    val baos = ByteArrayOutputStream()
    baos.bufferedWriter(StandardCharsets.UTF_8).use {
      it.write("<!doctype html>")
      it.appendHTML(
        prettyPrint = prettyhtml,
      ).htmlSuspend(
        block = builder
      )
    }
    return baos
  }
}

@HtmlTagMarker
public suspend inline fun <T, C : TagConsumer<T>> C.htmlSuspend(
  namespace : String? = null,
  crossinline block : suspend HTML.() -> Unit
) : T = HTML(
  emptyMap,
  this,
  namespace
).visitAndFinalizeSuspend(
  this,
  block,
)


public suspend inline fun <T : Tag, R> T.visitAndFinalizeSuspend(
  consumer: TagConsumer<R>,
  crossinline block: suspend T.() -> Unit
): R = visitTagAndFinalize(consumer) {
  block()
}
