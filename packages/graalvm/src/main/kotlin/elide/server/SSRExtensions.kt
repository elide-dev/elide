@file:Suppress("unused")

package elide.server

import elide.runtime.graalvm.JsRuntime
import elide.server.ssr.ServerRenderer
import elide.server.ssr.ServerSSRRenderer
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

// Path within app JARs for embedded script assets.
private const val embeddedRoot: String = "embedded"

// Production script name default.
private const val nodeProdDefault: String = "node-prod.pack.js"

// Development script name default.
private const val nodeDevDefault: String = "node-dev.pack.js"

// Default name if no mode is specified or resolvable.
public const val nodeSsrDefaultPath: String = nodeDevDefault

// Default ID to use in the DOM.
public const val defaultSsrDomId: String = "root"

/**
 * Load and serve a JavaScript bundle server-side, executing it within the context of an isolated GraalVM JavaScript
 * runtime; then, collect the output and return it as an HTTP response.
 *
 * Additional response properties, such as headers, may be set on the return result, as it is kept mutable. To change
 * initial parameters like the HTTP status, use the [response] parameter via constructors like [HttpResponse.notFound].
 *
 * @param path Path to the React SSR entrypoint script, which should be embedded within the asset section of the JAR.
 * @param response Mutable HTTP response to fill with the resulting SSR content. Sets the status and headers.
 * @return HTTP response wrapping the generated React SSR output, or an HTTP response which serves a 404 if the asset
 *    could not be located at the specified path.
 */
public suspend fun ssr(
  path: String = nodeSsrDefaultPath,
  response: MutableHttpResponse<ByteArrayOutputStream> = HttpResponse.ok(),
): MutableHttpResponse<ByteArrayOutputStream> {
  return if (path.isBlank()) {
    HttpResponse.notFound()
  } else {
    val renderer = ServerSSRRenderer(JsRuntime.EmbeddedScript(
      path = "/$embeddedRoot/$path",
    ))
    renderer.renderResponse(
      response
    ).characterEncoding(
      StandardCharsets.UTF_8
    ).contentType(
      MediaType("text/html; charset=utf-8", "html")
    )
  }
}


/**
 * Evaluate and inject SSR content into a larger HTML page, using a `<main>` tag as the root element in the dom; apply
 * [domId], [classes], and any additional [attrs] to the root element, if specified.
 *
 * SSR script content will be loaded from the path `node-prod.js` within the embedded asset section of the JAR (located
 * at `/embedded` at the time of this writing).
 *
 * @param domId ID of the root element to express within the DOM. Defaults to `root`.
 * @param classes List of classes to apply to the root DOM element. Defaults to an empty class list.
 * @param attrs Set of additional attribute pairs to apply in the DOM to the root element. Defaults to an empty set.
 * @param path Path within the embedded asset area of the JAR from which to load the SSR script. Defaults to
 *    `node-prod.js`, which is the default value used by the Node/Kotlin toolchain provided by Elide.
 */
public suspend fun BODY.injectSSR(
  domId: String = defaultSsrDomId,
  classes: Set<String> = emptySet(),
  attrs: List<Pair<String, String>> = emptyList(),
  path: String = nodeSsrDefaultPath,
): Unit = MAIN(
  attributesMapOf(
    "id",
    domId,
    "class",
    classes.joinToString(" "),
    "data-serving-mode",
    "ssr"
  ).plus(
    attrs
  ),
  consumer
).visitSuspend {
  // @TODO(sgammon): avoid blocking call here
  val content = ServerSSRRenderer(JsRuntime.Script.embedded(
    path = "/$embeddedRoot/$path",
  )).renderSuspend()

  unsafe {
    if (content != null) {
      +content
    } else {
      +"<!-- // -->"
    }
  }
}


/**
 * Load and serve a JavaScript bundle server-side, executing it within the context of an isolated GraalVM JavaScript
 * runtime; then, collect the output and return it as an HTTP response, within the provided HTML builder, which will be
 * used to render the initial page frame.
 *
 * Additional response properties, such as headers, may be set on the return result, as it is kept mutable. To change
 * initial parameters like the HTTP status, use the [response] parameter via constructors like [HttpResponse.notFound].
 *
 * @param path Path to the React SSR entrypoint script, which should be embedded within the asset section of the JAR.
 * @param response Mutable HTTP response to fill with the resulting SSR content. Sets the status and headers.
 * @param block
 * @return HTTP response wrapping the generated React SSR output, or an HTTP response which serves a 404 if the asset
 *    could not be located at the specified path.
 */
public suspend fun ssr(
  path: String = nodeSsrDefaultPath,
  response: MutableHttpResponse<ByteArrayOutputStream> = HttpResponse.ok(),
  block: suspend HTML.() -> Unit
): MutableHttpResponse<ByteArrayOutputStream> {
  return if (path.isBlank()) {
    HttpResponse.notFound()
  } else {
    return response.body(
      SSRContent(builder = block).render()
    ).characterEncoding(StandardCharsets.UTF_8).contentType(
      "text/html; charset=utf-8"
    )
  }
}

// SSR content rendering and container utility.
internal class SSRContent (
  private val prettyhtml: Boolean = false,
  private val builder: suspend HTML.() -> Unit
): ServerRenderer {
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
