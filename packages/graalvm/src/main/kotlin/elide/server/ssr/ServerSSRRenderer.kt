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
   * Render the attached [script] and return the resulting content as a regular [String].
   *
   * @return String render result from [script].
   */
  fun renderInline(): String?  {
    // acquire script runtime, execute the script, decode as string
    return JsRuntime.acquire().executeBlocking(
      script,
      String::class.java
    )
  }

  /**
   * Render the attached [script] with suspension support, and return the resulting content as a regular [String].
   *
   * @return String render result from [script].
   */
  suspend fun renderSuspend(): String? {
    return JsRuntime.acquire().executeAsync(
      script,
      String::class.java,
    ).await()
  }

  /**
   * Render the attached [script] and return the resulting content as a [ByteArrayOutputStream], built from the result
   * of [renderInline].
   *
   * @return Byte stream of resulting content.
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
   * Render the attached [script] into a [ByteArrayOutputStream], and wrap it in a Micronaut [MutableHttpResponse]
   * provided at [response].
   *
   * @param response Base mutable response to fill body data for.
   * @return Mutable [response] with body data filled in from the execution result of [script].
   */
  fun renderResponse(
    response: MutableHttpResponse<ByteArrayOutputStream>,
  ): MutableHttpResponse<ByteArrayOutputStream> {
    return response.body(
      render()
    )
  }
}
