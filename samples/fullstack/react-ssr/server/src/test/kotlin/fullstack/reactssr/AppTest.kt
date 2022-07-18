package fullstack.reactssr

import elide.server.ElideServerTest
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Example testsuite for the fullstack React SSR app sample ([App]). */
@MicronautTest class AppTest : ElideServerTest() {
  @Inject lateinit var index: App.Index

  // Make sure the handler can be injected.
  @Test fun testInjectable() {
    assertNotNull(index, "should be able to inject an instance of the `Index` controller")
  }

  // Make sure the index page (`/`) doesn't error and yields an HTTP 200.
  @Test fun testFetchIndex() {
    exchange(HttpRequest.GET("/")) {
      assertEquals(
        200,
        status.code,
        "status code should be '200' for index page"
      )
      assertTrue(
        headers.contains("Content-Type"),
        "response should specify a `Content-Type` header"
      )
      assertTrue(
        headers.get("Content-Type")!!.contains("text/html"),
        "`Content-Type` should include `text/html` from index page"
      )
      assertTrue(
        headers.contains("Content-Length"),
        "response should specify a `Content-Length` header"
      )
      assertNotNull(
        headers.get("Content-Length")!!.toLongOrNull(),
        "`Content-Length` should decode as a number"
      )
      assertTrue(
        headers.get("Content-Length")!!.toLong() > 0,
        "`Content-Length` should be greater than zero"
      )
    }
  }

  // Decode the returned page as HTML and assert against the structure.
  @Test fun testFetchIndexContent() {
    exchangeHTML(HttpRequest.GET("/")) {
      // you can still do basic assertions, because `this` == `HttpResponse`
      assertEquals(
        200,
        status.code,
        "status code should be '200' for index page"
      )
      assertTrue(
        headers.contains("Content-Type"),
        "response should specify a `Content-Type` header"
      )
      assertTrue(
        headers.get("Content-Type")!!.contains("text/html"),
        "`Content-Type` should include `text/html` from index page"
      )

      // assert basic page details
      assertEquals(StandardCharsets.UTF_8, it.charset())
      assertEquals("Hello, Elide!", it.title())

      // make sure the styles made it in
      val styleLinks = it.head().getElementsByTag("link").toList().filter { link ->
        link.attr("rel") == "stylesheet"
      }
      assertEquals(2, styleLinks.size, "should have 2 stylesheet links in the resulting page")
      val mainCss = styleLinks.find { link -> link.attr("href").contains("main.css") }
      assertNotNull(mainCss, "main.css should be findable in page output")

      // body assertions for SSR
      val root = it.body().getElementById("root")
      assertNotNull(root, "should be able to find root content element for React at `#root`")
      assertEquals("ssr", root.attr("data-serving-mode"), "serving-mode should be `ssr`")
      assertTrue(
        root.html().contains("Hello, Elide v3!")
      )
    }
  }

  @Test fun testFetchStaticCss() {
    exchange(HttpRequest.GET("/styles/main.css")) {
      assertEquals(
        200,
        status.code,
        "status code should be '200' for static CSS"
      )
      assertTrue(
        headers.contains("Content-Type"),
        "response should specify a `Content-Type` header"
      )
      assertTrue(
        headers.get("Content-Type")!!.contains("text/css"),
        "`Content-Type` header should include `text/css`"
      )
    }
  }

  @Test fun testFetchAssetCss() {
    // should be fetchable at the bound custom URL
    exchange(HttpRequest.GET(index.asset("styles.base").href)) {
      assertEquals(
        200,
        status.code,
        "status code should be '200' for asset CSS"
      )
      assertTrue(
        headers.contains("Content-Type"),
        "response should specify a `Content-Type` header"
      )
      assertTrue(
        headers.get("Content-Type")!!.contains("text/css"),
        "`Content-Type` header should include `text/css`"
      )
    }

    // should also be fetchable at the generated URL
    exchange(HttpRequest.GET("/styles/base.css")) {
      assertEquals(
        200,
        status.code,
        "status code should be '200' for asset CSS"
      )
      assertTrue(
        headers.contains("Content-Type"),
        "response should specify a `Content-Type` header"
      )
      assertTrue(
        headers.get("Content-Type")!!.contains("text/css"),
        "`Content-Type` header should include `text/css`"
      )
    }
  }

  @Test fun testFetchStaticJs() {
    exchange(HttpRequest.GET("/scripts/ui.js")) {
      assertEquals(
        200,
        status.code,
        "status code should be '200' for client-side JS"
      )
      assertTrue(
        headers.contains("Content-Type"),
        "response should specify a `Content-Type` header"
      )
      assertTrue(
        headers.get("Content-Type")!!.contains("application/javascript"),
        "`Content-Type` header should include `application/javascript`"
      )
    }
  }
}
