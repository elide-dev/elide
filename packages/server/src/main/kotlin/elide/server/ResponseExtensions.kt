@file:Suppress("unused")

package elide.server

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import kotlinx.coroutines.runBlocking
import kotlinx.css.CssBuilder
import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets


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
 * Responds to a client with an HTML response, using specified [block] to build an HTML page via Kotlin's HTML DSL.
 *
 * @param block Block to execute to build the HTML page.
 * @return HTTP response wrapping the HTML page, with a content type of `text/html; charset=utf-8`.
 */
public suspend fun html(block: suspend HTML.() -> Unit): HttpResponse<ByteArrayOutputStream> {
  return HttpResponse.ok(
    HtmlContent(builder = block).render()
  ).characterEncoding(StandardCharsets.UTF_8).contentType(
    "text/html; charset=utf-8"
  )
}

// HTML content rendering and container utility.
internal class HtmlContent (
  private val prettyhtml: Boolean = false,
  private val builder: suspend HTML.() -> Unit
): ResponseRenderer<ByteArrayOutputStream> {
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
public fun css(block: CssBuilder.() -> Unit): HttpResponse<ByteArray> {
  return HttpResponse.ok(
    CssContent(block).render()
  ).characterEncoding(
    StandardCharsets.UTF_8
  ).contentType(
    "text/css; chartset=utf-8"
  )
}

// HTML content rendering and container utility.
internal class CssContent (
  private val builder: CssBuilder.() -> Unit
): ResponseRenderer<ByteArray> {
  override fun render(): ByteArray {
    return CssBuilder().apply(builder).toString().toByteArray(
      StandardCharsets.UTF_8
    )
  }
}
