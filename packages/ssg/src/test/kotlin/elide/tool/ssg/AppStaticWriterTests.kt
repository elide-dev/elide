package elide.tool.ssg

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import tools.elide.meta.Endpoint
import tools.elide.meta.EndpointType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for the [AppStaticWriter] interface and [DefaultAppStaticWriter] implementation. */
@MicronautTest(startApplication = true) class AppStaticWriterTests : AbstractSSGCompilerTest() {
  @Inject lateinit var writer: AppStaticWriter

  private fun assertFragmentPath(expected: String, endpoint: Endpoint) {
    val path = (writer as DefaultAppStaticWriter).filepathForFragment(
      staticFragment(endpoint, "sample"),
    )
    assertEquals(
      expected,
      path,
      "path for fragment '$path' is not correct (expected: '$expected')",
    )
  }

  @Test fun testInjectableWriter() {
    assertNotNull(writer, "should be able to inject instance of `AppStaticWriter`")
  }

  @Test fun testComputePathForFragmentFromBase() {
    // try to compute a path for a root page
    assertFragmentPath("/index.html", endpoint(
      type = EndpointType.PAGE,
      base = "/",
      tail = "",
    ))

    // test for a root file path
    assertFragmentPath("/style.css", endpoint(
      type = EndpointType.ASSET,
      base = "/",
      tail = "style.css",
    ))

    // test for a nested file path (x1)
    assertFragmentPath("/test/style.css", endpoint(
      type = EndpointType.ASSET,
      base = "/test",
      tail = "style.css",
    ))

    // test for a nested file path (x2)
    assertFragmentPath("/test/example/style.css", endpoint(
      type = EndpointType.ASSET,
      base = "/test/example",
      tail = "style.css",
    ))

    // test for a root-relative file
    assertFragmentPath("/my/cool/style.css", endpoint(
      type = EndpointType.ASSET,
      base = "/",
      tail = "my/cool/style.css",
    ))

    // test for a double-rooted reference
    assertFragmentPath("/my/cool/style.css", endpoint(
      type = EndpointType.ASSET,
      base = "/",
      tail = "/my/cool/style.css",
    ))

    // test for a double-rooted reference with substance
    assertFragmentPath("/scope/my/cool/style.css", endpoint(
      type = EndpointType.ASSET,
      base = "/scope",
      tail = "/my/cool/style.css",
    ))
  }

  @Test fun testComputePathForFragmentFromTail() {
    // test for a root file path via tail only
    assertFragmentPath("/style.css", endpoint(
      type = EndpointType.ASSET,
      tail = "/style.css",
    ))

    // test for a nested file path (x1)
    assertFragmentPath("/test/style.css", endpoint(
      type = EndpointType.ASSET,
      tail = "/test/style.css",
    ))

    // test for a nested file path (x2)
    assertFragmentPath("/test/example/style.css", endpoint(
      type = EndpointType.ASSET,
      tail = "/test/example/style.css",
    ))

    // test for a root-relative file
    assertFragmentPath("/style.css", endpoint(
      type = EndpointType.ASSET,
      tail = "style.css",
    ))
  }
}
