package elide.runtime.gvm.internals.intrinsics.js.url

import org.junit.jupiter.api.*
import java.util.stream.Stream
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.err.ValueError
import elide.testing.annotations.TestCase

/** WPT compliance tests for the intrinsic `URL` implementation provided by Elide. */
@Ignore @TestCase
internal class URLComplianceTest : AbstractJsIntrinsicTest<URLIntrinsic>() {
  companion object {
    const val testExactHref = false
  }

  private inline fun <T> withURLCases(crossinline op: (URLParseTestCase) -> List<T>): Stream<T> {
    return withJSON<List<Any>, List<T>>(
      "/wpt/url/resources/urltestdata.json",
    ) { list ->
      var i = 0
      list.mapNotNull {
        when (it) {
          is Map<*, *> -> op.invoke(URLParseTestCase.fromMap(i++, it))
          else -> null
        }
      }.flatten()
    }.stream()
  }

  /** Represents a Web Platform Test case for the [URLIntrinsic]. */
  data class URLParseTestCase(
    val number: Int,
    val input: String,
    val failure: Boolean = false,
    val base: String? = null,
    val href: String? = null,
    val origin: String? = null,
    val protocol: String? = null,
    val username: String? = null,
    val password: String? = null,
    val host: String? = null,
    val hostname: String? = null,
    val port: String? = null,
    val pathname: String? = null,
    val search: String? = null,
    val hash: String? = null,
  ) {
    fun label(prop: String?): String = if (prop != null) "URL($number).$prop" else "URL($number) sanity"

    companion object {
      @JvmStatic fun fromMap(number: Int, map: Map<*, *>): URLParseTestCase {
        return URLParseTestCase(
          number = number,
          failure = map["failure"] as Boolean? ?: false,
          input = map["input"] as String,
          base = map["base"] as String?,
          href = map["href"] as String?,
          origin = map["origin"] as String?,
          protocol = map["protocol"] as String?,
          username = map["username"] as String?,
          password = map["password"] as String?,
          host = map["host"] as String?,
          hostname = map["hostname"] as String?,
          port = map["port"] as String?,
          pathname = map["pathname"] as String?,
          search = map["search"] as String?,
          hash = map["hash"] as String?,
        )
      }
    }
  }

  @Inject lateinit var urlIntrinsic: URLIntrinsic
  override fun provide(): URLIntrinsic = urlIntrinsic

  override fun testInjectable() {
    assertNotNull(urlIntrinsic)
  }

  private fun urlTest(case: URLParseTestCase, prop: String? = null, op: (URLIntrinsic.URLValue) -> Unit): DynamicTest {
    return DynamicTest.dynamicTest(case.label(prop)) {
      val subject = provide()
      assertNotNull(subject)

      // resolve base value as relative parse basis, or `null` for no relative parse
      val (base, baseString) = when (val baseUrl = case.base) {
        null -> null to null
        else -> assertDoesNotThrow {
          URLIntrinsic.URLValue.create(baseUrl) to baseUrl
        }
      }
      val mustPassToTest = !case.failure
      val innerParse: (String) -> URLIntrinsic.URLValue = {
        if (base != null && baseString != null) {
          URLIntrinsic.URLValue.create(it, baseString)
        } else {
          URLIntrinsic.URLValue.create(it)
        } as URLIntrinsic.URLValue
      }
      val parseUrl: (String) -> URLIntrinsic.URLValue = {
        try {
          innerParse.invoke(it)
        } catch (err: RuntimeException) {
          if (mustPassToTest) {
            Assumptions.abort<Any>("parse failed")
          }
          throw err
        }
      }

      if (case.failure) {
        assertThrows<ValueError> {
          op.invoke(parseUrl(case.input))  // if we didn't get an error, it should be `null`
        }
      } else assertDoesNotThrow {
        op.invoke(assertNotNull(parseUrl(case.input)))
      }
    }
  }

  @TestFactory fun cases(): Stream<DynamicTest> = withURLCases { case ->
    if (case.failure) listOfNotNull(
      urlTest(case) {
        // nothing at this time
      },
    ) else listOfNotNull(
      urlTest(case) { parsed ->
        assertNotNull(parsed, "should not get `null` for expected-success URL parse")
      },
      urlTest(case, "protocol") { parsed ->
        assertEquals(case.protocol, parsed.protocol, "protocol value should match for test case")
      },
      urlTest(case, "username") { parsed ->
        assertEquals(case.username, parsed.username, "username value should match for test case")
      },
      urlTest(case, "password") { parsed ->
        assertEquals(case.password, parsed.password, "password value should match for test case")
      },
      urlTest(case, "host") { parsed ->
        assertEquals(case.host, parsed.host, "host value should match for test case")
      },
      urlTest(case, "hostname") { parsed ->
        assertEquals(case.hostname, parsed.hostname, "hostname value should match for test case")
      },
      urlTest(case, "port") { parsed ->
        assertEquals(case.port?.toIntOrNull(), parsed.port, "port value should match for test case")
      },
      urlTest(case, "pathname") { parsed ->
        assertEquals(case.pathname, parsed.pathname, "pathname value should match for test case")
      },
      urlTest(case, "search") { parsed ->
        assertEquals(case.search, parsed.search, "search value should match for test case")
      },
      urlTest(case, "hash") { parsed ->
        assertEquals(case.hash, parsed.hash, "hash value should match for test case")
      },
      if (!testExactHref) null else urlTest(case, "href") { parsed ->
        assertEquals(case.href, parsed.href, "href value should match for test case")
      },
    )
  }
}
