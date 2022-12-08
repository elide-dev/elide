@file:Suppress("unused", "WildcardImport", "RedundantSuspendModifier")

package elide.server

import elide.runtime.graalvm.JsRuntime
import elide.server.controller.ElideController
import elide.server.ssr.ServerSSRRenderer
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

// Path within app JARs for embedded script assets.
public const val EMBEDDED_ROOT: String = "embedded"

// Production script name default.
private const val NODE_PROD_DEFAULT: String = "node-prod.pack.js"

// Development script name default.
private const val NODE_DEV_DEFAULT: String = "node-dev.opt.js"

// Default name if no mode is specified or resolvable.
public const val NODE_SSR_DEFAULT_PATH: String = NODE_DEV_DEFAULT

// Default base member for SSR invocation.
public val DEFAULT_INVOCATION_BASE: String? = null

// Default target name for SSR invocation.
public const val DEFAULT_INVOCATION_TARGET: String = "renderContent"

// Default target name for SSR invocation.
public const val DEFAULT_INVOCATION_STREAM_TARGET: String = "renderStream"

// Default ID to use in the DOM.
public const val DEFAULT_SSR_DOM_ID: String = "root"

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
 * @param streamed Whether to enable streaming SSR.
 * @param embeddedRoot Resource folder path where embedded scripts are held. Defaults to `embedded`.
 */
@Suppress("LongParameterList")
public suspend fun BODY.injectSSR(
  handler: ElideController,
  request: HttpRequest<*>,
  domId: String = DEFAULT_SSR_DOM_ID,
  classes: Set<String> = emptySet(),
  attrs: List<Pair<String, String>> = emptyList(),
  path: String? = null,
  streamed: Boolean = false,
  embeddedRoot: String? = null,
) {
  val rendered = ServerSSRRenderer(
    this,
    handler,
    request,
    if (path != null && embeddedRoot != null) {
      JsRuntime.Script.embedded(
        path = path,
        embeddedRoot = embeddedRoot,
      )
    } else {
      JsRuntime.Script.embedded()
    },
  ).renderSuspendAsync(streamed)

  MAIN(
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
    val content = rendered.await()

    unsafe {
      +(content.ifBlank { "<!-- // -->" })
    }
  }
}


/**
 * Stream and inject SSR content into a larger HTML page, using a `<main>` tag as the root element in the dom; apply
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
 * @param embeddedRoot Resource folder path where embedded scripts are held. Defaults to `embedded`.
 */
@Suppress("LongParameterList")
public suspend fun BODY.streamSSR(
  handler: ElideController,
  request: HttpRequest<*>,
  domId: String = DEFAULT_SSR_DOM_ID,
  classes: Set<String> = emptySet(),
  attrs: List<Pair<String, String>> = emptyList(),
  path: String = NODE_SSR_DEFAULT_PATH,
  embeddedRoot: String = EMBEDDED_ROOT,
): Unit = injectSSR(
  handler = handler,
  request = request,
  domId = domId,
  classes = classes,
  attrs = attrs,
  path = path,
  embeddedRoot = embeddedRoot,
  streamed = true,
)

/**
 * Load and serve a JavaScript bundle server-side, executing it within the context of an isolated GraalVM JavaScript
 * runtime; then, collect the output and return it as an HTTP response, within the provided HTML builder, which will be
 * used to render the initial page frame.
 *
 * Additional response properties, such as headers, may be set on the return result, as it is kept mutable. To change
 * initial parameters like the HTTP status, use the [response] parameter via constructors like [HttpResponse.notFound].
 *
 * @param request Request we are responding to.
 * @param path Path to the React SSR entrypoint script, which should be embedded within the asset section of the JAR.
 * @param response Mutable HTTP response to fill with the resulting SSR content. Sets the status and headers.
 * @param block
 * @return HTTP response wrapping the generated React SSR output, or an HTTP response which serves a 404 if the asset
 *    could not be located at the specified path.
 */
@Suppress("UNUSED_PARAMETER")
public suspend fun ssr(
  request: HttpRequest<*>,
  path: String = NODE_SSR_DEFAULT_PATH,
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
internal class SSRContent(
  private val prettyhtml: Boolean = false,
  private val builder: suspend HTML.() -> Unit
) : SuspensionRenderer<ByteArrayOutputStream> {
  override suspend fun render(): ByteArrayOutputStream {
    val baos = ByteArrayOutputStream()
    baos.bufferedWriter(StandardCharsets.UTF_8).use {
      it.appendHTML(
        prettyPrint = prettyhtml,
      ).htmlSuspend(
        block = builder
      )
    }
    return baos
  }
}
