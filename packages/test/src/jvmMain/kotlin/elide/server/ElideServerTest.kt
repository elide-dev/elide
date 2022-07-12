package elide.server

import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanContext
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Base class for Micronaut tests which want to use the enclosed convenience functions. */
@Suppress("unused", "RedundantVisibilityModifier", "UnnecessaryAbstractClass")
public abstract class ElideServerTest {
  // Application context.
  @Inject public lateinit var context: ApplicationContext

  // Bean context.
  @Inject public lateinit var beanContext: BeanContext

  // Embedded application server.
  @Inject public lateinit var app: EmbeddedServer

  // Embedded HTTP client.
  @Inject public lateinit var client: HttpClient

  /**
   * Assert that the test is ready to run and has all locally injected resources.
   */
  protected fun assertReady() {
    assertNotNull(app, "embedded app must be available for HTTP client testing")
    assertNotNull(client, "embedded client must be available for HTTP client testing")
  }

  /**
   * Exchange a test [request] with the current server, expecting [status] as an HTTP return status; after executing the
   * request to obtain the response, [block] is executed (if provided) to perform additional assertions on the response.
   *
   * In this case, no response payload type is provided, and the request payload type is also generic. If you would like
   * to decode the response and test against it, see other variants of this method.
   *
   * @see exchange which allows a response body type.
   * @param request HTTP request to submit to the current Elide server.
   * @param status Expected HTTP status for the response.
   * @param block Block of assertions to additionally perform.
   * @return HTTP response from the server.
   */
  @Suppress("HttpUrlsUsage")
  public fun exchange(
    request: MutableHttpRequest<Any>,
    status: Int? = 200,
    block: (HttpResponse<Any>.() -> Unit)? = null,
  ): HttpResponse<Any> {
    assertReady()
    val req: HttpRequest<Any> = request.uri(
      URI.create("http://${app.host}:${app.port}${request.uri}")
    )
    val client = client.toBlocking()
    val response = client.exchange<Any, Any>(req)
    assertNotNull(response, "should not get `null` response from Micronaut")
    if (status != null) {
      assertEquals(status, response.status.code, "expected status code '$status' did not match")
    }
    block?.let {
      response.it()
    }
    return response
  }

  /**
   * Exchange a test [request] with the current server, expecting [status] as an HTTP return status; after executing the
   * request to obtain the response, [block] is executed (if provided) to perform additional assertions on the response.
   *
   * @param request HTTP request to submit to the current Elide server.
   * @param status Expected HTTP status for the response.
   * @param responseType Type of the response payload.
   * @param block Block of assertions to additionally perform.
   * @return HTTP response from the server.
   */
  @Suppress("HttpUrlsUsage")
  public fun <P : Any, R : Any> exchange(
    request: MutableHttpRequest<P>,
    status: Int? = 200,
    responseType: Class<R>,
    block: (HttpResponse<R>.() -> Unit)? = null,
  ): HttpResponse<R> {
    assertReady()
    val req: HttpRequest<P> = request.uri(
      URI.create("http://${app.host}:${app.port}${request.uri}")
    )
    val client = client.toBlocking()
    val response = client.exchange(
      req,
      responseType,
    )
    assertNotNull(response, "should not get `null` response from Micronaut")
    if (status != null) {
      assertEquals(status, response.status.code, "expected status code '$status' did not match")
    }
    block?.let {
      response.it()
    }
    return response
  }

  /**
   * Syntactic sugar to exchange a [request] with the active Elide server which is expected to produce HTML; assertions
   * are run against the response (expected status, content-type/encoding/length, etc), and then the page is parsed with
   * Jsoup and handed to [block] for additional testing.
   *
   * If any step of the parsing or fetching process fails, the test invoking this method also fails (unless such errors
   * are caught by the invoker).
   *
   * @see exchange which allows an arbitrary response body type.
   * @param request HTTP request to submit to the current Elide server.
   * @param accept Accept value to append to the response; if `null` is passed, nothing is appended.
   * @param status Expected HTTP status for the response.
   * @param block Block of assertions to additionally perform, with access to the parsed page.
   * @return HTTP response from the server.
   */
  @Suppress("HttpUrlsUsage")
  public fun exchangeHTML(
    request: MutableHttpRequest<Any>,
    accept: String? = "text/html,*/*",
    status: Int? = 200,
    block: (HttpResponse<Any>.(Document) -> Unit)? = null,
  ): HttpResponse<Any> {
    assertReady()
    if (accept != null && accept.isNotBlank()) {
      request.headers[HttpHeaders.ACCEPT] = accept
    }
    val response = exchange(
      request,
      status,
      Any::class.java,
    ) {
      // parse the page with jsoup so that assertions can run against it.
      val page = Jsoup.parse(
        getBody(String::class.java).orElseThrow {
          IllegalStateException("Failed to decode response body into String to parse with JSoup.")
        }
      )
      block?.let {
        this.it(page)
      }
    }
    return response
  }
}
