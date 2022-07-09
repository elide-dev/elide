@file:Suppress("unused")

package elide.server

import elide.server.assets.AssetType
import elide.server.controller.ElideController
import elide.server.controller.PageController
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.server.netty.types.files.NettyStreamedFileCustomizableResponseType
import kotlinx.coroutines.runBlocking
import kotlinx.css.CssBuilder
import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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
public typealias StreamedAsset = NettyStreamedFileCustomizableResponseType

/**
 * Raw streamed file response, used internally for assets.
 */
public typealias StreamedAssetResponse = HttpResponse<StreamedAsset>

/** Describes the expected interface for a response rendering object. */
public interface ResponseRenderer<R> {
  /** @return Rendered result. */
  public fun render(): R
}

/** Describes the expected interface for a response rendering object which leverages co-routines. */
public interface SuspensionRenderer<R> {
  /** @return Rendered result. */
  public suspend fun render(): R
}

/** */
public fun interface ResponseConfigurator<Context : ResponseHandler<ResponseBody>, RequestBody, ResponseBody> {
  /**
   *
   */
  public fun respond(handler: Context.() -> Unit)
}

/**
 *
 */
public interface ResponseHandler<ResponseBody> {
  /**
   *
   */
  public suspend fun respond(response: HttpResponse<ResponseBody>)
}

// Shared logic for response handler contexts internal to Elide.
public abstract class BaseResponseHandler<ResponseBody> : ResponseHandler<ResponseBody> {
  private val acquired: AtomicBoolean = AtomicBoolean(false)
  private val response: AtomicReference<HttpResponse<ResponseBody>?> = AtomicReference(null)

  /** @inheritDoc */
  override suspend fun respond(response: HttpResponse<ResponseBody>) {
    this.acquired.compareAndSet(false, true)
    this.response.set(response)
  }

  /**
   *
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
  val target = HtmlContent::class.java.getResourceAsStream("/static/$file")
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
 * Serve an application asset file which is embedded in the application JAR, from the path `/assets/[type]/[path]`.
 *
 * @param path Path to the file within the provided [type] directory.
 * @param type Type of asset to serve; accepted values are `css` and `js`.
 * @param contentType Resolved [MediaType] to use when serving this asset. Must not be null.
 * @return HTTP response wrapping the desired asset, or an HTTP response which serves a 404 if the asset could not be
 *    located at the specified path.
 */
public fun asset(path: String, type: String, contentType: MediaType?): HttpResponse<*> {
  return if (path.isBlank() || type.isBlank() || contentType == null) {
    HttpResponse.notFound<Any>()
  } else {
    val file = HtmlContent::class.java.getResourceAsStream("/assets/$type/$path")
    if (file == null) {
      HttpResponse.notFound<Any>()
    } else {
      HttpResponse.ok(
        file
      ).characterEncoding(
        StandardCharsets.UTF_8
      ).contentType(
        contentType
      )
    }
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
 *
 */
public suspend fun PageController.script(request: HttpRequest<*>, moduleId: AssetModuleId): StreamedAssetResponse {
  return asset(
    request,
    moduleId,
    AssetType.SCRIPT,
  )
}

/**
 *
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
 *
 */
public suspend fun PageController.stylesheet(request: HttpRequest<*>, moduleId: AssetModuleId): StreamedAssetResponse {
  return asset(
    request,
    moduleId,
    AssetType.STYLESHEET,
  )
}

/**
 *
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
 *
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

  /** @inheritDoc */
  override suspend fun finalize(): HttpResponse<StreamedAsset> {
    return handler.assets().serveAsync(
      request
    ).await()
  }
}

/**
 * Responds to a client with an HTML response, using specified [block] to build an HTML page via Kotlin's HTML DSL.
 *
 * @param block Block to execute to build the HTML page.
 * @return HTTP response wrapping the HTML page, with a content type of `text/html; charset=utf-8`.
 */
public suspend fun html(block: suspend HTML.() -> Unit): RawResponse {
  return HttpResponse.ok(
    HtmlContent(builder = block).render()
  ).characterEncoding(StandardCharsets.UTF_8).contentType(
    "text/html; charset=utf-8"
  )
}

// HTML content rendering and container utility.
internal class HtmlContent(
  private val prettyhtml: Boolean = false,
  private val builder: suspend HTML.() -> Unit
) : ResponseRenderer<ByteArrayOutputStream> {
  override fun render(): ByteArrayOutputStream {
    val baos = ByteArrayOutputStream()
    baos.bufferedWriter(StandardCharsets.UTF_8).use {
      it.appendHTML(
        prettyPrint = prettyhtml,
      ).html(
        block = {
          runBlocking {
            builder()
          }
        }
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
    TODO("not yet implemented")
//    return CssBuilder().apply(builder).toString().toByteArray(
//      StandardCharsets.UTF_8
//    )
  }
}
