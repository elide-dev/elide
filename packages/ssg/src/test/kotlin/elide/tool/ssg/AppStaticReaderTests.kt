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

package elide.tool.ssg

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.nio.charset.StandardCharsets
import jakarta.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests for the [StaticContentReader] interface and [DefaultAppStaticReader] implementation. */
@MicronautTest(startApplication = false)
class AppStaticReaderTests {
  companion object {
    private const val validHtml = "<html><head><title>Test</title></head><body><p>Test</p></body></html>"
    private const val baseServerOrigin = "https://localhost:8443"
  }

  // Content reader under test.
  @Inject lateinit var reader: StaticContentReader

  // Base test server URL.
  private val baseServerUrl: URL = URL("$baseServerOrigin/")

  // Sample request.
  private val baseServerRequest: HttpRequest<*> = HttpRequest.GET<Any>(baseServerUrl.toURI())

  // Produce a valid HTML response which should be eligible for parsing.
  private fun htmlResponse(
    content: String = validHtml,
    status: HttpStatus = HttpStatus.OK,
    contentType: String? = "text/html",
  ): MutableHttpResponse<ByteArray> {
    val testResponseBytes = content.toByteArray(StandardCharsets.UTF_8)
    return HttpResponse.status<ByteArray>(
      status,
    ).body(
      testResponseBytes,
    ).contentLength(
      testResponseBytes.size.toLong(),
    ).headers {
      if (!contentType.isNullOrBlank()) {
        it["Content-Type"] = contentType
      }
    }
  }

  @Test fun testInjectable() {
    assertNotNull(reader, "should be able to inject instance of `StaticContentReader`")
  }

  @Test fun testReadEmptyResponse() {
    val response: MutableHttpResponse<ByteArray> = HttpResponse.ok<ByteArray>().contentLength(
      0,
    )
    val (shouldParse, content) = reader.consume(response)
    assertNotNull(shouldParse, "should not get null for `shouldParse` from reader consume")
    assertNotNull(content, "should not get null for `content` from reader consume")
    assertFalse(shouldParse, "should not parse non-HTML response")
    val bytecontent = content.array()
    assertFalse(bytecontent.isNotEmpty(), "byte array of response content should be empty")
  }

  @Test fun testReadNonOkResponse() {
    val response: MutableHttpResponse<ByteArray> = HttpResponse.accepted()
    val (shouldParse, content) = reader.consume(response)
    assertNotNull(shouldParse, "should not get null for `shouldParse` from reader consume")
    assertNotNull(content, "should not get null for `content` from reader consume")
    assertFalse(shouldParse, "should not parse non-HTML response")
    val bytecontent = content.array()
    assertFalse(bytecontent.isNotEmpty(), "byte array of response content should be empty")
  }

  @Test fun testReadInertResponse() {
    val testResponse = "hello i am some non-html response"
    val response = htmlResponse(content = testResponse, contentType = "text/plain")
    val (shouldParse, content) = reader.consume(response)
    assertNotNull(shouldParse, "should not get null for `shouldParse` from reader consume")
    assertNotNull(content, "should not get null for `content` from reader consume")
    assertFalse(shouldParse, "should not parse non-HTML response")
    val bytecontent = content.array()
    assertTrue(bytecontent.isNotEmpty(), "byte array of response content should not be empty")
    assertEquals(
      testResponse.toByteArray(StandardCharsets.UTF_8).size,
      bytecontent.size,
      "bytesize returned from response should match expected value",
    )
    assertEquals(
      testResponse,
      String(bytecontent, StandardCharsets.UTF_8),
      "string response content should be extracted properly",
    )
  }

  @Test fun testParseNonOkForbidden() {
    val response = htmlResponse(status = HttpStatus.ACCEPTED)
    val (shouldParse, content) = reader.consume(response)
    assertFalse(shouldParse, "non-HTTP-OK response status should not be parsed")
    assertTrue(content.array().isEmpty(), "response content should be empty for empty response")

    assertThrows<IllegalArgumentException> {
      reader.parse(baseServerRequest, response, content)
    }
  }

  @Test fun testParseMustHaveBody() {
    val response = htmlResponse(content = "")
    val (shouldParse, content) = reader.consume(response)
    assertFalse(shouldParse, "empty body should not be parsed")
    assertTrue(content.array().isEmpty(), "response content should be empty for empty response")

    assertThrows<IllegalArgumentException> {
      reader.parse(baseServerRequest, response, content)
    }
  }

  @Test fun testParseMustHaveHTMLContentType() {
    val response = htmlResponse(contentType = "text/plain")
    val (shouldParse, content) = reader.consume(response)
    assertFalse(shouldParse, "non-HTML body should not be parsed")
    assertFalse(content.array().isEmpty(), "response content should not be empty for non-empty response")

    assertThrows<IllegalArgumentException> {
      reader.parse(baseServerRequest, response, content)
    }
  }

  @Test fun testHtmlParseErrorSwallowed() {
    val response = htmlResponse(content = ".")
    val (shouldParse, content) = reader.consume(response)
    assertTrue(shouldParse, "HTML body should be parsed")
    assertFalse(content.array().isEmpty(), "response content should not be empty for non-empty response")

    val result = assertDoesNotThrow {
      reader.parse(baseServerRequest, response, content)
    }
    assertNotNull(result, "should not get `null` result from `parse` even for parse failure")
    assertTrue(result.isEmpty(), "should get empty result from `parse` even for parse failure")
  }

  @Test fun testNonUTF8ErrorSwallowed() {
    val response = htmlResponse(content = "\u0048\u0065\u006C\u006C\u006F World")
    val (shouldParse, content) = reader.consume(response)
    assertTrue(shouldParse, "HTML body should be parsed")
    assertFalse(content.array().isEmpty(), "response content should not be empty for non-empty response")

    val result = assertDoesNotThrow {
      reader.parse(baseServerRequest, response, content)
    }
    assertNotNull(result, "should not get `null` result from `parse` even for parse failure")
    assertTrue(result.isEmpty(), "should get empty result from `parse` even for parse failure")
  }

  @Test fun testScriptsDetected() {
    val response = htmlResponse(
      content = buildString {
      append("<html><head>")
      append("<script src=\"https://example.com/script.js\"></script>")
      append("<script src=\"/script2.js\"></script>")
      append("</head><body></body></html>")
    },
    )
    val (shouldParse, content) = reader.consume(response)
    assertTrue(shouldParse, "HTML body should be parsed")
    assertFalse(content.array().isEmpty(), "response content should not be empty for non-empty response")
    val result = assertDoesNotThrow {
      reader.parse(baseServerRequest, response, content)
    }
    assertNotNull(result, "should not get `null` result from `parse`")
    assertFalse(result.isEmpty(), "should not get empty result from `parse` for HTML response with links")
    assertEquals(2, result.size, "should detect 2 scripts in example response")
    val first = result.first()
    val second = result[1]
    assertEquals(
      StaticContentReader.ArtifactType.SCRIPT,
      first.type,
      "detected type of script resource should be `SCRIPT`",
    )
    assertEquals(
      "https://example.com/script.js",
      first.url.toString(),
      "should properly extract first example script URL",
    )
    assertEquals(
      StaticContentReader.ArtifactType.SCRIPT,
      second.type,
      "detected type of script resource should be `SCRIPT`",
    )
    assertEquals(
      "https://localhost:8443/script2.js",
      second.url.toString(),
      "should properly extract second example script URL",
    )
  }

  @Test fun testStylesheetsDetected() {
    val response = htmlResponse(
      content = buildString {
      append("<html><head>")
      append("<link rel=\"stylesheet\" href=\"https://example.com/style.css\" />")
      append("<link rel=\"inert\" href=\"/style2.css\" />")
      append("<link rel=\"stylesheet\" href=\"/style3.css\" />")
      append("</head><body></body></html>")
    },
    )
    val (shouldParse, content) = reader.consume(response)
    assertTrue(shouldParse, "HTML body should be parsed")
    assertFalse(content.array().isEmpty(), "response content should not be empty for non-empty response")
    val result = assertDoesNotThrow {
      reader.parse(baseServerRequest, response, content)
    }
    assertNotNull(result, "should not get `null` result from `parse`")
    assertFalse(result.isEmpty(), "should not get empty result from `parse` for HTML response with links")
    assertEquals(2, result.size, "should detect 2 scripts in example response")
    val first = result.first()
    val second = result[1]
    assertEquals(
      StaticContentReader.ArtifactType.STYLE,
      first.type,
      "detected type of stylesheet resource should be `STYLE`",
    )
    assertEquals(
      "https://example.com/style.css",
      first.url.toString(),
      "should properly extract first example stylesheet URL",
    )
    assertEquals(
      StaticContentReader.ArtifactType.STYLE,
      second.type,
      "detected type of stylesheet resource should be `STYLE`",
    )
    assertEquals(
      "https://localhost:8443/style3.css",
      second.url.toString(),
      "should properly extract second example stylesheet URL",
    )
  }

  @Test fun testImagesDetected() {
    val response = htmlResponse(
      content = buildString {
      append("<html><head></head><body>")
      append("<img src=\"https://example.com/hello.gif\" />")
      append("<img src=\"/hello2.gif\" />")
      append("</body></html>")
    },
    )
    val (shouldParse, content) = reader.consume(response)
    assertTrue(shouldParse, "HTML body should be parsed")
    assertFalse(content.array().isEmpty(), "response content should not be empty for non-empty response")
    val result = assertDoesNotThrow {
      reader.parse(baseServerRequest, response, content)
    }
    assertNotNull(result, "should not get `null` result from `parse`")
    assertFalse(result.isEmpty(), "should not get empty result from `parse` for HTML response with links")
    assertEquals(2, result.size, "should detect 2 scripts in example response")
    val first = result.first()
    val second = result[1]
    assertEquals(
      StaticContentReader.ArtifactType.IMAGE,
      first.type,
      "detected type of image resource should be `IMAGE`",
    )
    assertEquals(
      "https://example.com/hello.gif",
      first.url.toString(),
      "should properly extract first example image URL",
    )
    assertEquals(
      StaticContentReader.ArtifactType.IMAGE,
      second.type,
      "detected type of image resource should be `IMAGE`",
    )
    assertEquals(
      "https://localhost:8443/hello2.gif",
      second.url.toString(),
      "should properly extract second example image URL",
    )
  }

  @Test fun testSkipIneligibleTags() {
    val response = htmlResponse(
      content = buildString {
      append("<html><head>")
      append("<link rel=\"stylesheet\" href=\"\" />")
      append("<link rel=\"stylesheet\" />")
      append("<link rel=\"inert\" href=\"style.css\" />")
      append("<link rel=\"inert\" href=\"________\" />")
      append("<link rel=\"stylesheet\" href=\".\" />")
      append("<blah rel=\"stylesheet\" href=\"style.css\"></blah>")
      append("<blah href=\"style.css\" />")
      append("<script not-a-src=\"script.js\"></script>")
      append("<script src=\"\"></script>")
      append("<script></script>")
      append("</head><body>")
      append("<img not-a-src=\"script.js\" />")
      append("<img src=\"\" />")
      append("<img />")
      append("<something src=\"script.js\" />")
      append("</body></html>")
    },
    )
    val (shouldParse, content) = reader.consume(response)
    assertTrue(shouldParse, "HTML body should be parsed")
    assertFalse(content.array().isEmpty(), "response content should not be empty for non-empty response")
    val result = assertDoesNotThrow {
      reader.parse(baseServerRequest, response, content)
    }
    assertTrue(
      result.isEmpty(),
      "result list should be empty for all-inert artifacts, but found size(${result.size}): $result",
    )
  }

  @Test fun testUrlRelativity() {
    val response = htmlResponse(
      content = buildString {
      append("<html><head>")
      append("<link rel=\"stylesheet\" href=\"$baseServerOrigin/style.css\"></script>")
      append("<link rel=\"stylesheet\" href=\"://example.com/style.css\"></script>")
      append("<link rel=\"stylesheet\" href=\"/style.css\"></script>")
      append("<link rel=\"stylesheet\" href=\"style.css\"></script>")
      append("<link rel=\"stylesheet\" href=\"neat/style.css\"></script>")
      append("<link rel=\"\" href=\"style.css\" />")
      append("<link rel=\"inert\" href=\"neat/style.css\"></script>")
      append("<link rel=\"inert\" href=\"neat/several/nested/style.css\"></script>")
      append("<link rel=\"inert\" href=\"/neat/several/nested/style.css\"></script>")
      append("<script src=\"$baseServerOrigin/script.js\"></script>")
      append("<script src=\"://example.com/script.js\"></script>")
      append("<script src=\"/script.js\"></script>")
      append("<script src=\"script.js\"></script>")
      append("<script src=\"neat/script.js\"></script>")
      append("<script src=\"neat/several/nested/script.js\"></script>")
      append("<script src=\"/neat/several/nested/script.js\"></script>")
      append("</head><body>")
      append("<img src=\"$baseServerOrigin/hello.gif\">")
      append("<img src=\"://example.com/hello.gif\">")
      append("<img src=\"/hello.gif\">")
      append("<img src=\"hello.gif\">")
      append("<img src=\"neat/hello.gif\">")
      append("<img src=\"neat/several/nested/hello.gif\">")
      append("<img src=\"/neat/several/nested/hello.gif\">")
      append("</body></html>")
    },
    )
    val (shouldParse, content) = reader.consume(response)
    assertTrue(shouldParse, "HTML body should be parsed")
    assertFalse(content.array().isEmpty(), "response content should not be empty for non-empty response")
    val result = assertDoesNotThrow {
      reader.parse(baseServerRequest, response, content)
    }

    assertEquals(
      20,
      result.size,
      "expected exact count of detected artifacts",
    )

    // test generic values
    for (detected in result) {
      assertTrue(
        detected.url.toString().startsWith("https://"),
        "detected stylesheet URL should be absolute",
      )

      if (detected.url.host == "example.com") {
        assertEquals(
          "example.com",
          detected.url.host,
          "host should be expected value",
        )
      } else {
        assertEquals(
          "localhost",
          detected.url.host,
          "host should be expected value",
        )
      }

      when (detected.type) {
        StaticContentReader.ArtifactType.STYLE -> assertTrue(
          detected.url.toString().endsWith(".css"),
          "CSS should only be detected for CSS URLs",
        )

        StaticContentReader.ArtifactType.SCRIPT -> assertTrue(
          detected.url.toString().endsWith(".js"),
          "JS should only be detected for JS URLs",
        )

        StaticContentReader.ArtifactType.IMAGE -> assertTrue(
          detected.url.toString().endsWith(".gif"),
          "images should only be detected for image URLs",
        )

        else -> continue
      }
    }
  }
}
