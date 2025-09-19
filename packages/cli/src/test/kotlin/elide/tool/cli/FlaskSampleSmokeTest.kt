/*
 * Flask CLI template smoke test: init template, run server, hit endpoints.
 */
package elide.tool.cli

import org.junit.jupiter.api.Test
import java.net.URI
import java.net.ServerSocket
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlaskSampleSmokeTest : AbstractEntryTest() {
  @Test fun init_and_run_flask_template() {
    // Create a temporary project directory
    val tmpRoot: Path = Files.createTempDirectory("elide-flask-sample-")

    // 1) Initialize the Flask sample project (no install/build/test to keep it fast)
    assertToolExitsWithCode(
      0,
      "init",
      "flask",
      tmpRoot.absolutePathString(),
      "--yes",
      "--install=false",
      "--build=false",
      "--test=false",
    )

    // Disable native library loading for this test; we don't need sqlite/netty native accel
    System.setProperty("elide.disableNatives", "true")

    // 2) Pick a free port and start the server in a background daemon thread via CLI
    val port = ServerSocket(0).use { it.localPort }
    // Force the Elide HTTP server to bind to the selected port via a system property
    System.setProperty("elide.server.port", port.toString())
    val runArgs = arrayOf(
      "serve",
      "src/app.py",
      "--verbose",
      "--port",
      port.toString(),
      "-p",
      tmpRoot.absolutePathString(),
    )

    val runnerError = java.util.concurrent.atomic.AtomicReference<Throwable?>(null)
    val runner = Thread({
      try {
        Elide.exec(runArgs)
      } catch (t: Throwable) {
        runnerError.set(t)
        throw t
      }
    }, "elide-flask-sample-runner").apply {
      isDaemon = true
      uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e -> runnerError.set(e) }
      start()
    }

    // 3) Probe server readiness and verify endpoints
    val client = HttpClient.newHttpClient()
    val base = URI.create("http://localhost:$port")

    fun get(path: String): HttpResponse<String> = client.send(
      HttpRequest.newBuilder(base.resolve(path)).GET().build(),
      HttpResponse.BodyHandlers.ofString(),
    )

    fun postJson(path: String, json: String): HttpResponse<String> = client.send(
      HttpRequest.newBuilder(base.resolve(path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build(),
      HttpResponse.BodyHandlers.ofString(),
    )

    // Wait up to ~30s for server
    var ok = false
    val deadline = System.currentTimeMillis() + 30_000
    while (System.currentTimeMillis() < deadline) {
      try {
        val resp = get("/ping")
        if (resp.statusCode() == 200 && resp.body().trim() == "pong") {
          ok = true
          break
        }
      } catch (_: Exception) {
        // not up yet
      }
      Thread.sleep(200)
    }
    val err = runnerError.get()
    assertTrue(ok, "server did not become ready on /ping in time" + (err?.let { "\nRunner error: ${it.stackTraceToString()}" } ?: ""))

    val echo = postJson("/echo", "{\"hello\":\"world\"}")
    assertEquals(200, echo.statusCode())
    assertTrue(echo.body().contains("\"hello\":"))

    // 4) Stop the server thread if it's still alive (daemon thread will not block JVM exit)
    if (runner.isAlive) runner.interrupt()
  }
}

