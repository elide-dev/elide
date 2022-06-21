package elide.server.ssr

import elide.runtime.graalvm.JsRuntime
import elide.server.SuspensionRenderer
import io.micronaut.http.MutableHttpResponse
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets


/** Renderer class which executes JavaScript via SSR and provides the resulting response to Micronaut. */
class ServerSSRRenderer constructor (private val script: JsRuntime.ExecutableScript): ServerRenderer {
  /**
   *
   */
  fun renderInline(): String?  {
    // acquire script runtime, execute the script, decode as string
    return JsRuntime.acquire().execute(
      script,
      String::class.java
    )
  }

  /**
   *
   */
  override fun render(): ByteArrayOutputStream {
    val byteStream = ByteArrayOutputStream()
    byteStream.bufferedWriter(StandardCharsets.UTF_8).use {
      it.write(
        renderInline() ?:
        throw IllegalStateException("Failed to render JavaScript content from bundle '${script.getId()}'")
      )
    }
    return byteStream
  }

  /**
   *
   */
  fun renderResponse(
    response: MutableHttpResponse<ByteArrayOutputStream>,
  ): MutableHttpResponse<ByteArrayOutputStream> {
    return response.body(
      render()
    )
  }
}
