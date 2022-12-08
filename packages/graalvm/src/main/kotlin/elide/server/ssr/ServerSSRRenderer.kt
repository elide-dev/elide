@file:Suppress("WildcardImport")

package elide.server.ssr

import elide.annotations.core.Polyglot
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.graalvm.JsRuntime
import elide.runtime.ssr.ServerResponse
import elide.server.Application
import elide.server.ServerInitializer
import elide.server.SuspensionRenderer
import elide.server.annotations.Eager
import elide.server.controller.ElideController
import elide.server.controller.PageWithProps
import elide.server.type.RequestState
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import kotlinx.coroutines.*
import kotlinx.html.BODY
import kotlinx.html.script
import kotlinx.html.stream.appendHTML
import kotlinx.html.unsafe
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

/** Renderer class which executes JavaScript via SSR and provides the resulting response to Micronaut. */
@Suppress("MemberVisibilityCanBePrivate", "unused", "SpreadOperator")
public class ServerSSRRenderer constructor(
  private val body: BODY,
  private val handler: ElideController,
  private val request: HttpRequest<*>,
  private val script: JsRuntime.ExecutableScript,
  private val buffer: StringBuilder = StringBuilder(),
  private var job: AtomicReference<Job?> = AtomicReference(null),
) : SuspensionRenderer<ByteArrayOutputStream> {
  public companion object {
    /** ID in the DOM where SSR data is affixed, if present. */
    public const val ssrId: String = "ssr-data"
  }

  /** SSR service initializer. */
  @Context @Eager public class SSRInitializer : ServerInitializer {
    override fun initialize() {
      Application.Initialization.initializeOnWarmup {
        val maybeEmbedded = JsRuntime.Script.embedded()
        if (maybeEmbedded.valid()) {
          JsRuntime.acquire().apply {
            prewarmScript(maybeEmbedded)
          }
        }
      }
    }
  }

  // Logger.
  private val logging: Logger = Logging.of(ServerSSRRenderer::class)

  /** Execute the provided operation with any prepared SSR execution context. */
  internal suspend fun prepareContext(op: suspend (JsRuntime.ExecutionInputs<*>) -> StringBuilder): String {
    return if (handler is PageWithProps<*>) {
      // build context
      val state = RequestState(
        request,
        null,
      )
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
      op.invoke(JsRuntime.ExecutionInputs.EMPTY as JsRuntime.ExecutionInputs<*>).toString()
    }
  }

  // Handle a chunk which is ready to serve back to the invoking agent.
  @Polyglot private fun chunkReady(chunk: ServerResponse) {
    if (chunk.hasContent) {
      buffer.append(chunk.content)
    }
    if (chunk.fin) {
      job.get()?.cancel()
    }
  }

  /**
   * Render the attached [script] with suspension support, and return the resulting content as a regular [String].
   *
   * @return String render result from [script].
   */
  public suspend fun renderSuspendAsync(streamed: Boolean = false): Deferred<String> = coroutineScope {
    return@coroutineScope async {
      prepareContext { ctx ->
        val js = JsRuntime.acquire()

        if (!streamed) {
          val renderedContent = js.executeAsync(
            script,
            String::class.java,
            *ctx.buildArguments(),
          )

          // then apply rendered content
          buffer.apply {
            append(renderedContent.await())
          }
        } else {
          buffer.apply {
            logging.trace("Starting SSR execution")
            val op = js.executeStreaming(
              script,
              *ctx.buildArguments(),
              receiver = ::chunkReady
            )
            job.set(op)
            op.join()
          }
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
