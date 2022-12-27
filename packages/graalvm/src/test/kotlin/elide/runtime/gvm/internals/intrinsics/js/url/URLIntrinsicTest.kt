@file:Suppress("JSUnresolvedFunction", "JSUnresolvedVariable")

package elide.runtime.gvm.internals.intrinsics.js.url

import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic.URLValue
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.err.ValueError
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.net.URI

/** Tests for the intrinsic `URL` implementation provided by Elide. */
@Suppress("HttpUrlsUsage")
@TestCase internal class URLIntrinsicTest : AbstractJsIntrinsicTest<URLIntrinsic>() {
  @Inject lateinit var urlIntrinsic: URLIntrinsic
  private val sampleUrl = URLValue.fromString("https://google.com")

  override fun provide(): URLIntrinsic = urlIntrinsic

  @Test override fun testInjectable() {
    assertNotNull(urlIntrinsic, "URL intrinsic should be injectable.")
  }

  @Test fun testURLFromJavaURL() {
    assertNotNull(sampleUrl, "should be able to convert a Java `URL` to a wrapped intrinsic `URL`")
    assertEquals("https://google.com", sampleUrl.toString())
  }

  @Test fun testConstructFromString() = dual {
    val url = URLValue.fromString("https://google.com")
    assertNotNull(url, "should be able to construct a `URL` from a string")
    assertEquals("https://google.com", url.toString())
    val url2 = URLValue("https://google.com")
    assertEquals("https://google.com", url2.toString())
  }.guest {
    // language=javascript
    """
      test(new URL("https://google.com")).isNotNull("should be able to construct a `URL` from a string");
      test(new URL("https://google.com").toString()).isEqualTo("https://google.com");
      test(new URL("https://google.com/hello").toString()).isEqualTo("https://google.com/hello");
      test(new URL("https://google.com/hello?test=hi").toString()).isEqualTo("https://google.com/hello?test=hi");
      test(new URL("https://google.com/hello?test=hi#yo").toString()).isEqualTo("https://google.com/hello?test=hi#yo");
      test(new URL("https://user:pass@google.com").toString()).isEqualTo("https://user:pass@google.com");
    """
  }

  @Test fun testConstructInvalid() = dual {
    assertFailsWith<TypeError> {
      URLValue(5)
    }
    assertFailsWith<TypeError> {
      URLValue(1.5)
    }
    assertFailsWith<TypeError> {
      URLValue(HashMap<String, String>())
    }
    assertFailsWith<TypeError> {
      URLValue(false)
    }
    assertFailsWith<ValueError> {
      URLValue("")
    }
    assertFailsWith<ValueError> {
      URLValue("/relative")
    }
  }.guest {
    // language=javascript
    """
      test(() => new URL(5)).fails("should throw an exception when constructing an invalid `URL` (value: '5')");
      test(() => new URL({})).fails("should throw an exception when constructing an invalid `URL` (value: '{}')");
      test(() => new URL(1.5)).fails("should throw an exception when constructing an invalid `URL` (value: '1.5')");
      test(() => new URL(false)).fails("should throw an exception when constructing an invalid `URL` (value: 'false')");
      test(() => new URL("")).fails("should throw an exception when constructing an invalid `URL` (value: '')");
      test(() => new URL("/relative")).fails("should throw an exception when constructing a relative `URL`");
    """
  }

  @Test fun testConstructFromURL() = dual {
    val url = URLValue.fromURL(URI.create("https://google.com"))
    assertNotNull(url, "should be able to construct a `URL` from a string")
    assertEquals("https://google.com", url.toString())
    val url2 = URLValue(URLValue("https://google.com"))
    assertEquals("https://google.com", url2.toString())
    val url3 = URLValue.fromURL(URLValue("https://google.com"))
    assertEquals("https://google.com", url3.toString())
  }.guest {
    // language=javascript
    """
      test(new URL(new URL("https://google.com"))).isNotNull("should be able to construct a `URL` from a string");
      test(new URL(new URL("https://google.com")).toString()).isEqualTo("https://google.com");
      test(new URL(new URL("https://google.com/hello")).toString()).isEqualTo("https://google.com/hello");
      test(new URL(new URL("https://google.com/hello?test=hi")).toString())
                .isEqualTo("https://google.com/hello?test=hi");
      test(new URL(new URL("https://google.com/hello?test=hi#yo")).toString())
                .isEqualTo("https://google.com/hello?test=hi#yo");
      test(new URL(new URL("https://user:pass@google.com")).toString())
                .isEqualTo("https://user:pass@google.com");
    """
  }

  @CsvSource(value = [
    // Basic Cases: Normal
    "equal,https://google.com,https://google.com,true,URLs should be equal",
    "equal,https://github.com/elide-dev/v3,https://github.com/elide-dev/v3,true,URLs should be equal",
    "equal,https://dl.elide.dev/test?abc=123&def=456,https://dl.elide.dev/test?abc=123&def=456,true,URLs should be equal",
    "equal,https://dl.elide.dev:123/test?abc=123&def=456#hi,https://dl.elide.dev:123/test?abc=123&def=456#hi,true,URLs should be equal",
    "equal,http://www.google.com/#hello,http://www.google.com/#hello,true,URLs should be equal",
    "equal,//dl.elide.dev/test?abc=123&def=456,//dl.elide.dev/test?abc=123&def=456,true,URLs should be equal",
    "equal,ftp://hello.local.dev/hello/test,ftp://hello.local.dev/hello/test,true,URLs should be equal",
    "equal,file://here/is/a/file/path,file://here/is/a/file/path,true,URLs should be equal",
    "equal,blob://some-blob-id,blob://some-blob-id,true,URLs should be equal",
    "equal,blob://some-blob-id?neat=cool,blob://some-blob-id?neat=cool,true,URLs should be equal",
    "equal,https://user:pass@dl.elide.dev/test?abc=123&def=456,https://user:pass@dl.elide.dev/test?abc=123&def=456,true,URLs should be equal",

    // Smart Cases: Positive
    "equal-smart,https://google.com,https://google.com:443,true,two URLs with the same effective port should be equal",
    "equal-smart,https://google.com/,https://google.com,true,two URLs with the same effective path should be equal",
    "equal-smart,https://google.com/,https://google.com:443,true,two URLs with the same effective path/port should be equal",
    "equal-smart,https://google.com#,https://google.com,true,two URLs with the same effective fragment should be equal",
    "equal-smart,https://google.com?,https://google.com,true,two URLs with the same effective query should be equal",

    // Basic Cases: Negative
    "not-equal,http://google.com,https://google.com,false,two URLs which differ in protocol should not be equal",
    "not-equal,https://google.com,https://google.co.uk,false,two URLs which differ in host should not be equal",
    "not-equal,https://google.com,https://google.com:444,false,two URLs which differ in port should not be equal",
    "not-equal,https://github.com,https://github.com/elide-dev/v3,false,two URLs which differ in path should not be equal",
    "not-equal,https://github.com,https://google.com?hello=hi,false,two URLs which differ in query should not be equal",
    "not-equal,https://github.com,https://google.com?hello,false,two URLs which differ in query should not be equal",
  ])
  @ParameterizedTest(
    name = "[{index}:dual]: {0}: {4}",
  ) fun testURLEquals(label: String, base: String, comparison: String, expectEquals: Boolean, msg: String) = dual {
    val shouldEqual: (URLValue, URLValue) -> Unit = { left, right ->
      assertEquals(
        left,
        right,
        "case($label): $msg ($left != $right)"
      )
    }
    val shouldNotEqual: (URLValue, URLValue) -> Unit = { left, right ->
      assertNotEquals(
        left,
        right,
        "case($label): $msg ($left == $right)"
      )
    }
    val op = if (expectEquals) {
      shouldEqual
    } else {
      shouldNotEqual
    }
    op.invoke(URLValue.fromString(base), URLValue.fromString(comparison))
  }.guest {
    // language=javascript
    """
      if ($expectEquals) {
        test(new URL("$base")).isEqualTo(new URL("$comparison"), "$msg");
      } else {
        test(new URL("$base")).isNotEqualTo(new URL("$comparison"), "$msg");
      }
    """
  }

  @CsvSource(value = [
    "https://google.com,https://google.com",
    "https://github.com/elide-dev/v3,https://github.com/elide-dev/v3",
    "https://dl.elide.dev/test?abc=123&def=456,https://dl.elide.dev/test?abc=123&def=456",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,https://dl.elide.dev:123/test?abc=123&def=456#hi",
    "http://www.google.com/#hello,http://www.google.com/#hello",
    "//dl.elide.dev/test?abc=123&def=456,//dl.elide.dev/test?abc=123&def=456",
    "ftp://hello.local.dev/hello/test,ftp://hello.local.dev/hello/test",
    "file://here/is/a/file/path,file://here/is/a/file/path",
    "blob://some-blob-id,blob://some-blob-id",
    "blob://some-blob-id?neat=cool,blob://some-blob-id?neat=cool",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,https://user:pass@dl.elide.dev/test?abc=123&def=456",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLStrings(testString: String, expected: String) = dual {
    val url = URLValue(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected, url.toString())
    val url2 = URLValue.fromString(testString)
    assertNotNull(url2)
    assertEquals(expected, url2.toString())
    assertEquals(url, url2)
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull();
      test(new URL("$testString").toString()).isEqualTo("$expected");
      test(new URL("$testString").toString()).isEqualTo(new URL("$testString").toString());
      test(new URL("$testString").toString()).isEqualTo(new URL("$expected").toString());
    """
  }

  @CsvSource(value = [
    "https://google.com,",
    "https://github.com/elide-dev/v3,",
    "https://dl.elide.dev/test?abc=123&def=456,",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,#hi",
    "http://www.google.com/#hello,#hello",
    "//dl.elide.dev/test?abc=123&def=456,",
    "ftp://hello.local.dev/hello/test,",
    "file://here/is/a/file/path,",
    "blob://some-blob-id,",
    "blob://some-blob-id?neat=cool,",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLHash(testString: String, expected: String?) = dual {
    val url = URLValue.fromString(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.hash, "hash value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse hash-test URL");
      test(new URL("$testString").hash).isNotNull("should be able to access `hash` URL property");
      test(new URL("$testString").hash).isEqualTo("${expected ?: ""}");
    """
  }

  @Test fun testURLHashMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    url.hash = "hi"
    assertEquals("https://google.com/#hi", url.toString())
    url.hash = "#yo"
    assertEquals("https://google.com/#yo", url.toString())
    url.hash = ""
    assertEquals("https://google.com", url.toString())
    url.hash = "#"
    assertEquals("https://google.com", url.toString())
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      url.hash = "hi";
      test(url.hash).isEqualTo("#hi");
      test(url.toString()).isEqualTo("https://google.com/#hi");
      url.hash = "#yo";
      test(url.hash).isEqualTo("#yo");
      test(url.toString()).isEqualTo("https://google.com/#yo");
      url.hash = "";
      test(url.hash).isEqualTo("");
      test(url.toString()).isEqualTo("https://google.com");
      url.hash = "#";
      test(url.hash).isEqualTo("");
      test(url.toString()).isEqualTo("https://google.com");
    """
  }

  @CsvSource(value = [
    "https://google.com,google.com",
    "https://github.com/elide-dev/v3,github.com",
    "https://dl.elide.dev/test?abc=123&def=456,dl.elide.dev",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,dl.elide.dev:123",
    "http://www.google.com/#hello,www.google.com",
    "//dl.elide.dev/test?abc=123&def=456,dl.elide.dev",
    "ftp://hello.local.dev/hello/test,hello.local.dev",
    "file://here/is/a/file/path,",
    "blob://some-blob-id,",
    "blob://some-blob-id?neat=cool,",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,dl.elide.dev",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLHost(testString: String, expected: String?) = dual {
    val url = URLValue.fromString(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.host, "host value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").host).isNotNull("should be able to access `hash` URL property");
      test(new URL("$testString").host).isEqualTo("${expected ?: ""}");
    """
  }

  @Test fun testURLHostMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    assertEquals("google.com", url.host)
    url.host = "github.com"
    assertEquals("github.com", url.host)
    assertEquals("https://github.com", url.toString())
    url.host = "dl.elide.dev:123"
    assertEquals("dl.elide.dev:123", url.host)
    assertEquals("https://dl.elide.dev:123", url.toString())
    url.host = "google.com"
    assertEquals("google.com:123", url.host)
    assertEquals("https://google.com:123", url.toString())
    url.port = 443
    assertEquals("google.com", url.host)
    assertEquals("https://google.com", url.toString())
    assertFailsWith<ValueError> {
      url.host = "google.com:"
    }
    assertFailsWith<ValueError> {
      url.host = "google.com:1:2"
    }
    assertFailsWith<ValueError> {
      url.host = "google.com:-5"
    }
    assertFailsWith<ValueError> {
      url.host = "google.com:65536"
    }
    assertFailsWith<ValueError> {
      url.host = ""
    }
    assertFailsWith<ValueError> {
      url.host = " "
    }
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      url.host = "github.com";
      test(url.host).isEqualTo("github.com");
      test(url.toString()).isEqualTo("https://github.com");
      url.host = "dl.elide.dev:123";
      test(url.host).isEqualTo("dl.elide.dev:123");
      test(url.toString()).isEqualTo("https://dl.elide.dev:123");
      url.host = "google.com";
      test(url.host).isEqualTo("google.com:123");
      test(url.toString()).isEqualTo("https://google.com:123");
    """
  }

  @CsvSource(value = [
    "https://google.com,https:",
    "https://github.com/elide-dev/v3,https:",
    "https://dl.elide.dev/test?abc=123&def=456,https:",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,https:",
    "http://www.google.com/#hello,http:",
    "//dl.elide.dev/test?abc=123&def=456,",
    "ftp://hello.local.dev/hello/test,ftp:",
    "file://here/is/a/file/path,file:",
    "blob://some-blob-id,blob:",
    "blob://some-blob-id?neat=cool,blob:",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,https:",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLProtocol(testString: String, expected: String?) = dual {
    val url = URLValue.fromString(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.protocol, "protocol value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").protocol).isNotNull("URL 'protocol' field should never be 'null'");
      test(new URL("$testString").protocol).isEqualTo("${expected ?: ""}");
    """
  }

  @Test fun testURLProtocolMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    assertEquals("https:", url.protocol, "expected protocol mismatch")
    url.protocol = "ftp"
    assertEquals("ftp://google.com", url.toString(), "expected protocol mismatch")
    url.protocol = ""
    assertEquals("//google.com", url.toString(), "should be able to 'relativize' a protocol by setting it to empty")
    assertFailsWith<ValueError>("should reject blank protocols") {
      url.protocol = " "
    }
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      test(url.protocol).isEqualTo("https:");
      url.protocol = "ftp";
      test(url.toString()).isEqualTo("ftp://google.com");
      url.protocol = "";
      test(url.toString()).isEqualTo("//google.com");
      test(() => url.protocol = " ").fails("should reject blank protocols");
    """
  }

  @CsvSource(value = [
    "https://google.com,443",
    "https://github.com/elide-dev/v3,443",
    "https://dl.elide.dev/test?abc=123&def=456,443",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,123",
    "http://www.google.com/#hello,80",
    "//dl.elide.dev/test?abc=123&def=456,",
    "ftp://hello.local.dev/hello/test,21",
    "file://here/is/a/file/path,",
    "blob://some-blob-id,",
    "blob://some-blob-id?neat=cool,",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,443",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLPort(testString: String, expectedPort: String?) = dual {
    val expected = expectedPort?.toIntOrNull() ?: -1
    val url = URLValue.fromString(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected, url.port, "port value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").port).isEqualTo(${expectedPort ?: "-1"});
    """
  }

  @Test fun testURLPortMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    assertEquals(443, url.port, "expected port mismatch")
    url.port = 123
    assertEquals("https://google.com:123", url.toString(), "expected port mismatch")
    assertFailsWith<ValueError>("should reject ports which are too small") {
      url.port = -5
    }
    assertFailsWith<ValueError>("should reject ports which are too large") {
      url.port = 65536
    }
    url.port = 443
    assertEquals("https://google.com", url.toString(), "expected port mismatch")
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      test(url.port).isEqualTo(443);
      url.port = 123;
      test(url.toString()).isEqualTo("https://google.com:123");
      test(() => url.port = -5).fails("should reject ports which are too small");
      test(() => url.port = 65536).fails("should reject ports which are too large");
      url.port = 443;
      test(url.toString()).isEqualTo("https://google.com");
    """
  }

  @CsvSource(value = [
    "https://google.com,google.com",
    "https://github.com/elide-dev/v3,github.com",
    "https://dl.elide.dev/test?abc=123&def=456,dl.elide.dev",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,dl.elide.dev",
    "http://www.google.com/#hello,www.google.com",
    "//dl.elide.dev/test?abc=123&def=456,dl.elide.dev",
    "ftp://hello.local.dev/hello/test,hello.local.dev",
    "file://here/is/a/file/path,",
    "blob://some-blob-id,",
    "blob://some-blob-id?neat=cool,",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,dl.elide.dev",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLHostname(testString: String, expected: String?) = dual {
    val url = URLValue.fromString(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.hostname, "hostname value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").hostname).isNotNull();
      test(new URL("$testString").hostname).isEqualTo("${expected ?: ""}");
    """
  }

  @Test fun testURLHostnameMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    url.hostname = "github.com"
    assertEquals("https://github.com", url.toString())
    assertFailsWith<ValueError>("should reject URL hostname changes with a port") {
      url.hostname = "dl.elide.dev:123"
    }
    url.host = "github.com:123"
    assertEquals("https://github.com:123", url.toString())
    url.hostname = "testing.com"
    assertEquals(
      "https://testing.com:123",
      url.toString(),
      "hostname change should preserve port and scheme",
    )
    url.port = 443
    assertEquals("https://testing.com", url.toString())
    assertFailsWith<ValueError> {
      url.hostname = ""
    }
    assertFailsWith<ValueError> {
      url.hostname = " "
    }
    assertFailsWith<ValueError> {
      url.hostname = "some invalid hostname lol "
    }
    assertFailsWith<ValueError> {
      url.hostname = "https://"
    }
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      url.hostname = "github.com";
      test(url.toString()).isEqualTo("https://github.com");
      test(() => url.hostname = "dl.elide.dev:123").fails("should reject URL hostname changes with a port");
      url.host = "github.com:123";
      test(url.toString()).isEqualTo("https://github.com:123");
      url.hostname = "testing.com";
      test(url.toString()).isEqualTo("https://testing.com:123");
      url.port = 443;
      test(url.toString()).isEqualTo("https://testing.com");
      test(() => url.hostname = "").fails("should reject blank hostnames");
      test(() => url.hostname = " ").fails("should reject blank hostnames");
      test(() => url.hostname = "some invalid hostname lol ").fails("should reject invalid hostnames");
      test(() => url.hostname = "https://").fails("should reject invalid hostnames");
    """
  }

  @CsvSource(value = [
    "https://google.com,https://google.com",
    "https://github.com/elide-dev/v3,https://github.com/elide-dev/v3",
    "https://dl.elide.dev/test?abc=123&def=456,https://dl.elide.dev/test?abc=123&def=456",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,https://dl.elide.dev:123/test?abc=123&def=456#hi",
    "http://www.google.com/#hello,http://www.google.com/#hello",
    "//dl.elide.dev/test?abc=123&def=456,//dl.elide.dev/test?abc=123&def=456",
    "ftp://hello.local.dev/hello/test,ftp://hello.local.dev/hello/test",
    "file://here/is/a/file/path,file://here/is/a/file/path",
    "blob://some-blob-id,blob://some-blob-id",
    "blob://some-blob-id?neat=cool,blob://some-blob-id?neat=cool",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,https://user:pass@dl.elide.dev/test?abc=123&def=456",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLHref(testString: String, expected: String) = dual {
    val url = URLValue(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected, url.href)
    assertEquals(url.toString(), url.href)
    val url2 = URLValue.fromString(testString)
    assertNotNull(url2)
    assertEquals(expected, url2.href)
    assertEquals(url, url2)
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").href).isEqualTo("$expected");
      test(new URL("$testString").toString()).isEqualTo("$expected");
      test(new URL("$testString")).isEqualTo(new URL("$testString"));
      test(new URL("$testString").href).isEqualTo(new URL("$testString").href);
      test(new URL("$testString").toString()).isEqualTo(new URL("$testString").toString());
    """
  }

  @Test fun testURLHrefMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    url.href = "https://github.com"
    assertEquals("https://github.com", url.toString())
    assertFailsWith<ValueError>("should reject URL href changes which are not valid URLs") {
      url.href = "not a url lol"
    }
    url.href = "https://github.com?test=hello"
    assertEquals("https://github.com?test=hello", url.toString())
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      url.href = "https://github.com";
      test(url.toString()).isEqualTo("https://github.com");
      test(() => url.href = "not a url lol").fails("should reject URL href changes which are not valid URLs");
      url.href = "https://github.com?test=hello";
      test(url.toString()).isEqualTo("https://github.com?test=hello");
    """
  }

  @CsvSource(value = [
    "https://google.com,",
    "https://github.com/elide-dev/v3,",
    "https://dl.elide.dev/test?abc=123&def=456,",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,",
    "http://www.google.com/#hello,",
    "//dl.elide.dev/test?abc=123&def=456,",
    "ftp://hello.local.dev/hello/test,",
    "file://here/is/a/file/path,",
    "blob://some-blob-id,",
    "blob://some-blob-id?neat=cool,",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,pass",

    // Corner cases
    "https://:@dl.elide.dev/test?abc=123&def=456,",
    "https://user:@dl.elide.dev/test?abc=123&def=456,",
    "https://user@dl.elide.dev/test?abc=123&def=456,",
    "https://:pass@dl.elide.dev/test?abc=123&def=456,",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLUserPassword(testString: String, expected: String?) = dual {
    val url = URLValue(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.password, "password value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").password).isEqualTo("${expected ?: ""}");
    """
  }

  @CsvSource(value = [
    "https://google.com,",
    "https://github.com/elide-dev/v3,",
    "https://dl.elide.dev/test?abc=123&def=456,",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,",
    "http://www.google.com/#hello,",
    "//dl.elide.dev/test?abc=123&def=456,",
    "ftp://hello.local.dev/hello/test,",
    "file://here/is/a/file/path,",
    "blob://some-blob-id,",
    "blob://some-blob-id?neat=cool,",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,user",

    // Corner cases
    "https://:@dl.elide.dev/test?abc=123&def=456,",
    "https://user:@dl.elide.dev/test?abc=123&def=456,user",
    "https://user@dl.elide.dev/test?abc=123&def=456,user",
    "https://:pass@dl.elide.dev/test?abc=123&def=456,",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLUsername(testString: String, expected: String?) = dual {
    val url = URLValue(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.username, "username value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").username).isEqualTo("${expected ?: ""}");
    """
  }

  @Test fun testURLUsernamePasswordMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    url.password = "testing"
    assertEquals("https://google.com", url.toString())
    url.username = "hello"
    url.password = "testing"
    assertEquals("https://hello:testing@google.com", url.toString())
    url.username = ""
    assertEquals("https://google.com", url.toString())
    url.username = "hello"
    assertEquals("https://hello@google.com", url.toString())
    url.password = "testing"
    assertEquals("https://hello:testing@google.com", url.toString())
    url.password = ""
    assertEquals("https://hello@google.com", url.toString())
    url.username = ""
    assertEquals("https://google.com", url.toString())
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      url.password = "testing";
      test(url.toString()).isEqualTo("https://google.com");
      url.username = "hello";
      url.password = "testing";
      test(url.toString()).isEqualTo("https://hello:testing@google.com");
      url.username = "";
      test(url.toString()).isEqualTo("https://google.com");
      url.username = "hello";
      test(url.toString()).isEqualTo("https://hello@google.com");
      url.password = "testing";
      test(url.toString()).isEqualTo("https://hello:testing@google.com");
      url.password = "";
      test(url.toString()).isEqualTo("https://hello@google.com");
      url.username = "";
      test(url.toString()).isEqualTo("https://google.com");
    """
  }

  @CsvSource(value = [
    "https://google.com,/",
    "https://github.com/elide-dev/v3,/elide-dev/v3",
    "https://dl.elide.dev/test?abc=123&def=456,/test",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,/test",
    "http://www.google.com/#hello,/",
    "//dl.elide.dev/test?abc=123&def=456,/test",
    "ftp://hello.local.dev/hello/test,/hello/test",
    "file://here/is/a/file/path,/here/is/a/file/path",
    "blob://some-blob-id,some-blob-id",
    "blob://some-blob-id?neat=cool,some-blob-id",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,/test",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLPathname(testString: String, expected: String?) = dual {
    val url = URLValue.fromString(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.pathname, "pathname value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").pathname).isEqualTo("${expected ?: ""}");
    """
  }

  @Test fun testURLPathnameMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    url.pathname = "/hello/hi"
    assertEquals("https://google.com/hello/hi", url.toString())
    url.pathname = ""
    assertEquals("https://google.com", url.toString())
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      url.pathname = "/hello/hi";
      test(url.toString()).isEqualTo("https://google.com/hello/hi");
      url.pathname = "";
      test(url.toString()).isEqualTo("https://google.com");
    """
  }

  @CsvSource(value = [
    "https://google.com,",
    "https://github.com/elide-dev/v3,",
    "https://dl.elide.dev/test?abc=123&def=456,?abc=123&def=456",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,?abc=123&def=456",
    "http://www.google.com/#hello,",
    "//dl.elide.dev/test?abc=123&def=456,?abc=123&def=456",
    "ftp://hello.local.dev/hello/test,",
    "file://here/is/a/file/path,",
    "blob://some-blob-id,",
    "blob://some-blob-id?neat=cool,?neat=cool",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,?abc=123&def=456",
  ])
  @ParameterizedTest(name = "[{index}:dual]") fun testURLQuery(testString: String, expected: String?) = dual {
    val url = URLValue.fromString(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.search, "search value mismatch")
  }.guest {
    // language=javascript
    """
      test(new URL("$testString")).isNotNull("should be able to parse URL");
      test(new URL("$testString").search).isEqualTo("${expected ?: ""}");
    """
  }

  @Test fun testURLQueryMutability() = dual {
    val url = URLValue.fromString("https://google.com")
    url.search = "?hi=hello"
    assertEquals("https://google.com/?hi=hello", url.toString())
    url.search = "hey=yo"
    assertEquals("https://google.com/?hey=yo", url.toString())
    url.search = ""
    assertEquals("https://google.com", url.toString())
    url.search = "?"
    assertEquals("https://google.com", url.toString())
  }.guest {
    // language=javascript
    """
      const url = new URL("https://google.com");
      url.search = "?hi=hello";
      test(url.toString()).isEqualTo("https://google.com/?hi=hello");
      url.search = "hey=yo";
      test(url.toString()).isEqualTo("https://google.com/?hey=yo");
      url.search = "";
      test(url.toString()).isEqualTo("https://google.com");
      url.search = "?";
      test(url.toString()).isEqualTo("https://google.com");
    """
  }

  @CsvSource(value = [
    "https://google.com,https://google.com",
    "https://github.com/elide-dev/v3,https://github.com",
    "https://dl.elide.dev/test?abc=123&def=456,https://dl.elide.dev",
    "https://dl.elide.dev:123/test?abc=123&def=456#hi,https://dl.elide.dev:123",
    "http://www.google.com/#hello,https://www.google.com",
    "//dl.elide.dev/test?abc=123&def=456,//dl.elide.dev",
    "ftp://hello.local.dev/hello/test,",
    "file://here/is/a/file/path,",
    "blob://some-blob-id,",
    "blob://some-blob-id?neat=cool,",
    "https://user:pass@dl.elide.dev/test?abc=123&def=456,https://dl.elide.dev",
  ])
  @ParameterizedTest(name = "[{index}:dual]") @Disabled fun testURLOrigin(testString: String, expected: String?) = dual {
    val url = URLValue.fromString(testString)
    assertNotNull(url, "should be able to convert a string to a wrapped intrinsic `URL`")
    assertEquals(expected ?: "", url.origin, "origin value mismatch")
  }.guest {
    // language=javascript
    """
      // not yet implemented
    """
  }

  @Test @Disabled fun testURLSearchParams() = dual {
    // coming soon.
  }.guest {
    // language=javascript
    """
      // not yet implemented
    """
  }
}
