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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import elide.annotations.Inject
import elide.runtime.node.http.NodeHttpModule
import elide.testing.annotations.TestCase
import elide.runtime.plugins.js.javascript
import java.net.ServerSocket
import java.net.HttpURLConnection
import java.net.URL

/** Tests for Elide's implementation of the Node `http` built-in module. */
@TestCase internal class NodeHttpTest : NodeModuleConformanceTest<NodeHttpModule>() {
  override val moduleName: String get() = "http"
  override fun provide(): NodeHttpModule = NodeHttpModule()
  @Inject lateinit var http: NodeHttpModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Agent")
    yield("ClientRequest")
    yield("Server")
    yield("ServerResponse")
    yield("IncomingMessage")
    yield("OutgoingMessage")
    yield("METHODS")
    yield("STATUS_CODES")
    yield("createServer")
    yield("get")
    yield("globalAgent")
    yield("maxHeaderSize")
    yield("request")
    yield("validateHeaderName")
    yield("validateHeaderValue")
    yield("setMaxIdleHTTPParsers")
  }

  @Test override fun testInjectable() {
    assertNotNull(http)
  }

  private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

  @Test fun `http - createServer exposes listen and close`(): Unit = run {
    val out = polyglotContext.javascript(
      // language=js
      """
        import http from "http";
        const server = http.createServer((req, res) => {});
        export const listen = (...args) => server.listen(...args);
        export const close = (...args) => server.close(...args);
      """.trimIndent(),
      esm = true,
    )
    assertTrue(out.hasMembers())
    val keys = out.memberKeys.toSet()
    assertTrue(keys.contains("listen"))
    assertTrue(keys.contains("close"))
  }

  @Test fun `http - server responds to basic request`(): Unit = run {
    val port = findFreePort()
    val out = polyglotContext.javascript(
      // language=js
      """
        import http from "http";
        const server = http.createServer((req, res) => {
          res.statusCode = 200;
          res.end("ok");
        });
        await new Promise(resolve => server.listen($port, "127.0.0.1", resolve));
        export const ready = true;
        export const close = () => new Promise(r => server.close(() => r(true)));
      """.trimIndent(),
      esm = true,
    )
    assertTrue(out.hasMembers())
    assertTrue(out.getMember("ready").asBoolean())

    // issue a request from the host and assert the response
    val url = java.net.URI.create("http://127.0.0.1:$port/").toURL()
    val conn = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 2000
      readTimeout = 2000
    }
    conn.inputStream.bufferedReader().use { reader ->
      val body = reader.readText()
      assertEquals(200, conn.responseCode)
      assertEquals("ok", body)
    }

    // try to close the server (ignore result)
    runCatching { out.getMember("close").execute() }
  }
}
