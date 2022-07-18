@file:Suppress("WildcardImport")

package elide.server.ssr

import elide.runtime.graalvm.JsRuntime
import elide.server.SuspensionRenderer
import elide.server.controller.ElideController
import elide.server.controller.PageWithProps
import elide.server.type.RequestState
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import kotlinx.coroutines.*
import kotlinx.html.BODY
import kotlinx.html.script
import kotlinx.html.stream.appendHTML
import kotlinx.html.unsafe
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/** Renderer class which executes JavaScript via SSR and provides the resulting response to Micronaut. */
@Suppress("MemberVisibilityCanBePrivate", "unused", "SpreadOperator")
public class ServerSSRRenderer constructor(
  private val body: BODY,
  private val handler: ElideController,
  private val request: HttpRequest<*>,
  private val script: JsRuntime.ExecutableScript,
) : SuspensionRenderer<ByteArrayOutputStream> {
  public companion object {
    /** ID in the DOM where SSR data is affixed, if present. */
    public const val ssrId: String = "ssr-data"
  }

  /** Execute the provided operation with any prepared SSR execution context. */
  @Suppress("UNCHECKED_CAST")
  internal suspend fun prepareContext(op: suspend (JsRuntime.ExecutionInputs<*>) -> StringBuilder): String {
    return if (handler is PageWithProps<*>) {
      // build context
      val state = RequestState(request, null)
      val (props, serialized) = handler.finalizeAsync(state).await()
      val buf = op.invoke(
        JsRuntime.ExecutionInputs.fromRequestState(
          state,
          props,
        )
      )
      if (props != null && serialized != null) {
        val subBuffer = StringBuilder()
        subBuffer.appendHTML().script(type = "application/json") {
          attributes["id"] = ssrId
          unsafe {
            +serialized
          }
        }
        buf.append(subBuffer)
      }
      buf.toString()
    } else {
      @Suppress("UNCHECKED_CAST")
      op.invoke(JsRuntime.ExecutionInputs.EMPTY as JsRuntime.ExecutionInputs<*>).toString()
    }
  }

  /**
   * Render the attached [script] with suspension support, and return the resulting content as a regular [String].
   *
   * @return String render result from [script].
   */
  public suspend fun renderSuspendAsync(): Deferred<String> = coroutineScope {
    return@coroutineScope async {
      prepareContext { ctx ->
        val builder = StringBuilder()
        val renderedContent = JsRuntime.acquire().executeAsync(
          script,
          String::class.java,
          *ctx.buildArguments(),
        )

        // then apply rendered content
        builder.apply {
          append(renderedContent.await())
        }
      }
    }
  }

  /**
   * Render the attached [script] and return the resulting content as a [ByteArrayOutputStream], built from the result
   * of [renderSuspendAsync].
   *
   * @return Byte stream of resulting content.
   */
  override suspend fun render(): ByteArrayOutputStream {
    val byteStream = ByteArrayOutputStream()
    byteStream.bufferedWriter(StandardCharsets.UTF_8).use {
      it.write(
        renderSuspendAsync().await()
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
  public suspend fun renderResponse(
    response: MutableHttpResponse<ByteArrayOutputStream>,
  ): MutableHttpResponse<ByteArrayOutputStream> {
    return response.body(
      render()
    )
  }
}
