/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */

package elide.runtime.plugins.python.flask

import org.graalvm.polyglot.Source
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.intrinsics.server.http.HttpServerAgent
import elide.runtime.plugins.python.Python
import elide.runtime.plugins.js.JavaScript

@OptIn(DelicateElideApi::class)
class FlaskShimIntegrationTest {
  @Test fun testFlaskShim_ping_and_echo() {
    // Choose an uncommon test port to avoid collisions.
    val port = 18081

    // Build a small Flask-like app using the shim and configure the server.
    val appCode = """
      |from elide_flask import Flask, request, Response
      |
      |app = Flask(__name__)
      |
      |@app.route("/ping", methods=["GET"]) 
      |def ping():
      |  return "pong"
      |
      |@app.route("/echo", methods=["POST"]) 
      |def echo():
      |  data = request.get_json() or {}
      |  return Response({"echo": data}, status=200)
      |
      |# Configure HTTP server (Netty) via Elide intrinsics
      |config = Elide.http.server.config
      |config.port = ${port}
      |config.autoStart = True
      |""".trimMargin()

    // Create engine with Python and Flask plugin enabled.
    val engine = PolyglotEngine {
      // Install JS so the Elide intrinsic can be resolved from Python via polyglot.eval("js", "Elide")
      configure(JavaScript)
      configure(Python)
      configure(FlaskPlugin) { /* defaults */ }
    }

    val server = HttpServerAgent()
    val execProvider = GuestExecutorProvider { GuestExecution.direct() }

    // Pre-seed JS global Elide into Polyglot bindings to ensure Python can resolve it.
    val ctx = engine.acquire()
    ctx.evaluate(Source.newBuilder("js", "Polyglot.export('Elide', Elide)", "seed_elide.js").buildLiteral())

    // Start the server by evaluating the entrypoint; autoStart triggers listen.
    server.run(
      Source.newBuilder("python", appCode, "flask_app.py").buildLiteral(),
      execProvider
    ) { engine.acquire() }

    // Probe until server is reachable.
    val client = HttpClient.newHttpClient()
    val base = URI.create("http://localhost:${port}")

    fun get(path: String): HttpResponse<String> = client.send(
      HttpRequest.newBuilder(base.resolve(path)).GET().build(),
      HttpResponse.BodyHandlers.ofString()
    )
    fun postJson(path: String, body: String): HttpResponse<String> = client.send(
      HttpRequest.newBuilder(base.resolve(path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build(),
      HttpResponse.BodyHandlers.ofString()
    )

    // Wait until /ping responds or timeout ~5s.
    var ok = false
    repeat(50) {
      try {
        val resp = get("/ping")
        if (resp.statusCode() == 200 && resp.body() == "pong") {
          ok = true
          return@repeat
        }
      } catch (_: Exception) {
        // not up yet
      }
      Thread.sleep(100)
    }
    assertTrue(ok, "server should answer /ping with 200 'pong'")

    // Verify /echo JSON roundtrip
    val echo = postJson("/echo", """{"a":1}""")
    assertEquals(200, echo.statusCode())
    val body = assertNotNull(echo.body())
    assertTrue(body.contains("\"echo\""), "response should contain echo wrapper: $body")
    assertTrue(body.contains("\"a\": 1"), "response should echo submitted JSON: $body")
  }
}

