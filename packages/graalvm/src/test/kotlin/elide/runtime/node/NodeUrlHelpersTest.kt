/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.

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
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import elide.testing.annotations.TestCase

/** Targeted tests for Node `url` helpers. */
@TestCase internal class NodeUrlHelpersTest : GenericJsModuleTest<elide.runtime.node.url.NodeURLModule>() {
  override val moduleName: String get() = "url"
  override fun provide(): elide.runtime.node.url.NodeURLModule = elide.runtime.node.url.NodeURLModule()
  override fun expectCompliance(): Boolean = false

  @Test fun `domainToASCII basic`() {
    val v = require("url")
    val fn = v.getMember("domainToASCII")
    val ascii1 = fn.execute("example.com").asString()
    val ascii2 = fn.execute("mañana.com").asString()
    assertEquals("example.com", ascii1)
    // Allow either modern or legacy mapping so long as it's punycoded
    assertTrue(ascii2.startsWith("xn--"), "expected punycode output, got: $ascii2")
  }

  @Test fun `domainToUnicode basic`() {
    val v = require("url")
    val fn = v.getMember("domainToUnicode")
    val uni = fn.execute("xn--maana-pta.com").asString()
    assertTrue(uni.contains("maña".substring(0,3)), "expected unicode decoded domain, got: $uni")
  }

  @Test fun `fileURLToPath and pathToFileURL roundtrip`() {
    val v = require("url")
    val fileURLToPath = v.getMember("fileURLToPath")
    val pathToFileURL = v.getMember("pathToFileURL")

    val url = "file:///tmp/test/dir/file.txt"
    val path = fileURLToPath.execute(url).asString()
    // Should yield a non-empty local filesystem path
    assertTrue(path.isNotBlank(), "expected non-empty path")
    // Roundtrip should yield a file URL and preserve filename
    val back = pathToFileURL.execute(path)
    val href = back.getMember("href").asString()
    assertTrue(href.startsWith("file:///"), "expected file URL, got: $href")
    assertTrue(href.lowercase().contains("file.txt"), "expected filename preserved, got: $href")
  }

  @Test fun `urlToHttpOptions mapping`() {
    val v = require("url")
    val fn = v.getMember("urlToHttpOptions")
    val obj = fn.execute("https://user:pass@example.com:8080/path/name?q=1#frag")
    val host = obj.getMember("host").asString()
    val hostname = obj.getMember("hostname").asString()
    val port = obj.getMember("port").asString()
    val protocol = obj.getMember("protocol").asString()
    val path = obj.getMember("path").asString()
    // Basic expectations
    assertEquals("example.com:8080", host)
    assertEquals("example.com", hostname)
    assertEquals("8080", port)
    assertEquals("https:", protocol)
    assertEquals("/path/name?q=1", path)
  }
}

