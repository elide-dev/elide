/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import org.reactivestreams.Publisher
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.css.CssBuilder
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import elide.server.assets.AssetType
import elide.server.controller.ElideController
import elide.server.controller.PageController
import elide.ssr.ResponseRenderer

/**
 * Raw bytes body type used internally by Elide.
 */
public typealias RawPayload = ByteArrayOutputStream

/**
 * Raw bytes response typealias used internally by Elide.
 */
public typealias RawResponse = HttpResponse<RawPayload>

/**
 * Raw streamed file alias, used internally for assets.
 */
public typealias StreamedAsset = Pair<MediaType, InputStream>

/**
 * Raw streamed file response, used internally for assets.
 */
public typealias StreamedAssetResponse = MutableHttpResponse<StreamedAsset>

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
      target
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
 * Serve an application asset file which is embedded in the application JAR as a registered server asset, from the
 * application resource path `/assets`.
 *
 * To use module ID-based assets, files must be registered at build time through the Elide Plugin for Gradle, or must
 * produce an equivalent protocol buffer manifest.
 *
 * @param moduleId ID of the asset module we wish to serve.
 * @param type Specifies the asset type expected to be served by this call, if known.
 * @return HTTP response wrapping the desired asset, or an HTTP response which serves a 404 if the asset could not be
 *    located at the specified path.
 */
public suspend fun PageController.asset(
  request: HttpRequest<*>,
  moduleId: AssetModuleId,
  type: AssetType? = null
): StreamedAssetResponse {
  val handler = AssetHandler(type, this, request)
  handler.module(moduleId)
  return handler.finalize()
}

/**
 * Serve a static script asset embedded within the application, based on the provided [moduleId], and customizing the
 * response based on the provided [request].
 *
 * @param request Request to consider when creating the asset response.
 * @param moduleId Module ID for the asset which we wish to serve.
 * @return Streamed asset response for the desired module ID.
 */
public suspend fun PageController.script(request: HttpRequest<*>, moduleId: AssetModuleId): StreamedAssetResponse {
  return asset(
    request,
    moduleId,
    AssetType.SCRIPT,
  )
}

/**
 * Serve a static script asset embedded within the application, based on the provided [block], which should customize
 * the serving of the script and declare a module ID.
 *
 * @param request Request to consider when creating the asset response.
 * @param block Block which, when executed, will configure the script for a response.
 * @return Streamed asset response generated from the configuration provided by [block].
 */
public suspend fun PageController.script(
  request: HttpRequest<*>,
  block: AssetHandler.() -> Unit
): StreamedAssetResponse {
  return asset(
    request,
    AssetType.SCRIPT,
    block,
  )
}

/**
 * Serve a static stylesheet asset embedded within the application, based on the provided [moduleId], and customizing
 * the response based on the provided [request].
 *
 * @param request Request to consider when creating the asset response.
 * @param moduleId Module ID for the asset which we wish to serve.
 * @return Streamed asset response for the desired module ID.
 */
public suspend fun PageController.stylesheet(request: HttpRequest<*>, moduleId: AssetModuleId): StreamedAssetResponse {
  return asset(
    request,
    moduleId,
    AssetType.STYLESHEET,
  )
}

/**
 * Serve a static stylesheet asset embedded within the application, based on the provided [block], which should
 * customize the serving of the document and declare a module ID.
 *
 * @param request Request to consider when creating the asset response.
 * @param block Block which, when executed, will configure the stylesheet for a response.
 * @return Streamed asset response generated from the configuration provided by [block].
 */
public suspend fun PageController.stylesheet(
  request: HttpRequest<*>,
  block: AssetHandler.() -> Unit
): StreamedAssetResponse {
  return asset(
    request,
    AssetType.STYLESHEET,
    block,
  )
}

/**
 * Generate a [StreamedAssetResponse] which serves an asset embedded within the application, and specified by the
 * provided [block]; [request] will be considered when producing the response.
 *
 * @param request HTTP request to consider when producing the desired asset response.
 * @param type Type of asset expected to be returned with this response.
 * @param block Block to customize the serving of this asset and declare a module ID.
 * @return Structure which streams the resolved asset content as the response.
 */
public suspend fun PageController.asset(
  request: HttpRequest<*>,
  type: AssetType? = null,
  block: suspend AssetHandler.() -> Unit
): StreamedAssetResponse {
  val handler = AssetHandler(type, this, request)
  block.invoke(handler)
  return handler.finalize()
}

// Handler context for an asset serving cycle.
public class AssetHandler(
  initialExpectedType: AssetType? = null,
  private val handler: ElideController,
  private val request: HttpRequest<*>,
  private val moduleId: AtomicReference<AssetModuleId?> = AtomicReference(null),
  private val expectedType: AtomicReference<AssetType?> = AtomicReference(initialExpectedType),
) : BaseResponseHandler<StreamedAsset>() {
  /** Bind an asset handler to an asset module ID. */
  public fun module(id: AssetModuleId, type: AssetType? = null) {
    moduleId.set(id)
    if (type != null) expectedType.set(type)
  }

  /** Declare the expected asset type for this handler. Optional. */
  public fun assetType(type: AssetType) {
    expectedType.set(type)
  }

  override suspend fun finalize(): MutableHttpResponse<StreamedAsset> {
    return respond(
      handler.assets().serveAsync(
        request,
        moduleId.get(),
      ).await()
    )
  }
}

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
    ).render()
  ).characterEncoding(StandardCharsets.UTF_8).contentType(
    "text/html; charset=utf-8"
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

/**
 * Responds to a client with an HTML response, using specified [block] to build the CSS document via Kotlin's CSS DSL.
 *
 * @param block Block to execute to build the CSS document.
 * @return HTTP response wrapping the CSS content, with a content type of `text/css; charset=utf-8`.
 */
public fun css(block: CssBuilder.() -> Unit): StreamedAssetResponse {
  return HttpResponse.ok(
    CssContent(block).render()
  ).characterEncoding(
    StandardCharsets.UTF_8
  ).contentType(
    "text/css; chartset=utf-8"
  )
}

// HTML content rendering and container utility.
internal class CssContent(
  private val builder: CssBuilder.() -> Unit
) : ResponseRenderer<StreamedAsset> {
  override fun render(): StreamedAsset {
    val contentBytes = CssBuilder().apply(builder).toString().toByteArray(
      StandardCharsets.UTF_8
    )
    return AssetType.STYLESHEET.mediaType to ByteArrayInputStream(contentBytes)
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
